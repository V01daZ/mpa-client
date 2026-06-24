import { Loader2, Power } from 'lucide-react'
import type { ConnectionState } from '../../shared/types'

interface ConnectButtonProps {
  state: ConnectionState
  onToggle: () => void
}

const SIZE = 192
const STROKE = 4
const RADIUS = (SIZE - STROKE) / 2
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

export function ConnectButton({ state, onToggle }: ConnectButtonProps) {
  const isConnected = state === 'connected'
  const isBusy = state === 'connecting' || state === 'disconnecting'
  const isError = state === 'error'

  const ringColorClass = isError
    ? 'stroke-error'
    : isConnected
      ? 'stroke-connected'
      : isBusy
        ? 'stroke-accent'
        : 'stroke-border'

  return (
    <button
      type="button"
      onClick={onToggle}
      disabled={isBusy}
      aria-label={isConnected ? 'Отключиться' : 'Подключиться'}
      className="group relative flex h-48 w-48 items-center justify-center rounded-full bg-surface transition-colors hover:bg-surface-hover disabled:cursor-not-allowed"
    >
      <svg
        viewBox={`0 0 ${SIZE} ${SIZE}`}
        width={SIZE}
        height={SIZE}
        className={`absolute inset-0 -rotate-90 ${isConnected ? 'glow-connected' : ''}`}
      >
        <circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          fill="none"
          strokeWidth={STROKE}
          strokeLinecap="round"
          className={`${ringColorClass} ${isBusy ? 'animate-spin-ring' : ''}`}
          strokeDasharray={isBusy ? `${CIRCUMFERENCE * 0.18} ${CIRCUMFERENCE * 0.12}` : undefined}
        />
      </svg>

      <div className="flex flex-col items-center gap-2">
        {isBusy ? (
          <Loader2 className="h-10 w-10 animate-spin text-accent" />
        ) : (
          <Power
            className={`h-10 w-10 transition-colors ${
              isConnected
                ? 'text-connected'
                : isError
                  ? 'text-error'
                  : 'text-text-muted group-hover:text-text'
            }`}
          />
        )}
        <span className="font-display text-xs font-semibold uppercase tracking-[0.2em] text-text-muted">
          {isConnected ? 'выкл' : 'вкл'}
        </span>
      </div>
    </button>
  )
}
