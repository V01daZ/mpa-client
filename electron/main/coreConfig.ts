import net from 'node:net'
import type { ServerProfile } from '../../shared/types'

/**
 * Разбирает ссылку вида
 * vless://uuid@host:port?security=reality&pbk=...&fp=chrome&sni=...&sid=...&flow=...#Имя
 * в профиль сервера. Поддерживается только VLESS + Reality (как на сервере MPA).
 */
export function parseVlessUri(rawUri: string): Omit<ServerProfile, 'id'> {
  const trimmed = rawUri.trim()
  if (!trimmed.toLowerCase().startsWith('vless://')) {
    throw new Error('Ссылка должна начинаться с vless://')
  }

  const withoutScheme = trimmed.slice('vless://'.length)
  const hashIndex = withoutScheme.indexOf('#')
  const fragment = hashIndex >= 0 ? withoutScheme.slice(hashIndex + 1) : ''
  const withoutFragment = hashIndex >= 0 ? withoutScheme.slice(0, hashIndex) : withoutScheme

  const atIndex = withoutFragment.indexOf('@')
  if (atIndex < 0) {
    throw new Error('В ссылке отсутствует UUID пользователя')
  }
  const uuid = withoutFragment.slice(0, atIndex)
  const hostAndQuery = withoutFragment.slice(atIndex + 1)

  const queryIndex = hostAndQuery.indexOf('?')
  const hostPort = queryIndex >= 0 ? hostAndQuery.slice(0, queryIndex) : hostAndQuery
  const queryString = queryIndex >= 0 ? hostAndQuery.slice(queryIndex + 1) : ''

  const colonIndex = hostPort.lastIndexOf(':')
  if (colonIndex < 0) {
    throw new Error('Не указан порт сервера')
  }
  const address = hostPort.slice(0, colonIndex)
  const port = Number(hostPort.slice(colonIndex + 1))
  if (!address || !Number.isFinite(port)) {
    throw new Error('Не удалось разобрать адрес или порт сервера')
  }

  const params = new URLSearchParams(queryString)
  const security = params.get('security') ?? ''
  if (security !== 'reality') {
    throw new Error('Поддерживаются только конфигурации VLESS с Reality')
  }

  const publicKey = params.get('pbk') ?? ''
  if (!publicKey) {
    throw new Error('В ссылке отсутствует публичный ключ Reality (pbk)')
  }

  const name = fragment ? decodeURIComponent(fragment) : `${address}:${port}`

  return {
    name,
    address,
    port,
    uuid,
    flow: params.get('flow') ?? 'xtls-rprx-vision',
    network: 'tcp',
    security: 'reality',
    publicKey,
    shortId: params.get('sid') ?? '',
    serverName: params.get('sni') ?? address,
    fingerprint: params.get('fp') ?? 'chrome',
  }
}

export const MIXED_PROXY_PORT = 10808
export const TUN_INTERFACE_NAME = 'MPA'
export const TUN_ADDRESS = '172.19.0.1/30'

export interface CoreConfigOptions {
  tun: boolean
}

/**
 * Собирает конфиг sing-box для одного профиля.
 *
 * tun = false: локальный mixed (SOCKS+HTTP) вход на 127.0.0.1:10808,
 *              приложение само включает системный HTTP-прокси на этот порт.
 * tun = true:  TUN-интерфейс с auto_route - sing-box сам настраивает
 *              таблицу маршрутизации и DNS, весь трафик системы идёт
 *              через VPN без ручной настройки прокси.
 */
export function buildCoreConfig(profile: ServerProfile, options: CoreConfigOptions): object {
  const isServerIp = net.isIP(profile.address) !== 0

  const outboundProxy = {
    type: 'vless',
    tag: 'proxy',
    server: profile.address,
    server_port: profile.port,
    uuid: profile.uuid,
    flow: profile.flow || undefined,
    tls: {
      enabled: true,
      server_name: profile.serverName,
      utls: {
        enabled: true,
        fingerprint: profile.fingerprint || 'chrome',
      },
      reality: {
        enabled: true,
        public_key: profile.publicKey,
        short_id: profile.shortId,
      },
    },
  }

  const inbounds = options.tun
    ? [
        {
          type: 'tun',
          tag: 'tun-in',
          interface_name: TUN_INTERFACE_NAME,
          address: [TUN_ADDRESS],
          mtu: 1500,
          auto_route: true,
          strict_route: true,
          stack: 'system',
        },
      ]
    : [
        {
          type: 'mixed',
          tag: 'mixed-in',
          listen: '127.0.0.1',
          listen_port: MIXED_PROXY_PORT,
        },
      ]

  return {
    log: { level: 'warn', timestamp: true },
    dns: {
      servers: [
        { tag: 'dns-remote', type: 'https', server: '1.1.1.1', detour: 'proxy' },
        { tag: 'dns-direct', type: 'local' },
      ],
      rules: isServerIp
        ? []
        : [{ domain: [profile.address], server: 'dns-direct' }],
      final: 'dns-remote',
    },
    inbounds,
    outbounds: [outboundProxy, { type: 'direct', tag: 'direct' }],
    route: {
      default_domain_resolver: 'dns-direct',
      auto_detect_interface: true,
      final: 'proxy',
      rules: [
        { action: 'sniff' },
        { protocol: 'dns', action: 'hijack-dns' },
        { ip_is_private: true, outbound: 'direct' },
      ],
    },
  }
}
