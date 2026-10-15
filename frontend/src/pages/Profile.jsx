import { useState } from 'react'
import { users } from '../api'
import { useAuth } from '../context/AuthContext'
import { errMsg } from '../api/client'
import { Avatar } from '../components/ui'
import PageBanner from '../components/PageBanner'

export default function Profile() {
  const { user, reloadUser } = useAuth()
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
    </>
  )
}
