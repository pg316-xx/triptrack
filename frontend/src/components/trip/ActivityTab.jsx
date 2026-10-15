import { useEffect, useState } from 'react'
import { trips } from '../../api'
import { errMsg } from '../../api/client'
import { Avatar, Spinner } from '../ui'

export default function ActivityTab({ tripId }) {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    trips.auditLogs(tripId, page).then(setData).catch((e) => setError(errMsg(e)))
  }, [tripId, page])

  if (error) return <div className="error">{error} <span className="muted">(audit logs may be restricted to admins)</span></div>
  if (!data) return <Spinner />

  return (
    <div className="card">
      <h3>Activity log</h3>
      {data.content.length === 0 && <p className="muted">No activity yet.</p>}
      <div className="table-wrap"><table>
        <tbody>
          {data.content.map((l) => (
            <tr key={l.id}>
              <td className="muted">{new Date(l.createdAt).toLocaleString()}</td>
              <td><div className="row" style={{ flexWrap: 'nowrap' }}><Avatar name={l.userName} /> {l.userName}</div></td>
              <td><span className="pill">{l.action.toLowerCase()}</span> <span className="muted">{l.entityType?.toLowerCase()}</span></td>
            </tr>
          ))}
        </tbody>
      </table></div>
      <div className="row" style={{ marginTop: 8 }}>
        <button className="secondary small" disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
        <span className="muted">Page {page + 1} / {Math.max(data.totalPages, 1)}</span>
        <button className="secondary small" disabled={data.last} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </div>
  )
}
