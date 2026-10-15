import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import { Spinner } from './components/ui'
import ErrorBoundary from './components/ErrorBoundary'
import Login from './pages/Login'
import Register from './pages/Register'
import Trips from './pages/Trips'
import TripDetail from './pages/TripDetail'
import Invite from './pages/Invite'
import Profile from './pages/Profile'
import Notifications from './pages/Notifications'
import Wallet from './pages/Wallet'

function Protected({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <Spinner />
  if (!user) return <Navigate to="/login" replace />
  return (
    <>
      <Navbar />
      <main className="container"><ErrorBoundary>{children}</ErrorBoundary></main>
    </>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<Protected><Trips /></Protected>} />
      <Route path="/trips/:tripId" element={<Protected><TripDetail /></Protected>} />
      <Route path="/invite" element={<Protected><Invite /></Protected>} />
      <Route path="/profile" element={<Protected><Profile /></Protected>} />
      <Route path="/notifications" element={<Protected><Notifications /></Protected>} />
      <Route path="/wallet" element={<Protected><Wallet /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
