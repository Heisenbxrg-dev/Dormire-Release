import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

function parseEnvFile(text) {
  const result = {};
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const eq = line.indexOf("=");
    if (eq <= 0) continue;
    const key = line.slice(0, eq).trim();
    let val = line.slice(eq + 1).trim();
    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
      val = val.slice(1, -1);
    }
    result[key] = val;
  }
  return result;
}

function serializeEnv(cfg) {
  return [
    `BADGE_HOST=${cfg.host}`,
    `BADGE_PORT=${cfg.port}`,
    `BADGE_SERVER_URL=${cfg.serverUrl}`,
    `BADGE_TTL_SECONDS=${cfg.ttlSeconds}`,
    `BADGE_ADMIN_TOKEN=${cfg.adminToken}`,
    `BADGE_PRIVATE_KEY_B64=${cfg.privateKeyB64}`,
    `BADGE_PUBLIC_KEY_B64=${cfg.publicKeyB64}`,
    "",
  ].join("\n");
}

export function ensureEnv(envPath) {
  fs.mkdirSync(path.dirname(envPath), { recursive: true });
  const existing = fs.existsSync(envPath) ? parseEnvFile(fs.readFileSync(envPath, "utf8")) : {};
  let dirty = !fs.existsSync(envPath);

  const host = (existing.BADGE_HOST || "0.0.0.0").trim();
  const portNum = Number.parseInt(existing.BADGE_PORT || "8787", 10);
  const port = Number.isFinite(portNum) && portNum >= 0 && portNum <= 65535 ? portNum : 8787;
  const serverUrl = (existing.BADGE_SERVER_URL || `http://127.0.0.1:${port}`).trim().replace(/\/+$/, "");
  const ttlNum = Number.parseInt(existing.BADGE_TTL_SECONDS || "604800", 10);
  const ttlSeconds = Number.isFinite(ttlNum) && ttlNum > 0 ? ttlNum : 604800;

  let adminToken = (existing.BADGE_ADMIN_TOKEN || "").trim();
  if (!adminToken) {
    adminToken = crypto.randomBytes(32).toString("hex");
    dirty = true;
  }

  let privateKeyB64 = (existing.BADGE_PRIVATE_KEY_B64 || "").trim();
  let publicKeyB64 = (existing.BADGE_PUBLIC_KEY_B64 || "").trim();
  if (!privateKeyB64 || !publicKeyB64) {
    const pair = crypto.generateKeyPairSync("ec", {
      namedCurve: "prime256v1",
      publicKeyEncoding: { type: "spki", format: "der" },
      privateKeyEncoding: { type: "pkcs8", format: "der" },
    });
    privateKeyB64 = Buffer.from(pair.privateKey).toString("base64");
    publicKeyB64 = Buffer.from(pair.publicKey).toString("base64");
    dirty = true;
  }

  const cfg = { host, port, serverUrl, ttlSeconds, adminToken, privateKeyB64, publicKeyB64 };
  if (dirty) {
    const tmp = `${envPath}.tmp.${process.pid}.${Date.now()}`;
    fs.writeFileSync(tmp, serializeEnv(cfg), { encoding: "utf8", mode: 0o600 });
    fs.renameSync(tmp, envPath);
  }
  return cfg;
}

export function sanitizeField(str, maxLen = 64) {
  return String(str ?? "")
    .replace(/[\r\n\t]+/g, " ")
    .trim()
    .slice(0, maxLen);
}

export function normalizeBadge(input, fallbackId = "supporter") {
  const id = sanitizeField(input?.id || fallbackId, 32).toLowerCase().replace(/[^a-z0-9_-]/g, "") || fallbackId;
  const label = sanitizeField(input?.label || "Supporter", 32) || "Supporter";
  const rawColor = sanitizeField(input?.color || "#1ED760", 16);
  const color = /^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$/.test(rawColor) ? rawColor.toUpperCase() : "#1ED760";
  return { id, label, color };
}

export function canonicalPayload({ userId, perks, issuedAt, expiresAt, badges }) {
  const list = Array.isArray(badges) ? badges : [];
  return [
    "v1",
    String(userId),
    perks ? "1" : "0",
    String(issuedAt),
    String(expiresAt),
    ...list.map((b) => `${b.id}\t${b.label}\t${b.color}`),
  ].join("\n");
}

export function signAssertion({ userId, perks, badges, ttlSeconds, privateKeyB64, publicKeyB64, now = Date.now() }) {
  const cleanBadges = (Array.isArray(badges) ? badges : []).map((b) => normalizeBadge(b, b?.id || "badge"));
  const cleanPerks = Boolean(perks);
  const issuedAt = Math.trunc(now);
  const expiresAt = issuedAt + Math.trunc(ttlSeconds) * 1000;
  const payload = canonicalPayload({
    userId,
    perks: cleanPerks,
    issuedAt,
    expiresAt,
    badges: cleanBadges,
  });
  const keyObj = crypto.createPrivateKey({
    key: Buffer.from(privateKeyB64, "base64"),
    format: "der",
    type: "pkcs8",
  });
  const signature = crypto.sign("sha256", Buffer.from(payload, "utf8"), keyObj).toString("base64");
  return {
    userId: String(userId),
    perks: cleanPerks,
    badges: cleanBadges,
    issuedAt,
    expiresAt,
    publicKey: publicKeyB64,
    signature,
  };
}

export function verifyAssertion(assertion, expectedPublicKeyB64 = assertion?.publicKey) {
  if (!assertion || typeof assertion !== "object") return false;
  if (!expectedPublicKeyB64 || !assertion.signature || !assertion.userId) return false;
  try {
    const payload = canonicalPayload(assertion);
    const keyObj = crypto.createPublicKey({
      key: Buffer.from(expectedPublicKeyB64, "base64"),
      format: "der",
      type: "spki",
    });
    return crypto.verify(
      "sha256",
      Buffer.from(payload, "utf8"),
      keyObj,
      Buffer.from(assertion.signature, "base64"),
    );
  } catch {
    return false;
  }
}

export function timingSafeTokenEqual(provided, expected) {
  if (typeof provided !== "string" || typeof expected !== "string") return false;
  const a = crypto.createHash("sha256").update(provided, "utf8").digest();
  const b = crypto.createHash("sha256").update(expected, "utf8").digest();
  return crypto.timingSafeEqual(a, b);
}
