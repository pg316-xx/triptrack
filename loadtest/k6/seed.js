// Creates test data and writes it to users.json for api.js / ws.js.
//
//   k6 run -e BASE_URL=http://10.0.1.23:8080 -e GROUPS=100 seed.js
//
// Each group = 5 users sharing one trip (1 owner + 4 invited members), with
// 2 itinerary days, a few activities and a couple of expenses, so the read
// endpoints return realistic payloads. GROUPS=100 -> 500 users.
import http from 'k6/http';
import { check, fail } from 'k6';
import { BASE, PASSWORD, json, uuid } from './lib.js';

const GROUPS = Number(__ENV.GROUPS || 100);
const RUN = __ENV.RUN_ID || `${Date.now()}`;

export const options = {
  setupTimeout: '20m',
  scenarios: { seed: { executor: 'shared-iterations', vus: 1, iterations: 1 } },
};

const H = { 'Content-Type': 'application/json' };
const auth = (t) => ({ ...H, Authorization: `Bearer ${t}` });

export function setup() {
  const users = [];
  for (let g = 0; g < GROUPS; g++) {
    // register 5 users in parallel
    const regs = http.batch(
      Array.from({ length: 5 }, (_, i) => ['POST', `${BASE}/api/auth/register`,
        json({ name: `Load User ${g}-${i}`, email: `lt-${RUN}-${g}-${i}@load.test`, password: PASSWORD }), { headers: H }]),
    );
    if (!regs.every((r) => r.status < 300)) fail(`register failed in group ${g}: ${regs.map((r) => r.status)}`);
    const tokens = regs.map((r) => r.json('accessToken'));
    const ids = http.batch(tokens.map((t) => ['GET', `${BASE}/api/users/me`, null, { headers: auth(t) }])).map((r) => r.json('id'));

    const trip = http.post(`${BASE}/api/trips`, json({
      title: `Load trip ${g}`, description: 'k6 seed', destination: 'Goa, India', startDate: '2026-12-10', endDate: '2026-12-14',
    }), { headers: auth(tokens[0]) });
    check(trip, { 'trip created': (r) => r.status < 300 }) || fail(`trip failed: ${trip.status} ${trip.body}`);
    const tripId = trip.json('id');

    const inv = http.post(`${BASE}/api/trips/${tripId}/invitations`, json({ role: 'MEMBER', usageLimit: 10 }), { headers: auth(tokens[0]) });
    const token = inv.json('inviteLink').split('token=')[1];
    http.batch(tokens.slice(1).map((t) => ['POST', `${BASE}/api/invitations/accept?token=${token}`, null, { headers: auth(t) }]));

    let dayId = null;
    for (const [n, date] of [[1, '2026-12-10'], [2, '2026-12-11']]) {
      const day = http.post(`${BASE}/api/trips/${tripId}/days`, json({ dayNumber: n, date, title: `Day ${n}`, notes: '' }), { headers: auth(tokens[0]) });
      dayId = dayId || day.json('id');
      for (let a = 0; a < 3; a++) {
        http.post(`${BASE}/api/trips/${tripId}/days/${day.json('id')}/activities`, json({
          title: `Stop ${a}`, location: 'Baga', startTime: `${date}T0${4 + a}:00:00Z`, estimatedCost: 300,
        }), { headers: auth(tokens[0]) });
      }
    }
    for (let e = 0; e < 2; e++) {
      http.post(`${BASE}/api/trips/${tripId}/expenses`, json({
        title: `Seed expense ${e}`, category: 'FOOD', amount: 1000 + e * 250, participantIds: ids,
      }), { headers: { ...auth(tokens[e]), 'Idempotency-Key': uuid() } });
    }

    ids.forEach((id, i) => users.push({ email: `lt-${RUN}-${g}-${i}@load.test`, userId: id, tripId, dayId, memberIds: ids }));
    if ((g + 1) % 10 === 0) console.log(`seeded ${g + 1}/${GROUPS} groups`);
  }
  return { users };
}

export default function () {}

// Write the seeded accounts next to the scripts (k6 runs this after setup).
export function handleSummary(data) {
  if (!data.setup_data || !data.setup_data.users) {
    return { stdout: 'seed produced no data\n' };
  }
  return {
    'users.json': JSON.stringify(data.setup_data),
    stdout: `\nWrote users.json with ${data.setup_data.users.length} users in ${GROUPS} trips\n`,
  };
}
