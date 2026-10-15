import { useEffect, useState } from 'react'
import { itinerary } from '../../api'
import { errMsg } from '../../api/client'
import { connectSocket } from '../../api/socket'
import { Empty, fmtDate, money } from '../ui'

const emptyAct = { title: '', location: '', startTime: '', endTime: '', estimatedCost: '' }

// datetime-local gives "2026-01-01T10:00"; backend wants an OffsetDateTime -> send ISO with Z.
const toIso = (v) => (v ? new Date(v).toISOString() : null)

export default function ItineraryTab({ tripId }) {
  const [days, setDays] = useState([])
  const [error, setError] = useState('')
  const [dayForm, setDayForm] = useState({ date: '', title: '', notes: '' })
  const [actForm, setActForm] = useState({}) // keyed by dayId

  const load = () =>
    itinerary.get(tripId).then((r) => setDays((r.days || []).map((d) => ({ ...d, activities: d.activities || [] })))).catch((e) => setError(errMsg(e)))
  useEffect(() => {
    load()
    // Any member's change (including our own) arrives here after it commits;
    // re-fetch so everyone converges on the server's order and versions.
    const sock = connectSocket([{ topic: `/topic/trip/${tripId}/itinerary`, handler: () => load() }])
    return () => { sock.deactivate() }
  }, [tripId])

  const run = async (fn) => {
    setError('')
    try { await fn(); await load() } catch (e) {
      setError(errMsg(e))
      // 409: someone else changed it first — show their version
      if (e?.response?.status === 409) load()
    }
  }

  // ---- reordering (drag-and-drop + up/down buttons) ----
  const [drag, setDrag] = useState(null) // activity being dragged
  const [hint, setHint] = useState(null) // { id, pos: 'before'|'after' } or { dayId }

  const move = (a, targetDayId, afterActivityId) =>
    run(() => itinerary.moveActivity(tripId, a.id, { targetDayId, afterActivityId, version: a.version }))

  const dropPos = (e) => {
    const r = e.currentTarget.getBoundingClientRect()
    return e.clientY < r.top + r.height / 2 ? 'before' : 'after'
  }

  const dropOnActivity = (e, day, target) => {
    e.preventDefault()
    e.stopPropagation()
    const a = drag
    setDrag(null); setHint(null)
    if (!a || a.id === target.id) return
    const rest = day.activities.filter((x) => x.id !== a.id)
    const i = rest.findIndex((x) => x.id === target.id)
    const after = dropPos(e) === 'after' ? target.id : i > 0 ? rest[i - 1].id : null
    move(a, day.id, after)
  }

  // Dropping on the card but not on an activity appends to the end of that day.
  const dropOnDay = (e, day) => {
    e.preventDefault()
    const a = drag
    setDrag(null); setHint(null)
    if (!a) return
    const rest = day.activities.filter((x) => x.id !== a.id)
    move(a, day.id, rest.length ? rest[rest.length - 1].id : null)
  }

  const nudge = (day, i, dir) => {
    const acts = day.activities
    if (dir < 0 && i > 0) move(acts[i], day.id, i >= 2 ? acts[i - 2].id : null)
    if (dir > 0 && i < acts.length - 1) move(acts[i], day.id, acts[i + 1].id)
  }

  const addDay = (e) => {
    e.preventDefault()
    const nextNumber = days.length ? Math.max(...days.map((d) => d.dayNumber)) + 1 : 1
    run(async () => {
      await itinerary.createDay(tripId, { dayNumber: nextNumber, ...dayForm })
      setDayForm({ date: '', title: '', notes: '' })
    })
  }

  const addActivity = (e, day) => {
    e.preventDefault()
    const f = actForm[day.id] || emptyAct
    run(async () => {
      await itinerary.createActivity(tripId, day.id, {
        title: f.title,
        location: f.location || null,
        startTime: toIso(f.startTime),
        endTime: toIso(f.endTime),
        estimatedCost: f.estimatedCost ? Number(f.estimatedCost) : null,
      })
      setActForm({ ...actForm, [day.id]: emptyAct })
    })
  }

  const setAct = (dayId, k) => (e) =>
    setActForm({ ...actForm, [dayId]: { ...(actForm[dayId] || emptyAct), [k]: e.target.value } })

  const editActivity = (a) => {
    const title = prompt('Activity title', a.title)
    if (!title) return
    // `version` is sent back for the backend's optimistic locking
    run(() => itinerary.updateActivity(tripId, a.id, { ...a, title }))
  }

  return (
    <>
      {error && <div className="error">{error}</div>}

      <form className="card inline" onSubmit={addDay}>
        <h3 style={{ width: '100%', margin: '0 0 4px' }}>Add a day</h3>
        <div className="field"><label>Date</label><input type="date" value={dayForm.date} onChange={(e) => setDayForm({ ...dayForm, date: e.target.value })} required /></div>
        <div className="field"><label>Title</label><input value={dayForm.title} onChange={(e) => setDayForm({ ...dayForm, title: e.target.value })} required /></div>
        <div className="field"><label>Notes</label><input value={dayForm.notes} onChange={(e) => setDayForm({ ...dayForm, notes: e.target.value })} /></div>
        <button>+ Add day</button>
      </form>

      {days.length === 0 && <Empty icon="🗺️" title="No days planned yet">Add your first day above to start the itinerary.</Empty>}
      {days.map((day) => {
        const f = actForm[day.id] || emptyAct
        return (
          <div
            className="card day-card"
            key={day.id}
            onDragOver={(e) => { if (drag) { e.preventDefault(); setHint({ dayId: day.id }) } }}
            onDrop={(e) => dropOnDay(e, day)}
          >
            <div className="row between" style={{ alignItems: 'flex-start' }}>
              <div className="row" style={{ gap: 12, flexWrap: 'nowrap' }}>
                <div className="day-num"><div><small>Day</small>{day.dayNumber}</div></div>
                <div>
                  <h3 style={{ margin: 0 }}>{day.title}</h3>
                  <div className="muted">{fmtDate(day.date, { weekday: 'long', day: 'numeric', month: 'short' })}</div>
                </div>
              </div>
              <div className="row">
                <button className="secondary small" onClick={() => {
                  const title = prompt('Day title', day.title)
                  if (title) run(() => itinerary.updateDay(tripId, day.id, { title, notes: day.notes }))
                }}>Edit</button>
                <button className="danger small" onClick={() => confirm('Delete day?') && run(() => itinerary.deleteDay(tripId, day.id))}>Delete</button>
              </div>
            </div>
            {day.notes && <p className="muted" style={{ margin: '10px 0 0' }}>{day.notes}</p>}

            {day.activities.length > 0 && (
              <ul className={`timeline${hint?.dayId === day.id ? ' drop-end' : ''}`}>
                {day.activities.map((a, i) => (
                  <li
                    key={a.id}
                    draggable
                    className={[
                      drag?.id === a.id && 'dragging',
                      hint?.id === a.id && `drop-${hint.pos}`,
                    ].filter(Boolean).join(' ')}
                    onDragStart={(e) => { e.dataTransfer.effectAllowed = 'move'; setDrag(a) }}
                    onDragEnd={() => { setDrag(null); setHint(null) }}
                    onDragOver={(e) => { if (drag) { e.preventDefault(); e.stopPropagation(); setHint({ id: a.id, pos: dropPos(e) }) } }}
                    onDrop={(e) => dropOnActivity(e, day, a)}
                  >
                    <div className="row" style={{ gap: 12, alignItems: 'flex-start', flexWrap: 'nowrap', flex: 1, minWidth: 0 }}>
                      <span className="time">{a.startTime ? new Date(a.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</span>
                      <div style={{ minWidth: 0 }}>
                        <strong>{a.title}</strong>
                        <div className="muted">
                          {a.location && <>📍 {a.location}</>}
                          {a.location && a.estimatedCost != null && ' · '}
                          {a.estimatedCost != null && money(a.estimatedCost)}
                        </div>
                      </div>
                    </div>
                    <div className="row" style={{ flexWrap: 'nowrap' }}>
                      <button className="ghost small" aria-label="Move up" disabled={i === 0} onClick={() => nudge(day, i, -1)}>↑</button>
                      <button className="ghost small" aria-label="Move down" disabled={i === day.activities.length - 1} onClick={() => nudge(day, i, 1)}>↓</button>
                      <button className="ghost small" onClick={() => editActivity(a)}>Edit</button>
                      <button className="danger small" aria-label="Delete activity" onClick={() => run(() => itinerary.deleteActivity(tripId, a.id))}>✕</button>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            <form className="inline" style={{ marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--border)' }} onSubmit={(e) => addActivity(e, day)}>
              <div className="field"><input placeholder="Activity" value={f.title} onChange={setAct(day.id, 'title')} required /></div>
              <div className="field"><input placeholder="Location" value={f.location} onChange={setAct(day.id, 'location')} /></div>
              <div className="field"><input type="datetime-local" value={f.startTime} onChange={setAct(day.id, 'startTime')} /></div>
              <div className="field"><input type="number" min="0" placeholder="Cost" value={f.estimatedCost} onChange={setAct(day.id, 'estimatedCost')} /></div>
              <button className="small">+ Add activity</button>
            </form>
          </div>
        )
      })}
    </>
  )
}
