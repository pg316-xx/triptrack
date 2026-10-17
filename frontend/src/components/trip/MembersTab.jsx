import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { trips, invitations } from '../../api'
import { errMsg } from '../../api/client'
import { Avatar } from '../ui'

export default function MembersTab({ tripId, members, user, myRole, canManage, reloadMembers }) {
  const navigate = useNavigate()
  const [invites, setInvites] = useState([])
  const [newLink, setNewLink] = useState(null)
  const [role, setRole] = useState('MEMBER')
  const [limit, setLimit] = useState(5)
  const [email, setEmail] = useState('')
  const isOwner = myRole === 'OWNER'
  // Owner can remove anyone but themselves; admins can remove plain members.
  const canRemove = (m) => m.userId !== user.id && m.role !== 'OWNER' && (isOwner || (canManage && m.role === 'MEMBER'))
  const [error, setError] = useState('')

  const loadInvites = () => canManage && trips.invitations(tripId).then(setInvites).catch(() => {})
  useEffect(() => { loadInvites() }, [tripId, canManage])

  const run = async (fn) => {
    setError('')
    try { await fn() } catch (e) { setError(errMsg(e)) }
  }

  const createInvite = (e) => {
    e.preventDefault()
    run(async () => {
      const sentTo = email.trim()
      setNewLink({ ...(await trips.invite(tripId, { role, usageLimit: Number(limit), email: sentTo || undefined })), emailedTo: sentTo })
      setEmail('')
      loadInvites()
    })
  }

  return (
    <>
      {error && <div className="error">{error}</div>}

      <div className="card">
        <h3>Members <span className="muted" style={{ fontWeight: 400 }}>· {members.length}</span></h3>
        <div className="table-wrap"><table>
          <tbody>
            {members.map((m) => (
              <tr key={m.userId}>
                <td>
                  <div className="row" style={{ flexWrap: 'nowrap', gap: 10 }}>
                    <Avatar name={m.name} id={m.userId} />
                    <div><strong>{m.name}</strong>{m.userId === user.id && <span className="muted"> (you)</span>}<div className="muted">{m.email}</div></div>
                  </div>
                </td>
                <td>
                  {isOwner && m.role !== 'OWNER' && m.userId !== user.id ? (
                    <select style={{ width: 'auto' }} value={m.role}
                      onChange={(e) => run(async () => { await trips.setRole(tripId, m.userId, e.target.value); reloadMembers() })}>
                      <option>ADMIN</option><option>MEMBER</option>
                    </select>
                  ) : <span className={`pill ${m.role === 'OWNER' ? 'warning' : ''}`}>{m.role.toLowerCase()}</span>}
                </td>
                <td style={{ textAlign: 'right' }}>
                  {canRemove(m) && (
                    <button className="danger small" onClick={() => confirm(`Remove ${m.name}?`) && run(async () => { await trips.removeMember(tripId, m.userId); reloadMembers() })}>Remove</button>
                  )}
                  {myRole === 'OWNER' && m.userId !== user.id && (
                    <button className="secondary small" style={{ marginLeft: 4 }} onClick={() => confirm(`Make ${m.name} the owner?`) && run(async () => { await trips.transferOwner(tripId, m.userId); reloadMembers() })}>Make owner</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
        {myRole !== 'OWNER' && (
          <button className="danger small" style={{ marginTop: 8 }} onClick={() => confirm('Leave this trip?') && run(async () => { await trips.leave(tripId); navigate('/') })}>
            Leave trip
          </button>
        )}
      </div>

      {canManage && (
        <div className="card">
          <h3>Invite people</h3>
          <form className="inline" onSubmit={createInvite}>
            <div className="field"><label>Role</label>
              <select value={role} onChange={(e) => setRole(e.target.value)}><option>MEMBER</option>{isOwner && <option>ADMIN</option>}</select>
            </div>
            <div className="field"><label>Max uses</label><input type="number" min="1" max="100" value={limit} onChange={(e) => setLimit(e.target.value)} /></div>
            <div className="field" style={{ flex: 2, minWidth: 200 }}><label>Email it to (optional)</label><input type="email" placeholder="friend@example.com" value={email} onChange={(e) => setEmail(e.target.value)} /></div>
            <button>Create link</button>
          </form>
          {newLink && (
            <div className="success" style={{ marginTop: 14 }}>
              <strong>Share this link</strong> — it's only shown once.{newLink.emailedTo && <> We also emailed it to {newLink.emailedTo}.</>}
              <div className="row" style={{ marginTop: 8, flexWrap: 'nowrap' }}>
                <input readOnly value={newLink.inviteLink} onFocus={(e) => e.target.select()} />
                <button type="button" className="small" onClick={() => navigator.clipboard?.writeText(newLink.inviteLink)}>Copy</button>
              </div>
              <div className="muted" style={{ marginTop: 6 }}>Expires {new Date(newLink.expiresAt).toLocaleString()}</div>
            </div>
          )}
          {invites.length > 0 && (
            <table style={{ marginTop: 12 }}>
              <tbody>
                {invites.map((i) => (
                  <tr key={i.invitationId}>
                    <td><span className="pill">{i.role.toLowerCase()}</span></td>
                    <td>{i.usageCount}/{i.usageLimit} used</td>
                    <td className="muted">{i.revoked ? 'revoked' : `expires ${new Date(i.expiresAt).toLocaleDateString()}`}</td>
                    <td style={{ textAlign: 'right' }}>
                      {!i.revoked && <button className="danger small" onClick={() => run(async () => { await invitations.revoke(i.invitationId); loadInvites() })}>Revoke</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </>
  )
}
