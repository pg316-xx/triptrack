import { useEffect, useRef, useState } from 'react'
import { places } from '../api'
import { IconPin } from './icons'

// Text input with place suggestions (backend proxies OpenStreetMap / Google Places).
// Typing freely still works: picking a suggestion also fills in coordinates.
//   value: { location, latitude, longitude }
export default function LocationInput({ value, onChange, placeholder = 'Location', id }) {
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const timer = useRef(null)
  const wrap = useRef(null)
  const text = value?.location || ''

  useEffect(() => {
    const close = (e) => { if (wrap.current && !wrap.current.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const type = (t) => {
    onChange({ location: t, latitude: null, longitude: null })
    clearTimeout(timer.current)
    if (t.trim().length < 3) { setResults([]); return }
    // debounce: one request after the user pauses typing
    timer.current = setTimeout(() => {
      places.search(t.trim()).then((r) => { setResults(r); setOpen(true); setActive(-1) }).catch(() => setResults([]))
    }, 450)
  }

  const pick = (p) => {
    onChange({ location: p.name || p.address, latitude: p.latitude, longitude: p.longitude })
    setOpen(false)
  }

  const key = (e) => {
    if (!open || results.length === 0) return
    if (e.key === 'ArrowDown') { e.preventDefault(); setActive((a) => Math.min(a + 1, results.length - 1)) }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setActive((a) => Math.max(a - 1, 0)) }
    else if (e.key === 'Enter' && active >= 0) { e.preventDefault(); pick(results[active]) }
    else if (e.key === 'Escape') setOpen(false)
  }

  return (
    <div className="loc-input" ref={wrap}>
      <input id={id} placeholder={placeholder} value={text} autoComplete="off"
        role="combobox" aria-expanded={open} aria-autocomplete="list"
        onChange={(e) => type(e.target.value)} onFocus={() => results.length && setOpen(true)} onKeyDown={key} />
      {value?.latitude != null && <span className="loc-pinned" title="Coordinates saved"><IconPin size={14} /></span>}
      {open && results.length > 0 && (
        <ul className="loc-menu" role="listbox">
          {results.map((p, i) => (
            <li key={`${p.latitude},${p.longitude},${i}`} role="option" aria-selected={i === active}
              className={i === active ? 'active' : ''} onMouseDown={(e) => { e.preventDefault(); pick(p) }}>
              <strong>{p.name}</strong>
              <span>{p.address}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
