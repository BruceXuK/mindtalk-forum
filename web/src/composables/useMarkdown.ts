import { computed, type Ref } from 'vue'

interface TocItem {
  id: string
  level: number
  text: string
}

// Simple syntax highlighting via CSS classes
function highlightCode(code: string, lang: string): string {
  const escaped = code
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')

  if (!lang || lang === 'plain' || lang === 'text') {
    return `<span class="token plain">${escaped}</span>`
  }

  const keywords = new Set([
    'const', 'let', 'var', 'function', 'return', 'if', 'else', 'for', 'while',
    'do', 'switch', 'case', 'break', 'continue', 'new', 'this', 'class',
    'extends', 'import', 'export', 'default', 'from', 'async', 'await',
    'try', 'catch', 'finally', 'throw', 'typeof', 'instanceof', 'in',
    'of', 'void', 'null', 'undefined', 'true', 'false', 'static', 'get',
    'set', 'public', 'private', 'protected', 'readonly', 'interface',
    'type', 'enum', 'namespace', 'module', 'declare', 'abstract', 'implements',
    'yield', 'super', 'require', 'module', 'exports',
    'int', 'long', 'double', 'float', 'boolean', 'char', 'byte', 'short',
    'String', 'Integer', 'List', 'Map', 'Set', 'Optional', 'Stream',
    'def', 'self', 'init', 'print', 'lambda', 'raise', 'except', 'finally',
    'elif', 'pass', 'with', 'as', 'nonlocal', 'global',
    'func', 'var', 'package', 'select', 'chan', 'go', 'defer', 'map',
    'struct', 'range', 'println', 'fmt',
  ])

  let result = ''
  let i = 0

  while (i < escaped.length) {
    // Line comment //
    if (escaped[i] === '/' && escaped[i + 1] === '/') {
      const end = escaped.indexOf('\n', i)
      const comment = end === -1 ? escaped.slice(i) : escaped.slice(i, end)
      result += `<span class="token comment">${comment}</span>`
      i = end === -1 ? escaped.length : end
      continue
    }
    // Block comment /* */
    if (escaped[i] === '/' && escaped[i + 1] === '*') {
      const end = escaped.indexOf('*/', i + 2)
      const comment = end === -1 ? escaped.slice(i) : escaped.slice(i, end + 2)
      result += `<span class="token comment">${comment}</span>`
      i = end === -1 ? escaped.length : end + 2
      continue
    }
    // String (double quote)
    if (escaped[i] === '"') {
      const start = i
      i++
      while (i < escaped.length && escaped[i] !== '"') {
        if (escaped[i] === '\\') i++
        i++
      }
      if (i < escaped.length) i++
      result += `<span class="token string">${escaped.slice(start, i)}</span>`
      continue
    }
    // String (single quote)
    if (escaped[i] === "'") {
      const start = i
      i++
      while (i < escaped.length && escaped[i] !== "'") {
        if (escaped[i] === '\\') i++
        i++
      }
      if (i < escaped.length) i++
      result += `<span class="token string">${escaped.slice(start, i)}</span>`
      continue
    }
    // Template literal
    if (escaped[i] === '`') {
      const start = i
      i++
      while (i < escaped.length && escaped[i] !== '`') {
        if (escaped[i] === '\\') i++
        i++
      }
      if (i < escaped.length) i++
      result += `<span class="token string">${escaped.slice(start, i)}</span>`
      continue
    }
    // Number
    if (/[0-9]/.test(escaped[i]) && (i === 0 || /[\s(,;:=[+\-*/<>!&|^~?]/.test(escaped[i - 1]))) {
      const start = i
      while (i < escaped.length && /[0-9a-fA-F.xX_]/ .test(escaped[i])) i++
      result += `<span class="token number">${escaped.slice(start, i)}</span>`
      continue
    }
    // Word (keyword or identifier)
    if (/[a-zA-Z_$]/.test(escaped[i])) {
      const start = i
      while (i < escaped.length && /[a-zA-Z0-9_$]/.test(escaped[i])) i++
      const word = escaped.slice(start, i)
      if (keywords.has(word)) {
        result += `<span class="token keyword">${word}</span>`
      } else if (i < escaped.length && escaped[i] === '(') {
        result += `<span class="token function">${word}</span>`
      } else {
        result += word
      }
      continue
    }
    result += escaped[i]
    i++
  }

  return result
}

export function renderMarkdown(text: string): string {
  if (!text) return ''

  let html = text

  // Fenced code blocks — must be processed first to protect content
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_m, lang, code) => {
    const trimmedCode = code.trimEnd()
    // Mermaid diagrams — render as a special container for client-side rendering
    if (lang === 'mermaid') {
      const encoded = encodeURIComponent(trimmedCode)
      return `<div class="mermaid-wrapper"><div class="mermaid" data-mermaid="${encoded}">${trimmedCode}</div></div>`
    }
    const langLabel = lang || 'code'
    const highlighted = highlightCode(trimmedCode, lang)
    const copyBtn = `<button class="code-copy-btn" data-code="${encodeURIComponent(trimmedCode)}" title="复制代码"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg></button>`
    return `<div class="code-block-wrapper"><div class="code-block-header"><span class="code-lang">${langLabel}</span>${copyBtn}</div><pre><code class="language-${lang}">${highlighted}</code></pre></div>`
  })

  // Code sandbox embeds — standalone URLs from known providers
  html = html.replace(
    /^(https?:\/\/(?:codesandbox\.io\/s\/[\w-]+|codepen\.io\/[\w-]+\/(?:pen|embed)\/[\w-]+|jsfiddle\.net\/[\w-]+\/[\w-]+|stackblitz\.com\/edit\/[\w-]+)(?:\/)?)\s*$/gm,
    (_m: string, url: string) => {
      let embedUrl = ''
      if (url.includes('codesandbox.io/s/')) {
        embedUrl = url.replace('/s/', '/embed/')
      } else if (url.includes('codepen.io') && url.includes('/pen/')) {
        embedUrl = url.replace('/pen/', '/embed/')
      } else if (url.includes('jsfiddle.net')) {
        embedUrl = url.replace(/\/$/, '') + '/embedded/'
      } else if (url.includes('stackblitz.com/edit/')) {
        embedUrl = url + '?embed=1'
      }
      if (!embedUrl) return _m

      let platform = 'Code Sandbox'
      if (url.includes('codepen.io')) platform = 'CodePen'
      else if (url.includes('jsfiddle.net')) platform = 'JSFiddle'
      else if (url.includes('stackblitz.com')) platform = 'StackBlitz'
      else if (url.includes('codesandbox.io')) platform = 'CodeSandbox'

      return `<div class="sandbox-embed"><div class="sandbox-embed__header"><span>${platform}</span><a href="${url}" target="_blank" rel="noopener noreferrer" class="sandbox-embed__link">在新窗口打开</a></div><div class="sandbox-embed__iframe"><iframe src="${embedUrl}" sandbox="allow-scripts allow-same-origin allow-popups allow-forms allow-modals" loading="lazy" allowfullscreen></iframe></div></div>`
    }
  )

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')

  // Headings (with IDs for TOC)
  html = html.replace(/^#### (.+)$/gm, (_m: string, text: string) => {
    const id = slugify(text)
    return `<h4 id="${id}">${text}</h4>`
  })
  html = html.replace(/^### (.+)$/gm, (_m: string, text: string) => {
    const id = slugify(text)
    return `<h3 id="${id}">${text}</h3>`
  })
  html = html.replace(/^## (.+)$/gm, (_m: string, text: string) => {
    const id = slugify(text)
    return `<h2 id="${id}">${text}</h2>`
  })
  html = html.replace(/^# (.+)$/gm, (_m: string, text: string) => {
    const id = slugify(text)
    return `<h1 id="${id}">${text}</h1>`
  })

  // Table
  html = html.replace(/^\|(.+)\|\n\|[-| :]+\|\n((?:^\|.+\|\n?)+)/gm, (_m: string, header: string, body: string) => {
    const headers = header.split('|').map(h => h.trim()).filter(Boolean)
    const rows = body.trim().split('\n').map(row =>
      row.split('|').map(c => c.trim()).filter(Boolean)
    )
    const thead = `<thead><tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr></thead>`
    const tbody = `<tbody>${rows.map(row => `<tr>${row.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}</tbody>`
    return `<table>${thead}${tbody}</table>`
  })

  // Horizontal rules
  html = html.replace(/^(---|\*\*\*|___)$/gm, '<hr />')

  // Blockquotes
  html = html.replace(/^&gt; (.+)$/gm, '<blockquote><p>$1</p></blockquote>')
  html = html.replace(/<\/blockquote>\n<blockquote>/g, '\n')

  // Bold+Italic
  html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
  // Bold
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // Italic
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  // Strikethrough
  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>')

  // Links
  html = html.replace(/(?<!!)\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

  // Images (not already in code blocks)
  html = html.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, '<figure class="content-image-figure"><img src="$2" alt="$1" class="content-image" loading="lazy" /></figure>')

  // Unordered lists
  html = html.replace(/^[\*\-] (.+)$/gm, '<li>$1</li>')
  html = html.replace(/((?:<li>.*<\/li>\n?)+)/g, (match: string) => {
    if (match.includes('<li>')) return `<ul>${match}</ul>`
    return match
  })

  // Ordered lists
  html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>')

  // Paragraphs for remaining non-tag lines
  const blockTags = /^<(h[1-4]|ul|ol|li|pre|div|table|thead|tbody|tr|th|td|blockquote|hr|p|img|a|strong|em|del|code)/i
  html = html.split('\n\n').map(block => {
    const trimmed = block.trim()
    if (!trimmed) return ''
    if (blockTags.test(trimmed)) return trimmed
    return `<p>${trimmed}</p>`
  }).join('\n')

  return html
}

export function slugify(text: string): string {
  return text
    .toLowerCase()
    .replace(/[^\w一-鿿\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .trim()
}

/**
 * 渲染页面中的 Mermaid 图表，应在内容挂载后调用
 */
export async function renderMermaidDiagrams(container: HTMLElement) {
  const mermaidEls = container.querySelectorAll('.mermaid')
  if (mermaidEls.length === 0) return

  try {
    const mermaid = await import('mermaid')
    mermaid.default.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
    })
    for (const el of mermaidEls) {
      try {
        const id = 'mermaid-' + Math.random().toString(36).slice(2, 8)
        const code = decodeURIComponent(el.getAttribute('data-mermaid') || '')
        const { svg } = await mermaid.default.render(id, code)
        el.innerHTML = svg
      } catch {
        el.innerHTML = '<p class="mermaid-error">Mermaid 图表渲染失败</p>'
      }
    }
  } catch {
    // mermaid failed to load
  }
}

export function extractToc(markdown: string): TocItem[] {
  const items: TocItem[] = []
  const headingRegex = /^(#{1,3})\s+(.+)$/gm
  let match: RegExpExecArray | null

  while ((match = headingRegex.exec(markdown)) !== null) {
    const level = match[1].length
    const text = match[2].trim()
    const id = slugify(text)
    items.push({ id, level, text })
  }

  return items
}

export function useMarkdown(contentRef: Ref<string>) {
  const renderedHtml = computed(() => renderMarkdown(contentRef.value))
  const tocItems = computed(() => extractToc(contentRef.value))

  return { renderedHtml, tocItems }
}
