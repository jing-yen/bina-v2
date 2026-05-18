import * as pdfjsLib from 'pdfjs-dist';
import mammoth from 'mammoth';

// Configure pdf.js worker — use the bundled worker from the package
pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url,
).toString();

/**
 * Extract text from a PDF file using pdfjs-dist.
 * Iterates over every page and joins text items with spaces/newlines.
 */
async function extractTextFromPdf(file: File): Promise<string> {
  const arrayBuffer = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
  const pages: string[] = [];

  for (let i = 1; i <= pdf.numPages; i++) {
    const page = await pdf.getPage(i);
    const stream = page.streamTextContent();
    const reader = stream.getReader();
    const items: string[] = [];
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      if (value?.items) {
        for (const item of value.items) {
          if ('str' in item) items.push((item as { str: string }).str);
        }
      }
    }
    pages.push(items.join(' '));
  }

  return pages.join('\n\n');
}

/**
 * Extract raw text from a DOCX file using mammoth.
 */
async function extractTextFromDocx(file: File): Promise<string> {
  const arrayBuffer = await file.arrayBuffer();
  const result = await mammoth.extractRawText({ arrayBuffer });
  return result.value;
}

/**
 * Extract text from a .doc file.
 * Old .doc files saved by modern tools are often HTML; detect that case
 * and strip tags with DOMParser. For genuine binary .doc files we fall
 * back to file.text() which will be lossy but won't crash.
 */
async function extractTextFromDoc(file: File): Promise<string> {
  const raw = await file.text();
  const trimmed = raw.trimStart();

  // Many ".doc" files are actually HTML — check for leading angle bracket
  if (trimmed.startsWith('<')) {
    const doc = new DOMParser().parseFromString(raw, 'text/html');
    return (doc.body.textContent || doc.body.innerText || '').replace(/\n{3,}/g, '\n\n').trim();
  }

  // Genuine binary .doc — not much we can do in the browser without a
  // heavyweight parser. Return whatever text() gives us.
  return raw;
}

/**
 * Read a plain-text file (TXT, MD, CSV, etc.) directly.
 */
async function extractTextFromPlain(file: File): Promise<string> {
  return file.text();
}

/**
 * Detect the file type by extension and extract text accordingly.
 * Throws on unrecoverable errors (corrupt PDF, etc.).
 */
export async function extractTextFromFile(file: File): Promise<string> {
  const ext = file.name.split('.').pop()?.toLowerCase() ?? '';

  switch (ext) {
    case 'pdf':
      return extractTextFromPdf(file);
    case 'docx':
      return extractTextFromDocx(file);
    case 'doc':
      return extractTextFromDoc(file);
    case 'txt':
    case 'md':
    case 'csv':
    default:
      return extractTextFromPlain(file);
  }
}
