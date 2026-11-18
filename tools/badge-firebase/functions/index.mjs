import crypto from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";
import { onRequest } from "firebase-functions/v2/https";
import {
  normalizeBadge,
  sanitizeField,
  signAssertion,
  timingSafeTokenEqual,
} from "./crypto.mjs";

initializeApp();
const db = getFirestore();

const ADMIN_TOKEN = defineSecret("BADGE_ADMIN_TOKEN");
const PRIVATE_KEY_B64 = defineSecret("BADGE_PRIVATE_KEY_B64");
const PUBLIC_KEY_B64 = defineSecret("BADGE_PUBLIC_KEY_B64");
const TTL_SECONDS = 604800;

const USERS_COL = "badges_users";
const CODES_COL = "badges_codes";

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

function mergeBadge(badges, badge) {
  const next = Array.isArray(badges) ? badges.filter((b) => b.id !== badge.id) : [];
  next.push(badge);
  return next;
}

function sendJson(res, status, body) {
  res.set("Cache-Control", "no-store");
  res.status(status).json(body);
}

function parseBody(req) {
  if (!req.body) return {};
  if (typeof req.body === "object" && !Array.isArray(req.body)) return req.body;
  if (typeof req.body === "string") {
    const parsed = JSON.parse(req.body);
    if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return parsed;
  }
  throw Object.assign(new Error("invalid_json"), { status: 400 });
}

export const api = onRequest(
  {
    region: "europe-west1",
    secrets: [ADMIN_TOKEN, PRIVATE_KEY_B64, PUBLIC_KEY_B64],
  },
  async (req, res) => {
    try {
      const pathname = req.path || "/";
      const method = req.method || "GET";
      const publicKeyB64 = PUBLIC_KEY_B64.value().trim();
      const privateKeyB64 = PRIVATE_KEY_B64.value().trim();
      const adminToken = ADMIN_TOKEN.value().trim();

      const signFor = (userId, entry) =>
        signAssertion({
          userId,
          perks: Boolean(entry?.perks),
          badges: entry?.badges || [],
          ttlSeconds: TTL_SECONDS,
          privateKeyB64,
          publicKeyB64,
        });

      const isAuthorized = () => {
        const auth = req.headers.authorization || "";
        if (!auth.startsWith("Bearer ")) return false;
        return timingSafeTokenEqual(auth.slice(7).trim(), adminToken);
      };

      if (method === "GET" && pathname === "/health") {
        sendJson(res, 200, { ok: true, publicKey: publicKeyB64 });
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
        const snap = await db.collection(USERS_COL).doc(userId).get();
        sendJson(res, 200, signFor(userId, snap.exists ? snap.data() : null));
        return;
      }

      if (method === "POST" && pathname === "/api/v1/badges/redeem") {
        const body = parseBody(req);
        const userId = sanitizeField(body.userId, 128);
        const code = sanitizeField(body.code, 64).toUpperCase();
        if (!userId || !code) {
          sendJson(res, 400, { error: "missing_fields" });
          return;
        }

        const outcome = await db.runTransaction(async (tx) => {
          const codeRef = db.collection(CODES_COL).doc(code);
          const userRef = db.collection(USERS_COL).doc(userId);
          const codeSnap = await tx.get(codeRef);
          if (!codeSnap.exists) return { status: 404, body: { error: "invalid_code" } };

          const entry = codeSnap.data();
          if (entry.expiresAt && Date.now() > entry.expiresAt) {
            return { status: 410, body: { error: "code_expired" } };
          }

          const redeemedBy = Array.isArray(entry.redeemedBy) ? [...entry.redeemedBy] : [];
          let usesLeft = Number(entry.usesLeft ?? 0);
          if (!redeemedBy.includes(userId)) {
            if (usesLeft <= 0) {
              return { status: 409, body: { error: "code_exhausted" } };
            }
            usesLeft -= 1;
            redeemedBy.push(userId);
            tx.update(codeRef, { usesLeft, redeemedBy });
          }

          const userSnap = await tx.get(userRef);
          const prev = userSnap.exists ? userSnap.data() : { perks: false, badges: [] };
          const nextUser = {
            perks: Boolean(prev.perks || entry.perks),
            badges: entry.badge
              ? mergeBadge(prev.badges, normalizeBadge(entry.badge, entry.badge.id || "supporter"))
              : Array.isArray(prev.badges) ? prev.badges : [],
          };
          tx.set(userRef, nextUser);
          return { status: 200, body: signFor(userId, nextUser) };
        });

        sendJson(res, outcome.status, outcome.body);
        return;
      }

      if (pathname.startsWith("/api/v1/admin/")) {
        if (!isAuthorized()) {
          sendJson(res, 401, { error: "unauthorized" });
          return;
        }

        if (method === "GET" && pathname === "/api/v1/admin/users") {
          const [usersSnap, codesSnap] = await Promise.all([
            db.collection(USERS_COL).get(),
            db.collection(CODES_COL).get(),
          ]);
          const users = {};
          for (const doc of usersSnap.docs) users[doc.id] = doc.data();
          const codes = {};
          for (const doc of codesSnap.docs) codes[doc.id] = doc.data();
          sendJson(res, 200, { users, codes });
          return;
        }

        if (method === "POST" && pathname === "/api/v1/admin/grant") {
          const body = parseBody(req);
          const userId = sanitizeField(body.userId, 128);
          if (!userId) {
            sendJson(res, 400, { error: "missing_user_id" });
            return;
          }
          const userRef = db.collection(USERS_COL).doc(userId);
          const snap = await userRef.get();
          const prev = snap.exists ? snap.data() : { perks: false, badges: [] };
          const perks = typeof body.perks === "boolean"
            ? body.perks
            : (body.perks === undefined && !prev.perks ? true : Boolean(prev.perks));
          const badges = body.badge
            ? mergeBadge(prev.badges, normalizeBadge(body.badge, body.badge.id || "supporter"))
            : (Array.isArray(prev.badges) ? prev.badges : []);
          const next = { perks, badges };
          await userRef.set(next);
          sendJson(res, 200, signFor(userId, next));
          return;
        }

        if (method === "POST" && pathname === "/api/v1/admin/revoke") {
          const body = parseBody(req);
          const userId = sanitizeField(body.userId, 128);
          const badgeId = sanitizeField(body.badgeId || "", 32).toLowerCase();
          if (!userId) {
            sendJson(res, 400, { error: "missing_user_id" });
            return;
          }
          const userRef = db.collection(USERS_COL).doc(userId);
          if (!badgeId || badgeId === "*") {
            await userRef.delete();
            sendJson(res, 200, signFor(userId, null));
            return;
          }
          const snap = await userRef.get();
          if (!snap.exists) {
            sendJson(res, 200, signFor(userId, null));
            return;
          }
          const prev = snap.data();
          const badges = (Array.isArray(prev.badges) ? prev.badges : []).filter((b) => b.id !== badgeId);
          if (badges.length === 0 && !prev.perks) {
            await userRef.delete();
            sendJson(res, 200, signFor(userId, null));
            return;
          }
          const next = { perks: Boolean(prev.perks), badges };
          await userRef.set(next);
          sendJson(res, 200, signFor(userId, next));
          return;
        }

        if (method === "POST" && pathname === "/api/v1/admin/codes") {
          const body = parseBody(req);
          let code = sanitizeField(body.code || "", 64).toUpperCase().replace(/[^A-Z0-9_-]/g, "");
          if (!code) {
            for (let i = 0; i < 8; i += 1) {
              const candidate = generateCode();
              const exists = await db.collection(CODES_COL).doc(candidate).get();
              if (!exists.exists) {
                code = candidate;
                break;
              }
            }
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
          await db.collection(CODES_COL).doc(code).set(entry);
          sendJson(res, 201, { code, ...entry });
          return;
        }
      }

      sendJson(res, 404, { error: "not_found" });
    } catch (err) {
      const status = err?.status || 500;
      const code = status === 500 ? "internal_error" : err.message;
      sendJson(res, status, { error: code });
    }
  },
);
