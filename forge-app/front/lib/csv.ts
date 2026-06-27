// Export CSV côté client (séparateur ; + BOM UTF-8 pour Excel FR).
export function exportCsv(filename: string, headers: string[], rows: (string | number)[][]) {
  const esc = (v: string | number) => `"${String(v).replace(/"/g, '""')}"`
  const csv = [headers.map(esc).join(";"), ...rows.map((r) => r.map(esc).join(";"))].join("\r\n")
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" })
  const a = document.createElement("a")
  a.href = URL.createObjectURL(blob)
  a.download = filename.endsWith(".csv") ? filename : `${filename}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
}
