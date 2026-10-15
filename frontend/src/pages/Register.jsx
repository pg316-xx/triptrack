import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { errMsg } from '../api/client'
import AuthLayout from '../components/AuthLayout'
import { IconArrowRight } from '../components/icons'

export default function Register() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  if (user) return <Navigate to="/" replace />

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await register(form.name, form.email, form.password)
      navigate('/')
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Discover nature. Plan your escape." text="Create a free account to plan trips with friends, split costs fairly and keep every ticket in one place.">
      <form className="card auth" onSubmit={submit}>
        <div className="eyebrow">Get started</div>
        <h2 style={{ marginTop: 6 }}>Create your account</h2>
        <p className="muted sub">Start planning your next trip together.</p>
        {error && <div className="error">{error}</div>}
        <div className="field"><label htmlFor="name">Name</label><input id="name" autoComplete="name" value={form.name} onChange={set('name')} required autoFocus /></div>
        <div className="field"><label htmlFor="email">Email</label><input id="email" type="email" autoComplete="email" value={form.email} onChange={set('email')} required /></div>
        <div className="field"><label htmlFor="password">Password</label><input id="password" type="password" autoComplete="new-password" value={form.password} onChange={set('password')} required /></div>
        <button className="block accent" disabled={busy}>{busy ? 'Creating account…' : 'Create account'} <IconArrowRight size={16} /></button>
        <p className="muted foot">Have an account? <Link to="/login">Log in</Link></p>
        <p className="muted foot" style={{ marginTop: 6 }}><Link to="/" style={{ color: 'var(--muted)' }}>← Back to home</Link></p>
      </form>
    </AuthLayout>
  )
}
