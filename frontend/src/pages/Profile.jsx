import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { tokens } from "../api/client"
import { users } from '../api'
import { useAuth } from '../context/AuthContext'
import { errMsg } from '../api/client'
import { Avatar } from '../components/ui'
import PageBanner from '../components/PageBanner'

export default function Profile() {
  const { user, reloadUser, logout } = useAuth()
  const navigate = useNavigate()
  const [deleteError, setDeleteError] = useState("")

  const deleteAccount = async () => {
    if (!confirm("Delete your account? Your name, email and private wallet files are removed and you are signed out everywhere. Trips you share with others must be handed to another owner first.")) return
    if (prompt("Type DELETE to confirm") !== "DELETE") return
    setDeleteError("")
    try {
      await users.deleteMe()
      tokens.clear()
      await logout().catch(() => {})
      navigate("/")
    } catch (err) {
      setDeleteError(errMsg(err))
    }
  }
  const [name, setName] = useState(user.name)
  const [img, setImg] = useState(user.profileImageUrl || '')
  const [msg, setMsg] = useState('')

  const save = async (e) => {
    e.preventDefault()
    try {
      await users.update({ name, profileImageUrl: img || null })
      await reloadUser()
      setMsg('Saved ✔')
    } catch (err) {
      setMsg(errMsg(err))
    }
  }

  return (
    <>
    <PageBanner eyebrow="Profile" title="Your traveller profile" text="How your name and photo appear to your travel crew." />
    <form className="card" onSubmit={save} style={{ maxWidth: 620 }}>
      <div className="row" style={{ gap: 16, marginBottom: 20 }}>
        <Avatar name={name || user.name} id={user.id} src={img || undefined} size="lg" />
        <div>
          <h2>{user.name}</h2>
          <div className="muted">{user.email} {user.status && <span className="pill success" style={{ marginLeft: 6 }}>{user.status.toLowerCase()}</span>}</div>
        </div>
      </div>
      <div className="field"><label>Name</label><input value={name} onChange={(e) => setName(e.target.value)} minLength={2} maxLength={100} required /></div>
      <div className="field"><label>Profile image URL</label><input type="url" placeholder="https://…" value={img} onChange={(e) => setImg(e.target.value)} /></div>
      <div className="row"><button className="accent">Save changes</button> <span className="muted">{msg}</span></div>
    </form>

    <div className="card danger-zone" style={{ maxWidth: 620 }}>
      <h3>Delete account</h3>
      <p className="muted" style={{ marginTop: 0 }}>Your past expenses stay in your trips (shown as 'Former Member') so everyone's balances remain correct. Everything personal is removed.</p>
      {deleteError && <div className="error">{deleteError}</div>}
      <button className="danger" onClick={deleteAccount}>Delete my account</button>
    </div>
    </>
  )
}
