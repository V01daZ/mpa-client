interface TunToggleProps {
  enabled: boolean
  disabled?: boolean
  onChange: (enabled: boolean) => void
}

export function TunToggle({ enabled, disabled, onChange }: TunToggleProps) {
  return (
    <div
      className={`flex items-center justify-between gap-3 rounded-xl border border-border bg-surface px-3 py-2.5 ${
        disabled ? 'opacity-60' : ''
      }`}
    >
      <div className="text-sm font-medium text-text">TUN-режим</div>
      <button
        type="button"
        role="switch"
        aria-checked={enabled}
        aria-label="TUN-режим"
        disabled={disabled}
        onClick={() => onChange(!enabled)}
        className={`relative h-6 w-11 flex-shrink-0 rounded-full transition-colors ${
          enabled ? 'bg-accent' : 'bg-border'
        } disabled:cursor-not-allowed`}
      >
        <span
          className={`absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-ink transition-transform ${
            enabled ? 'translate-x-5' : 'translate-x-0'
          }`}
        />
      </button>
    </div>
  )
}
