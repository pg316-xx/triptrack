import { Fragment, useEffect, useRef, useState } from 'react'
import { chat } from '../../api'
import { connectSocket, sendChat } from '../../api/socket'
import { errMsg } from '../../api/client'
import { Avatar, Empty } from '../ui'

const dayKey = (d) => new Date(d).toDateString()
const dayLabel = (d) => {
  const key = dayKey(d)
  if (key === new Date().toDateString()) return 'Today'
  if (key === new Date(Date.now() - 86400000).toDateString()) return 'Yesterday'
  return new Date(d).toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' })
}

export default function ChatTab({ tripId, user }) {
  const [messages, setMessages] = useState([])
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const sockRef = useRef(null)
  const [connected, setConnected] = useState(false)
  const bottom = useRef(null)

  useEffect(() => {
    // history comes newest-first from the REST endpoint; show oldest-first
    chat.messages(tripId)
      .then((p) => setMessages([...p.content].reverse()))
      .catch((e) => setError(errMsg(e)))

    const sock = connectSocket([
      {
        topic: `/topic/trip/${tripId}/chat`,
        handler: (m) => setMessages((prev) => (prev.some((x) => x.id === m.id) ? prev : [...prev, m])),
      },
    ])
    sock.onConnect = ((orig) => (f) => { orig?.(f); setConnected(true) })(sock.onConnect)
    sock.onWebSocketClose = () => setConnected(false)
    sockRef.current = sock
    return () => { sock.deactivate() }
  }, [tripId])

  useEffect(() => { bottom.current?.scrollIntoView({ block: 'nearest' }) }, [messages])

  const send = async (e) => {
    e.preventDefault()
    if (!text.trim()) return
    try {
      if (sockRef.current?.connected) {
        sendChat(sockRef.current, tripId, text) // broadcast arrives via the topic subscription
      } else {
        const saved = await chat.send(tripId, text) // REST fallback
        setMessages((prev) => [...prev, saved])
      }
      setText('')
    } catch (err) {
      setError(errMsg(err))
    }
  }

  const remove = async (id) => {
    if (!confirm('Delete this message?')) return
    try {
      await chat.remove(tripId, id)
      setMessages((prev) => prev.filter((m) => m.id !== id))
    } catch (err) {
      setError(errMsg(err))
    }
  }

  return (
    <>
      {error && <div className="error">{error}</div>}
      <div className="card chat-card">
        <div className="chat-head">
          <strong>Trip chat</strong>
          <span className="muted">
            <span className={`dot ${connected ? 'on' : 'off'}`} />
            {connected ? 'Live' : 'Connecting… (sending via REST)'}
          </span>
        </div>
        <div className="chat">
          {messages.length === 0 && <Empty icon="💬" title="No messages yet">Say hi to your travel crew!</Empty>}
          {messages.map((m, i) => {
            const prev = messages[i - 1]
            const newDay = !prev || dayKey(prev.createdAt) !== dayKey(m.createdAt)
            const cont = !newDay && prev.senderId === m.senderId
            const mine = m.senderId === user.id
            return (
              <Fragment key={m.id}>
                {newDay && <div className="day-sep">{dayLabel(m.createdAt)}</div>}
                <div className={`msg-row ${mine ? 'mine' : ''} ${cont ? 'cont' : ''}`}>
                  {!mine && <div className="avatar-slot">{!cont && <Avatar name={m.senderName} id={m.senderId} />}</div>}
                  <div className="msg">
                    {!mine && !cont && <div className="who">{m.senderName}</div>}
                    <span>{m.message}</span>
                    <span className="time">{new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    {mine && <span className="del" role="button" title="Delete" onClick={() => remove(m.id)}>✕</span>}
                  </div>
                </div>
              </Fragment>
            )
          })}
          <div ref={bottom} />
        </div>
        <form className="chat-input" onSubmit={send}>
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Type a message…" aria-label="Message" />
          <button disabled={!text.trim()}>Send</button>
        </form>
      </div>
    </>
  )
}
