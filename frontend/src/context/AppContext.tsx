import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  clearAuthToken,
  getAuthToken,
  getClienteId,
  setAuthToken,
  setClienteId as persistClienteId,
} from '../lib/storage'

const USER_KEY = 'ticketya_user'
const ROLES_KEY = 'ticketya_roles'

function getStoredUser(): string | null {
  return localStorage.getItem(USER_KEY)
}

function getStoredRoles(): string[] {
  const raw = localStorage.getItem(ROLES_KEY)
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((r): r is string => typeof r === 'string') : []
  } catch {
    return []
  }
}

interface LoginPayload {
  token: string
  username?: string
  roles?: string[]
  clienteId?: string
}

interface AppContextValue {
  clienteId: string
  username: string | null
  roles: string[]
  token: string | null
  isAuthenticated: boolean
  changeClienteId: (value: string) => void
  login: (payload: LoginPayload | string, newClienteId?: string) => void
  logout: () => void
}

const AppContext = createContext<AppContextValue | null>(null)

export function AppProvider({ children }: { children: ReactNode }) {
  const [clienteId, setClienteIdState] = useState<string>(() => getClienteId())
  const [token, setTokenState] = useState<string | null>(() => getAuthToken())
  const [username, setUsernameState] = useState<string | null>(() => getStoredUser())
  const [roles, setRolesState] = useState<string[]>(() => getStoredRoles())

  const changeClienteId = useCallback((value: string) => {
    setClienteIdState(persistClienteId(value))
  }, [])

  const login = useCallback((payload: LoginPayload | string, newClienteId?: string) => {
    const normalized: LoginPayload =
      typeof payload === 'string'
        ? { token: payload, clienteId: newClienteId }
        : payload

    setTokenState(setAuthToken(normalized.token))

    if (normalized.username) {
      localStorage.setItem(USER_KEY, normalized.username)
      setUsernameState(normalized.username)
    }

    if (normalized.roles) {
      localStorage.setItem(ROLES_KEY, JSON.stringify(normalized.roles))
      setRolesState(normalized.roles)
    }

    if (normalized.clienteId) {
      setClienteIdState(persistClienteId(normalized.clienteId))
    }
  }, [])

  const logout = useCallback(() => {
    clearAuthToken()
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(ROLES_KEY)
    setTokenState(null)
    setUsernameState(null)
    setRolesState([])
  }, [])

  const value = useMemo(
    () => ({
      clienteId,
      username,
      roles,
      token,
      isAuthenticated: Boolean(token),
      changeClienteId,
      login,
      logout,
    }),
    [clienteId, username, roles, token, changeClienteId, login, logout],
  )

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp(): AppContextValue {
  const context = useContext(AppContext)
  if (!context) {
    throw new Error('useApp debe usarse dentro de AppProvider')
  }
  return context
}