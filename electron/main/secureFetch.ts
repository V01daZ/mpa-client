import http from 'node:http'
import https from 'node:https'
import { URL } from 'node:url'

// У сервера MPA самоподписанный сертификат (как и у панели 3x-ui,
// откуда бот ходит с verify=False) - повторяем тот же подход здесь.
const insecureAgent = new https.Agent({ rejectUnauthorized: false })

export function fetchText(urlString: string, redirectsLeft = 5): Promise<string> {
  return new Promise((resolve, reject) => {
    let url: URL
    try {
      url = new URL(urlString)
    } catch {
      reject(new Error(`Некорректный URL: ${urlString}`))
      return
    }

    const lib = url.protocol === 'http:' ? http : https
    const options: https.RequestOptions = {
      method: 'GET',
      headers: { 'User-Agent': 'MPA-client' },
      agent: url.protocol === 'https:' ? insecureAgent : undefined,
    }

    const req = lib.request(url, options, (res) => {
      const status = res.statusCode ?? 0

      if (status >= 300 && status < 400 && res.headers.location && redirectsLeft > 0) {
        res.resume()
        const nextUrl = new URL(res.headers.location, url).toString()
        fetchText(nextUrl, redirectsLeft - 1).then(resolve, reject)
        return
      }

      if (status < 200 || status >= 300) {
        res.resume()
        reject(new Error(`HTTP ${status} для ${urlString}`))
        return
      }

      const chunks: Buffer[] = []
      res.on('data', (chunk: Buffer) => chunks.push(chunk))
      res.on('end', () => resolve(Buffer.concat(chunks).toString('utf-8')))
      res.on('error', reject)
    })

    req.on('error', reject)
    req.setTimeout(10000, () => req.destroy(new Error(`Таймаут запроса к ${urlString}`)))
    req.end()
  })
}

export async function fetchJson<T>(urlString: string): Promise<T> {
  const text = await fetchText(urlString)
  return JSON.parse(text) as T
}
