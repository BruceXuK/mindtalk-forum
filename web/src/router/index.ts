import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { getAppConfig } from '@/composables/useAppConfig'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/posts',
    name: 'PostList',
    component: () => import('@/views/post/PostListView.vue'),
    meta: { title: '帖子列表' }
  },
  {
    path: '/posts/create',
    name: 'CreatePost',
    component: () => import('@/views/create-post/CreatePostView.vue'),
    meta: { title: '发帖', requiresAuth: true }
  },
  {
    path: '/posts/:id',
    name: 'PostDetail',
    component: () => import('@/views/post/PostDetailView.vue'),
    meta: { title: '帖子详情' }
  },
  {
    path: '/posts/:id/edit',
    name: 'EditPost',
    component: () => import('@/views/create-post/CreatePostView.vue'),
    meta: { title: '编辑帖子', requiresAuth: true }
  },
  {
    path: '/users/:id',
    name: 'UserProfile',
    component: () => import('@/views/user/UserProfileView.vue'),
    meta: { title: '用户主页' }
  },
  {
    path: '/profile',
    name: 'MyProfile',
    component: () => import('@/views/user/UserProfileView.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('@/views/message/MessageView.vue'),
    meta: { title: '消息中心', requiresAuth: true }
  },
  {
    path: '/messages/chat',
    name: 'Chat',
    component: () => import('@/views/chat/ChatView.vue'),
    meta: { title: '私信', requiresAuth: true }
  },
  {
    path: '/series/create',
    name: 'CreateSeries',
    component: () => import('@/views/series/CreateSeriesView.vue'),
    meta: { title: '创建系列', requiresAuth: true }
  },
  {
    path: '/series/:id',
    name: 'SeriesDetail',
    component: () => import('@/views/series/SeriesDetailView.vue'),
    meta: { title: '系列详情' }
  },
  {
    path: '/settings/notifications',
    name: 'NotificationSettings',
    component: () => import('@/views/settings/NotificationSettingsView.vue'),
    meta: { title: '通知设置', requiresAuth: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/SearchView.vue'),
    meta: { title: '搜索' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/AdminView.vue'),
    redirect: '/admin/users',
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/users/AdminUsersView.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'posts',
        name: 'AdminPosts',
        component: () => import('@/views/admin/posts/AdminPostsView.vue'),
        meta: { title: '帖子审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'comments',
        name: 'AdminComments',
        component: () => import('@/views/admin/comments/AdminCommentsView.vue'),
        meta: { title: '评论审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: () => import('@/views/admin/reports/AdminReportsView.vue'),
        meta: { title: '举报处理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'roles',
        name: 'AdminRoles',
        component: () => import('@/views/admin/roles/AdminRolesView.vue'),
        meta: { title: '权限管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'stats',
        name: 'AdminStats',
        component: () => import('@/views/admin/stats/AdminStatsView.vue'),
        meta: { title: '统计分析', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/categories/AdminCategoriesView.vue'),
        meta: { title: '分类管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'tags',
        name: 'AdminTags',
        component: () => import('@/views/admin/tags/AdminTagsView.vue'),
        meta: { title: '标签管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'sensitive-words',
        name: 'AdminSensitiveWords',
        component: () => import('@/views/admin/words/AdminSensitiveWordsView.vue'),
        meta: { title: '敏感词管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/views/admin/announcements/AdminAnnouncementsView.vue'),
        meta: { title: '公告管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/logs/AdminLogsView.vue'),
        meta: { title: '操作日志', requiresAuth: true, requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

function parseRoleFromToken(token: string): string {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.role || 'USER'
  } catch {
    return 'USER'
  }
}

router.beforeEach((to, _from, next) => {
  const config = getAppConfig()
  const siteTitle = `${config.siteName} ${config.siteTitle}`
  document.title = (to.meta.title as string) ? `${to.meta.title} - ${siteTitle}` : siteTitle

  const token = sessionStorage.getItem('access_token')

  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin) {
    let roles: string[] = JSON.parse(sessionStorage.getItem('user_roles') || '[]')
    // 如果 sessionStorage 中没有角色信息，从 JWT 中解析
    if (roles.length === 0 && token) {
      roles = [parseRoleFromToken(token)]
      sessionStorage.setItem('user_roles', JSON.stringify(roles))
    }
    if (!roles.includes('ADMIN')) {
      next({ name: 'Home' })
      return
    }
  }

  next()
})

export default router
