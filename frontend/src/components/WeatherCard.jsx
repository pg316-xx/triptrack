import { useEffect, useState } from 'react'
import { places, weather } from '../api'

const ICON = { sun: '☀️', 'cloud-sun': '⛅', cloud: '☁️', fog: '🌫️', drizzle: '🌦️', rain: '🌧️', snow: '❄️', storm: '⛈️' }

// 5-day forecast for a trip's destination. The destination text is geocoded
// once, then the backend's cached weather is fetched. Failing quietly is fine:
// weather is a nice-to-have, never a blocker (SRS 7.11).
export default function WeatherCard({ destination }) {
  const [state, setState] = useState({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    setState({ status: 'loading' })
    places.search(destination)
      .then((results) => {
        if (!results.length) throw new Error('not found')
        const p = results[0]
        return weather.get(p.latitude, p.longitude).then((w) => ({ w, place: p }))
      })
      .then(({ w, place }) => { if (!cancelled) setState({ status: w.available ? 'ok' : 'none', w, place }) })
      .catch(() => { if (!cancelled) setState({ status: 'none' }) })
    return () => { cancelled = true }
  }, [destination])

  if (state.status === 'loading') return <div className="card weather"><span className="muted">Checking the weather in {destination}…</span></div>
  if (state.status === 'none') return null

  const { w, place } = state
  const day = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, { weekday: 'short' })

  return (
    <div className="card weather">
      <div className="weather-now">
        <span className="big-icon">{ICON[w.current?.icon] || '🌡️'}</span>
        <div>
          <div className="temp">{Math.round(w.current?.temperatureC)}°C</div>
          <div className="muted">{w.current?.condition} · wind {Math.round(w.current?.windKph)} km/h</div>
          <div className="muted" style={{ fontSize: '0.75rem' }}>
            {place.name}{w.stale ? ' · showing last known forecast' : ''}
          </div>
        </div>
      </div>
      <div className="weather-days">
        {w.daily.map((d) => (
          <div key={d.date} className="wday">
            <span className="muted">{day(d.date)}</span>
            <span className="wicon">{ICON[d.icon] || '🌡️'}</span>
            <strong>{Math.round(d.maxC)}°</strong>
            <span className="muted">{Math.round(d.minC)}°</span>
            {d.precipitationChance != null && d.precipitationChance > 0 && <span className="rain">💧{d.precipitationChance}%</span>}
          </div>
        ))}
      </div>
    </div>
  )
}
