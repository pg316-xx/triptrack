import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { trips } from '../api'
import { errMsg } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { Spinner, fmtDate, gradientFor, tripLength, tripStatus } from '../components/ui'
import ItineraryTab from '../components/trip/ItineraryTab'
import ExpensesTab from '../components/trip/ExpensesTab'
import ChatTab from '../components/trip/ChatTab'
import FilesTab from '../components/trip/FilesTab'
import MembersTab from '../components/trip/MembersTab'
import ActivityTab from '../components/trip/ActivityTab'

const TABS = [
  ['Itinerary', '🗓️'],
  ['Expenses', '💸'],
  ['Chat', '💬'],
  ['Files', '📁'],
  ['Members', '👥'],
  ['Activity', '📜'],
]

export default function TripDetail() {
  const { tripId } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [trip, setTrip] = useState(null)
  const [members, setMembers] = useState([])
  const [tab, setTab] = useState('Itinerary')
  const [error, setError] = useState('')

  const loadMembers = () => trips.members(tripId).then(setMembers)

  useEffect(() => {
    trips.get(tripId).then(setTrip).catch((e) => setError(errMsg(e)))
    loadMembers().catch((e) => setError(errMsg(e)))
  }, [tripId])

  if (error) return <><div className="error">{error}</div><Link to="/">← Back to trips</Link></>
  if (!trip) return <Spinner />

  const myRole = members.find((m) => m.userId === user.id)?.role
  const canManage = myRole === 'OWNER' || myRole === 'ADMIN'
  const ctx = { tripId, trip, members, user, myRole, canManage, reloadMembers: loadMembers }
  const status = tripStatus(trip.startDate, trip.endDate)
  const days = tripLength(trip.startDate, trip.endDate)

  const rename = async () => {
    const title = prompt('New trip title', trip.name)
    if (!title) return
    try { setTrip(await trips.update(tripId, { title })) } catch (e) { alert(errMsg(e)) }
  }

  const remove = async () => {
    if (!confirm('Delete this trip permanently?')) return
    try { await trips.remove(tripId); navigate('/') } catch (e) { alert(errMsg(e)) }
  }

  return (
    <>
      <Link to="/" className="muted" style={{ display: 'inline-block', marginBottom: 12 }}>← All trips</Link>
      <div className="hero" style={{ background: gradientFor(trip.id) }}>
        <div className="row between" style={{ alignItems: 'flex-start' }}>
          <div>
            <div className="row" style={{ marginBottom: 6 }}>
              {status && <span className="pill">{status.label}</span>}
              {myRole && <span className="pill">{myRole.toLowerCase()}</span>}
            </div>
            <h2>{trip.name}</h2>
            <div className="meta">
              <span>📍 {trip.destination}</span>
              <span>🗓️ {fmtDate(trip.startDate)} – {fmtDate(trip.endDate)}{days ? ` · ${days} days` : ''}</span>
              <span>👥 {members.length} member{members.length === 1 ? '' : 's'}</span>
            </div>
          </div>
          {canManage && (
            <div className="actions">
              <button className="secondary small" onClick={rename}>Rename</button>
              {myRole === 'OWNER' && <button className="danger small" onClick={remove}>Delete</button>}
            </div>
          )}
        </div>
        {trip.description && <p>{trip.description}</p>}
      </div>

      <div className="tabs" role="tablist">
        {TABS.map(([t, icon]) => (
          <button key={t} role="tab" aria-selected={tab === t} className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>
            {icon} {t}
          </button>
        ))}
      </div>

      {tab === 'Itinerary' && <ItineraryTab {...ctx} />}
      {tab === 'Expenses' && <ExpensesTab {...ctx} />}
      {tab === 'Chat' && <ChatTab {...ctx} />}
      {tab === 'Files' && <FilesTab {...ctx} />}
      {tab === 'Members' && <MembersTab {...ctx} />}
      {tab === 'Activity' && <ActivityTab {...ctx} />}
    </>
  )
}
