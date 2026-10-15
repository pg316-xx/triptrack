import { useEffect, useState } from 'react'
import { trips as tripsApi, wallet } from '../api'
import { errMsg } from '../api/client'
import { Empty } from '../components/ui'
import PageBanner from '../components/PageBanner'

const TYPES = ['TICKET', 'CONFIRMATION', 'ID']
const TYPE_ICON = { TICKET: '🎫', CONFIRMATION: '🧾', ID: '🪪' }
const emptyForm = { title: '', itemType: 'TICKET', tripId: '', shared: false }

// Personal travel wallet: private by default. Linking an item to a trip and
// ticking "share" is the only way other trip members can see it.
export default function Wallet() {
  const [items, setItems] = useState([])
  const [myTrips, setMyTrips] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = () => wallet.list().then(setItems).catch((e) => setError(errMsg(e)))
  useEffect(() => {
    load()
    tripsApi.list().then(setMyTrips).catch(() => setMyTrips([]))
  }, [])

  const tripName = (id) => myTrips.find((t) => t.id === id)?.name || 'a trip'

  const run = async (fn) => {
    setError('')
    try { await fn(); await load() } catch (e) { setError(errMsg(e)) }
  }

  const add = (e) => {
    e.preventDefault()
    if (!file) return
    setBusy(true)
    run(async () => {
      await wallet.add(file, {
        title: form.title,
        itemType: form.itemType,
        tripId: form.tripId || null,
        shared: Boolean(form.tripId) && form.shared,
      })
      setForm(emptyForm)
      setFile(null)
      e.target.reset()
    }).finally(() => setBusy(false))
  }

  // PATCH applies every field, so always send the item's full metadata.
  const save = (item, changes) =>
    run(() => wallet.update(item.id, {
      title: item.title,
      itemType: item.itemType,
      tripId: item.tripId,
      shared: item.shared,
      ...changes,
    }))

  return (
    <>
      <PageBanner eyebrow="Travel wallet" title="Your tickets, all in one place"
        text="Boarding passes, booking confirmations and IDs. Private unless you share one with a trip."
        image="/images/story-backpacker.jpg" />
      {error && <div className="error">{error}</div>}

      <form className="card" onSubmit={add}>
        <h3>Add to wallet</h3>
        <div className="row" style={{ alignItems: 'flex-start' }}>
          <div className="field" style={{ flex: 2, minWidth: 180 }}><label>Title</label>
            <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} maxLength={200} required /></div>
          <div className="field" style={{ flex: 1, minWidth: 140 }}><label>Type</label>
            <select value={form.itemType} onChange={(e) => setForm({ ...form, itemType: e.target.value })}>
              {TYPES.map((t) => <option key={t} value={t}>{TYPE_ICON[t]} {t.toLowerCase()}</option>)}
            </select></div>
          <div className="field" style={{ flex: 1, minWidth: 160 }}><label>Trip (optional)</label>
            <select value={form.tripId} onChange={(e) => setForm({ ...form, tripId: e.target.value, shared: e.target.value ? form.shared : false })}>
              <option value="">— none —</option>
              {myTrips.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select></div>
        </div>
        <div className="row">
          <input type="file" accept="application/pdf,image/jpeg,image/png" style={{ width: 'auto' }} onChange={(e) => setFile(e.target.files[0] || null)} required />
          <label className="chip-check">
            <input type="checkbox" disabled={!form.tripId} checked={form.shared} onChange={(e) => setForm({ ...form, shared: e.target.checked })} />
            share with trip members
          </label>
          <button disabled={busy || !file}>{busy ? 'Uploading…' : 'Add'}</button>
        </div>
        <p className="muted" style={{ margin: '8px 0 0', fontSize: '0.8rem' }}>PDF, JPEG or PNG, up to 10 MB.</p>
      </form>

      <div className="card">
        {items.length === 0 && <Empty icon="👛" title="Your wallet is empty">Add a boarding pass, hotel confirmation or ID above.</Empty>}
        <div className="table-wrap"><table>
          <tbody>
            {items.map((it) => (
              <tr key={it.id}>
                <td style={{ width: 36, fontSize: '1.3rem' }}>{TYPE_ICON[it.itemType]}</td>
                <td>
                  <a href={it.fileUrl} target="_blank" rel="noreferrer"><strong>{it.title}</strong></a>
                  <div className="muted">{it.fileName} · {(it.fileSizeBytes / 1024).toFixed(0)} KB</div>
                </td>
                <td>
                  <select style={{ width: 'auto' }} value={it.tripId || ''}
                    onChange={(e) => save(it, { tripId: e.target.value || null, shared: e.target.value ? it.shared : false })}>
                    <option value="">no trip</option>
                    {myTrips.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
                  </select>
                </td>
                <td>
                  <label className="chip-check" title={it.tripId ? '' : 'Link a trip to share'}>
                    <input type="checkbox" disabled={!it.tripId} checked={it.shared} onChange={(e) => save(it, { shared: e.target.checked })} />
                    {it.shared ? `shared with ${tripName(it.tripId)}` : 'private'}
                  </label>
                </td>
                <td style={{ textAlign: 'right' }}>
                  <button className="danger small" aria-label="Delete item" onClick={() => confirm('Delete this wallet item?') && run(() => wallet.remove(it.id))}>✕</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </div>
    </>
  )
}
