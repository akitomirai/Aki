function escapeCsvCell(value) {
  const text = String(value ?? '')
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function toDataRows(columns, rows) {
  return rows.map((row) => columns.map((column) => escapeCsvCell(row?.[column.key] ?? '')))
}

export function downloadCsvFile({ filename, columns, rows }) {
  const header = columns.map((column) => escapeCsvCell(column.label)).join(',')
  const body = toDataRows(columns, rows).map((row) => row.join(',')).join('\r\n')
  const content = `\uFEFF${header}\r\n${body}`
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

export function downloadJsonFile({ filename, data }) {
  const content = JSON.stringify(data ?? {}, null, 2)
  const blob = new Blob([content], { type: 'application/json;charset=utf-8;' })
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
}

export function openPrintPreviewWindow(title = '打印预览') {
  const popup = window.open('', '_blank')
  if (!popup) {
    throw new Error('浏览器拦截了打印预览窗口，请允许当前站点打开新窗口后重试。')
  }
  try {
    popup.opener = null
  } catch (error) {
  }
  popup.document.write(`<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <title>${escapeHtml(title)}</title>
    <style>
      body {
        margin: 0;
        font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
        background: #f4f7fb;
        color: #173252;
      }
      .loading {
        min-height: 100vh;
        display: grid;
        place-items: center;
        text-align: center;
        padding: 32px;
      }
      .loading h1 {
        margin: 0 0 12px;
        font-size: 28px;
      }
      .loading p {
        margin: 0;
        color: #64809f;
      }
    </style>
  </head>
  <body>
    <main class="loading" data-testid="qr-bulk-print-page">
      <div>
        <h1>${escapeHtml(title)}</h1>
        <p>正在整理二维码内容，请稍等...</p>
      </div>
    </main>
  </body>
</html>`)
  popup.document.close()
  return popup
}

export function renderQrPrintPreview(popup, payload) {
  const title = escapeHtml(payload?.title || '二维码批量打印预览')
  const subtitle = escapeHtml(payload?.subtitle || '')
  const printedAt = escapeHtml(payload?.printedAt || '')
  const cards = (payload?.items ?? [])
    .map((item) => {
      const batchCode = escapeHtml(item.batchCode)
      const productName = escapeHtml(item.productName)
      const companyName = escapeHtml(item.companyName)
      const qrToken = escapeHtml(item.qrToken)
      const publicUrl = escapeHtml(item.publicUrl)
      const imageUrl = escapeHtml(item.imageUrl)
      return `<article class="qr-card">
        <header class="qr-card-head">
          <strong>${productName}</strong>
          <span>${batchCode}</span>
        </header>
        <div class="qr-card-body">
          <img src="${imageUrl}" alt="${productName} 二维码">
          <div class="qr-card-meta">
            <p><span>所属企业</span><strong>${companyName}</strong></p>
            <p><span>公开标识</span><strong>${qrToken}</strong></p>
            <p><span>公开入口</span><strong>${publicUrl}</strong></p>
          </div>
        </div>
      </article>`
    })
    .join('')

  popup.document.open()
  popup.document.write(`<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <title>${title}</title>
    <style>
      @page {
        size: A4 portrait;
        margin: 12mm;
      }
      body {
        margin: 0;
        font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
        color: #173252;
        background: #eef4fb;
      }
      .page {
        min-height: 100vh;
        box-sizing: border-box;
        padding: 18px 18px 28px;
      }
      .page-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 16px;
        margin-bottom: 18px;
      }
      .page-header h1 {
        margin: 0;
        font-size: 28px;
      }
      .page-header p {
        margin: 10px 0 0;
        color: #64809f;
      }
      .page-actions {
        display: flex;
        gap: 10px;
      }
      .page-actions button {
        height: 40px;
        padding: 0 16px;
        border: 0;
        border-radius: 999px;
        background: #2f7dd8;
        color: #fff;
        font: inherit;
        cursor: pointer;
      }
      .page-actions button.secondary {
        background: #fff;
        color: #2f7dd8;
        border: 1px solid #bfd2e9;
      }
      .qr-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;
      }
      .qr-card {
        break-inside: avoid;
        padding: 18px;
        border: 1px solid #c8d7ea;
        border-radius: 22px;
        background: #fff;
        box-shadow: 0 12px 22px rgba(27, 66, 110, 0.08);
      }
      .qr-card-head strong,
      .qr-card-meta strong {
        display: block;
      }
      .qr-card-head strong {
        font-size: 22px;
      }
      .qr-card-head span,
      .qr-card-meta span,
      .footer-note {
        color: #64809f;
      }
      .qr-card-body {
        display: grid;
        grid-template-columns: 160px minmax(0, 1fr);
        gap: 18px;
        align-items: center;
        margin-top: 18px;
      }
      .qr-card-body img {
        width: 160px;
        height: 160px;
        object-fit: contain;
        padding: 10px;
        border: 1px solid #d8e4f2;
        border-radius: 18px;
        background: #fff;
      }
      .qr-card-meta {
        display: grid;
        gap: 12px;
      }
      .qr-card-meta p {
        margin: 0;
      }
      .footer-note {
        margin-top: 18px;
        font-size: 13px;
      }
      @media print {
        body {
          background: #fff;
        }
        .page {
          padding: 0;
        }
        .page-actions {
          display: none;
        }
        .qr-card {
          box-shadow: none;
        }
      }
    </style>
  </head>
  <body data-testid="qr-bulk-print-page">
    <main class="page">
      <section class="page-header">
        <div>
          <h1>${title}</h1>
          <p>${subtitle}</p>
        </div>
        <div class="page-actions">
          <button type="button" onclick="window.print()">打印</button>
          <button type="button" class="secondary" onclick="window.close()">关闭</button>
        </div>
      </section>
      <section class="qr-grid">
        ${cards}
      </section>
      <p class="footer-note">打印时间：${printedAt}</p>
    </main>
  </body>
</html>`)
  popup.document.close()
}
