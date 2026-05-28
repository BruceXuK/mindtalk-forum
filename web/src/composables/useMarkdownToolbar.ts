import type { Ref } from 'vue'

interface TextareaContext {
  getValue(): string
  setValue(v: string): void
  getSelection(): { start: number; end: number }
  setSelection(start: number, end: number): void
  focus(): void
}

function wrapSelection(ctx: TextareaContext, before: string, after: string, placeholder: string) {
  const { start, end } = ctx.getSelection()
  const text = ctx.getValue()
  const selected = text.substring(start, end)
  const replacement = selected ? before + selected + after : before + placeholder + after
  ctx.setValue(text.substring(0, start) + replacement + text.substring(end))
  const cursor = start + before.length + (selected ? selected.length : placeholder.length)
  ctx.setSelection(cursor, cursor)
  ctx.focus()
}

function prependLine(ctx: TextareaContext, prefix: string, placeholder: string) {
  const { start } = ctx.getSelection()
  const text = ctx.getValue()
  // Find line start
  let lineStart = start
  while (lineStart > 0 && text[lineStart - 1] !== '\n') lineStart--
  const insertion = prefix + placeholder
  ctx.setValue(text.substring(0, lineStart) + insertion + text.substring(lineStart))
  const cursor = lineStart + insertion.length
  ctx.setSelection(cursor, cursor)
  ctx.focus()
}

function insertBlock(ctx: TextareaContext, before: string, after: string, placeholder: string) {
  const { start, end } = ctx.getSelection()
  const text = ctx.getValue()
  const selected = text.substring(start, end)
  // Ensure newline padding
  const padBefore = start === 0 || text[start - 1] === '\n' ? '' : '\n'
  const padAfter = end === text.length || text[end] === '\n' ? '' : '\n'
  const insBefore = padBefore + before
  const insAfter = after + padAfter
  const replacement = selected ? insBefore + selected + insAfter : insBefore + placeholder + insAfter
  ctx.setValue(text.substring(0, start) + replacement + text.substring(end))
  const cursor = start + insBefore.length + (selected ? selected.length : placeholder.length)
  ctx.setSelection(cursor, cursor)
  ctx.focus()
}

function insertLine(ctx: TextareaContext, line: string) {
  const { start } = ctx.getSelection()
  const text = ctx.getValue()
  const padBefore = start === 0 || text[start - 1] === '\n' ? '' : '\n'
  const padAfter = start === text.length || text[start] === '\n' ? '\n' : '\n\n'
  const insertion = padBefore + line + padAfter
  ctx.setValue(text.substring(0, start) + insertion + text.substring(start))
  const cursor = start + insertion.length
  ctx.setSelection(cursor, cursor)
  ctx.focus()
}

export interface ToolbarActions {
  bold: () => void
  italic: () => void
  strikethrough: () => void
  inlineCode: () => void
  heading: () => void
  blockquote: () => void
  codeBlock: () => void
  unorderedList: () => void
  orderedList: () => void
  divider: () => void
  link: () => void
}

export function useMarkdownToolbar(
  textareaRef: Ref<HTMLTextAreaElement | undefined>,
  onChanged: (value: string) => void
): ToolbarActions {
  function ctx(): TextareaContext | null {
    const el = textareaRef.value
    if (!el) return null
    return {
      getValue: () => el.value,
      setValue: (v) => { el.value = v; onChanged(v) },
      getSelection: () => ({ start: el.selectionStart, end: el.selectionEnd }),
      setSelection: (s, e) => { el.selectionStart = s; el.selectionEnd = e },
      focus: () => el.focus(),
    }
  }

  function act(fn: (c: TextareaContext) => void) {
    const c = ctx()
    if (c) fn(c)
  }

  return {
    bold:            () => act(c => wrapSelection(c, '**', '**', '粗体文字')),
    italic:          () => act(c => wrapSelection(c, '*', '*', '斜体文字')),
    strikethrough:   () => act(c => wrapSelection(c, '~~', '~~', '删除文字')),
    inlineCode:      () => act(c => wrapSelection(c, '`', '`', '代码')),
    heading:         () => act(c => prependLine(c, '## ', '标题')),
    blockquote:      () => act(c => prependLine(c, '> ', '引用')),
    codeBlock:       () => act(c => insertBlock(c, '```\n', '\n```', '代码块')),
    unorderedList:   () => act(c => prependLine(c, '- ', '列表项')),
    orderedList:     () => act(c => prependLine(c, '1. ', '列表项')),
    divider:         () => act(c => insertLine(c, '---')),
    link:            () => act(c => wrapSelection(c, '[', '](url)', '链接文字')),
  }
}
