import { useEffect, useState } from 'react'
import { files, wallet } from '../../api'
import { errMsg } from '../../api/client'
import { Empty } from '../ui'

const DOC_TYPES = ['FLIGHT_TICKET', 'HOTEL_BOOKING', 'PASSPORT', 'VISA', 'INSURANCE', 'OTHER']

export default function FilesTab({ tripId }) {
  const [docs, setDocs] = useState([])
  const [media, setMedia] = useState([])
  const [shared, setShared] = useState([])
  const [error, setError] = useState('')
  const [docType, setDocType] = useState('OTHER')
  const [caption, setCaption] = useState('')

  const load = async () => {
    try {
      const [d, m, w] = await Promise.all([files.documents(tripId), files.media(tripId), wallet.sharedWithTrip(tripId)])
      setDocs(d); setMedia(m); setShared(w)
    } catch (e) { setError(errMsg(e)) }
  }
  useEffect(() => { load() }, [tripId])

  const run = async (fn) => {
    setError('')
    try { await fn(); await load() } catch (e) { setError(errMsg(e)) }
  }

  const uploadDoc = (e) => {
    const file = e.target.files[0]
    if (file) run(() => files.uploadDocument(tripId, file, docType))
    e.target.value = ''
  }

  const uploadMedia = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const type = file.type.startsWith('video') ? 'VIDEO' : 'PHOTO'
    run(() => files.uploadMedia(tripId, file, type, caption || undefined))
    e.target.value = ''
    setCaption('')
  }

  return (
    <>
      {error && <div className="error">{error}</div>}

      <div className="card">
        <h3>📄 Documents</h3>
        <div className="row" style={{ marginBottom: 8 }}>
          <select style={{ width: 'auto' }} value={docType} onChange={(e) => setDocType(e.target.value)}>
            {DOC_TYPES.map((t) => <option key={t} value={t}>{t.replace('_', ' ').toLowerCase()}</option>)}
          </select>
          <input type="file" style={{ width: 'auto' }} onChange={uploadDoc} />
        </div>
        {docs.length === 0 && <p className="muted">No documents yet — tickets, bookings and visas go here.</p>}
        <div className="table-wrap"><table>
          <tbody>
            {docs.map((d) => (
              <tr key={d.id}>
                <td><a href={d.fileUrl} target="_blank" rel="noreferrer">{d.fileName}</a></td>
                <td><span className="pill">{d.documentType.replace('_', ' ').toLowerCase()}</span></td>
                <td className="muted">{(d.fileSizeBytes / 1024).toFixed(0)} KB</td>
                <td style={{ textAlign: 'right' }}>
                  <button className="danger small" onClick={() => run(() => files.deleteDocument(tripId, d.id))}>✕</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </div>


      <div className="card">
        <h3>👛 Shared from wallets</h3>
        {shared.length === 0 && <p className="muted">Nothing shared yet. Members can share tickets or confirmations from their Wallet.</p>}
        <div className="table-wrap"><table>
          <tbody>
            {shared.map((w) => (
              <tr key={w.id}>
                <td><a href={w.fileUrl} target="_blank" rel="noreferrer">{w.title}</a></td>
                <td><span className="pill">{w.itemType.toLowerCase()}</span></td>
                <td className="muted">from {w.ownerName}</td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </div>
      <div className="card">
        <h3>📸 Photos & videos</h3>
        <div className="row" style={{ marginBottom: 8 }}>
          <input placeholder="Caption (optional)" style={{ width: 200 }} value={caption} onChange={(e) => setCaption(e.target.value)} />
          <input type="file" accept="image/*,video/*" style={{ width: 'auto' }} onChange={uploadMedia} />
        </div>
        {media.length === 0 && <Empty icon="📷" title="No photos yet">Upload memories from the trip.</Empty>}
        <div className="gallery">
          {media.map((m) => (
            <div key={m.id} className="tile">
              {m.mediaType === 'VIDEO' ? <video src={m.fileUrl} controls /> : <img src={m.fileUrl} alt={m.caption || ''} />}
              <div className="row between">
                <span className="muted" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1 }}>{m.caption}</span>
                <button className="danger small" onClick={() => run(() => files.deleteMedia(tripId, m.id))}>✕</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
