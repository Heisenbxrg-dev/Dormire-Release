import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { verifyAssertion } from "./crypto.mjs";
import { startServer } from "./server.mjs";

test("badge server e2e issuance, redemption, verification and revocation", async (t) => {
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "orpheon-badge-test-"));
  const envPath = path.join(tmpDir, ".env");
  const dataDir = path.join(tmpDir, "data");
  const logFile = path.join(tmpDir, "logs", "server.log");

  const handle = await startServer({ envPath, dataDir, logFile, host: "127.0.0.1", port: 0 });
  t.after(async () => {
    await handle.close();
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  assert.equal(fs.existsSync(envPath), true);

  const healthRes = await fetch(`${handle.url}/health`);
  assert.equal(healthRes.status, 200);
  const health = await healthRes.json();
  assert.equal(health.ok, true);
  assert.equal(health.publicKey, handle.config.publicKeyB64);

  const unauthRes = await fetch(`${handle.url}/api/v1/admin/users`);
  assert.equal(unauthRes.status, 401);

  const adminHeaders = {
    Authorization: `Bearer ${handle.config.adminToken}`,
    "Content-Type": "application/json",
  };

  const grantRes = await fetch(`${handle.url}/api/v1/admin/grant`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({
      userId: "user_alpha",
      perks: true,
      badge: { id: "vip", label: "VIP", color: "#FFD700" },
    }),
  });
  assert.equal(grantRes.status, 200);
  const granted = await grantRes.json();
  assert.equal(granted.userId, "user_alpha");
  assert.equal(granted.perks, true);
  assert.equal(granted.badges.length, 1);
  assert.equal(verifyAssertion(granted, health.publicKey), true);

  const fetchRes = await fetch(`${handle.url}/api/v1/badges/user_alpha`);
  assert.equal(fetchRes.status, 200);
  const fetched = await fetchRes.json();
  assert.equal(verifyAssertion(fetched, health.publicKey), true);
  assert.equal(fetched.badges[0].label, "VIP");

  const tampered = { ...fetched, perks: false };
  assert.equal(verifyAssertion(tampered, health.publicKey), false);

  const codeRes = await fetch(`${handle.url}/api/v1/admin/codes`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({
      badge: { id: "early", label: "Early", color: "#509BF5" },
      perks: true,
      maxUses: 1,
    }),
  });
  assert.equal(codeRes.status, 201);
  const createdCode = await codeRes.json();
  assert.match(createdCode.code, /^ORPH-[A-Z0-9]{4}-[A-Z0-9]{4}$/);

  const redeemOk = await fetch(`${handle.url}/api/v1/badges/redeem`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_beta", code: createdCode.code.toLowerCase() }),
  });
  assert.equal(redeemOk.status, 200);
  const redeemed = await redeemOk.json();
  assert.equal(verifyAssertion(redeemed, health.publicKey), true);
  assert.equal(redeemed.badges[0].id, "early");

  const redeemAgain = await fetch(`${handle.url}/api/v1/badges/redeem`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_gamma", code: createdCode.code }),
  });
  assert.equal(redeemAgain.status, 409);

  const redeemInvalid = await fetch(`${handle.url}/api/v1/badges/redeem`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId: "user_gamma", code: "NOPE-0000" }),
  });
  assert.equal(redeemInvalid.status, 404);

  const revokeRes = await fetch(`${handle.url}/api/v1/admin/revoke`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({ userId: "user_alpha" }),
  });
  assert.equal(revokeRes.status, 200);
  const revoked = await revokeRes.json();
  assert.equal(revoked.perks, false);
  assert.equal(revoked.badges.length, 0);
  assert.equal(verifyAssertion(revoked, health.publicKey), true);
});
