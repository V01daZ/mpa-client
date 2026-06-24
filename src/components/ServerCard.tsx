import { Trash2, Wifi } from 'lucide-react'
import type { ServerProfile } from '../../shared/types'

interface ServerCardProps {
  profile: ServerProfile
  isActive: boolean
  ping: number | null | undefined
  onSelect: () => void
  onRemove: () => void
}

export function ServerCard({ profile, isActive, ping, onSelect, onRemove }: ServerCardProps) {
  const pingLabel = ping === undefined ? '...' : ping === null ? '—' : `${ping} мс`

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') onSelect()
      }}
      className={`group flex cursor-pointer items-center gap-3 rounded-xl border px-3 py-2.5 transition-colors ${
        isActive
          ? 'border-accent bg-accent-soft'
          : 'border-border bg-surface hover:bg-surface-hover'
      }`}
    >
      <span className={`h-2.5 w-2.5 flex-shrink-0 rounded-full ${isActive ? 'bg-accent' : 'bg-border'}`} />

      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium text-text">{profile.name}</div>
        <div className="truncate font-mono text-xs text-text-muted">
          {profile.address}:{profile.port}
        </div>
      </div>

      <div className="flex items-center gap-1 font-mono text-xs text-text-muted">
        <Wifi className="h-3.5 w-3.5" />
        {pingLabel}
      </div>

      <button
        type="button"
        onClick={(event) => {
          event.stopPropagation()
          onRemove()
        }}
        aria-label="Удалить сервер"
        className="rounded-lg p-1.5 text-text-muted opacity-0 transition-opacity hover:bg-error-soft hover:text-error group-hover:opacity-100"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  )
}
