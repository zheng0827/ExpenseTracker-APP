import assert from 'node:assert/strict';
import { createSyncServer } from './server.js';

const TOKEN = process.env.SYNC_TOKEN || 'oneledger-dev-token';
const server = createSyncServer();

await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const { port } = server.address();
const baseUrl = `http://127.0.0.1:${port}`;

try {
  // 1. Health check
  const health = await fetch(`${baseUrl}/health`).then((r) => r.json());
  assert.equal(health.ok, true);

  // 2. Unauthorized rejected
  const unauth = await fetch(`${baseUrl}/v1/sync`, { method: 'POST' });
  assert.equal(unauth.status, 401);

  // 3. Device A pushes a transaction + an isolated debt record
  const now = Date.now();
  const pushA = await fetch(`${baseUrl}/v1/sync`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${TOKEN}`,
    },
    body: JSON.stringify({
      deviceId: 'galaxy-s25',
      since: 0,
      changes: {
        transactions: [
          {
            id: 'tx-1001',
            type: 'expense',
            description: '阿美自助餐，漢堡、蛋餅',
            amount: 180,
            category: '餐飲',
            date: '2026-11-18',
            updatedAt: now,
          },
        ],
        debts: [
          {
            id: 'debt-2001',
            direction: 'lent',
            person: '阿明',
            amount: 650,
            settled: false,
            updatedAt: now,
          },
        ],
      },
    }),
  }).then((r) => r.json());

  assert.equal(pushA.ok, true);
  assert.equal(pushA.appliedCount, 2);

  // 4. Device B pulls incremental changes since 0 and receives both tables
  const pullB = await fetch(`${baseUrl}/v1/sync`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${TOKEN}`,
    },
    body: JSON.stringify({
      deviceId: 'galaxy-tab',
      since: 0,
      changes: {},
    }),
  }).then((r) => r.json());

  assert.ok(pullB.pull.transactions.some((t) => t.id === 'tx-1001' && t.description === '阿美自助餐，漢堡、蛋餅'));
  assert.ok(pullB.pull.debts.some((d) => d.id === 'debt-2001' && d.person === '阿明'));

  console.log('PASS: Multi-device SQLite Sync Server tests passed!');
} finally {
  server.close();
}
