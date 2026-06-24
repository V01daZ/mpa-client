import net from 'node:net'

/**
 * Измеряет время установления TCP-соединения до host:port.
 * Возвращает null, если сервер недоступен или не ответил за timeoutMs.
 */
export function tcpPing(host: string, port: number, timeoutMs = 2000): Promise<number | null> {
  return new Promise((resolve) => {
    const start = Date.now()
    const socket = new net.Socket()
    let done = false

    const finish = (result: number | null) => {
      if (done) return
      done = true
      socket.destroy()
      resolve(result)
    }

    socket.setTimeout(timeoutMs)
    socket.once('connect', () => finish(Date.now() - start))
    socket.once('timeout', () => finish(null))
    socket.once('error', () => finish(null))

    socket.connect(port, host)
  })
}
