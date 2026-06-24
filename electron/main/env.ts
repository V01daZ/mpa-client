import fs from 'node:fs'
import path from 'node:path'

/**
 * Загружает .env из корня проекта (если он есть) в process.env -
 * не перетирает уже заданные переменные (например, из окружения CI).
 * Минимальный парсер без зависимостей: KEY=VALUE построчно, # - комментарий.
 */
export function loadEnv(rootDir: string): void {
  const envPath = path.join(rootDir, '.env')
  if (!fs.existsSync(envPath)) return

  const content = fs.readFileSync(envPath, 'utf-8')
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue

    const eqIndex = line.indexOf('=')
    if (eqIndex === -1) continue

    const key = line.slice(0, eqIndex).trim()
    let value = line.slice(eqIndex + 1).trim()

    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }

    if (key && process.env[key] === undefined) {
      process.env[key] = value
    }
  }
}
