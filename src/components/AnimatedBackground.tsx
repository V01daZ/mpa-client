/**
 * Лёгкий анимированный фон: два размытых пятна (медь/тил) плавно
 * перемещаются по затемнённому холсту. Чистый CSS, без JS-цикла -
 * не нагружает CPU/GPU так, как могло бы canvas-решение.
 */
export function AnimatedBackground() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <div
        className="bg-blob bg-blob-1 h-72 w-72 opacity-25"
        style={{ background: 'var(--color-accent)', top: '-4rem', left: '-4rem' }}
      />
      <div
        className="bg-blob bg-blob-2 h-80 w-80 opacity-20"
        style={{ background: 'var(--color-connected)', bottom: '-5rem', right: '-5rem' }}
      />
    </div>
  )
}
