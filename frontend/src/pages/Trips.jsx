import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { trips } from '../api'
import { errMsg } from '../api/client'
import { Empty, Spinner, fmtDate, gradientFor, initials, tripLength, tripStatus } from '../components/ui'

const empty = { title: '', description: '', destination: '', startDate: '', endDate: '' }

export default function Trips() {
  const navigate = useNavigate()
  const [list, setList] = useState(null)
  const [form, setForm] = useState(empty)
  const [show, setShow] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const load = () => trips.list().then(setList).catch((e) => { setList([]); setError(errMsg(e)) })
  useEffect(() => { load() }, [])

  const create = async (e) => {
    e.preventDefault()
    setError('')
    if (form.endDate < form.startDate) return setError('End date must be on or after the start date.')
    setBusy(true)
    try {
      await trips.create(form)
      setForm(empty)
      setShow(false)
      load()
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h2>My trips</h2>
          <div className="muted">{list ? `${list.length} trip${list.length === 1 ? '' : 's'}` : ' '}</div>
        </div>
        <button className={show ? 'secondary' : ''} onClick={() => setShow(!show)}>{show ? 'Cancel' : '+ New trip'}</button>
      </div>
      {error && <div className="error">{error}</div>}

      {show && (
        <form className="card" onSubmit={create}>
          <h3>Plan a new trip</h3>
          <div className="row" style={{ alignItems: 'flex-start' }}>
            <div className="field" style={{ flex: 1, minWidth: 200 }}><label>Title</label><input value={form.title} onChange={set('title')} placeholder="Goa with friends" required autoFocus /></div>
            <div className="field" style={{ flex: 1, minWidth: 200 }}><label>Destination</label><input value={form.destination} onChange={set('destination')} placeholder="Goa, India" required /></div>
          </div>
          <div className="field"><label>Description</label><textarea value={form.description} onChange={set('description')} placeholder="What's the plan?" /></div>
          <div className="row" style={{ alignItems: 'flex-start' }}>
            <div className="field" style={{ flex: 1, minWidth: 140 }}><label>Start</label><input type="date" value={form.startDate} onChange={set('startDate')} required /></div>
            <div className="field" style={{ flex: 1, minWidth: 140 }}><label>End</label><input type="date" min={form.startDate || undefined} value={form.endDate} onChange={set('endDate')} required /></div>
          </div>
          <button disabled={busy}>{busy ? 'Creating…' : 'Create trip'}</button>
        </form>
      )}

      {!list && <Spinner />}
      {list?.length === 0 && !show && (
        <Empty icon="🧳" title="No trips yet">
          <p>Create your first trip, or join one with an invite link.</p>
          <div className="row" style={{ justifyContent: 'center' }}>
            <button onClick={() => setShow(true)}>+ New trip</button>
            <button className="secondary" onClick={() => navigate('/invite')}>Join with invite</button>
          </div>
        </Empty>
      )}

      <div className="grid">
        {list?.map((t) => {
          const status = tripStatus(t.startDate, t.endDate)
          const days = tripLength(t.startDate, t.endDate)
          return (
            <Link key={t.id} to={`/trips/${t.id}`} className="card trip-card">
              <div className="trip-banner" style={{ background: gradientFor(t.id) }}>
                {status && <span className="pill">{status.label}</span>}
                <span className="initial">{initials(t.name)}</span>
              </div>
              <div className="trip-body">
                <h3>{t.name}</h3>
                <div className="trip-meta">
                  <span>📍 {t.destination}</span>
                  {days && <span>🗓️ {days} day{days === 1 ? '' : 's'}</span>}
                </div>
                <div className="trip-meta">
                  <span>{fmtDate(t.startDate, { day: 'numeric', month: 'short' })} – {fmtDate(t.endDate)}</span>
                </div>
                <div className="muted" style={{ marginTop: 'auto', paddingTop: 8 }}>by {t.createdByName}</div>
              </div>
            </Link>
          )
        })}
      </div>
    </>
  )
}
