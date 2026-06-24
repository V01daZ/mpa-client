import { useState } from 'react'
import { X } from 'lucide-react'

interface AddServerDialogProps {
  onClose: () => void
  onAdd: (uri: string) => Promise<void>
}

export function AddServerDialog({ onClose, onAdd }: AddServerDialogProps) {
  const [uri, setUri] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async () => {
    const trimmed = uri.trim()
    if (!trimmed) return

    setSubmitting(true)
    setError(null)
    try {
      await onAdd(trimmed)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="absolute inset-0 z-20 flex items-center justify-center bg-ink/85 p-6">
      <div className="w-full max-w-sm rounded-2xl border border-border bg-surface p-5 shadow-xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-display text-base font-semibold">Добавить сервер</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Закрыть"
            className="rounded-lg p-1 text-text-muted hover:bg-surface-hover hover:text-text"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <label className="mb-1 block text-xs font-medium uppercase tracking-wide text-text-muted">
          Ссылка или ключ
        </label>
        <textarea
          value={uri}
          onChange={(event) => setUri(event.target.value)}
          rows={4}
          placeholder="vless://... / ссылка на подписку / ключ активации"
          className="w-full resize-none rounded-xl border border-border bg-ink px-3 py-2 font-mono text-xs text-text outline-none focus:border-accent"
        />

        {error && <p className="mt-2 text-xs leading-5 text-error">{error}</p>}

        <button
          type="button"
          onClick={handleSubmit}
          disabled={submitting || !uri.trim()}
          className="mt-4 w-full rounded-xl bg-accent px-4 py-2 text-sm font-semibold text-ink transition-opacity disabled:opacity-50"
        >
          {submitting ? 'Добавление...' : 'Добавить'}
        </button>

        <p className="mt-3 text-xs leading-5 text-text-muted">
          Сюда можно вставить ссылку <code>vless://...</code>, ссылку на
          подписку или короткий ключ активации от бота - формат
          определится автоматически. Профили из подписки/ключа обновляются
          раз в час сами.
        </p>
      </div>
    </div>
  )
}
