import { useCallback, useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import type { ConnectionState, ConnectionStatus, ServerProfile } from '../shared/types'
import { ConnectButton } from './components/ConnectButton'
import { ServerCard } from './components/ServerCard'
import { AddServerDialog } from './components/AddServerDialog'
import { TunToggle } from './components/TunToggle'
import { AnimatedBackground } from './components/AnimatedBackground'

const STATUS_LABEL: Record<ConnectionState, string> = {
  disconnected: 'Отключено',
  connecting: 'Подключение...',
  connected: 'Подключено',
  disconnecting: 'Отключение...',
  error: 'Ошибка',
}

const STATUS_PILL_CLASS: Record<ConnectionState, string> = {
  disconnected: 'bg-surface text-text-muted',
  connecting: 'bg-accent-soft text-accent',
  connected: 'bg-connected-soft text-connected',
  disconnecting: 'bg-accent-soft text-accent',
  error: 'bg-error-soft text-error',
}

function App() {
  const [profiles, setProfiles] = useState<ServerProfile[]>([])
  const [activeProfileId, setActiveProfileId] = useState<string | null>(null)
  const [status, setStatus] = useState<ConnectionStatus>({ state: 'disconnected', profileId: null })
  const [pings, setPings] = useState<Record<string, number | null>>({})
  const [dialogOpen, setDialogOpen] = useState(false)
  const [tunEnabled, setTunEnabledState] = useState(false)

  const refreshPings = useCallback((list: ServerProfile[]) => {
    for (const profile of list) {
      void window.mpa.pingProfile(profile.id).then((ms) => {
        setPings((prev) => ({ ...prev, [profile.id]: ms }))
      })
    }
  }, [])

  useEffect(() => {
    let unsubscribeStatus: (() => void) | undefined
    let unsubscribeProfiles: (() => void) | undefined

    void (async () => {
      const [loadedProfiles, activeId, currentStatus, tun] = await Promise.all([
        window.mpa.getProfiles(),
        window.mpa.getActiveProfileId(),
        window.mpa.getStatus(),
        window.mpa.getTunEnabled(),
      ])
      setProfiles(loadedProfiles)
      setActiveProfileId(activeId)
      setStatus(currentStatus)
      setTunEnabledState(tun)
      refreshPings(loadedProfiles)

      unsubscribeStatus = window.mpa.onStatusChanged(setStatus)
      unsubscribeProfiles = window.mpa.onProfilesChanged(() => {
        void window.mpa.getProfiles().then((next) => {
          setProfiles(next)
          refreshPings(next)
        })
      })
    })()

    return () => {
      unsubscribeStatus?.()
      unsubscribeProfiles?.()
    }
  }, [refreshPings])

  useEffect(() => {
    if (profiles.length === 0) return
    const interval = setInterval(() => refreshPings(profiles), 15000)
    return () => clearInterval(interval)
  }, [profiles, refreshPings])

  const activeProfile = profiles.find((p) => p.id === activeProfileId)

  const handleToggleConnection = useCallback(async () => {
    if (status.state === 'connected' || status.state === 'connecting') {
      await window.mpa.disconnect()
      return
    }
    const targetId = activeProfileId ?? profiles[0]?.id
    if (!targetId) {
      setDialogOpen(true)
      return
    }
    await window.mpa.connect(targetId)
  }, [status.state, activeProfileId, profiles])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Enter') return
      const target = event.target as HTMLElement | null
      const isTyping = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA'
      if (isTyping || dialogOpen) return
      void handleToggleConnection()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [handleToggleConnection, dialogOpen])

  const handleSelectProfile = async (id: string) => {
    setActiveProfileId(id)
    await window.mpa.setActiveProfile(id)
  }

  const handleAddProfile = async (uri: string) => {
    const profile = await window.mpa.addProfileFromUri(uri)
    setProfiles((prev) => [...prev, profile])
    if (!activeProfileId) {
      setActiveProfileId(profile.id)
      await window.mpa.setActiveProfile(profile.id)
    }
    refreshPings([profile])
  }

  const handleRemoveProfile = async (id: string) => {
    await window.mpa.removeProfile(id)
    setProfiles((prev) => prev.filter((p) => p.id !== id))
    if (activeProfileId === id) {
      const next = profiles.find((p) => p.id !== id) ?? null
      setActiveProfileId(next?.id ?? null)
    }
  }

  const handleToggleTun = async (enabled: boolean) => {
    setTunEnabledState(enabled)
    await window.mpa.setTunEnabled(enabled)
  }

  const description =
    status.state === 'error' && status.error
      ? status.error
      : status.state === 'connected' && activeProfile
        ? activeProfile.name
        : activeProfile
          ? activeProfile.name
          : 'Нет выбранного сервера'

  return (
    <div className="relative flex h-screen flex-col overflow-hidden bg-ink font-sans text-text">
      <AnimatedBackground />

      <header className="relative z-10 flex items-center justify-between px-5 pt-5">
        <span className="font-display text-lg font-semibold tracking-wide">MPA</span>
        <span className={`rounded-full px-3 py-1 text-xs font-medium ${STATUS_PILL_CLASS[status.state]}`}>
          {STATUS_LABEL[status.state]}
        </span>
      </header>

      <main className="relative z-10 flex flex-1 flex-col items-center justify-center gap-4 px-6">
        <ConnectButton state={status.state} onToggle={handleToggleConnection} />
        <p className="max-w-[260px] text-center text-sm leading-5 text-text-muted">
          {description}
        </p>
        <div className="w-full max-w-[260px]">
          <TunToggle
            enabled={tunEnabled}
            disabled={status.state === 'connecting' || status.state === 'disconnecting'}
            onChange={handleToggleTun}
          />
        </div>
      </main>

      <section className="relative z-10 flex flex-col gap-2 border-t border-border bg-ink/60 px-4 py-4 backdrop-blur-sm">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-medium uppercase tracking-wide text-text-muted">Серверы</span>
          <button
            type="button"
            onClick={() => setDialogOpen(true)}
            className="flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-accent hover:bg-accent-soft"
          >
            <Plus className="h-3.5 w-3.5" />
            Добавить
          </button>
        </div>

        <div className="flex max-h-48 flex-col gap-2 overflow-y-auto">
          {profiles.length === 0 && (
            <p className="px-1 py-2 text-sm leading-5 text-text-muted">
              Серверов пока нет - добавь первый, нажав «Добавить», и вставь
              ссылку vless:// от бота.
            </p>
          )}
          {profiles.map((profile) => (
            <ServerCard
              key={profile.id}
              profile={profile}
              isActive={profile.id === activeProfileId}
              ping={pings[profile.id]}
              onSelect={() => handleSelectProfile(profile.id)}
              onRemove={() => handleRemoveProfile(profile.id)}
            />
          ))}
        </div>
      </section>

      {dialogOpen && <AddServerDialog onClose={() => setDialogOpen(false)} onAdd={handleAddProfile} />}
    </div>
  )
}

export default App
