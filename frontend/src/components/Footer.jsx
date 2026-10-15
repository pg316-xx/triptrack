import { Link } from 'react-router-dom'
import Brand from './Brand'

export default function Footer({ signedIn }) {
  return (
    <footer className="footer">
      <div className="footer-inner">
        <div>
          <Brand />
          <p>Plan trips together, split costs fairly, and keep every ticket and booking in one place.</p>
        </div>
        <div>
          <h4>{signedIn ? 'Your space' : 'Product'}</h4>
          {signedIn ? (
            <>
              <Link to="/">My trips</Link>
              <Link to="/wallet">Travel wallet</Link>
              <Link to="/invite">Join a trip</Link>
              <Link to="/notifications">Alerts</Link>
            </>
          ) : (
            <>
              <a href="#features">Features</a>
              <a href="#destinations">Destinations</a>
              <Link to="/register">Create account</Link>
              <Link to="/login">Log in</Link>
            </>
          )}
        </div>
        <div>
          <h4>Plan together</h4>
          <a href={signedIn ? '/' : '#features'}>Day-by-day itinerary</a>
          <a href={signedIn ? '/' : '#features'}>Split expenses</a>
          <a href={signedIn ? '/' : '#features'}>Group chat</a>
          <a href={signedIn ? '/wallet' : '#features'}>Shared documents</a>
        </div>
        <div>
          <h4>{signedIn ? 'Next adventure' : 'Start planning'}</h4>
          <p style={{ marginTop: 0, marginBottom: 14 }}>
            {signedIn ? 'Create a trip and invite your crew in seconds.' : 'Free to use. Invite friends with a single link.'}
          </p>
          <Link to={signedIn ? '/?new=1' : '/register'} className="btn accent small" style={{ color: '#fff', display: 'inline-flex' }}>
            {signedIn ? 'Plan a new trip' : 'Create free account'}
          </Link>
        </div>
      </div>
      <div className="copy">© {new Date().getFullYear()} TripNest. Made for people who travel together.</div>
    </footer>
  )
}
