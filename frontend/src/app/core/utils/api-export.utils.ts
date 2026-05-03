/** Trigger a browser download for an in-memory blob. */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.rel = 'noopener';
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  // Revoking immediately can cancel the download in Chrome/Edge; release after the browser has consumed the URL.
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Decode Base64 PDF payload and download as a file. */
export function downloadPdfFromBase64(base64: string | null | undefined, filename: string): void {
  if (base64 == null || String(base64).trim() === '') {
    throw new Error('PDF data (pdfBase64) is missing from the export response.');
  }
  const clean = String(base64).replace(/\s/g, '');
  let bytes: Uint8Array;
  try {
    bytes = Uint8Array.from(atob(clean), (c) => c.charCodeAt(0));
  } catch {
    throw new Error('PDF data is not valid Base64.');
  }
  downloadBlob(new Blob([bytes as BlobPart], { type: 'application/pdf' }), filename);
}
