import { BrowserWindow } from 'electron'
import { IPC } from '../../shared/ipc'
import { getProfiles, updateProfile } from './store'
import { refetchProfile } from './subscription'
import { connect, getStatus } from './connection'

const REFRESH_INTERVAL_MS = 60 * 60 * 1000 // раз в час

export function startAutoRefresh(): void {
  setInterval(() => {
    void refreshAllProfiles()
  }, REFRESH_INTERVAL_MS)
}

/**
 * Обновляет все профили, добавленные по подписке/ключу активации.
 * Профили, добавленные как обычная vless-ссылка, не трогаем -
 * у них нет источника для обновления.
 */
export async function refreshAllProfiles(): Promise<void> {
  const profiles = getProfiles()
  let changed = false

  for (const profile of profiles) {
    if (profile.sourceType !== 'subscription' && profile.sourceType !== 'activation') {
      continue
    }

    try {
      const fresh = await refetchProfile(profile)
      if (!fresh) continue

      updateProfile(profile.id, fresh)
      changed = true

      const status = getStatus()
      if (status.state === 'connected' && status.profileId === profile.id) {
        // Переподключаемся, чтобы применить свежие параметры (UUID, Reality-ключи и т.п.)
        await connect(profile.id)
      }
    } catch (err) {
      console.error(`[auto-refresh] профиль ${profile.id}:`, err)
    }
  }

  if (changed) {
    for (const win of BrowserWindow.getAllWindows()) {
      win.webContents.send(IPC.ProfilesChanged)
    }
  }
}
