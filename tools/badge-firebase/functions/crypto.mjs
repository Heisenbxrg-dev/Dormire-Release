import crypto from "node:crypto";

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

export function timingSafeTokenEqual(provided, expected) {
  if (typeof provided !== "string" || typeof expected !== "string") return false;
  const a = crypto.createHash("sha256").update(provided, "utf8").digest();
  const b = crypto.createHash("sha256").update(expected, "utf8").digest();
  return crypto.timingSafeEqual(a, b);
}
