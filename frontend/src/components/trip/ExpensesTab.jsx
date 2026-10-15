import { useEffect, useState } from 'react'
import { expenses } from '../../api'
import { errMsg } from '../../api/client'
import { Avatar, Empty, money } from '../ui'

const CATEGORIES = ['FOOD', 'TRANSPORT', 'HOTEL', 'SHOPPING', 'ACTIVITY', 'OTHER']
const CATEGORY_ICON = { FOOD: '🍽️', TRANSPORT: '🚕', HOTEL: '🏨', SHOPPING: '🛍️', ACTIVITY: '🎟️', OTHER: '📦' }

export default function ExpensesTab({ tripId, members, user, canManage }) {
  const [list, setList] = useState([])
  const [partial, setPartial] = useState(false)
  const [balances, setBalances] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [settlements, setSettlements] = useState([])
  const [error, setError] = useState('')
  const [form, setForm] = useState({ title: '', description: '', category: 'FOOD', amount: '' })
  const [participants, setParticipants] = useState(members.map((m) => m.userId))
  const [splitMode, setSplitMode] = useState('equal') // 'equal' | 'custom'
  const [custom, setCustom] = useState({}) // userId -> amount string

  // Work in cents so the "remaining" check matches the backend's exact rule.
  const cents = (v) => Math.round(Number(v || 0) * 100)
  const customRows = members.filter((m) => cents(custom[m.userId]) > 0)
  const remainingCents = cents(form.amount) - customRows.reduce((s, m) => s + cents(custom[m.userId]), 0)

  const nameOf = (id) => members.find((m) => m.userId === id)?.name || 'Unknown'

  const load = async () => {
    try {
      const [e, b, s, st] = await Promise.all([
        expenses.list(tripId),
        expenses.balances(tripId),
        expenses.suggestions(tripId),
        expenses.settlements(tripId),
      ])
      setList(e.content); setPartial(!e.last); setBalances(b); setSuggestions(s); setSettlements(st)
    } catch (err) {
      setError(errMsg(err))
    }
  }
  useEffect(() => { load() }, [tripId])

  const run = async (fn) => {
    setError('')
    try { await fn(); await load() } catch (e) { setError(errMsg(e)) }
  }

  const toggle = (id) =>
    setParticipants((p) => (p.includes(id) ? p.filter((x) => x !== id) : [...p, id]))

  const total = list.reduce((sum, x) => sum + Number(x.amount), 0)
  const iPaid = list.filter((x) => x.paidByUserId === user.id).reduce((sum, x) => sum + Number(x.amount), 0)
  // positive = others owe me, negative = I owe others
  const net = balances.reduce(
    (sum, b) => sum + (b.toUserId === user.id ? Number(b.amount) : 0) - (b.fromUserId === user.id ? Number(b.amount) : 0),
    0,
  )

  const add = (e) => {
    e.preventDefault()
    const body = { ...form, amount: Number(form.amount) }
    if (splitMode === 'equal') body.participantIds = participants
    else body.splits = customRows.map((m) => ({ userId: m.userId, amount: Number(custom[m.userId]) }))
    run(async () => {
      await expenses.create(tripId, body)
      setForm({ title: '', description: '', category: 'FOOD', amount: '' })
      setCustom({})
    })
  }

  return (
    <>
      {error && <div className="error">{error}</div>}

      <div className="stats">
        <div className="stat"><div className="label">Total spent{partial ? ' (latest 50)' : ''}</div><div className="value">{money(total)}</div></div>
        <div className="stat"><div className="label">You paid</div><div className="value">{money(iPaid)}</div></div>
        <div className="stat">
          <div className="label">{net > 0 ? 'You are owed' : net < 0 ? 'You owe' : 'Your balance'}</div>
          <div className={`value ${net > 0 ? 'pos' : net < 0 ? 'neg' : ''}`}>{net === 0 ? 'Settled ✓' : money(Math.abs(net))}</div>
        </div>
      </div>

      <form className="card" onSubmit={add}>
        <h3>Add expense <span className="muted" style={{ fontWeight: 400 }}>· you paid</span></h3>
        <div className="row" style={{ alignItems: 'flex-start' }}>
          <div className="field" style={{ flex: 2, minWidth: 180 }}><label>Title</label><input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></div>
          <div className="field" style={{ flex: 1, minWidth: 110 }}><label>Amount (₹)</label><input type="number" min="0.01" step="0.01" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required /></div>
          <div className="field" style={{ flex: 1, minWidth: 140 }}>
            <label>Category</label>
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{CATEGORY_ICON[c]} {c.toLowerCase()}</option>)}
            </select>
          </div>
        </div>
        <div className="field"><label>Description</label><input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
        <div className="row" style={{ marginBottom: 8 }}>
          <label style={{ margin: 0 }}>Split</label>
          <label className="chip-check"><input type="radio" name="splitMode" checked={splitMode === 'equal'} onChange={() => setSplitMode('equal')} /> equally</label>
          <label className="chip-check"><input type="radio" name="splitMode" checked={splitMode === 'custom'} onChange={() => setSplitMode('custom')} /> by amount</label>
        </div>
        {splitMode === 'equal' ? (
          <div className="row" style={{ marginBottom: 12 }}>
            {members.map((m) => (
              <label key={m.userId} className="chip-check">
                <input type="checkbox" checked={participants.includes(m.userId)} onChange={() => toggle(m.userId)} />
                {m.name}
              </label>
            ))}
          </div>
        ) : (
          <div style={{ marginBottom: 12 }}>
            {members.map((m) => (
              <div key={m.userId} className="row" style={{ marginBottom: 6, flexWrap: 'nowrap' }}>
                <span style={{ flex: 1, minWidth: 0 }}>{m.name}</span>
                <input type="number" min="0" step="0.01" placeholder="0.00" style={{ width: 120 }}
                  value={custom[m.userId] || ''} onChange={(e) => setCustom({ ...custom, [m.userId]: e.target.value })} />
              </div>
            ))}
            {/* splits must add up to the total exactly (backend rejects INVALID_SPLIT_SUM) */}
            <div className={remainingCents === 0 ? 'muted' : 'text-danger'}>
              {remainingCents === 0 ? 'Splits add up ✓' : `${money(Math.abs(remainingCents) / 100)} ${remainingCents > 0 ? 'left to assign' : 'over the total'}`}
            </div>
          </div>
        )}
        <button disabled={splitMode === 'equal' ? participants.length === 0 : remainingCents !== 0 || customRows.length === 0}>Add expense</button>
      </form>

      <div className="grid">
        <div className="card">
          <h3>Who owes whom</h3>
          {balances.length === 0 && <p className="muted">All settled 🎉</p>}
          {balances.map((b, i) => (
            <div key={i} className="row between" style={{ padding: '6px 0' }}>
              <span className="row" style={{ gap: 6 }}>
                <Avatar name={b.fromUserName} id={b.fromUserId} /> <strong>{b.fromUserName}</strong>
                <span className="muted">owes</span> {b.toUserName}
              </span>
              <span className="amount">{money(b.amount)}</span>
            </div>
          ))}
        </div>
        <div className="card">
          <h3>Suggested settlements</h3>
          {suggestions.length === 0 && <p className="muted">Nothing to settle.</p>}
          {suggestions.map((s, i) => (
            <div key={i} className="row between" style={{ marginBottom: 6 }}>
              <span>{s.fromUserName} → {s.toUserName} <span className="amount">{money(s.amount)}</span></span>
              {s.fromUserId === user.id && (
                <button className="small" onClick={() => run(() => expenses.settle(tripId, { paidToUserId: s.toUserId, amount: s.amount }))}>
                  Mark paid
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <h3>Expenses</h3>
        {list.length === 0 && <Empty icon="💸" title="No expenses yet">Add the first one above.</Empty>}
        <div className="table-wrap"><table>
          <tbody>
            {list.map((x) => (
              <tr key={x.id}>
                <td style={{ width: 36, fontSize: '1.3rem' }}>{CATEGORY_ICON[x.category] || '📦'}</td>
                <td>
                  <strong>{x.title}</strong><div className="muted">{x.description}</div>
                  {x.splits?.length > 0 && (
                    <div className="muted" style={{ fontSize: '0.8rem' }}>
                      {x.splits.map((s) => `${s.userId === user.id ? 'you' : s.userName} ${money(s.amount)}`).join(' · ')}
                    </div>
                  )}
                </td>
                <td className="muted">paid by {x.paidByUserId === user.id ? 'you' : nameOf(x.paidByUserId)}</td>
                <td className="amount">{money(x.amount)}</td>
                <td style={{ textAlign: 'right' }}>
                  {(x.paidByUserId === user.id || canManage) && (
                    <button className="danger small" onClick={() => confirm('Delete expense?') && run(() => expenses.remove(tripId, x.id))}>✕</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </div>

      {settlements.length > 0 && (
        <div className="card">
          <h3>Settlement history</h3>
          {settlements.map((s) => (
            <div key={s.id} className="row between" style={{ padding: '4px 0' }}>
              <span>✅ {nameOf(s.paidByUserId)} paid {nameOf(s.paidToUserId)} <span className="amount">{money(s.amount)}</span></span>
              <span className="muted">{new Date(s.createdAt).toLocaleDateString()}</span>
            </div>
          ))}
        </div>
      )}
    </>
  )
}
