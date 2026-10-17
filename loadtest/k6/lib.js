// Shared helpers for the TripNest k6 scripts.
import http from 'k6/http';

export const BASE = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
export const PASSWORD = 'LoadTest123!';

export const json = (body) => JSON.stringify(body);

// RFC 4122-ish v4 id, good enough for Idempotency-Key headers.
export function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export function headers(token, extra = {}) {
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}`, ...extra };
}

// Access tokens live 15 minutes; each VU re-logs in every 10 minutes.
// Login runs BCrypt, so keep it rare: it is deliberately not part of the hot path.
const session = { token: null, at: 0, email: null };
export function tokenFor(user) {
  if (session.email !== user.email || !session.token || Date.now() - session.at > 10 * 60 * 1000) {
    const res = http.post(`${BASE}/api/auth/login`, json({ email: user.email, password: PASSWORD }), {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'POST /auth/login' },
    });
    if (res.status !== 200) return null;
    session.token = res.json('accessToken');
    session.at = Date.now();
    session.email = user.email;
  }
  return session.token;
}
