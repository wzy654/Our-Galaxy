<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import UserProfileDialog from '@/components/UserProfileDialog.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const profileOpen = ref(false)

async function handleLogout() {
  try {
    await auth.logout()
    ElMessage.success('已安全退出')
    router.push('/')
  } catch {
    ElMessage.error('退出失败，请稍后重试')
  }
}
</script>

<template>
  <header class="app-header">
    <RouterLink class="brand" to="/" aria-label="Our Galaxy 首页">
      <span class="brand-orbit" aria-hidden="true"><i /></span>
      <span class="brand-copy">
        <strong>OUR GALAXY</strong>
        <small>我们的宇宙</small>
      </span>
    </RouterLink>

    <div v-if="auth.isAuthenticated" class="user-actions">
      <button type="button" class="profile-trigger" aria-label="编辑个人资料" title="个人资料" @click="profileOpen = true">
        <UserAvatar :file-id="auth.user?.avatarFileId || null" :name="auth.user?.nickname || '星'" :size="30" />
        <span class="profile-name">{{ auth.user?.nickname }}</span>
      </button>
      <button type="button" @click="handleLogout">退出</button>
    </div>
  </header>
  <UserProfileDialog v-model="profileOpen" />
</template>

<style scoped lang="scss">
.app-header {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(1280px, calc(100% - 64px));
  min-height: 92px;
  margin: 0 auto;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand {
  display: inline-flex;
  gap: 13px;
  align-items: center;
  color: #f7f1e6;
  text-decoration: none;
}

.brand-orbit {
  position: relative;
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border: 1px solid rgba(218, 187, 128, 0.24);
  border-radius: 50%;
  transform: rotate(-24deg);
  transition: transform 500ms ease, border-color 300ms ease;
}

.brand-orbit::before {
  position: absolute;
  top: 7px;
  right: 3px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #b9c4dc;
  box-shadow: 0 0 9px rgba(185, 196, 220, 0.28);
  content: '';
}

.brand-orbit::after {
  position: absolute;
  width: 52px;
  height: 18px;
  border: 1px solid rgba(218, 187, 128, 0.38);
  border-radius: 50%;
  content: '';
}

.brand-orbit i {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 28%, #ead8b1, #a48554 75%);
  box-shadow: 0 0 16px rgba(216, 187, 128, 0.18);
}

.brand:hover .brand-orbit {
  border-color: rgba(218, 187, 128, 0.45);
  transform: rotate(-12deg);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-copy strong {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.24em;
}

.brand-copy small {
  color: #777f94;
  font-size: 9px;
  letter-spacing: 0.34em;
}

.user-actions {
  display: flex;
  align-items: center;
}

.user-actions {
  gap: 10px;
  color: #a8afc1;
  font-size: 11px;
}

.user-actions button {
  padding: 5px 8px;
  border: 1px solid rgba(255, 255, 255, 0.09);
  border-radius: 5px;
  background: transparent;
  color: #8d95aa;
  cursor: pointer;
  font: inherit;
  transition: border-color 200ms ease, color 200ms ease;
}

.user-actions button:hover {
  border-color: rgba(216, 187, 128, 0.25);
  color: #d8c9ad;
}

.user-actions .profile-trigger {
  display: inline-flex;
  padding: 3px 8px 3px 3px;
  align-items: center;
  border-color: transparent;
  border-radius: 999px;
  gap: 8px;
  color: #b1b8c8;
}

@media (max-width: 768px) {
  .app-header {
    width: calc(100% - 40px);
    min-height: 76px;
  }

}

@media (max-width: 640px) {
  .app-header {
    width: calc(100% - 32px);
  }

  .brand {
    gap: 9px;
  }

  .brand-orbit {
    width: 34px;
    height: 34px;
  }

  .brand-orbit::after {
    width: 44px;
    height: 15px;
  }

  .brand-copy strong {
    font-size: 11px;
    letter-spacing: 0.18em;
  }

  .brand-copy small {
    display: none;
  }

  .profile-name {
    display: none;
  }
}
</style>
