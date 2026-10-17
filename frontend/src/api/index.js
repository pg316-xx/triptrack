// One function per backend endpoint. Paths mirror the Spring controllers exactly.
import axios from 'axios'
import client from './client'

const d = (p) => p.then((r) => r.data)

export const auth = {
  register: (body) => d(client.post('/api/auth/register', body)), // {name,email,password}
  login: (body) => d(client.post('/api/auth/login', body)), // {email,password}
  google: (idToken) => d(client.post('/api/auth/google-login', { idToken })),
  logout: (refreshToken) => client.post("/api/auth/logout", { refreshToken }),
  forgotPassword: (email) => d(client.post("/api/auth/forgot-password", { email })),
  resetPassword: (token, newPassword) => d(client.post("/api/auth/reset-password", { token, newPassword })),
}

export const users = {
  me: () => d(client.get('/api/users/me')),
  update: (body) => d(client.put('/api/users/me', body)), // {name,profileImageUrl}
  deleteMe: () => client.delete("/api/users/me"),
}

export const trips = {
  // paginated on the server; the dashboard shows up to 100 trips
  list: () => d(client.get("/api/trips", { params: { page: 0, size: 100 } })).then((p) => p.content),
  get: (id) => d(client.get(`/api/trips/${id}`)),
  create: (body) => d(client.post('/api/trips', body)), // {title,description,destination,startDate,endDate}
  update: (id, body) => d(client.patch(`/api/trips/${id}`, body)),
  remove: (id) => client.delete(`/api/trips/${id}`),
  members: (id) => d(client.get(`/api/trips/${id}/members`)),
  removeMember: (id, userId) => client.delete(`/api/trips/${id}/members/${userId}`),
  setRole: (id, userId, role) => client.patch(`/api/trips/${id}/members/${userId}/role`, { role }),
  leave: (id) => client.post(`/api/trips/${id}/leave`),
  transferOwner: (id, userId) => client.patch(`/api/trips/${id}/owner`, { userId }),
  invitations: (id) => d(client.get(`/api/trips/${id}/invitations`)),
  invite: (id, body) => d(client.post(`/api/trips/${id}/invitations`, body)), // {role,usageLimit,email?}
  auditLogs: (id, page = 0) =>
    d(client.get(`/api/trips/${id}/audit-logs`, { params: { page, size: 20 } })),
}

export const invitations = {
  accept: (token) => client.post('/api/invitations/accept', null, { params: { token } }),
  revoke: (invitationId) => client.delete(`/api/invitations/${invitationId}`),
}

export const itinerary = {
  get: (tripId) => d(client.get(`/api/trips/${tripId}/days/itinerary`)),
  createDay: (tripId, body) => d(client.post(`/api/trips/${tripId}/days`, body)), // {dayNumber,date,title,notes}
  updateDay: (tripId, dayId, body) => d(client.patch(`/api/trips/${tripId}/days/${dayId}`, body)),
  deleteDay: (tripId, dayId) => client.delete(`/api/trips/${tripId}/days/${dayId}`),
  createActivity: (tripId, dayId, body) =>
    d(client.post(`/api/trips/${tripId}/days/${dayId}/activities`, body)),
  updateActivity: (tripId, id, body) =>
    d(client.patch(`/api/trips/${tripId}/days/activities/${id}`, body)),
  // body: { targetDayId?, afterActivityId (null = first), version }
  moveActivity: (tripId, id, body) =>
    d(client.patch(`/api/trips/${tripId}/days/activities/${id}/move`, body)),
  deleteActivity: (tripId, id) => client.delete(`/api/trips/${tripId}/days/activities/${id}`),
}

export const expenses = {
  list: (tripId, page = 0) =>
    d(client.get(`/api/trips/${tripId}/expenses`, { params: { page, size: 50 } })),
  // body: {title,description,category,amount} + participantIds (equal) OR splits [{userId,amount}]
  create: (tripId, body, key) => d(client.post(`/api/trips/${tripId}/expenses`, body, withIdempotencyKey(key))),
  update: (tripId, id, body) => d(client.patch(`/api/trips/${tripId}/expenses/${id}`, body)),
  remove: (tripId, id) => client.delete(`/api/trips/${tripId}/expenses/${id}`),
  balances: (tripId) => d(client.get(`/api/trips/${tripId}/expenses/balances`)),
  suggestions: (tripId) => d(client.get(`/api/trips/${tripId}/expenses/settlements/suggestions`)),
  settlements: (tripId) => d(client.get(`/api/trips/${tripId}/expenses/settlements`)),
  settle: (tripId, body, key) => d(client.post(`/api/trips/${tripId}/expenses/settlements`, body, withIdempotencyKey(key))), // {paidToUserId,amount}
}

export const chat = {
  messages: (tripId, page = 0) =>
    d(client.get(`/api/trips/${tripId}/messages`, { params: { page, size: 50, sort: 'createdAt,desc' } })),
  send: (tripId, message) => d(client.post(`/api/trips/${tripId}/messages`, { message })),
  remove: (tripId, id) => client.delete(`/api/trips/${tripId}/messages/${id}`),
}

// Two-phase direct upload (SRS 11.4): ask the backend for a pre-signed PUT URL,
// send the file straight to storage, then commit its metadata to the backend.
// The PUT goes through plain axios: no Authorization header, and Content-Type
// must match what was signed.
async function directUpload(uploadUrlPath, file) {
  const contentType = file.type || 'application/octet-stream'
  const target = await d(client.post(uploadUrlPath, { fileName: file.name, contentType, sizeBytes: file.size }))
  await axios.put(target.uploadUrl, file, { headers: { 'Content-Type': target.contentType } })
  return target.storageKey
}

// Financial POSTs must carry an Idempotency-Key; one key per user action, so a
// network retry of the same action can't create a duplicate.
const withIdempotencyKey = (key = crypto.randomUUID()) => ({ headers: { 'Idempotency-Key': key } })

export const files = {
  documents: (tripId) => d(client.get(`/api/trips/${tripId}/documents`)),
  uploadDocument: async (tripId, file, documentType) => {
    const storageKey = await directUpload(`/api/trips/${tripId}/documents/upload-url`, file)
    return d(client.post(`/api/trips/${tripId}/documents`, { storageKey, fileName: file.name, documentType }))
  },
  deleteDocument: (tripId, id) => client.delete(`/api/trips/${tripId}/documents/${id}`),
  // paginated: returns { content, last, number, ... }
  media: (tripId, page = 0) => d(client.get(`/api/trips/${tripId}/media`, { params: { page, size: 24 } })),
  updateMedia: (tripId, id, caption) => d(client.patch(`/api/trips/${tripId}/media/${id}`, { caption })),
  uploadMedia: async (tripId, file, mediaFileType, caption) => {
    const storageKey = await directUpload(`/api/trips/${tripId}/media/upload-url`, file)
    return d(client.post(`/api/trips/${tripId}/media`, { storageKey, mediaFileType, caption }))
  },
  deleteMedia: (tripId, id) => client.delete(`/api/trips/${tripId}/media/${id}`),
}

export const wallet = {
  list: () => d(client.get('/api/wallet')),
  // meta: { title, itemType: TICKET|CONFIRMATION|ID, tripId?, shared? }
  add: async (file, meta) => {
    const storageKey = await directUpload('/api/wallet/items/upload-url', file)
    return d(client.post('/api/wallet/items', { ...meta, storageKey, fileName: file.name }))
  },
  get: (id) => d(client.get(`/api/wallet/items/${id}`)),
  update: (id, body) => d(client.patch(`/api/wallet/items/${id}`, body)), // {title,itemType,tripId,shared}
  remove: (id) => client.delete(`/api/wallet/items/${id}`),
  sharedWithTrip: (tripId) => d(client.get(`/api/trips/${tripId}/wallet-items`)),
}

export const notifications = {
  list: (page = 0) => d(client.get('/api/notifications', { params: { page, size: 20 } })),
  unreadCount: () => d(client.get('/api/notifications/unread-count')), // {unreadCount}
  markRead: (id) => client.patch(`/api/notifications/${id}/read`),
  markAllRead: () => client.patch('/api/notifications/read-all'),
}

// Place search and weather go through the backend (keys stay server-side; results cached in Redis).
export const places = {
  search: (q) => d(client.get('/api/locations/search', { params: { q } })), // [{name,address,latitude,longitude}]
}

export const weather = {
  get: (lat, lon) => d(client.get('/api/weather', { params: { lat, lon } })),
}
