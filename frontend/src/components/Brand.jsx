import { Link } from 'react-router-dom'
import { Logo } from './icons'

export default function Brand({ to = '/', light = false }) {
  return (
    <Link to={to} className="brand" style={light ? { color: '#fff' } : undefined}>
      <Logo size={42} />
      <span>
        <span className="name" style={{ display: 'block' }}>TripNest</span>
        <span className="tagline" style={{ display: 'block' }}>Plan. Split. Explore.</span>
      </span>
    </Link>
  )
}
