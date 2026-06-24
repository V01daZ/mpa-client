import type { ConnectionStatus, ServerProfile } from './types'

export interface MpaApi {
  getProfiles: () => Promise<ServerProfile[]>
  addProfileFromUri: (uri: string) => Promise<ServerProfile>
  removeProfile: (id: string) => Promise<void>
  getActiveProfileId: () => Promise<string | null>
  setActiveProfile: (id: string) => Promise<void>
  pingProfile: (id: string) => Promise<number | null>
  getStatus: () => Promise<ConnectionStatus>
  connect: (id: string) => Promise<ConnectionStatus>
  disconnect: () => Promise<ConnectionStatus>
  onStatusChanged: (callback: (status: ConnectionStatus) => void) => () => void
  onProfilesChanged: (callback: () => void) => () => void
  getTunEnabled: () => Promise<boolean>
  setTunEnabled: (enabled: boolean) => Promise<ConnectionStatus>
}
