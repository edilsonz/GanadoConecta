const formato = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 3 })

export function GraficoPeso({ mediciones }: { mediciones: { id: string; kilos: number; fecha: string }[] }) {
  const dias = mediciones.map(m => Date.parse(m.fecha))
  const minimo = mediciones.reduce((a, m) => Math.min(a, m.kilos), Infinity)
  const maximo = mediciones.reduce((a, m) => Math.max(a, m.kilos), 0)
  const amplitud = dias[dias.length - 1] - dias[0]
  const puntos = mediciones.map((m, i) => [
    amplitud ? 16 + 288 * (dias[i] - dias[0]) / amplitud : 160,
    maximo === minimo ? 55 : 96 - 82 * (m.kilos - minimo) / (maximo - minimo),
  ])
  return <figure style={{ margin: 0 }}>
    <svg viewBox="0 0 320 110" width="100%" role="img" aria-label="Evolución del peso según fecha de medición">
      <polyline points={puntos.map(p => p.join(',')).join(' ')} fill="none" stroke="#386650" strokeWidth="2" />
      {puntos.map(([x, y], i) => <circle key={mediciones[i].id} cx={x} cy={y} r="3" fill="#386650" />)}
    </svg>
    <figcaption>{mediciones[0].fecha} — {mediciones[mediciones.length - 1].fecha} ·
      Rango {formato.format(minimo)} a {formato.format(maximo)} kg</figcaption>
  </figure>
}
