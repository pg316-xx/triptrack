import { useEffect, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { notifications } from '../api'
import { connectSocket } from '../api/socket'
import { Avatar } from './ui'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    notifications.unreadCount().then((r) => setUnread(r.unreadCount)).catch(() => {})
    // live notifications: /topic/user/{userId}/notifications
    const sock = connectSocket([
      { topic: `/topic/user/${user.id}/notifications`, handler: () => setUnread((n) => n + 1) },
    ])
    return () => { sock.deactivate() }
  }, [user.id])

  return (
    <nav className="nav">
      <div className="nav-inner">
        <Link to="/" className="brand"><span className="brand-mark">🧭</span>TripNest</Link>
        <div className="links">
          <NavLink to="/" end>🗺️ <span className="nav-label">Trips</span></NavLink>
          <NavLink to="/wallet">👛 <span className="nav-label">Wallet</span></NavLink>
          <NavLink to="/invite">🎟️ <span className="nav-label">Join</span></NavLink>
          <NavLink to="/notifications" onClick={() => setUnread(0)}>
            🔔 <span className="nav-label">Alerts</span>{unread > 0 && <span className="badge">{unread > 99 ? '99+' : unread}</span>}
          </NavLink>
          <NavLink to="/profile" className="nav-user">
            <Avatar name={user.name} id={user.id} src={user.profileImageUrl} />
            <span className="nav-label">{user.name}</span>
          </NavLink>
          <button className="ghost small" onClick={async () => { await logout(); navigate('/login') }}>
            Log out
          </button>
        </div>
      </div>
    </nav>
  )
}
