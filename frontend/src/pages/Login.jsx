import { useState } from 'react'
import { Link, useNavigate, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { errMsg } from '../api/client'
import AuthLayout from '../components/AuthLayout'
import { IconArrowRight } from '../components/icons'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Your crew is waiting." text="Jump back into your itineraries, shared costs and group chat.">
      <form className="card auth" onSubmit={submit}>
        <div className="eyebrow">Welcome back</div>
        <h2 style={{ marginTop: 6 }}>Log in to TripNest</h2>
        <p className="muted sub">Plan, split and chat with your travel crew.</p>
        {error && <div className="error">{error}</div>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </div>
        <div className="field">
          <div className="row between" style={{ marginBottom: 6 }}>
            <label htmlFor="password" style={{ margin: 0 }}>Password</label>
            <Link to="/forgot-password" style={{ fontSize: '0.8rem' }}>Forgot password?</Link>
          </div>
          <input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        <button className="block accent" disabled={busy}>{busy ? 'Logging in…' : 'Log in'} <IconArrowRight size={16} /></button>
        <p className="muted foot">No account? <Link to="/register">Create one</Link></p>
        <p className="muted foot" style={{ marginTop: 6 }}><Link to="/" style={{ color: 'var(--muted)' }}>← Back to home</Link></p>
      </form>
    </AuthLayout>
  )
}
