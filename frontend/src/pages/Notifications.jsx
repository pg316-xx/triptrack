import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { notifications } from '../api'
import { errMsg } from '../api/client'
import { Empty } from '../components/ui'

export default function Notifications() {
  const [items, setItems] = useState([])
  const [error, setError] = useState('')

  const load = () =>
    notifications.list().then((p) => setItems(p.content)).catch((e) => setError(errMsg(e)))
  useEffect(() => { load() }, [])

  const read = async (n) => {
    if (!n.isRead) await notifications.markRead(n.id)
    load()
  }

  return (
    <>
      <div className="page-head">
        <h2>Notifications</h2>
        <button className="secondary small" disabled={!items.some((n) => !n.isRead)} onClick={async () => { await notifications.markAllRead(); load() }}>
          Mark all read
        </button>
      </div>
      {error && <div className="error">{error}</div>}
      {items.length === 0 && <Empty icon="🔔" title="You're all caught up">New invites, expenses and messages will show up here.</Empty>}
      {items.map((n) => (
        <div key={n.id} className={`card notif ${n.isRead ? '' : 'unread'}`}>
          <div className="icon">{n.isRead ? '📭' : '📬'}</div>
          <div className="body">
            <div className="row between">
              <strong>{n.title}</strong>
              <span className="pill neutral">{n.type?.replaceAll('_', ' ').toLowerCase()}</span>
            </div>
            <div style={{ margin: '2px 0 8px' }}>{n.message}</div>
            <div className="row">
              <span className="muted">{new Date(n.createdAt).toLocaleString()}</span>
              {n.tripId && <Link to={`/trips/${n.tripId}`}>Open trip →</Link>}
              {!n.isRead && <button className="small ghost" onClick={() => read(n)}>Mark read</button>}
            </div>
          </div>
        </div>
      ))}
    </>
  )
}
