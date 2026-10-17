// WebSocket load test: thousands of users keep the app open (STOMP over /ws-trip),
// subscribed to their trip's chat + itinerary topics and their notifications.
// About 1 in 10 also sends a chat message every ~30 s, which fans out to the
// other members of that trip, so you measure delivery as well as idle connections.
//
//   k6 run -e BASE_URL=http://10.0.1.23:8080 -e WS_VUS=5000 -e HOLD_MIN=10 ws.js
//
// Each connection is one VU; budget ~1-2 MB of load-generator RAM per VU.
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE, tokenFor } from './lib.js';

const users = new SharedArray('users', () => JSON.parse(open('./users.json')).users);
const WS_VUS = Number(__ENV.WS_VUS || 500);
const HOLD_MS = Number(__ENV.HOLD_MIN || 5) * 60 * 1000;
const RAMP = __ENV.RAMP || '5m';

const connected = new Rate('stomp_connected');
const denied = new Counter('stomp_errors');
const received = new Counter('stomp_messages_received');
const sent = new Counter('stomp_messages_sent');
const connectTime = new Trend('stomp_connect_ms', true);

export const options = {
  scenarios: {
    sockets: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [{ duration: RAMP, target: WS_VUS }, { duration: `${Math.ceil(HOLD_MS / 60000)}m`, target: WS_VUS }],
      gracefulRampDown: '10s',
      gracefulStop: '30s',
    },
  },
  thresholds: {
    stomp_connected: ['rate>0.99'],
    stomp_connect_ms: ['p(95)<1000'],
  },
};

const NUL = '\u0000';
const frame = (cmd, hdrs, body = '') =>
  `${cmd}\n${Object.entries(hdrs).map(([k, v]) => `${k}:${v}`).join('\n')}\n\n${body}${NUL}`;

export default function () {
  const u = users[(__VU - 1) % users.length];
  const token = tokenFor(u);
  if (!token) { sleep(5); return; }

  const url = `${BASE.replace(/^http/, 'ws')}/ws-trip`;
  const talker = __VU % 10 === 0;
  const started = Date.now();

  const res = ws.connect(url, {}, (socket) => {
    socket.on('open', () => {
      socket.send(frame('CONNECT', { 'accept-version': '1.2', host: 'tripnest', 'heart-beat': '0,0', Authorization: `Bearer ${token}` }));
    });

    socket.on('message', (msg) => {
      if (msg.startsWith('CONNECTED')) {
        connected.add(true);
        connectTime.add(Date.now() - started);
        socket.send(frame('SUBSCRIBE', { id: 'chat', destination: `/topic/trip/${u.tripId}/chat` }));
        socket.send(frame('SUBSCRIBE', { id: 'itin', destination: `/topic/trip/${u.tripId}/itinerary` }));
        socket.send(frame('SUBSCRIBE', { id: 'notif', destination: `/topic/user/${u.userId}/notifications` }));
        if (talker) {
          socket.setInterval(() => {
            socket.send(frame('SEND', { destination: `/app/trip/${u.tripId}/chat`, 'content-type': 'application/json' },
              JSON.stringify({ message: `ws ping from VU ${__VU}` })));
            sent.add(1);
          }, 25000 + Math.random() * 10000);
        }
      } else if (msg.startsWith('MESSAGE')) {
        received.add(1);
      } else if (msg.startsWith('ERROR')) {
        denied.add(1);
        socket.close();
      }
    });

    socket.on('error', () => connected.add(false));
    // keep the connection open for the rest of the test
    socket.setTimeout(() => socket.close(), HOLD_MS + 60000);
  });

  check(res, { 'ws upgraded (101)': (r) => r && r.status === 101 }) || connected.add(false);
  sleep(1);
}
