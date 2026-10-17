import { useState } from 'react'
import { Link } from 'react-router-dom'
import { auth } from '../api'
import { errMsg } from '../api/client'
import AuthLayout from '../components/AuthLayout'
import { IconArrowRight } from '../components/icons'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await auth.forgotPassword(email)
      setSent(true)
    } catch (err) {
      setError(errMsg(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Lost your way?" text="It happens to every traveller. We'll email you a link to set a new password.">
      <form className="card auth" onSubmit={submit}>
        <div className="eyebrow">Account help</div>
        <h2 style={{ marginTop: 6 }}>Reset your password</h2>
        <p className="muted sub">Enter the email you signed up with.</p>
        {error && <div className="error">{error}</div>}
        {sent ? (
          <div className="success">
            If <strong>{email}</strong> has an account, a reset link is on its way. It expires in 30 minutes.
          </div>
        ) : (
          <>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
            </div>
            <button className="block accent" disabled={busy}>{busy ? 'Sending…' : 'Send reset link'} <IconArrowRight size={16} /></button>
          </>
        )}
        <p className="muted foot"><Link to="/login">← Back to log in</Link></p>
      </form>
    </AuthLayout>
  )
}
