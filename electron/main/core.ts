import { app } from 'electron'
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import type { ServerProfile } from '../../shared/types'
import { buildCoreConfig } from './coreConfig'

let proc: ChildProcessWithoutNullStreams | null = null

function getCoreDir(): string {
  // В собранном приложении ресурсы лежат в resources/core рядом с asar,
  // в режиме разработки - в resources/core в корне проекта.
  return app.isPackaged
    ? path.join(process.resourcesPath, 'core')
    : path.join(app.getAppPath(), 'resources', 'core')
}

function getBinaryPath(): string {
  const binName = process.platform === 'win32' ? 'sing-box.exe' : 'sing-box'
  return path.join(getCoreDir(), binName)
}

function getConfigPath(): string {
  return path.join(app.getPath('userData'), 'core-config.json')
}

export function isRunning(): boolean {
  return proc !== null
}

export async function startCore(profile: ServerProfile, tun: boolean): Promise<void> {
  if (proc) {
    throw new Error('sing-box уже запущен')
  }

  const binaryPath = getBinaryPath()
  if (!fs.existsSync(binaryPath)) {
    throw new Error(
      `Не найден sing-box по пути "${binaryPath}". ` +
        'Запусти "npm run build" (или "npm run dev") - бинарник скачивается ' +
        'автоматически скриптом scripts/fetch-core.mjs.',
    )
  }

  const config = buildCoreConfig(profile, { tun })
  const configPath = getConfigPath()
  fs.mkdirSync(path.dirname(configPath), { recursive: true })
  fs.writeFileSync(configPath, JSON.stringify(config, null, 2), 'utf-8')

  await new Promise<void>((resolve, reject) => {
    const child = spawn(binaryPath, ['run', '-c', configPath], {
      // wintun.dll должен лежать рядом с sing-box.exe - запускаем из той же папки
      cwd: getCoreDir(),
      windowsHide: true,
    })

    let settled = false
    let lastOutput = ''

    const onOutput = (data: Buffer) => {
      const text = data.toString()
      lastOutput += text
      if (!settled && /sing-box started/i.test(text)) {
        settled = true
        resolve()
      }
    }

    child.stdout.on('data', onOutput)
    child.stderr.on('data', onOutput)

    child.on('error', (err) => {
      proc = null
      if (!settled) {
        settled = true
        reject(err)
      }
    })

    child.on('exit', (code) => {
      proc = null
      if (!settled) {
        settled = true
        const tail = lastOutput.trim().split('\n').slice(-6).join('\n')
        reject(new Error(`sing-box завершился с кодом ${code}${tail ? `:\n${tail}` : ''}`))
      }
    })

    // Старые версии могут не печатать "started" - считаем успехом,
    // если процесс не упал в первые 2 секунды.
    setTimeout(() => {
      if (!settled) {
        settled = true
        resolve()
      }
    }, 2000)

    proc = child
  })
}

export function stopCore(): void {
  if (proc) {
    proc.kill()
    proc = null
  }
}
