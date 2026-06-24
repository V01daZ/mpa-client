import { app, BrowserWindow, Menu, Tray, nativeImage } from 'electron'
import { connect, disconnect, getStatus } from './connection'
import { getActiveProfileId, getProfiles } from './store'

export function createTray(win: BrowserWindow, iconPath: string): Tray {
  const icon = nativeImage.createFromPath(iconPath)
  const tray = new Tray(icon.isEmpty() ? icon : icon.resize({ width: 16, height: 16 }))
  tray.setToolTip('MPA')

  const rebuildMenu = (): void => {
    const status = getStatus()
    const isConnected = status.state === 'connected'

    const menu = Menu.buildFromTemplate([
      {
        label: isConnected ? 'Отключиться' : 'Подключиться',
        enabled: status.state !== 'connecting' && status.state !== 'disconnecting',
        click: async () => {
          if (isConnected) {
            await disconnect()
          } else {
            const activeId = getActiveProfileId() ?? getProfiles()[0]?.id
            if (activeId) await connect(activeId)
          }
          rebuildMenu()
        },
      },
      { type: 'separator' },
      {
        label: 'Открыть MPA',
        click: () => {
          win.show()
          win.focus()
        },
      },
      {
        label: 'Выход',
        click: () => {
          app.quit()
        },
      },
    ])

    tray.setContextMenu(menu)
  }

  rebuildMenu()

  tray.on('click', () => {
    win.show()
    win.focus()
  })

  // Перестраиваем меню трея при каждом изменении статуса подключения,
  // чтобы пункт "Подключиться/Отключиться" был актуальным.
  const interval = setInterval(rebuildMenu, 2000)
  win.on('closed', () => clearInterval(interval))

  return tray
}
