#!/usr/bin/env node
// Summarises a JMeter CSV results file (.jtl): per-sampler average, P95, throughput and error
// rate, plus the booking-conflict rate. Intentional 409s are labelled "(409 conflict)" by the
// test plan and pass its assertion, so they are reported as conflicts, not errors.
//
// Usage: node summarize-results.mjs results/results.jtl
import { readFileSync } from 'node:fs'

const file = process.argv[2]
if (!file) {
  console.error('Usage: node summarize-results.mjs <results.jtl>')
  process.exit(1)
}

const [header, ...lines] = readFileSync(file, 'utf8').trim().split(/\r?\n/)
const columns = header.split(',')
const col = (name) => columns.indexOf(name)
const [TS, ELAPSED, LABEL, CODE, SUCCESS] = ['timeStamp', 'elapsed', 'label', 'responseCode', 'success'].map(col)

const samples = lines.map((line) => {
  const cells = splitCsv(line)
  return {
    ts: Number(cells[TS]),
    elapsed: Number(cells[ELAPSED]),
    label: cells[LABEL],
    code: cells[CODE],
    success: cells[SUCCESS] === 'true',
  }
})

const byLabel = new Map()
for (const sample of samples) {
  if (sample.label.startsWith('Setup')) continue
  const key = sample.label.replace(' (409 conflict)', '')
  if (!byLabel.has(key)) byLabel.set(key, [])
  byLabel.get(key).push(sample)
}

const rows = [...byLabel.entries()].map(([label, group]) => summarize(label, group))
const all = [...byLabel.values()].flat()
rows.push(summarize('TOTAL', all))

console.table(
  Object.fromEntries(
    rows.map((row) => [
      row.label,
      {
        samples: row.count,
        'avg ms': row.avg,
        'p95 ms': row.p95,
        'req/s': row.throughput,
        'error %': row.errorRate,
        'conflict %': row.conflictRate,
      },
    ]),
  ),
)

const bookings = all.filter((sample) => sample.label.includes('Book'))
const booked = bookings.filter((sample) => sample.code === '201').length
const conflicts = bookings.filter((sample) => sample.code === '409').length
console.log(`\nBooking attempts: ${bookings.length}  booked (201): ${booked}  conflicts (409): ${conflicts}  ` +
  `conflict rate: ${percent(conflicts, bookings.length)}%`)

function summarize(label, group) {
  const times = group.map((sample) => sample.elapsed).sort((a, b) => a - b)
  const start = Math.min(...group.map((sample) => sample.ts))
  const end = Math.max(...group.map((sample) => sample.ts + sample.elapsed))
  return {
    label,
    count: group.length,
    avg: Math.round(times.reduce((sum, value) => sum + value, 0) / times.length),
    p95: times[Math.min(times.length - 1, Math.ceil(times.length * 0.95) - 1)],
    throughput: Number((group.length / Math.max((end - start) / 1000, 0.001)).toFixed(1)),
    errorRate: percent(group.filter((sample) => !sample.success).length, group.length),
    conflictRate: percent(group.filter((sample) => sample.code === '409').length, group.length),
  }
}

function percent(part, total) {
  return total ? Number(((part / total) * 100).toFixed(2)) : 0
}

function splitCsv(line) {
  const cells = []
  let current = ''
  let quoted = false
  for (const char of line) {
    if (char === '"') quoted = !quoted
    else if (char === ',' && !quoted) {
      cells.push(current)
      current = ''
    } else current += char
  }
  cells.push(current)
  return cells
}
