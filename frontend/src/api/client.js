import axios from 'axios'

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const tokens = {
  get access() { return localStorage.getItem('accessToken') },
  get refresh() { return localStorage.getItem('refreshToken') },
  set({ accessToken, refreshToken }) {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
  },
  clear() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  },
}

const client = axios.create({ baseURL: API_URL })

client.interceptors.request.use((config) => {
  if (tokens.access && !config.url.startsWith('/api/auth/')) {
    config.headers.Authorization = `Bearer ${tokens.access}`
  }
  return config
})

// The backend has no custom entry point, so an expired token comes back as 403
// (not 401). Try a refresh once on either; if the retry still fails it is a real error.
let refreshing = null
client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const { response, config } = error
    const status = response?.status
    if ((status === 401 || status === 403) && !config._retry && tokens.refresh
        && !config.url.startsWith('/api/auth/')) {
      config._retry = true
      try {
        refreshing ??= axios
          .post(`${API_URL}/api/auth/refresh`, { refreshToken: tokens.refresh })
          .finally(() => { refreshing = null })
        const { data } = await refreshing
        tokens.set(data)
        config.headers.Authorization = `Bearer ${data.accessToken}`
        return client(config)
      } catch {
        tokens.clear()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export const errMsg = (e) =>
  e?.response?.data?.message || e?.response?.data?.error || e?.message || 'Something went wrong'

export default client
