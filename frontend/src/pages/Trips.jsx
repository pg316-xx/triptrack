import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { trips } from '../api'
import { errMsg } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { Spinner, fmtDate, tripImage, tripLength, tripStatus } from '../components/ui'
import {
  IconArrowRight, IconCalendar, IconClock, IconCompass, IconLeaf, IconPin, IconPlus, IconRoute,
  IconSearch, IconSparkle, IconTicket, Swash,
} from '../components/icons'

const empty = { title: '', description: '', destination: '', startDate: '', endDate: '' }
const FILTERS = [['all', 'All trips'], ['upcoming', 'Upcoming'], ['ongoing', 'Ongoing'], ['completed', 'Completed']]

const phase = (t) => {
  const s = tripStatus(t.startDate, t.endDate)
  if (!s) return 'upcoming'
  return s.label === 'Completed' ? 'completed' : s.label === 'Ongoing' ? 'ongoing' : 'upcoming'
}

// A trip started on the landing page before signing up.
const takeDraft = () => {
  try {
    const raw = sessionStorage.getItem('tripDraft')
    sessionStorage.removeItem('tripDraft')
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}

export default function Trips() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const [list, setList] = useState(null)
  const [form, setForm] = useState(empty)
  const [show, setShow] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('all')
  const formRef = useRef(null)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const load = () => trips.list().then(setList).catch((e) => { setList([]); setError(errMsg(e)) })
  useEffect(() => { load() }, [])

  const openForm = (prefill = {}) => {
    setForm({ ...empty, ...prefill })
    setShow(true)
    setTimeout(() => formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 50)
  }

  useEffect(() => {
    const draft = takeDraft()
    if (draft) {
      openForm({ title: draft.title || '', destination: draft.destination || '', startDate: draft.startDate || '', endDate: draft.endDate || '' })
    } else if (params.get('new')) {
      openForm()
    }
    if (params.get('new')) setParams({}, { replace: true })
  }, []) // eslint-disable-line

  const create = async (e) => {
    e.preventDefault()
    setError('')
    if (form.endDate < form.startDate) return setError('End date must be on or after the start date.')
    setBusy(true)
    try {
      const created = await trips.create(form)
      setForm(empty)
      setShow(false)
      if (created?.id) navigate(`/trips/${created.id}`)
      else load()
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  const counts = useMemo(() => {
    const c = { all: 0, upcoming: 0, ongoing: 0, completed: 0, days: 0 }
    for (const t of list || []) { c.all++; c[phase(t)]++; c.days += tripLength(t.startDate, t.endDate) || 0 }
    return c
  }, [list])

  const visible = (list || []).filter((t) => {
    const text = `${t.name} ${t.destination}`.toLowerCase()
    return (filter === 'all' || phase(t) === filter) && text.includes(query.trim().toLowerCase())
  })

  const firstName = user.name.split(' ')[0]

  return (
    <>
      <section className="hero-photo" style={{ '--hero-img': 'url(/images/hero-lake.jpg)', minHeight: 400 }}>
        <IconLeaf className="leaf-deco" size={30} style={{ top: 16, left: 16, transform: 'rotate(-30deg)' }} />
        <div className="eyebrow">Welcome back, {firstName}</div>
        <h1 style={{ marginTop: 12, fontSize: 'clamp(2rem, 4.4vw, 3.2rem)' }}>
          Where to next?<br />
          <span className="green">Plan your escape.<Swash /></span>
        </h1>
        <p className="lead">Pick up where your crew left off, or start something new.</p>
        <div className="actions">
          <button className="accent" onClick={() => openForm()}>Plan a new trip <IconArrowRight size={18} /></button>
          <button className="secondary" onClick={() => navigate('/invite')}>Join with invite</button>
        </div>
      </section>

      <form className="search-bar" onSubmit={(e) => e.preventDefault()} role="search">
        <div className="seg" style={{ flex: 2 }}>
          <IconSearch />
          <div style={{ minWidth: 0, flex: 1 }}>
            <label htmlFor="q">Find a trip</label>
            <input id="q" placeholder="Name or destination" value={query} onChange={(e) => setQuery(e.target.value)} />
          </div>
        </div>
        <div className="seg">
          <IconCalendar />
          <div style={{ minWidth: 0, flex: 1 }}>
            <label htmlFor="f">When</label>
            <select id="f" value={filter} onChange={(e) => setFilter(e.target.value)}>
              {FILTERS.map(([v, l]) => <option key={v} value={v}>{l} ({counts[v]})</option>)}
            </select>
          </div>
        </div>
        <button type="button" onClick={() => openForm()}>New trip <IconPlus size={16} /></button>
      </form>

      <section className="feature-strip">
        {[[IconCompass, counts.all, 'Trips planned'], [IconClock, counts.upcoming, 'Coming up'], [IconRoute, counts.ongoing, 'On the road now'], [IconCalendar, counts.days, 'Days of travel']].map(([Icon, n, label]) => (
          <div key={label} className="feature">
            <div className="ic"><Icon size={20} /></div>
            <div><div className="big">{list ? n : '–'}</div><span>{label}</span></div>
          </div>
        ))}
      </section>

      {error && <div className="error" style={{ marginTop: 20 }}>{error}</div>}

      {show && (
        <form ref={formRef} className="card" onSubmit={create} style={{ marginTop: 28 }}>
          <div className="row between" style={{ marginBottom: 6 }}>
            <h3 style={{ margin: 0 }}>Plan a new trip</h3>
            <button type="button" className="ghost small" onClick={() => setShow(false)}>Cancel</button>
          </div>
          <div className="row" style={{ alignItems: 'flex-start' }}>
            <div className="field" style={{ flex: 1, minWidth: 200 }}><label>Title</label><input value={form.title} onChange={set('title')} placeholder="Goa with friends" required autoFocus /></div>
            <div className="field" style={{ flex: 1, minWidth: 200 }}><label>Destination</label><input value={form.destination} onChange={set('destination')} placeholder="Goa, India" required /></div>
          </div>
          <div className="field"><label>Description</label><textarea value={form.description} onChange={set('description')} placeholder="What's the plan?" /></div>
          <div className="row" style={{ alignItems: 'flex-start' }}>
            <div className="field" style={{ flex: 1, minWidth: 140 }}><label>Start</label><input type="date" value={form.startDate} onChange={set('startDate')} required /></div>
            <div className="field" style={{ flex: 1, minWidth: 140 }}><label>End</label><input type="date" min={form.startDate || undefined} value={form.endDate} onChange={set('endDate')} required /></div>
          </div>
          <button className="accent" disabled={busy}>{busy ? 'Creating…' : 'Create trip'} <IconArrowRight size={16} /></button>
        </form>
      )}

      <div className="section-title">
        <h2>Your Trips <IconLeaf className="leaf" size={18} /></h2>
        {list && list.length > 0 && <span className="muted">{visible.length} of {list.length}</span>}
      </div>

      {!list && <Spinner />}
      {list && (
        <div className="dest-grid">
          {visible.map((t) => {
            const status = tripStatus(t.startDate, t.endDate)
            const days = tripLength(t.startDate, t.endDate)
            return (
              <Link key={t.id} to={`/trips/${t.id}`} className="dest-card" style={{ backgroundImage: `url(${t.coverImageUrl || tripImage(t.id, t.destination)})` }}>
                <div className="top">
                  {status && <span className="pill dark"><IconSparkle size={11} /> {status.label}</span>}
                </div>
                <div className="bottom">
                  <div style={{ minWidth: 0 }}>
                    <h3>{t.name}</h3>
                    <div className="sub" style={{ display: 'flex', alignItems: 'center', gap: 4 }}><IconPin size={13} /> {t.destination}</div>
                    <div className="sub">{fmtDate(t.startDate, { day: 'numeric', month: 'short' })} – {fmtDate(t.endDate)}</div>
                  </div>
                  {days && <div className="price"><small>trip</small>{days} day{days === 1 ? '' : 's'}</div>}
                </div>
              </Link>
            )
          })}
          {visible.length === 0 && list.length > 0 && (
            <div className="empty" style={{ gridColumn: '1 / -1' }}><h3>No trips match</h3>Try another name or filter.</div>
          )}
          {filter === 'all' && !query && (
            <button type="button" className="dest-card new" onClick={() => openForm()}>
              <div>
                <div className="plus"><IconPlus size={24} /></div>
                <strong style={{ color: 'var(--text)' }}>Plan a new trip</strong>
                <div className="muted">or join one with an invite</div>
              </div>
            </button>
          )}
        </div>
      )}

      <section className="promo">
        <div className="img" style={{ backgroundImage: 'url(/images/banner-mountains.jpg)' }} />
        <div className="txt">
          <div className="eyebrow"><IconTicket size={12} /> Travel wallet</div>
          <h2>All your tickets, one tap away</h2>
          <p>Keep boarding passes, bookings and IDs private, and share them with a trip only when you want to.</p>
          <Link to="/wallet" className="btn accent">Open wallet <IconArrowRight size={16} /></Link>
        </div>
      </section>
    </>
  )
}
