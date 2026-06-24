import type { ServerProfile } from '../../shared/types'
import { parseVlessUri } from './coreConfig'
import { fetchText, fetchJson } from './secureFetch'

// Должно совпадать с тем, что задеплоено в mpa-server-activation
// Адрес сервиса активации - задаётся при сборке/запуске через переменную
// окружения MPA_ACTIVATION_API (см. .env.example), чтобы не хардкодить
// домен сервера прямо в исходниках, которые лежат в открытом репозитории.
// Читаем процессное окружение лениво (а не в константу на этапе импорта
// модуля), чтобы не зависеть от порядка инициализации ESM-модулей -
// main/index.ts гарантированно вызывает loadEnv до первого реального
// запроса к этой функции.
function getActivationApiBase(): string {
  const value = process.env.MPA_ACTIVATION_API
  if (!value) {
    throw new Error(
      'Не задан MPA_ACTIVATION_API - скопируй .env.example в .env и впиши адрес сервиса активации',
    )
  }
  return value
}

export type ProfileSource =
  | { type: 'vless' }
  | { type: 'subscription'; url: string }
  | { type: 'activation'; key: string; url: string }

export interface ResolvedProfile {
  profile: Omit<ServerProfile, 'id'>
  source: ProfileSource
}

/**
 * Определяет формат вставленной строки и возвращает готовый профиль:
 * - vless://...                - прямая ссылка
 * - http(s)://...               - ссылка на подписку (base64 с vless-ссылками)
 * - число (например 4781726354019) - ключ активации, резолвится через
 *   /activate/<key> в ссылку на подписку
 */
export async function resolveProfileInput(rawInput: string): Promise<ResolvedProfile> {
  const input = rawInput.trim()

  if (input.toLowerCase().startsWith('vless://')) {
    return { profile: parseVlessUri(input), source: { type: 'vless' } }
  }

  if (/^https?:\/\//i.test(input)) {
    const profile = await fetchVlessFromSubscription(input)
    return { profile, source: { type: 'subscription', url: input } }
  }

  if (/^\d+$/.test(input)) {
    const subscriptionUrl = await resolveActivationKey(input)
    const profile = await fetchVlessFromSubscription(subscriptionUrl)
    return { profile, source: { type: 'activation', key: input, url: subscriptionUrl } }
  }

  throw new Error(
    'Не удалось распознать формат - вставь ссылку vless://, ссылку на подписку или ключ активации',
  )
}

/**
 * Повторно резолвит профиль по сохранённому источнику (для автообновления
 * и кнопки "Обновить подписку"). Для sourceType "vless" обновлять нечего -
 * возвращает null.
 */
export async function refetchProfile(
  profile: ServerProfile,
): Promise<Omit<ServerProfile, 'id'> | null> {
  if (profile.sourceType === 'activation' && profile.activationKey) {
    const subscriptionUrl = await resolveActivationKey(profile.activationKey)
    const fresh = await fetchVlessFromSubscription(subscriptionUrl)
    return { ...fresh, sourceType: 'activation', activationKey: profile.activationKey, sourceUrl: subscriptionUrl, updatedAt: Date.now() }
  }

  if (profile.sourceType === 'subscription' && profile.sourceUrl) {
    const fresh = await fetchVlessFromSubscription(profile.sourceUrl)
    return { ...fresh, sourceType: 'subscription', sourceUrl: profile.sourceUrl, updatedAt: Date.now() }
  }

  return null
}

async function resolveActivationKey(key: string): Promise<string> {
  try {
    const data = await fetchJson<{ subscription_url: string }>(`${getActivationApiBase()}/activate/${key}`)
    if (!data.subscription_url) {
      throw new Error('Сервер не вернул ссылку на подписку')
    }
    return data.subscription_url
  } catch (err) {
    if (err instanceof Error) {
      if (/HTTP 404/.test(err.message)) throw new Error('Ключ активации не найден')
      if (/HTTP 403/.test(err.message)) throw new Error('Ключ активации отозван')
    }
    throw err
  }
}

async function fetchVlessFromSubscription(url: string): Promise<Omit<ServerProfile, 'id'>> {
  const raw = await fetchText(url)
  const decoded = decodeSubscription(raw)
  const line = decoded
    .split(/\r?\n/)
    .map((l) => l.trim())
    .find((l) => l.toLowerCase().startsWith('vless://'))

  if (!line) {
    throw new Error('В подписке не найдено ни одной ссылки vless://')
  }

  return parseVlessUri(line)
}

function decodeSubscription(raw: string): string {
  const trimmed = raw.trim()
  if (/vless:\/\//i.test(trimmed)) {
    // Уже список ссылок в открытом виде
    return trimmed
  }
  try {
    return Buffer.from(trimmed, 'base64').toString('utf-8')
  } catch {
    return trimmed
  }
}
