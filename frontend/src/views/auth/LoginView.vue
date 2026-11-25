<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const submitting = ref(false)

const form = reactive({
  email: '',
  password: '',
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度应为 8–72 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  submitting.value = true
  try {
    await auth.login(form)
    ElMessage.success('欢迎回到我们的宇宙')
    router.push(String(route.query.redirect || '/space'))
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-view">
    <div class="auth-card">
      <p class="auth-kicker">WELCOME BACK</p>
      <h1>登录我们的宇宙</h1>
      <p class="auth-intro">继续收藏属于两个人的星光。</p>

      <el-form :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" autocomplete="email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button class="submit-button" type="primary" :loading="submitting" @click="submit">
          登录
        </el-button>
      </el-form>

      <p class="auth-switch">
        还没有账号？<RouterLink to="/register">创建账号</RouterLink>
      </p>
    </div>
  </section>
</template>

<style scoped lang="scss">
.auth-view {
  display: grid;
  width: min(1180px, calc(100% - 48px));
  min-height: calc(100svh - 180px);
  margin: 0 auto;
  place-items: center;
}

.auth-card {
  width: min(440px, 100%);
  padding: 38px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(13, 17, 34, 0.78);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(18px);
}

.auth-kicker {
  margin: 0 0 12px;
  color: #b8a477;
  font-size: 9px;
  letter-spacing: 0.28em;
}

h1 {
  margin: 0;
  font-family: 'Songti SC', Georgia, serif;
  font-size: 30px;
  font-weight: 400;
}

.auth-intro {
  margin: 12px 0 28px;
  color: #8d95aa;
  font-size: 13px;
}

.submit-button {
  width: 100%;
  margin-top: 6px;
  border-color: #d8bb80;
  background: #d8bb80;
  color: #171526;
  font-weight: 600;
}

.auth-switch {
  margin: 24px 0 0;
  color: #8d95aa;
  font-size: 12px;
  text-align: center;
}

.auth-switch a {
  color: #d8c9ad;
  text-decoration: none;
}

@media (max-width: 640px) {
  .auth-view {
    width: calc(100% - 32px);
  }

  .auth-card {
    padding: 28px 22px;
  }
}
</style>
