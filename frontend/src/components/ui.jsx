// Small shared presentational helpers.

const PALETTE = [
  ['#1f4d36', '#3f7d58'],
  ['#f28a1e', '#f6b04f'],
  ['#2f6f73', '#58a3a0'],
  ['#8a5a2b', '#c48a4e'],
  ['#3b5d2a', '#7fa35a'],
  ['#c8572b', '#f08a4b'],
  ['#24506b', '#4f8bb0'],
]

const hash = (s = '') => [...String(s)].reduce((h, c) => (h * 31 + c.charCodeAt(0)) >>> 0, 7)

// Stable gradient per string (trip id, user id…), so the same trip always gets the same colors.
export const gradientFor = (key) => {
  const [a, b] = PALETTE[hash(key) % PALETTE.length]
  return `linear-gradient(135deg, ${a}, ${b})`
}

export const initials = (name = '?') =>
  name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase()).join('') || '?'

export function Avatar({ name, id, src, size }) {
  const cls = `avatar${size === 'lg' ? ' lg' : ''}`
  if (src) return <img className={cls} src={src} alt={name} />
  return (
    <span className={cls} style={{ background: gradientFor(id || name) }} title={name}>
      {initials(name)}
    </span>
  )
}

export const Spinner = () => <div className="spinner" role="status" aria-label="Loading" />

export function Empty({ icon, title, children }) {
  return (
    <div className="empty">
      <div className="icon">{icon}</div>
      {title && <h3>{title}</h3>}
      {children}
    </div>
  )
}

export const money = (n) =>
  `₹${Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`

const DAY = 86400000
// "2026-10-05" is parsed as UTC by Date; parse plain dates as local so they don't shift a day.
const toDate = (d) =>
  typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d) ? new Date(`${d}T00:00:00`) : new Date(d)
const startOfDay = (d) => { const x = toDate(d); x.setHours(0, 0, 0, 0); return x }

// Upcoming / Ongoing / Completed status for a trip's date range.
export function tripStatus(start, end) {
  if (!start || !end) return null
  const today = startOfDay(new Date())
  const s = startOfDay(start)
  const e = startOfDay(end)
  if (today < s) {
    const days = Math.round((s - today) / DAY)
    return { label: days === 1 ? 'Tomorrow' : `In ${days} days`, tone: 'warning' }
  }
  if (today > e) return { label: 'Completed', tone: 'neutral' }
  return { label: 'Ongoing', tone: 'success' }
}

export const tripLength = (start, end) =>
  start && end ? Math.round((startOfDay(end) - startOfDay(start)) / DAY) + 1 : null

export const fmtDate = (d, opts = { day: 'numeric', month: 'short', year: 'numeric' }) =>
  d ? toDate(d).toLocaleDateString(undefined, opts) : ''

// Scenic photo for a trip: matched to the destination when we have a fitting
// one, otherwise a stable pick from public/images/trip-1..10.jpg by id.
const PLACE_PHOTOS = [
  [/goa|beach|kerala|caribbean|thailand|phuket/i, 1],
  [/paris|france/i, 2],
  [/japan|tokyo|kyoto|osaka|fuji/i, 3],
  [/lake|canada|finland|sweden|norway/i, 4],
  [/scotland|ireland|\buk\b|england|wales|highland/i, 5],
  [/bali|indonesia/i, 6],
  [/italy|cinque|amalfi|rome|venice|europe/i, 7],
  [/hawaii|maldives|island|sri lanka/i, 8],
  [/desert|nevada|utah|arizona|road trip|\busa\b|america/i, 9],
  [/mountain|himalaya|alps|trek|hike|manali|nepal/i, 10],
]

export const tripImage = (key, destination = "") => {
  const match = PLACE_PHOTOS.find(([re]) => re.test(destination || ""))
  const n = match ? match[1] : (hash(key) % 10) + 1
  return `/images/trip-${n}.jpg`
}
