import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { invitations } from '../api'
import { errMsg } from '../api/client'

// The backend returns an invite link like http://localhost:5173/invite?token=XYZ, which lands here
// and accepts automatically. Users can also paste a link or bare token into the form.
export default function Invite() {
  const [params] = useSearchParams()
  const [value, setValue] = useState(params.get('token') || '')
  const [status, setStatus] = useState(null)

  const extractToken = (v) => {
    try { return new URL(v).searchParams.get('token') || v } catch { return v.trim() }
  }

  const accept = async (e) => {
    e?.preventDefault()
    setStatus(null)
    try {
      await invitations.accept(extractToken(value))
      setStatus({ ok: true })
    } catch (err) {
      setStatus({ ok: false, msg: errMsg(err) })
    }
  }

  useEffect(() => { if (params.get('token')) accept() }, []) // eslint-disable-line

  return (
    <form className="card" onSubmit={accept} style={{ maxWidth: 560 }}>
      <h2>🎟️ Join a trip</h2>
      <p className="muted" style={{ marginTop: 0 }}>Paste the invite link a friend shared with you.</p>
      <div className="field">
        <label>Invite link or token</label>
        <input value={value} onChange={(e) => setValue(e.target.value)} placeholder="https://…/accept?token=…" required autoFocus />
      </div>
      <button>Accept invitation</button>
      {status?.ok && <div className="success" style={{ marginTop: 12 }}>✅ You're in! <Link to="/">Go to your trips →</Link></div>}
      {status && !status.ok && <div className="error" style={{ marginTop: 12 }}>{status.msg}</div>}
    </form>
  )
}
