export interface ServerProfile {
  id: string
  name: string
  address: string
  port: number
  uuid: string
  flow: string
  network: 'tcp'
  security: 'reality'
  publicKey: string
  shortId: string
  serverName: string
  fingerprint: string
  /** Как был добавлен профиль - влияет на то, можно ли его автообновлять. */
  sourceType?: 'vless' | 'subscription' | 'activation'
  /** Для subscription - сама ссылка подписки; для activation - resolved subscription_url. */
  sourceUrl?: string
  /** Для activation - исходный короткий ключ (чтобы перепроверять статус при обновлении). */
  activationKey?: string
  /** Когда последний раз успешно обновлялись данные из подписки. */
  updatedAt?: number
}

export type ConnectionState =
  | 'disconnected'
  | 'connecting'
  | 'connected'
  | 'disconnecting'
  | 'error'

export interface ConnectionStatus {
  state: ConnectionState
  profileId: string | null
  error?: string
  startedAt?: number
}
