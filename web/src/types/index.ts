// 统一响应类型
export interface Result<T> {
  code: number
  message: string
  data: T
}

// 分页参数
export interface PageQuery {
  page: number
  size: number
}

// 分页结果
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

// ── 用户 ──
export interface UserVO {
  id: number
  username: string
  email?: string
  nickname: string
  avatarUrl?: string
  bio?: string
  gender?: number
  location?: string
  followingCount?: number
  followerCount?: number
  postCount?: number
  createTime: string
}

export interface UserProfileVO extends UserVO {
  isFollowing?: boolean
}

export interface LoginResultVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserVO
}

// ── 帖子 ──
export interface PostVO {
  id: number
  title: string
  summary: string
  content?: string
  contentText?: string
  author: UserVO
  category?: CategoryVO
  tags: TagVO[]
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount?: number
  isPinned: boolean
  isFeatured: boolean
  pinnedUntil?: string
  featuredUntil?: string
  isLiked?: boolean
  isCollected?: boolean
  authorIsFollowing?: boolean
  status: number
  createTime: string
  updateTime: string
}

export interface CategoryVO {
  id: number
  name: string
  description?: string
  icon?: string
  sortOrder?: number
  postCount?: number
}

export interface TagVO {
  id: number
  name: string
  postCount?: number
}

// ── 评论 ──
export interface CommentVO {
  id: number
  postId: number
  user: UserVO
  parentId?: number
  replyTo?: UserVO
  content: string
  likeCount: number
  isLiked?: boolean
  replies?: CommentVO[]
  replyCount?: number
  createTime: string
}

// ── 通知 ──
export interface NotificationVO {
  id: number
  userId: number
  fromUser?: UserVO
  notifyType: string
  title: string
  content: string
  targetType?: string
  targetId?: number
  isRead: boolean
  createTime: string
}

// ── 管理端 ──
export interface AdminUserVO {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl?: string
  status: number
  roles: string[]
  postCount?: number
  commentCount?: number
  createTime: string
}

export interface ReportVO {
  id: number
  reporter: UserVO
  targetType: string
  targetId: number
  reason: string
  description?: string
  status: number
  handler?: UserVO
  handleResult?: string
  handleTime?: string
  createTime: string
}

export interface RoleVO {
  id: number
  roleName: string
  roleCode: string
  description: string
  sortOrder: number
  status: number
}

export interface PermissionVO {
  id: number
  parentId: number
  permName: string
  permCode: string
  permType: number
  path: string
  icon: string
  sortOrder: number
}

export interface PermissionTreeVO extends PermissionVO {
  children: PermissionTreeVO[]
}

export interface StatsOverviewVO {
  totalUsers: number
  totalPosts: number
  totalComments: number
  totalReports: number
  todayNewUsers: number
  todayNewPosts: number
  todayNewComments: number
  pendingReports: number
}

export interface StatsTrendVO {
  date: string
  newUsers: number
  newPosts: number
  newComments: number
}

// ── 系列/合集 ──
export interface SeriesVO {
  id: number
  author: UserVO
  title: string
  description?: string
  coverUrl?: string
  postCount: number
  sortOrder?: number
  createTime: string
  updateTime: string
}

export interface SeriesDetailVO extends SeriesVO {
  posts: PostVO[]
}

export interface PostNavigationVO {
  id: number
  title: string
}

export interface PostSeriesContextVO {
  series: SeriesVO
  prevPost?: PostNavigationVO
  nextPost?: PostNavigationVO
}

export interface CreateSeriesDTO {
  title: string
  description?: string
  coverUrl?: string
  postIds?: number[]
}

export interface UpdateSeriesDTO {
  title?: string
  description?: string
  coverUrl?: string
  status?: number
}

// ── 阅读历史 ──
export interface ReadingHistoryVO {
  id: number
  postId: number
  postTitle: string
  readAt: string
  createTime: string
}

// ── 稍后阅读 ──
export interface ReadLaterVO {
  id: number
  postId: number
  postTitle: string
  createTime: string
}

// ── 勋章 ──
export interface BadgeVO {
  id: number
  code: string
  name: string
  description?: string
  iconUrl?: string
  category: string
  sortOrder: number
  unlockedAt?: string
}

// ── 通知设置 ──
export interface NotificationSettingVO {
  notifyType: string
  label: string
  enabled: boolean
}

// ── 私信聊天 ──
export interface ConversationVO {
  id: number
  otherUserId: number
  otherUsername: string
  otherNickname: string
  otherAvatarUrl?: string
  lastMessage?: string
  lastMessageAt?: string
  unreadCount: number
  createTime: string
}

export interface MessageVO {
  id: number
  conversationId: number
  senderId: number
  content: string
  isRead: boolean
  readAt?: string
  createTime: string
}

// ── 管理后台 ──
export interface AdminLogVO {
  id: number
  adminId: number
  adminName: string
  action: string
  targetType: string
  targetId: number
  detail: string
  ip: string
  createTime: string
}
