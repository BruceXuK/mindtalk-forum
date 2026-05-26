const defaultConfig: AppConfig = {
  apiBaseURL: '/api',
  apiTimeout: 15000,
  siteName: 'MindTalk',
  siteTitle: '思享论坛',
  siteDescription: '分享技术，连接思想',
  siteURL: 'https://mindtalk.example.com',
  logoText: 'MindTalk',
  pwa: {
    name: 'MindTalk 思享论坛',
    shortName: 'MindTalk',
    description: '现代化知识社区',
    themeColor: '#2563EB',
    backgroundColor: '#F8FAFC'
  },
  search: {
    placeholder: '搜索内容...',
    homePlaceholder: '搜索感兴趣的内容...'
  },
  polling: {
    unreadCount: 30000,
    mentionDebounce: 200
  },
  pagination: {
    homePageSize: 20,
    recommendedSize: 20,
    rankingSize: 20,
    followingSize: 5,
    hotPostsLimit: 5,
    categoryLimit: 8,
    tagLimit: 12,
    mentionLimit: 8
  },
  rss: {
    path: '/api/rss/posts'
  },
  footer: {
    links: [
      { label: '关于', to: '/' },
      { label: '帮助', to: '/' },
      { label: '隐私', to: '/' }
    ],
    copyright: 'MindTalk © 2026'
  },
  home: {
    heroTitle: '思享论坛',
    heroSubtitle: '分享技术，连接思想',
    feedTabs: [
      { key: 'recommended', label: '推荐' },
      { key: 'all', label: '全部' },
      { key: 'following', label: '关注' },
      { key: 'ranking', label: '排行榜' }
    ],
    rankingPeriods: [
      { key: 'weekly', label: '本周' },
      { key: 'monthly', label: '本月' }
    ]
  },
  hotkeys: {
    search: 'Ctrl+K',
    createPost: 'Ctrl+N',
    quickSearch: '/'
  },
  locale: 'zh-CN',
  admin: {
    sidebarBg: '#1E293B',
    sidebarBgDark: '#0F172A',
    sidebarTextColor: '#bfcbd9',
    sidebarTextColorDark: '#F1F5F9',
    sidebarActiveColor: '#409eff',
    sidebarWidth: 220
  }
}

export function useAppConfig(): AppConfig {
  if (typeof window !== 'undefined' && window.__APP_CONFIG__) {
    return window.__APP_CONFIG__
  }
  return defaultConfig
}

export function getAppConfig(): AppConfig {
  if (typeof window !== 'undefined' && window.__APP_CONFIG__) {
    return window.__APP_CONFIG__
  }
  return defaultConfig
}
