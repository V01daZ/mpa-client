import { app, BrowserWindow, ipcMain, Menu } from 'electron'
import type { IpcMainInvokeEvent } from 'electron'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

import { loadEnv } from './env'
import { IPC } from '../../shared/ipc'
import {
  getProfiles,
  addProfile,
  removeProfile,
  getActiveProfileId,
  setActiveProfileId,
  getTunEnabled,
  setTunEnabled,
} from './store'
import { resolveProfileInput } from './subscription'
import { startAutoRefresh } from './autoRefresh'
import { connect, disconnect, getStatus, setTunMode } from './connection'
import { tcpPing } from './ping'
import { createTray } from './tray'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// Подхватываем .env из корня проекта (в dev) или из ресурсов приложения
// (в собранном виде) - сюда кладётся MPA_ACTIVATION_API, чтобы не хардкодить
// домен сервера в исходниках.
loadEnv(path.join(__dirname, '../..'))

// The built directory structure
//
// ├─┬ dist-electron
// │ ├─┬ main
// │ │ └── index.js    > Electron-Main
// │ └─┬ preload
// │   └── index.mjs   > Preload-Scripts
// ├─┬ dist
// │ └── index.html    > Electron-Renderer
process.env.APP_ROOT = path.join(__dirname, '../..')

export const MAIN_DIST = path.join(process.env.APP_ROOT, 'dist-electron')
export const RENDERER_DIST = path.join(process.env.APP_ROOT, 'dist')
export const VITE_DEV_SERVER_URL = process.env.VITE_DEV_SERVER_URL

process.env.VITE_PUBLIC = VITE_DEV_SERVER_URL
  ? path.join(process.env.APP_ROOT, 'public')
  : RENDERER_DIST

if (process.platform === 'win32') app.setAppUserModelId('MPA')

// Убираем стандартную полосу меню (File/Edit/View/Window) - не нужна
// для компактного однооконного приложения.
Menu.setApplicationMenu(null)

// Автозапуск с Windows: добавляется в реестр автозагрузки самим Electron,
// с флагом --autostart, чтобы при таком старте окно не мешало под глазами
// и сразу включался VPN (см. ниже).
if (process.platform === 'win32' && app.isPackaged) {
  const current = app.getLoginItemSettings()
  if (!current.openAtLogin) {
    app.setLoginItemSettings({
      openAtLogin: true,
      args: ['--autostart'],
    })
  }
}

const isAutostart = process.argv.includes('--autostart')

if (!app.requestSingleInstanceLock()) {
  app.quit()
  process.exit(0)
}

declare global {
  // eslint-disable-next-line no-var
  var __mpaQuitting: boolean | undefined
}

let win: BrowserWindow | null = null
const preload = path.join(__dirname, '../preload/index.mjs')
const indexHtml = path.join(RENDERER_DIST, 'index.html')

function createWindow(): void {
  win = new BrowserWindow({
    title: 'MPA',
    width: 380,
    height: 640,
    minWidth: 360,
    minHeight: 560,
    backgroundColor: '#12161C',
    show: !isAutostart,
    icon: path.join(process.env.VITE_PUBLIC as string, 'favicon.ico'),
    webPreferences: {
      preload,
    },
  })

  if (VITE_DEV_SERVER_URL) {
    win.loadURL(VITE_DEV_SERVER_URL)
    win.webContents.openDevTools({ mode: 'detach' })
  } else {
    win.loadFile(indexHtml)
  }

  // Подключение должно жить, даже если окно закрыто крестиком -
  // сворачиваем в трей вместо полного выхода.
  win.on('close', (event) => {
    if (!globalThis.__mpaQuitting) {
      event.preventDefault()
      win?.hide()
    }
  })

  createTray(win, path.join(process.env.VITE_PUBLIC as string, 'favicon.ico'))
}

app.whenReady().then(async () => {
  createWindow()
  startAutoRefresh()

  if (isAutostart) {
    const targetId = getActiveProfileId() ?? getProfiles()[0]?.id
    if (targetId) {
      // При запуске вместе с Windows всегда поднимаем TUN-режим -
      // это и есть смысл автозапуска: полноценный VPN без лишних кликов.
      if (!getTunEnabled()) setTunEnabled(true)
      await connect(targetId)
    }
  }
})

app.on('before-quit', () => {
  globalThis.__mpaQuitting = true
})

app.on('window-all-closed', () => {
  win = null
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  } else {
    win?.show()
  }
})

// --- IPC: профили ---

ipcMain.handle(IPC.GetProfiles, () => getProfiles())

ipcMain.handle(IPC.AddProfileFromUri, async (_event: IpcMainInvokeEvent, uri: string) => {
  const { profile, source } = await resolveProfileInput(uri)
  return addProfile({
    ...profile,
    sourceType: source.type,
    sourceUrl: source.type === 'vless' ? undefined : source.url,
    activationKey: source.type === 'activation' ? source.key : undefined,
    updatedAt: Date.now(),
  })
})

ipcMain.handle(IPC.RemoveProfile, (_event: IpcMainInvokeEvent, id: string) => {
  removeProfile(id)
})

ipcMain.handle(IPC.GetActiveProfile, () => getActiveProfileId())

ipcMain.handle(IPC.SetActiveProfile, (_event: IpcMainInvokeEvent, id: string) => {
  setActiveProfileId(id)
})

ipcMain.handle(IPC.PingProfile, async (_event: IpcMainInvokeEvent, id: string) => {
  const profile = getProfiles().find((p) => p.id === id)
  if (!profile) return null
  return tcpPing(profile.address, profile.port)
})

// --- IPC: подключение ---

ipcMain.handle(IPC.GetStatus, () => getStatus())

ipcMain.handle(IPC.Connect, (_event: IpcMainInvokeEvent, id: string) => connect(id))

ipcMain.handle(IPC.Disconnect, () => disconnect())

// --- IPC: настройки ---

ipcMain.handle(IPC.GetTunEnabled, () => getTunEnabled())

ipcMain.handle(IPC.SetTunEnabled, (_event: IpcMainInvokeEvent, enabled: boolean) =>
  setTunMode(enabled, setTunEnabled),
)
