import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { auth } from '../api'
import { errMsg } from '../api/client'
import AuthLayout from '../components/AuthLayout'
import { IconArrowRight } from '../components/icons'

// Opened from the link in the reset email: /reset-password?token=...
export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (password !== confirm) return setError("The passwords don't match.")
    setBusy(true)
    try {
      await auth.resetPassword(token, password)
      setDone(true)
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Fresh start." text="Choose a new password. You'll be signed out on every other device.">
      <form className="card auth" onSubmit={submit}>
        <div className="eyebrow">Account help</div>
        <h2 style={{ marginTop: 6 }}>Choose a new password</h2>
        {!token && <div className="error">This link is missing its token. Request a new reset email.</div>}
        {error && <div className="error">{error}</div>}
        {done ? (
          <div className="success">Password updated. <Link to="/login">Log in →</Link></div>
        ) : (
          <>
            <p className="muted sub">At least 8 characters.</p>
            <div className="field">
              <label htmlFor="pw">New password</label>
              <input id="pw" type="password" autoComplete="new-password" minLength={8} maxLength={72} value={password} onChange={(e) => setPassword(e.target.value)} required autoFocus />
            </div>
            <div className="field">
              <label htmlFor="pw2">Confirm password</label>
              <input id="pw2" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
            </div>
            <button className="block accent" disabled={busy || !token}>{busy ? 'Saving…' : 'Update password'} <IconArrowRight size={16} /></button>
          </>
        )}
        <p className="muted foot"><Link to="/forgot-password">Request a new link</Link></p>
      </form>
    </AuthLayout>
  )
}
