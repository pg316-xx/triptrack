import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { auth, users } from '../api'
import { tokens } from '../api/client'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(!!tokens.access)

  const loadMe = useCallback(async () => {
    try {
      setUser(await users.me())
    } catch {
      tokens.clear()
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (tokens.access) loadMe()
  }, [loadMe])

  const login = async (email, password) => {
    tokens.set(await auth.login({ email, password }))
    await loadMe()
  }

  const register = async (name, email, password) => {
    // register already returns a token pair; no second (BCrypt-costly) login needed
    tokens.set(await auth.register({ name, email, password }))
    await loadMe()
  }

  const logout = async () => {
    try {
      if (tokens.refresh) await auth.logout(tokens.refresh)
    } catch { /* ignore */ }
    tokens.clear()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, reloadUser: loadMe }}>
      {children}
    </AuthContext.Provider>
  )
}
