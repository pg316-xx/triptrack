import { useEffect, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { notifications } from '../api'
import { connectSocket } from '../api/socket'
import { Avatar } from './ui'
import Brand from './Brand'
import { IconBell, IconCompass, IconLogout, IconTicket, IconWallet } from './icons'

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

  useEffect(() => {
    document.body.classList.add('has-mobile-tabs')
    return () => document.body.classList.remove('has-mobile-tabs')
  }, [])

  const signOut = async () => { await logout(); navigate('/') }

  return (
    <>
      <nav className="nav">
        <div className="nav-inner">
          <Brand />
          <div className="links">
            <NavLink to="/" end>My Trips</NavLink>
            <NavLink to="/wallet">Wallet</NavLink>
            <NavLink to="/invite">Join a Trip</NavLink>
            <NavLink to="/notifications" onClick={() => setUnread(0)}>Alerts</NavLink>
          </div>
          <div className="tools">
            <Link to="/notifications" className="icon-btn" aria-label="Notifications" onClick={() => setUnread(0)}>
              <IconBell size={18} />
              {unread > 0 && <span className="badge">{unread > 99 ? '99+' : unread}</span>}
            </Link>
            <NavLink to="/profile" className="nav-user" title="Profile">
              <Avatar name={user.name} id={user.id} src={user.profileImageUrl} />
              <span className="nav-label">{user.name.split(' ')[0]}</span>
            </NavLink>
            <button className="icon-btn" aria-label="Log out" title="Log out" onClick={signOut}>
              <IconLogout size={18} />
            </button>
          </div>
        </div>
      </nav>

      {/* bottom tab bar on phones (the top links are hidden there) */}
      <nav className="mobile-tabs" aria-label="Main">
        <NavLink to="/" end><IconCompass size={20} />Trips</NavLink>
        <NavLink to="/wallet"><IconWallet size={20} />Wallet</NavLink>
        <NavLink to="/invite"><IconTicket size={20} />Join</NavLink>
        <NavLink to="/notifications" onClick={() => setUnread(0)}><IconBell size={20} />Alerts</NavLink>
      </nav>
    </>
  )
}
