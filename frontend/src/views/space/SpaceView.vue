<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createInvitation,
  createSpace,
  fetchCurrentSpace,
  joinSpace,
} from '@/api/spaces'
import type { CoupleSpace, Invitation } from '@/types/api'

const space = ref<CoupleSpace | null>(null)
const invitation = ref<Invitation | null>(null)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

const createForm = reactive({
  name: '',
  anniversaryDate: '',
  description: '',
})
const joinForm = reactive({ code: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    space.value = await fetchCurrentSpace()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '空间加载失败'
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请先为空间取一个名字')
    return
  }
  submitting.value = true
  try {
    space.value = await createSpace({
      name: createForm.name,
      anniversaryDate: createForm.anniversaryDate || null,
      description: createForm.description,
    })
    ElMessage.success('情侣空间创建成功')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '创建空间失败')
  } finally {
    submitting.value = false
  }
}

async function submitJoin() {
  if (!joinForm.code.trim()) {
    ElMessage.warning('请输入邀请码')
    return
  }
  submitting.value = true
  try {
    space.value = await joinSpace(joinForm.code)
    ElMessage.success('已加入情侣空间')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加入空间失败')
  } finally {
    submitting.value = false
  }
}

async function generateInvitation() {
  try {
    invitation.value = await createInvitation()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '邀请码生成失败')
  }
}

async function copyInvitation() {
  if (!invitation.value) return
  try {
    await navigator.clipboard.writeText(invitation.value.code)
    ElMessage.success('邀请码已复制')
  } catch {
    ElMessage.info('请手动复制邀请码')
  }
}

onMounted(load)
</script>

<template>
  <section class="space-view">
    <header class="page-heading">
      <p>OUR UNIVERSE</p>
      <h1>只属于两个人的空间</h1>
      <span>空间身份由后端成员关系校验，前端无法指定或切换其他空间。</span>
    </header>

    <p v-if="loading" class="state-copy">正在寻找你的宇宙…</p>
    <p v-else-if="error" class="state-copy error">{{ error }} <button @click="load">重试</button></p>

    <template v-else-if="space">
      <div class="space-overview">
        <div>
          <p class="section-kicker">CURRENT UNIVERSE</p>
          <h2>{{ space.name }}</h2>
          <p v-if="space.anniversaryDate" class="anniversary">纪念日 · {{ space.anniversaryDate }}</p>
          <p v-if="space.description" class="description">{{ space.description }}</p>
        </div>
        <div class="member-count">
          <strong>{{ space.memberCount }} / 2</strong>
          <span>宇宙成员</span>
        </div>
      </div>

      <div class="members-grid">
        <article v-for="member in space.members" :key="member.userId" class="member-card">
          <span class="member-avatar">{{ member.nickname.slice(0, 1).toUpperCase() }}</span>
          <div><strong>{{ member.nickname }}</strong><small>{{ member.role === 'OWNER' ? '空间所有者' : '另一半' }}</small></div>
        </article>
        <article v-if="space.memberCount < 2" class="member-card empty">
          <span class="member-avatar">+</span>
          <div><strong>等待另一半</strong><small>分享邀请码完成相遇</small></div>
        </article>
      </div>

      <div v-if="space.role === 'OWNER' && space.memberCount < 2" class="invitation-panel">
        <div>
          <h3>邀请你的另一半</h3>
          <p>邀请码仅可使用一次，有效期 7 天。</p>
        </div>
        <el-button type="primary" @click="generateInvitation">生成邀请码</el-button>
        <div v-if="invitation" class="invitation-code">
          <code>{{ invitation.code }}</code>
          <el-button text @click="copyInvitation">复制</el-button>
          <small>有效期至 {{ new Date(invitation.expiresAt).toLocaleString() }}</small>
        </div>
      </div>

      <RouterLink class="primary-link" to="/memories">进入共同回忆 <span>→</span></RouterLink>
    </template>

    <div v-else class="onboarding-grid">
      <article class="onboarding-card">
        <p class="section-kicker">CREATE</p>
        <h2>创建新的宇宙</h2>
        <p>为你们的关系建立一个私密空间。</p>
        <el-form label-position="top">
          <el-form-item label="空间名称">
            <el-input v-model="createForm.name" maxlength="100" placeholder="例如：小星和月月的宇宙" />
          </el-form-item>
          <el-form-item label="纪念日（可选）">
            <el-date-picker
              v-model="createForm.anniversaryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择纪念日"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="一句话介绍（可选）">
            <el-input v-model="createForm.description" maxlength="500" type="textarea" :rows="3" />
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitCreate">创建空间</el-button>
        </el-form>
      </article>

      <article class="onboarding-card">
        <p class="section-kicker">JOIN</p>
        <h2>加入已有的宇宙</h2>
        <p>输入另一半分享的 12 位邀请码。</p>
        <el-form label-position="top" @submit.prevent="submitJoin">
          <el-form-item label="邀请码">
            <el-input v-model="joinForm.code" maxlength="32" placeholder="输入邀请码" @keyup.enter="submitJoin" />
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitJoin">加入空间</el-button>
        </el-form>
      </article>
    </div>
  </section>
</template>

<style scoped lang="scss">
.space-view {
  width: min(1000px, calc(100% - 48px));
  margin: 0 auto;
  padding: 58px 0 80px;
}

.page-heading {
  margin-bottom: 34px;
}

.page-heading p,
.section-kicker {
  margin: 0 0 10px;
  color: #b8a477;
  font-size: 9px;
  letter-spacing: 0.24em;
}

.page-heading h1 {
  margin: 0;
  font-family: 'Songti SC', Georgia, serif;
  font-size: clamp(30px, 5vw, 44px);
  font-weight: 400;
}

.page-heading span {
  display: block;
  margin-top: 12px;
  color: #818aa1;
  font-size: 12px;
  line-height: 1.7;
}

.state-copy {
  padding: 30px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: rgba(13, 17, 34, 0.7);
  color: #9da5ba;
  text-align: center;
}

.state-copy.error {
  color: #e3a3a3;
}

.state-copy button {
  border: 0;
  background: none;
  color: #d8bb80;
  cursor: pointer;
}

.space-overview {
  display: flex;
  padding: 30px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border: 1px solid rgba(216, 187, 128, 0.2);
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(216, 187, 128, 0.09), rgba(13, 17, 34, 0.82));
}

.space-overview h2,
.onboarding-card h2 {
  margin: 0;
  font-family: 'Songti SC', Georgia, serif;
  font-size: 26px;
  font-weight: 400;
}

.anniversary {
  margin: 12px 0 0;
  color: #d8c9ad;
  font-size: 13px;
}

.description {
  margin: 10px 0 0;
  color: #8d95aa;
  font-size: 13px;
  line-height: 1.7;
}

.member-count {
  display: flex;
  min-width: 110px;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.member-count strong {
  color: #d8bb80;
  font-family: Georgia, serif;
  font-size: 28px;
  font-weight: 400;
}

.member-count span {
  color: #818aa1;
  font-size: 10px;
}

.members-grid {
  display: grid;
  margin-top: 18px;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.member-card {
  display: flex;
  padding: 20px;
  align-items: center;
  gap: 14px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 12px;
  background: rgba(13, 17, 34, 0.66);
}

.member-avatar {
  display: grid;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid rgba(216, 187, 128, 0.25);
  border-radius: 50%;
  color: #d8bb80;
  font-family: Georgia, serif;
}

.member-card div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.member-card strong {
  font-size: 14px;
  font-weight: 500;
}

.member-card small,
.invitation-panel p {
  color: #818aa1;
  font-size: 11px;
}

.member-card.empty {
  border-style: dashed;
}

.invitation-panel {
  display: flex;
  margin-top: 18px;
  padding: 22px;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 12px;
  background: rgba(13, 17, 34, 0.66);
}

.invitation-panel h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 500;
}

.invitation-panel p {
  margin: 0;
}

.invitation-panel > .el-button {
  margin-left: auto;
}

.invitation-code {
  display: flex;
  width: 100%;
  padding-top: 16px;
  align-items: center;
  gap: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.invitation-code code {
  color: #e4d1a9;
  font-size: 20px;
  letter-spacing: 0.16em;
}

.invitation-code small {
  margin-left: auto;
  color: #737d94;
  font-size: 10px;
}

.primary-link {
  display: flex;
  width: 100%;
  margin-top: 22px;
  padding: 18px 22px;
  justify-content: space-between;
  border: 1px solid rgba(216, 187, 128, 0.24);
  border-radius: 12px;
  background: rgba(216, 187, 128, 0.08);
  color: #d8c9ad;
  font-size: 14px;
  text-decoration: none;
}

.onboarding-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.onboarding-card {
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(13, 17, 34, 0.72);
}

.onboarding-card > p:not(.section-kicker) {
  margin: 10px 0 24px;
  color: #818aa1;
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 720px) {
  .onboarding-grid,
  .members-grid {
    grid-template-columns: 1fr;
  }

  .invitation-panel > .el-button {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 640px) {
  .space-view {
    width: calc(100% - 32px);
    padding-top: 38px;
  }

  .space-overview {
    padding: 22px;
    align-items: flex-start;
  }

  .onboarding-card {
    padding: 22px;
  }

  .invitation-code {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .invitation-code small {
    width: 100%;
    margin-left: 0;
  }
}
</style>
