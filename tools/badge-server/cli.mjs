import path from "node:path";
import { fileURLToPath } from "node:url";
import { ensureEnv } from "./crypto.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ENV_PATH = path.join(HERE, ".env");

function parseArgs(argv) {
  const positionals = [];
  const flags = {};
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--no-perks") {
      flags.noPerks = true;
    } else if (arg.startsWith("--")) {
      const eq = arg.indexOf("=");
      if (eq > 2) {
        flags[arg.slice(2, eq)] = arg.slice(eq + 1);
      } else if (i + 1 < argv.length) {
        flags[arg.slice(2)] = argv[i + 1];
        i += 1;
      }
    } else {
      positionals.push(arg);
    }
  }
  return { positionals, flags };
}

async function api(cfg, method, route, body) {
  const res = await fetch(`${cfg.serverUrl}${route}`, {
    method,
    headers: {
      Authorization: `Bearer ${cfg.adminToken}`,
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(5000),
  });
  const text = await res.text();
  let json = {};
  try {
    json = JSON.parse(text);
  } catch {
    json = { raw: text };
  }
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${json.error || text}`);
  }
  return json;
}

async function main() {
  const { positionals, flags } = parseArgs(process.argv.slice(2));
  const cmd = positionals[0];
  const cfg = ensureEnv(ENV_PATH);

  if (!cmd || cmd === "help") {
    process.stdout.write(
      [
        "Usage:",
        "  node cli.mjs init",
        "  node cli.mjs grant <userId> [label] [color] [--id <id>] [--no-perks]",
        "  node cli.mjs revoke <userId> [badgeId]",
        "  node cli.mjs code-create [label] [color] [--id <id>] [--uses <n>] [--code <custom>] [--hours <h>] [--no-perks]",
        "  node cli.mjs list",
        "",
      ].join("\n"),
    );
    return;
  }

  if (cmd === "init") {
    process.stdout.write(`.env ready\nPublic key: ${cfg.publicKeyB64}\n`);
    return;
  }

  if (cmd === "grant") {
    const userId = positionals[1];
    if (!userId) throw new Error("userId is required");
    const label = positionals[2] || "Supporter";
    const color = positionals[3] || "#1ED760";
    const id = flags.id || label.toLowerCase().replace(/[^a-z0-9_-]/g, "") || "supporter";
    const result = await api(cfg, "POST", "/api/v1/admin/grant", {
      userId,
      perks: !flags.noPerks,
      badge: { id, label, color },
    });
    process.stdout.write(
      `Granted user=${result.userId} perks=${result.perks} badges=${result.badges.map((b) => `${b.id}:${b.label}(${b.color})`).join(",")}\n`,
    );
    return;
  }

  if (cmd === "revoke") {
    const userId = positionals[1];
    if (!userId) throw new Error("userId is required");
    const badgeId = positionals[2] || flags.id || "*";
    const result = await api(cfg, "POST", "/api/v1/admin/revoke", { userId, badgeId });
    process.stdout.write(
      `Revoked user=${result.userId} remaining=${result.badges.length} perks=${result.perks}\n`,
    );
    return;
  }

  if (cmd === "code-create") {
    const label = positionals[1] || "Supporter";
    const color = positionals[2] || "#1ED760";
    const id = flags.id || label.toLowerCase().replace(/[^a-z0-9_-]/g, "") || "supporter";
    const maxUses = flags.uses ? Number.parseInt(flags.uses, 10) : 1;
    const expiresInHours = flags.hours ? Number(flags.hours) : null;
    const result = await api(cfg, "POST", "/api/v1/admin/codes", {
      code: flags.code || "",
      badge: { id, label, color },
      perks: !flags.noPerks,
      maxUses,
      expiresInHours,
    });
    process.stdout.write(
      `Code: ${result.code} (badge=${result.badge.label} ${result.badge.color}, perks=${result.perks}, uses=${result.usesLeft})\n`,
    );
    return;
  }

  if (cmd === "list") {
    const data = await api(cfg, "GET", "/api/v1/admin/users");
    const userKeys = Object.keys(data.users || {});
    const codeKeys = Object.keys(data.codes || {});
    process.stdout.write(`Users (${userKeys.length}):\n`);
    for (const u of userKeys) {
      const entry = data.users[u];
      const badges = (entry.badges || []).map((b) => `${b.id}:${b.label}(${b.color})`).join(", ");
      process.stdout.write(`  - ${u}: perks=${Boolean(entry.perks)} [${badges}]\n`);
    }
    process.stdout.write(`Codes (${codeKeys.length}):\n`);
    for (const c of codeKeys) {
      const entry = data.codes[c];
      process.stdout.write(
        `  - ${c}: ${entry.badge?.label || ""} (${entry.badge?.color || ""}), usesLeft=${entry.usesLeft}/${entry.maxUses}, perks=${Boolean(entry.perks)}\n`,
      );
    }
    return;
  }

  throw new Error(`Unknown command: ${cmd}`);
}

main().catch((err) => {
  process.stderr.write(`${err?.message || err}\n`);
  process.exitCode = 1;
});
