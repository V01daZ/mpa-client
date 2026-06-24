#!/usr/bin/env node
// Скачивает sing-box (Windows x64) и wintun.dll и кладёт их в resources/core,
// чтобы при `npm run dev` / `npm run build` не нужно было делать это руками.
//
// sing-box нужен и для обычного режима (mixed-прокси), и для TUN-режима.
// wintun.dll нужен только для TUN-режима, но качаем сразу, чтобы переключатель
// в приложении работал из коробки.

import { mkdir, rm, readdir } from 'node:fs/promises'
import { existsSync, createWriteStream } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { pipeline } from 'node:stream/promises'
import { Readable } from 'node:stream'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

const execFileAsync = promisify(execFile)

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.join(__dirname, '..')
const CORE_DIR = path.join(ROOT, 'resources', 'core')
const SING_BOX_EXE = path.join(CORE_DIR, 'sing-box.exe')
const WINTUN_DLL = path.join(CORE_DIR, 'wintun.dll')

const WINTUN_VERSION = '0.14.1'
const WINTUN_URL = `https://www.wintun.net/builds/wintun-${WINTUN_VERSION}.zip`

async function fetchJson(url) {
  const res = await fetch(url, {
    headers: { 'User-Agent': 'mpa-client-build', Accept: 'application/vnd.github+json' },
  })
  if (!res.ok) {
    throw new Error(`GitHub API вернул ${res.status} для ${url}`)
  }
  return res.json()
}

async function downloadFile(url, destPath) {
  const res = await fetch(url, { headers: { 'User-Agent': 'mpa-client-build' } })
  if (!res.ok || !res.body) {
    throw new Error(`Не удалось скачать ${url}: ${res.status}`)
  }
  await pipeline(Readable.fromWeb(res.body), createWriteStream(destPath))
}

async function extractZip(zipPath, destDir) {
  if (process.platform === 'win32') {
    await execFileAsync('powershell', [
      '-NoProfile',
      '-Command',
      `Expand-Archive -Force -Path "${zipPath}" -DestinationPath "${destDir}"`,
    ])
  } else {
    await execFileAsync('unzip', ['-o', zipPath, '-d', destDir])
  }
}

/** Рекурсивно ищет файл с заданным именем внутри директории (если она существует). */
async function findFile(dir, fileName) {
  let entries
  try {
    entries = await readdir(dir, { withFileTypes: true })
  } catch {
    return null
  }
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      const found = await findFile(full, fileName)
      if (found) return found
    } else if (entry.name.toLowerCase() === fileName.toLowerCase()) {
      return full
    }
  }
  return null
}

async function moveAndCleanup(foundPath, destPath, extractDir) {
  const { rename } = await import('node:fs/promises')
  await rename(foundPath, destPath)
  await rm(extractDir, { recursive: true, force: true })
}

async function ensureSingBox() {
  if (existsSync(SING_BOX_EXE)) {
    console.log('[fetch-core] sing-box.exe уже есть, пропускаю')
    return
  }

  console.log('[fetch-core] Ищу последний релиз sing-box на GitHub...')
  const release = await fetchJson('https://api.github.com/repos/SagerNet/sing-box/releases/latest')

  const asset = release.assets.find(
    (a) =>
      /windows-amd64\.zip$/i.test(a.name) &&
      !/legacy|arm/i.test(a.name),
  )

  if (!asset) {
    throw new Error(
      `Не нашёл сборку windows-amd64 в релизе ${release.tag_name} sing-box. ` +
        'Скачай вручную с https://github.com/SagerNet/sing-box/releases ' +
        'и положи sing-box.exe в resources/core/sing-box.exe',
    )
  }

  console.log(`[fetch-core] Скачиваю ${asset.name} (релиз ${release.tag_name})...`)
  await mkdir(CORE_DIR, { recursive: true })
  const zipPath = path.join(CORE_DIR, asset.name)
  await downloadFile(asset.browser_download_url, zipPath)

  const extractDir = path.join(CORE_DIR, '_sing-box-extract')
  await rm(extractDir, { recursive: true, force: true })
  await mkdir(extractDir, { recursive: true })

  console.log('[fetch-core] Распаковываю...')
  await extractZip(zipPath, extractDir)
  await rm(zipPath)

  const found = await findFile(extractDir, 'sing-box.exe')
  if (!found) {
    throw new Error('После распаковки sing-box.exe не найден в архиве')
  }
  await moveAndCleanup(found, SING_BOX_EXE, extractDir)

  console.log('[fetch-core] Готово: resources/core/sing-box.exe')

  if (process.env.MPA_UPX === '1') {
    await tryCompressWithUpx(SING_BOX_EXE)
  }
}

/**
 * Необязательное сжатие бинарника через UPX (~50-70% меньше размер файла).
 * Выключено по умолчанию: некоторые антивирусы расценивают UPX-упакованные
 * .exe как подозрительные (частая сигнатура у малвари-паковщиков), так что
 * включать стоит сознательно, через переменную окружения MPA_UPX=1, и
 * проверить итоговый .exe на virustotal перед раздачей пользователям.
 */
async function tryCompressWithUpx(exePath) {
  try {
    await execFileAsync('upx', ['--best', '--lzma', exePath])
    console.log('[fetch-core] sing-box.exe сжат через UPX')
  } catch {
    console.warn(
      '[fetch-core] UPX не найден или сжатие не удалось - пропускаю ' +
        '(установи UPX и добавь в PATH, чтобы включить сжатие: https://upx.github.io)',
    )
  }
}

async function ensureWintun() {
  if (existsSync(WINTUN_DLL)) {
    console.log('[fetch-core] wintun.dll уже есть, пропускаю')
    return
  }

  console.log(`[fetch-core] Скачиваю wintun ${WINTUN_VERSION}...`)
  await mkdir(CORE_DIR, { recursive: true })
  const zipPath = path.join(CORE_DIR, 'wintun.zip')

  try {
    await downloadFile(WINTUN_URL, zipPath)
  } catch (err) {
    console.warn(
      `[fetch-core] Не удалось скачать wintun.dll автоматически (${err.message}). ` +
        `Скачай вручную с https://www.wintun.net/, возьми wintun/bin/amd64/wintun.dll ` +
        `и положи в resources/core/wintun.dll - без него заработает только режим ` +
        `системного прокси, TUN-режим будет недоступен.`,
    )
    await rm(zipPath, { force: true })
    return
  }

  const extractDir = path.join(CORE_DIR, '_wintun-extract')
  await rm(extractDir, { recursive: true, force: true })
  await mkdir(extractDir, { recursive: true })

  console.log('[fetch-core] Распаковываю wintun...')
  await extractZip(zipPath, extractDir)
  await rm(zipPath)

  const found =
    (await findFile(path.join(extractDir, 'wintun', 'bin', 'amd64'), 'wintun.dll')) ??
    (await findFile(extractDir, 'wintun.dll'))

  if (!found) {
    console.warn('[fetch-core] wintun.dll не найден в архиве - TUN-режим будет недоступен')
    await rm(extractDir, { recursive: true, force: true })
    return
  }

  await moveAndCleanup(found, WINTUN_DLL, extractDir)
  console.log('[fetch-core] Готово: resources/core/wintun.dll')
}

async function main() {
  await ensureSingBox()
  await ensureWintun()
}

main().catch((err) => {
  console.error('[fetch-core] Ошибка:', err.message)
  process.exit(1)
})
