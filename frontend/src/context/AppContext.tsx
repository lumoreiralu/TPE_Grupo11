import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { getClienteId, setClienteId as persistClienteId } from '../lib/storage'

interface AppContextValue {
  clienteId: string
  changeClienteId: (value: string) => void
}

const AppContext = createContext<AppContextValue | null>(null)

export function AppProvider({ children }: { children: ReactNode }) {
  const [clienteId, setClienteIdState] = useState<string>(() => getClienteId())

  const changeClienteId = useCallback((value: string) => {
    setClienteIdState(persistClienteId(value))
  }, [])

  const value = useMemo(
    () => ({ clienteId, changeClienteId }),
    [clienteId, changeClienteId],
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
