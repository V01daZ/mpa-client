import { exec } from 'node:child_process'

const REG_PATH = 'HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings'

function run(cmd: string): Promise<void> {
  return new Promise((resolve, reject) => {
    exec(cmd, (error) => {
      if (error) reject(error)
      else resolve()
    })
  })
}

/**
 * Рассылает WM_SETTINGCHANGE, чтобы браузеры и другие приложения
 * подхватили новые настройки прокси без перезапуска.
 */
async function refreshSystemSettings(): Promise<void> {
  try {
    await run('RUNDLL32.EXE user32.dll,UpdatePerUserSystemParameters ,1 ,True')
  } catch {
    // Не критично, если рассылка не сработала - часть приложений
    // подхватит прокси и так.
  }
}

export async function enableSystemProxy(host: string, port: number): Promise<void> {
  if (process.platform !== 'win32') return
  await run(`reg add "${REG_PATH}" /v ProxyServer /t REG_SZ /d "${host}:${port}" /f`)
  await run(`reg add "${REG_PATH}" /v ProxyEnable /t REG_DWORD /d 1 /f`)
  await refreshSystemSettings()
}

export async function disableSystemProxy(): Promise<void> {
  if (process.platform !== 'win32') return
  await run(`reg add "${REG_PATH}" /v ProxyEnable /t REG_DWORD /d 0 /f`)
  await refreshSystemSettings()
}
