/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare global {
  interface AppConfig {
    apiBaseURL: string
    apiTimeout: number
    siteName: string
    siteTitle: string
    siteDescription: string
    siteURL: string
    logoText: string
    pwa: {
      name: string
      shortName: string
      description: string
      themeColor: string
      backgroundColor: string
    }
    search: {
      placeholder: string
      homePlaceholder: string
    }
    polling: {
      unreadCount: number
      mentionDebounce: number
    }
    pagination: {
      homePageSize: number
      recommendedSize: number
      rankingSize: number
      followingSize: number
      hotPostsLimit: number
      categoryLimit: number
      tagLimit: number
      mentionLimit: number
    }
    rss: {
      path: string
    }
    footer: {
      links: { label: string; to: string }[]
      copyright: string
    }
    home: {
      heroTitle: string
      heroSubtitle: string
      feedTabs: { key: string; label: string }[]
      rankingPeriods: { key: string; label: string }[]
    }
    hotkeys: {
      search: string
      createPost: string
      quickSearch: string
    }
    locale: string
    admin: {
      sidebarBg: string
      sidebarBgDark: string
      sidebarTextColor: string
      sidebarTextColorDark: string
      sidebarActiveColor: string
      sidebarWidth: number
    }
  }

  interface Window {
    __APP_CONFIG__: AppConfig
  }
}

export {}
