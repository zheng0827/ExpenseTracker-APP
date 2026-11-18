// 簡記輕量多裝置雲端同步伺服器（零外部相依，使用 Node 內建 node:sqlite）
// 支援：獨立 ledger / debts / recurring / credit_cards 四張表之增量 LWW 雙向同步
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { DatabaseSync } from 'node:sqlite';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.PORT || 8787);
const SYNC_TOKEN = process.env.SYNC_TOKEN || 'oneledger-dev-token';
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, 'data');

fs.mkdirSync(DATA_DIR, { recursive: true });
const db = new DatabaseSync(path.join(DATA_DIR, 'cloud_sync.sqlite'));

db.exec(`
  PRAGMA journal_mode = WAL;

  CREATE TABLE IF NOT EXISTS sync_records (
    entity_type TEXT NOT NULL,
    id TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    deleted_at INTEGER,
    device_id TEXT,
    PRIMARY KEY (entity_type, id)
  );

  CREATE INDEX IF NOT EXISTS idx_sync_updated
    ON sync_records (entity_type, updated_at);
`);

const ALLOWED_TYPES = new Set(['transactions', 'debts', 'recurring', 'credit_cards']);

const upsertStmt = db.prepare(`
  INSERT INTO sync_records (entity_type, id, payload_json, updated_at, deleted_at, device_id)
  VALUES (?, ?, ?, ?, ?, ?)
  ON CONFLICT(entity_type, id) DO UPDATE SET
    payload_json = excluded.payload_json,
    updated_at = excluded.updated_at,
    deleted_at = excluded.deleted_at,
    device_id = excluded.device_id
  WHERE excluded.updated_at >= sync_records.updated_at
`);

const pullStmt = db.prepare(`
  SELECT id, payload_json, updated_at, deleted_at
  FROM sync_records
  WHERE entity_type = ? AND updated_at > ?
  ORDER BY updated_at ASC
`);

function sendJson(res, status, data) {
  const body = JSON.stringify(data);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
  });
  res.end(body);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let total = 0;
    req.on('data', (chunk) => {
      total += chunk.length;
      if (total > 15 * 1024 * 1024) {
        reject(new Error('Payload too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      try {
        const raw = Buffer.concat(chunks).toString('utf-8');
        resolve(raw ? JSON.parse(raw) : {});
      } catch (err) {
        reject(err);
      }
    });
    req.on('error', reject);
  });
}

function checkAuth(req) {
  const auth = req.headers.authorization || '';
  return auth === `Bearer ${SYNC_TOKEN}`;
}

export function createSyncServer() {
  return http.createServer(async (req, res) => {
    const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);

    if (req.method === 'GET' && url.pathname === '/health') {
      return sendJson(res, 200, { ok: true, engine: 'node:sqlite', timestamp: Date.now() });
    }

    if (!checkAuth(req)) {
      return sendJson(res, 401, { error: '未授權的同步存取，請檢查 Bearer Token' });
    }

    // 雙向增量同步端點：POST /v1/sync
    // Body: { deviceId, since, changes: { transactions: [...], debts: [...], recurring: [...], credit_cards: [...] } }
    if (req.method === 'POST' && url.pathname === '/v1/sync') {
      try {
        const body = await readJsonBody(req);
        const deviceId = String(body.deviceId || 'unknown');
        const since = Number(body.since || 0);
        const changes = body.changes || {};
        const serverNow = Date.now();

        let applied = 0;
        db.exec('BEGIN');
        try {
          for (const [entityType, items] of Object.entries(changes)) {
            if (!ALLOWED_TYPES.has(entityType) || !Array.isArray(items)) continue;
            for (const item of items) {
              if (!item || !item.id) continue;
              const updatedAt = Number(item.updatedAt || serverNow);
              const deletedAt = item.deletedAt ? Number(item.deletedAt) : null;
              upsertStmt.run(
                entityType,
                String(item.id),
                JSON.stringify(item),
                updatedAt,
                deletedAt,
                deviceId
              );
              applied++;
            }
          }
          db.exec('COMMIT');
        } catch (txErr) {
          db.exec('ROLLBACK');
          throw txErr;
        }

        const pull = {};
        for (const entityType of ALLOWED_TYPES) {
          const rows = pullStmt.all(entityType, since);
          pull[entityType] = rows.map((r) => JSON.parse(r.payload_json));
        }

        return sendJson(res, 200, {
          ok: true,
          serverTime: serverNow,
          appliedCount: applied,
          pull,
        });
      } catch (err) {
        return sendJson(res, 400, { error: err.message || '同步失敗' });
      }
    }

    return sendJson(res, 404, { error: 'Not Found' });
  });
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const server = createSyncServer();
  server.listen(PORT, () => {
    console.log(`OneLedger SQLite Sync Server running on http://0.0.0.0:${PORT}`);
  });
}
