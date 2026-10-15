// Small shared presentational helpers.

const PALETTE = [
  ['#6366f1', '#06b6d4'],
  ['#f43f5e', '#f59e0b'],
  ['#10b981', '#06b6d4'],
  ['#8b5cf6', '#ec4899'],
  ['#0ea5e9', '#6366f1'],
  ['#f97316', '#e11d48'],
  ['#14b8a6', '#84cc16'],
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
