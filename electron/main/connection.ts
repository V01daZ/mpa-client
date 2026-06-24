import { BrowserWindow } from 'electron'
import type { ConnectionStatus } from '../../shared/types'
import { IPC } from '../../shared/ipc'
import { getProfile, getTunEnabled } from './store'
import { startCore, stopCore } from './core'
import { MIXED_PROXY_PORT } from './coreConfig'
import { enableSystemProxy, disableSystemProxy } from './proxy'

let status: ConnectionStatus = { state: 'disconnected', profileId: null }

function setStatus(next: ConnectionStatus): void {
  status = next
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.StatusChanged, status)
  }
}

export function getStatus(): ConnectionStatus {
  return status
}

export async function connect(profileId: string): Promise<ConnectionStatus> {
  const profile = getProfile(profileId)
  if (!profile) {
    setStatus({ state: 'error', profileId: null, error: 'Профиль не найден' })
    return status
  }

  if (status.state === 'connected' || status.state === 'connecting') {
    await disconnect()
  }

  setStatus({ state: 'connecting', profileId })

  const tun = getTunEnabled()

  try {
    await startCore(profile, tun)
    if (!tun) {
      await enableSystemProxy('127.0.0.1', MIXED_PROXY_PORT)
    }
    setStatus({ state: 'connected', profileId, startedAt: Date.now() })
  } catch (err) {
    stopCore()
    await disableSystemProxy()
    setStatus({
      state: 'error',
      profileId: null,
      error: err instanceof Error ? err.message : String(err),
    })
  }

  return status
}

export async function disconnect(): Promise<ConnectionStatus> {
  setStatus({ state: 'disconnecting', profileId: status.profileId })
  stopCore()
  await disableSystemProxy()
  setStatus({ state: 'disconnected', profileId: null })
  return status
}

/**
 * Переключение TUN-режима. Если есть активное подключение - переподключаемся
 * с новым режимом, чтобы изменение применилось немедленно.
 */
export async function setTunMode(
  enabled: boolean,
  persist: (value: boolean) => void,
): Promise<ConnectionStatus> {
  const wasConnectedProfileId = status.state === 'connected' ? status.profileId : null

  if (status.state === 'connected' || status.state === 'connecting') {
    await disconnect()
  }

  persist(enabled)

  if (wasConnectedProfileId) {
    await connect(wasConnectedProfileId)
  }

  return status
}
