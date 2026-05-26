<template>
  <div class="not-found-page">
    <div class="nf-code">404</div>
    <h2>页面不存在</h2>
    <p>你访问的页面可能已被删除或移动</p>
    <el-button type="primary" size="large" round @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style lang="scss" scoped>
.not-found-page {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  min-height: 60vh; text-align: center;
  padding: var(--spacing-lg);
}
.nf-code {
  font-size: clamp(64px, 12vw, 96px); font-weight: var(--font-weight-bold);
  color: var(--color-text-tertiary); line-height: 1; margin-bottom: var(--spacing-md);
  opacity: 0.3;
}
h2 { font-size: var(--font-size-xl); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: var(--spacing-sm); }
p { font-size: var(--font-size-base); color: var(--color-text-secondary); margin-bottom: var(--spacing-xl); }
</style>
