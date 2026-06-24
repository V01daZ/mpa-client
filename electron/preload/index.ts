import { contextBridge, ipcRenderer } from 'electron'
import type { IpcRendererEvent } from 'electron'
import { IPC } from '../../shared/ipc'
import type { ConnectionStatus, ServerProfile } from '../../shared/types'
import type { MpaApi } from '../../shared/api'

const api: MpaApi = {
  getProfiles: (): Promise<ServerProfile[]> => ipcRenderer.invoke(IPC.GetProfiles),

  addProfileFromUri: (uri: string): Promise<ServerProfile> =>
    ipcRenderer.invoke(IPC.AddProfileFromUri, uri),

  removeProfile: (id: string): Promise<void> => ipcRenderer.invoke(IPC.RemoveProfile, id),

  getActiveProfileId: (): Promise<string | null> => ipcRenderer.invoke(IPC.GetActiveProfile),

  setActiveProfile: (id: string): Promise<void> => ipcRenderer.invoke(IPC.SetActiveProfile, id),

  pingProfile: (id: string): Promise<number | null> => ipcRenderer.invoke(IPC.PingProfile, id),

  getStatus: (): Promise<ConnectionStatus> => ipcRenderer.invoke(IPC.GetStatus),

  connect: (id: string): Promise<ConnectionStatus> => ipcRenderer.invoke(IPC.Connect, id),

  disconnect: (): Promise<ConnectionStatus> => ipcRenderer.invoke(IPC.Disconnect),

  onStatusChanged: (callback: (status: ConnectionStatus) => void): (() => void) => {
    const listener = (_event: IpcRendererEvent, status: ConnectionStatus) => callback(status)
    ipcRenderer.on(IPC.StatusChanged, listener)
    return () => ipcRenderer.removeListener(IPC.StatusChanged, listener)
  },

  onProfilesChanged: (callback: () => void): (() => void) => {
    const listener = () => callback()
    ipcRenderer.on(IPC.ProfilesChanged, listener)
    return () => ipcRenderer.removeListener(IPC.ProfilesChanged, listener)
  },

  getTunEnabled: (): Promise<boolean> => ipcRenderer.invoke(IPC.GetTunEnabled),

  setTunEnabled: (enabled: boolean): Promise<ConnectionStatus> =>
    ipcRenderer.invoke(IPC.SetTunEnabled, enabled),
}

contextBridge.exposeInMainWorld('mpa', api)
