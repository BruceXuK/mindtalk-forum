/**
 * MindTalk 前端运行时配置
 * 此文件在构建时复制到 dist/，部署时可单独替换，无需重新构建。
 * 在 index.html 中通过 <script src="/config.js"> 加载。
 */
window.__APP_CONFIG__ = {
  // ── API ──
  apiBaseURL: '/api',
  apiTimeout: 15000,

  // ── 站点信息 ──
  siteName: 'MindTalk',
  siteTitle: '思享论坛',
  siteDescription: '分享技术，连接思想',
  siteURL: 'https://mindtalk.example.com',
  logoText: 'MindTalk',

  // ── PWA ──
  pwa: {
    name: 'MindTalk 思享论坛',
    shortName: 'MindTalk',
    description: '现代化知识社区',
    themeColor: '#2563EB',
    backgroundColor: '#F8FAFC'
  },

  // ── 搜索 ──
  search: {
    placeholder: '搜索内容...',
    homePlaceholder: '搜索感兴趣的内容...'
  },

  // ── 轮询间隔 (ms) ──
  polling: {
    unreadCount: 30000,
    mentionDebounce: 200
  },

  // ── 分页默认值 ──
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

  // ── RSS ──
  rss: {
    path: '/api/rss/posts'
  },

  // ── 页脚 ──
  footer: {
    links: [
      { label: '关于', to: '/' },
      { label: '帮助', to: '/' },
      { label: '隐私', to: '/' }
    ],
    copyright: 'MindTalk © 2026'
  },

  // ── 首页 Hero ──
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

  // ── 热键 ──
  hotkeys: {
    search: 'Ctrl+K',
    createPost: 'Ctrl+N',
    quickSearch: '/'
  },

  // ── 格式化 ──
  locale: 'zh-CN',

  // ── 管理后台 ──
  admin: {
    sidebarBg: '#1E293B',
    sidebarBgDark: '#0F172A',
    sidebarTextColor: '#bfcbd9',
    sidebarTextColorDark: '#F1F5F9',
    sidebarActiveColor: '#409eff',
    sidebarWidth: 220
  }
}
