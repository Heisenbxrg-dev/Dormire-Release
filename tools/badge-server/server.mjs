import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import {
  ensureEnv,
  normalizeBadge,
  sanitizeField,
  signAssertion,
  timingSafeTokenEqual,
} from "./crypto.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const MAX_BODY_BYTES = 16384;

function generateCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = crypto.randomBytes(8);
  let out = "ORPH-";
  for (let i = 0; i < 8; i += 1) {
    if (i === 4) out += "-";
    out += alphabet[bytes[i] % alphabet.length];
  }
  return out;
}

function loadStore(storePath) {
  if (!fs.existsSync(storePath)) return { users: {}, codes: {} };
  try {
    const parsed = JSON.parse(fs.readFileSync(storePath, "utf8"));
    return {
      users: parsed && typeof parsed.users === "object" && parsed.users ? parsed.users : {},
      codes: parsed && typeof parsed.codes === "object" && parsed.codes ? parsed.codes : {},
    };
  } catch {
    return { users: {}, codes: {} };
  }
}

function saveStore(storePath, store) {
  fs.mkdirSync(path.dirname(storePath), { recursive: true });
  const tmp = `${storePath}.tmp.${process.pid}.${Date.now()}`;
  fs.writeFileSync(tmp, JSON.stringify(store, null, 2), "utf8");
  fs.renameSync(tmp, storePath);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let total = 0;
    req.on("data", (chunk) => {
      total += chunk.length;
      if (total > MAX_BODY_BYTES) {
        reject(Object.assign(new Error("payload_too_large"), { status: 413 }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => {
      if (chunks.length === 0) {
        resolve({});
        return;
      }
      try {
        const parsed = JSON.parse(Buffer.concat(chunks).toString("utf8"));
        if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
          reject(Object.assign(new Error("invalid_json"), { status: 400 }));
          return;
        }
        resolve(parsed);
      } catch {
        reject(Object.assign(new Error("invalid_json"), { status: 400 }));
      }
    });
    req.on("error", (err) => reject(err));
  });
}

function sendJson(res, status, body) {
  const raw = JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(raw, "utf8"),
    "Cache-Control": "no-store",
  });
  res.end(raw);
}

function mergeBadge(user, badge) {
  const next = Array.isArray(user.badges) ? user.badges.filter((b) => b.id !== badge.id) : [];
  next.push(badge);
  user.badges = next;
}

export async function startServer(opts = {}) {
  const envPath = opts.envPath || path.join(HERE, ".env");
  const dataDir = opts.dataDir || path.join(HERE, "data");
  const logFile = opts.logFile || path.join(HERE, "logs", "badge-server.log");
  const storePath = path.join(dataDir, "store.json");

  const cfg = ensureEnv(envPath);
  const host = opts.host ?? cfg.host;
  const port = opts.port ?? cfg.port;
  const store = loadStore(storePath);

  fs.mkdirSync(path.dirname(logFile), { recursive: true });
  const writeLog = (msg) => {
    try {
      fs.appendFileSync(logFile, `${new Date().toISOString()} ${msg}\n`, "utf8");
    } catch {
    }
  };

  const signFor = (userId) => {
    const entry = store.users[userId];
    return signAssertion({
      userId,
      perks: Boolean(entry?.perks),
      badges: entry?.badges || [],
      ttlSeconds: cfg.ttlSeconds,
      privateKeyB64: cfg.privateKeyB64,
      publicKeyB64: cfg.publicKeyB64,
    });
  };

  const isAuthorized = (req) => {
    const auth = req.headers.authorization || "";
    if (!auth.startsWith("Bearer ")) return false;
    const token = auth.slice(7).trim();
    return timingSafeTokenEqual(token, cfg.adminToken);
  };

  const server = http.createServer(async (req, res) => {
    try {
      const parsedUrl = new URL(req.url || "/", "http://127.0.0.1");
      const pathname = parsedUrl.pathname;
      const method = req.method || "GET";

      if (method === "GET" && pathname === "/health") {
        sendJson(res, 200, { ok: true, publicKey: cfg.publicKeyB64 });
        return;
      }

      if (method === "GET" && pathname.startsWith("/api/v1/badges/")) {
        const rawUserId = pathname.slice("/api/v1/badges/".length);
        let userId = "";
        try {
          userId = sanitizeField(decodeURIComponent(rawUserId), 128);
        } catch {
          sendJson(res, 400, { error: "invalid_user_id" });
          return;
        }
        if (!userId || userId.includes("/")) {
          sendJson(res, 400, { error: "invalid_user_id" });
          return;
        }
        sendJson(res, 200, signFor(userId));
        return;
      }

      if (method === "POST" && pathname === "/api/v1/badges/redeem") {
        const body = await readJsonBody(req);
        const userId = sanitizeField(body.userId, 128);
        const code = sanitizeField(body.code, 64).toUpperCase();
        if (!userId || !code) {
          sendJson(res, 400, { error: "missing_fields" });
          return;
        }
        const entry = store.codes[code];
        if (!entry) {
          sendJson(res, 404, { error: "invalid_code" });
          return;
        }
        if (entry.expiresAt && Date.now() > entry.expiresAt) {
          sendJson(res, 410, { error: "code_expired" });
          return;
        }
        const redeemedBy = Array.isArray(entry.redeemedBy) ? entry.redeemedBy : (entry.redeemedBy = []);
        if (!redeemedBy.includes(userId)) {
          if (entry.usesLeft <= 0) {
            sendJson(res, 409, { error: "code_exhausted" });
            return;
          }
          entry.usesLeft -= 1;
          redeemedBy.push(userId);
        }
        const user = store.users[userId] || (store.users[userId] = { perks: false, badges: [] });
        user.perks = Boolean(user.perks || entry.perks);
        if (entry.badge) {
          mergeBadge(user, normalizeBadge(entry.badge, entry.badge.id || "supporter"));
        }
        saveStore(storePath, store);
        writeLog(`redeem user=${userId} code=${code} usesLeft=${entry.usesLeft}`);
        sendJson(res, 200, signFor(userId));
        return;
      }

      if (pathname.startsWith("/api/v1/admin/")) {
        if (!isAuthorized(req)) {
          sendJson(res, 401, { error: "unauthorized" });
          return;
        }

        if (method === "GET" && pathname === "/api/v1/admin/users") {
          sendJson(res, 200, { users: store.users, codes: store.codes });
          return;
        }

        if (method === "POST" && pathname === "/api/v1/admin/grant") {
          const body = await readJsonBody(req);
          const userId = sanitizeField(body.userId, 128);
          if (!userId) {
            sendJson(res, 400, { error: "missing_user_id" });
            return;
          }
          const user = store.users[userId] || (store.users[userId] = { perks: false, badges: [] });
          if (typeof body.perks === "boolean") {
            user.perks = body.perks;
          } else if (body.perks === undefined && !user.perks) {
            user.perks = true;
          }
          if (body.badge) {
            mergeBadge(user, normalizeBadge(body.badge, body.badge.id || "supporter"));
          }
          saveStore(storePath, store);
          writeLog(`grant user=${userId} perks=${user.perks} badges=${user.badges.length}`);
          sendJson(res, 200, signFor(userId));
          return;
        }

        if (method === "POST" && pathname === "/api/v1/admin/revoke") {
          const body = await readJsonBody(req);
          const userId = sanitizeField(body.userId, 128);
          const badgeId = sanitizeField(body.badgeId || "", 32).toLowerCase();
          if (!userId) {
            sendJson(res, 400, { error: "missing_user_id" });
            return;
          }
          if (!badgeId || badgeId === "*") {
            delete store.users[userId];
          } else if (store.users[userId]) {
            store.users[userId].badges = (store.users[userId].badges || []).filter((b) => b.id !== badgeId);
            if (store.users[userId].badges.length === 0 && !store.users[userId].perks) {
              delete store.users[userId];
            }
          }
          saveStore(storePath, store);
          writeLog(`revoke user=${userId} badgeId=${badgeId || "*"}`);
          sendJson(res, 200, signFor(userId));
          return;
        }

        if (method === "POST" && pathname === "/api/v1/admin/codes") {
          const body = await readJsonBody(req);
          let code = sanitizeField(body.code || "", 64).toUpperCase().replace(/[^A-Z0-9_-]/g, "");
          if (!code) {
            do {
              code = generateCode();
            } while (store.codes[code]);
          }
          const badge = normalizeBadge(body.badge || {}, body.badge?.id || "supporter");
          const perks = body.perks !== false;
          const maxUsesNum = Number.parseInt(String(body.maxUses ?? 1), 10);
          const maxUses = Number.isFinite(maxUsesNum) && maxUsesNum > 0 ? maxUsesNum : 1;
          const hoursNum = body.expiresInHours == null ? null : Number(body.expiresInHours);
          const expiresAt = hoursNum != null && Number.isFinite(hoursNum) && hoursNum > 0
            ? Date.now() + Math.round(hoursNum * 3600_000)
            : null;
          const entry = {
            badge,
            perks,
            usesLeft: maxUses,
            maxUses,
            expiresAt,
            redeemedBy: [],
          };
          store.codes[code] = entry;
          saveStore(storePath, store);
          writeLog(`code-create code=${code} maxUses=${maxUses} perks=${perks}`);
          sendJson(res, 201, { code, ...entry });
          return;
        }
      }

      sendJson(res, 404, { error: "not_found" });
    } catch (err) {
      const status = err?.status || 500;
      const code = status === 500 ? "internal_error" : err.message;
      writeLog(`error status=${status} msg=${err?.message || err}`);
      if (!res.headersSent) {
        sendJson(res, status, { error: code });
      }
    }
  });

  server.requestTimeout = 10_000;
  server.headersTimeout = 10_000;

  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(port, host, () => {
      server.off("error", reject);
      resolve();
    });
  });

  const addr = server.address();
  const actualPort = typeof addr === "object" && addr ? addr.port : port;
  const boundUrl = `http://${host === "0.0.0.0" ? "127.0.0.1" : host}:${actualPort}`;
  writeLog(`listening url=${boundUrl}`);

  return {
    server,
    config: cfg,
    port: actualPort,
    url: boundUrl,
    close: () => new Promise((resolve) => server.close(() => resolve())),
  };
}

if (process.argv[1] && pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url) {
  startServer()
    .then((handle) => {
      process.stdout.write(`Revera badge server listening on ${handle.url}\n`);
      process.stdout.write(`Public key: ${handle.config.publicKeyB64}\n`);
    })
    .catch((err) => {
      process.stderr.write(`Failed to start badge server: ${err?.message || err}\n`);
      process.exitCode = 1;
    });
}
