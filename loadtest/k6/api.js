// HTTP load test: virtual users browse their trip and occasionally write.
//
//   k6 run -e BASE_URL=http://10.0.1.23:8080 -e PROFILE=load api.js
//
// PROFILE:
//   smoke   10 users for 1 minute (does everything work?)
//   load    ramp to TARGET_VUS (default 300 ~= the 100K-registered-user peak), hold 10 min
//   stress  step up to MAX_VUS (default 1500) to find where it breaks
//
// One VU ~= one person actively using the app: ~10 requests then 1-4 s of
// "reading" between screens, i.e. roughly 2 requests/second per VU.
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter } from 'k6/metrics';
import { BASE, headers, json, tokenFor, uuid } from './lib.js';

const users = new SharedArray('users', () => JSON.parse(open('./users.json')).users);
const PROFILE = __ENV.PROFILE || 'smoke';
const TARGET = Number(__ENV.TARGET_VUS || 300);
const MAX = Number(__ENV.MAX_VUS || 1500);

const writes = new Counter('writes');

const PROFILES = {
  smoke: [{ duration: '1m', target: 10 }],
  load: [
    { duration: '5m', target: TARGET },
    { duration: '10m', target: TARGET },
    { duration: '1m', target: 0 },
  ],
  stress: [
    { duration: '2m', target: Math.round(MAX * 0.2) },
    { duration: '3m', target: Math.round(MAX * 0.4) },
    { duration: '3m', target: Math.round(MAX * 0.6) },
    { duration: '3m', target: Math.round(MAX * 0.8) },
    { duration: '3m', target: MAX },
    { duration: '1m', target: 0 },
  ],
};

export const options = {
  scenarios: {
    users: { executor: 'ramping-vus', startVUs: 0, stages: PROFILES[PROFILE], gracefulRampDown: '30s' },
  },
  // Pass/fail targets. In stress mode these just report; the point is to find the knee.
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false }],
    'http_req_duration{kind:read}': ['p(95)<300'],
    'http_req_duration{kind:write}': ['p(95)<800'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const get = (path, token, name) =>
  http.get(`${BASE}${path}`, { headers: headers(token), tags: { name, kind: 'read' } });
const post = (path, token, body, name, extra) =>
  http.post(`${BASE}${path}`, json(body), { headers: headers(token, extra), tags: { name, kind: 'write' } });

export default function () {
  const u = users[(__VU - 1) % users.length];
  const token = tokenFor(u);
  if (!token) { sleep(1); return; }
  const T = `/api/trips/${u.tripId}`;

  group('open app', () => {
    check(get('/api/trips', token, 'GET /trips'), { 'trips 200': (r) => r.status === 200 });
    get('/api/notifications/unread-count', token, 'GET /notifications/unread-count');
  });
  sleep(1 + Math.random() * 2);

  group('open trip', () => {
    check(get(T, token, 'GET /trips/{id}'), { 'trip 200': (r) => r.status === 200 });
    get(`${T}/members`, token, 'GET /trips/{id}/members');
    check(get(`${T}/days/itinerary`, token, 'GET /itinerary'), { 'itinerary 200': (r) => r.status === 200 });
  });
  sleep(1 + Math.random() * 3);

  group('expenses tab', () => {
    check(get(`${T}/expenses?page=0&size=20`, token, 'GET /expenses'), { 'expenses 200': (r) => r.status === 200 });
    get(`${T}/expenses/balances`, token, 'GET /balances');
    get(`${T}/expenses/settlements/suggestions`, token, 'GET /settlements/suggestions');
  });
  sleep(1 + Math.random() * 2);

  group('chat tab', () => {
    get(`${T}/messages?page=0&size=30`, token, 'GET /messages');
  });

  // ~30% of sessions write something, like real users do.
  const roll = Math.random();
  if (roll < 0.12) {
    const r = post(`${T}/expenses`, token, {
      title: 'Load test lunch', category: 'FOOD', amount: Math.round(100 + Math.random() * 900), participantIds: u.memberIds,
    }, 'POST /expenses', { 'Idempotency-Key': uuid() });
    check(r, { 'expense 201': (x) => x.status === 201 });
    writes.add(1);
  } else if (roll < 0.22) {
    const r = post(`${T}/messages`, token, { message: `hello from VU ${__VU}` }, 'POST /messages');
    check(r, { 'message ok': (x) => x.status < 300 });
    writes.add(1);
  } else if (roll < 0.30 && u.dayId && u.email.endsWith('-0@load.test')) {
    // itinerary edits are owner/admin only; user 0 of each seeded group owns the trip
    const r = post(`${T}/days/${u.dayId}/activities`, token, {
      title: 'Load test stop', location: 'Calangute', startTime: '2026-12-10T08:00:00Z', estimatedCost: 100,
    }, 'POST /activities');
    if (r.status === 201) {
      const a = r.json();
      http.patch(`${BASE}${T}/days/activities/${a.id}/move`, json({ afterActivityId: null, version: a.version }),
        { headers: headers(token), tags: { name: 'PATCH /activities/{id}/move', kind: 'write' } });
      http.del(`${BASE}${T}/days/activities/${a.id}`, null,
        { headers: headers(token), tags: { name: 'DELETE /activities/{id}', kind: 'write' } });
      writes.add(3);
    }
  }
  sleep(1 + Math.random() * 3);
}
