<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const submitting = ref(false)

const form = reactive({
  nickname: '',
  email: '',
  password: '',
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 64, message: '昵称不能超过 64 个字符', trigger: 'blur' },
  ],
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
    await auth.register(form)
    ElMessage.success('账号创建成功，欢迎来到我们的宇宙')
    router.push('/memories')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-view">
    <div class="auth-card">
      <p class="auth-kicker">A NEW STAR IS BORN</p>
      <h1>创建你的账号</h1>
      <p class="auth-intro">下一步，邀请最重要的人进入同一个宇宙。</p>

      <el-form :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="64" placeholder="希望对方如何称呼你" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" autocomplete="email" placeholder="you@example.com" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-button class="submit-button" type="primary" :loading="submitting" @click="submit">
          创建账号
        </el-button>
      </el-form>

      <p class="auth-switch">
        已有账号？<RouterLink to="/login">直接登录</RouterLink>
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
  letter-spacing: 0.24em;
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
  line-height: 1.7;
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
