<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createInvitation,
  createSpace,
  fetchCurrentSpace,
  joinSpace,
} from '@/api/spaces'
import type { CoupleSpace, Invitation } from '@/types/api'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  changed: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const space = ref<CoupleSpace | null>(null)
const invitation = ref<Invitation | null>(null)
const mode = ref<'create' | 'join'>('create')
const loading = ref(false)
const submitting = ref(false)
const createForm = reactive({
  name: '',
  anniversaryDate: '',
  description: '',
})
const joinForm = reactive({ code: '' })

watch(() => props.modelValue, async (open) => {
  if (!open) return
  invitation.value = null
  mode.value = 'create'
  await load()
})

async function load() {
  loading.value = true
  try {
    space.value = await fetchCurrentSpace()
  } catch (cause) {
    space.value = null
    ElMessage.error(cause instanceof Error ? cause.message : '宇宙状态读取失败')
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请先为宇宙取一个名字')
    return
  }
  submitting.value = true
  try {
    space.value = await createSpace({
      name: createForm.name.trim(),
      anniversaryDate: createForm.anniversaryDate || null,
      description: createForm.description.trim(),
    })
    createForm.description = ''
    ElMessage.success('宇宙已创建')
    emit('changed')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '创建失败')
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
    space.value = await joinSpace(joinForm.code.trim())
    joinForm.code = ''
    ElMessage.success('已进入同一个宇宙')
    emit('changed')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加入失败')
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
</script>

<template>
  <el-dialog
    v-model="visible"
    class="space-dialog"
    width="min(440px, calc(100vw - 32px))"
    align-center
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    :show-close="false"
  >
    <div class="space-dialog-content">
      <header class="dialog-heading">
        <div>
          <p>PRIVATE UNIVERSE</p>
          <h2>{{ space ? '你们的宇宙' : mode === 'create' ? '建立你们的宇宙' : '进入同一个宇宙' }}</h2>
        </div>
        <button type="button" aria-label="关闭" @click="visible = false">×</button>
      </header>

      <p v-if="loading" class="dialog-status">正在寻找属于你们的星光…</p>

      <template v-else-if="space">
        <div class="space-summary">
          <span class="space-orbit" aria-hidden="true"><i /></span>
          <div>
            <h3>{{ space.name }}</h3>
            <p v-if="space.anniversaryDate">纪念日 · {{ space.anniversaryDate }}</p>
            <p v-else-if="space.description">{{ space.description }}</p>
          </div>
        </div>

        <div class="member-row" aria-label="宇宙成员">
          <span v-for="member in space.members" :key="member.userId">{{ member.nickname }}</span>
          <span v-if="space.memberCount < 2" class="is-empty">等待另一半</span>
        </div>

        <div v-if="space.role === 'OWNER' && space.memberCount < 2" class="invitation-block">
          <button type="button" class="text-action" @click="generateInvitation">
            {{ invitation ? '重新生成邀请码' : '邀请另一半' }} <span>↗</span>
          </button>
          <div v-if="invitation" class="invitation-code">
            <code>{{ invitation.code }}</code>
            <button type="button" @click="copyInvitation">复制</button>
            <small>有效期至 {{ new Date(invitation.expiresAt).toLocaleString() }}</small>
          </div>
        </div>

        <button type="button" class="dialog-primary" @click="visible = false">回到星海</button>
      </template>

      <template v-else>
        <div class="mode-switch" role="tablist" aria-label="宇宙加入方式">
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'create'"
            :class="{ 'is-active': mode === 'create' }"
            @click="mode = 'create'"
          >
            创建宇宙
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'join'"
            :class="{ 'is-active': mode === 'join' }"
            @click="mode = 'join'"
          >
            加入宇宙
          </button>
        </div>

        <form v-if="mode === 'create'" class="space-form" @submit.prevent="submitCreate">
          <label>
            <span>宇宙名称</span>
            <input v-model="createForm.name" maxlength="100" placeholder="例如：小星和月月的宇宙" autofocus />
          </label>
          <label>
            <span>纪念日 <small>可选</small></span>
            <input v-model="createForm.anniversaryDate" type="date" />
          </label>
          <label>
            <span>一句话介绍 <small>可选</small></span>
            <textarea v-model="createForm.description" maxlength="500" rows="3" placeholder="写下属于你们的故事" />
          </label>
          <button type="submit" class="dialog-primary" :disabled="submitting">
            {{ submitting ? '正在建立…' : '建立宇宙' }}
          </button>
        </form>

        <form v-else class="space-form" @submit.prevent="submitJoin">
          <label>
            <span>邀请码</span>
            <input v-model="joinForm.code" maxlength="32" placeholder="输入另一半分享的邀请码" autofocus />
          </label>
          <button type="submit" class="dialog-primary" :disabled="submitting">
            {{ submitting ? '正在进入…' : '进入宇宙' }}
          </button>
        </form>
      </template>
    </div>
  </el-dialog>
</template>

<style scoped lang="scss">
:global(.space-dialog) {
  --el-dialog-bg-color: #090d19;
  --el-dialog-title-font-size: 20px;
  padding: 0;
  border: 1px solid rgba(216, 187, 128, 0.16);
  border-radius: 18px;
  background: rgba(9, 13, 25, 0.96);
  box-shadow: 0 32px 120px rgba(0, 0, 0, 0.52);
  overflow: hidden;
  backdrop-filter: blur(24px);
}

:global(.space-dialog .el-dialog__header),
:global(.space-dialog .el-dialog__body) {
  padding: 0;
  margin: 0;
}

.space-dialog-content {
  padding: 30px;
}

.dialog-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.dialog-heading p {
  margin: 0 0 9px;
  color: #8f8268;
  font-size: 8px;
  letter-spacing: 0.24em;
}

.dialog-heading h2 {
  margin: 0;
  color: #eee5d6;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 25px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.dialog-heading > button {
  display: grid;
  width: 30px;
  height: 30px;
  padding: 0;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  background: transparent;
  color: #828ba0;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.dialog-status {
  margin: 28px 0 4px;
  color: #818ba2;
  font-size: 12px;
  text-align: center;
}

.space-summary {
  display: flex;
  margin-top: 28px;
  padding: 22px;
  align-items: center;
  border: 1px solid rgba(216, 187, 128, 0.12);
  border-radius: 14px;
  background: radial-gradient(circle at 12% 20%, rgba(216, 187, 128, 0.07), transparent 58%);
  gap: 18px;
}

.space-orbit {
  position: relative;
  display: grid;
  width: 54px;
  height: 54px;
  flex: none;
  place-items: center;
  border: 1px solid rgba(216, 187, 128, 0.22);
  border-radius: 50%;
}

.space-orbit::after {
  position: absolute;
  width: 68px;
  height: 22px;
  border: 1px solid rgba(216, 187, 128, 0.2);
  border-radius: 50%;
  content: '';
  transform: rotate(-24deg);
}

.space-orbit i {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 25%, #ead8b1, #8c6e42 78%);
  box-shadow: 0 0 22px rgba(216, 187, 128, 0.25);
}

.space-summary h3 {
  margin: 0;
  color: #e3dacb;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 20px;
  font-weight: 400;
}

.space-summary p {
  margin: 8px 0 0;
  color: #7f899f;
  font-size: 11px;
  line-height: 1.6;
}

.member-row {
  display: flex;
  margin-top: 14px;
  flex-wrap: wrap;
  gap: 8px;
}

.member-row span {
  padding: 6px 10px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 999px;
  color: #aeb6c8;
  font-size: 10px;
}

.member-row .is-empty {
  border-style: dashed;
  color: #626d84;
}

.invitation-block {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.text-action {
  padding: 0;
  border: 0;
  background: transparent;
  color: #c9b48d;
  cursor: pointer;
  font: inherit;
  font-size: 11px;
  letter-spacing: 0.05em;
}

.text-action span {
  margin-left: 6px;
}

.invitation-code {
  display: grid;
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(216, 187, 128, 0.12);
  border-radius: 10px;
  background: rgba(2, 5, 12, 0.38);
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
}

.invitation-code code {
  color: #e4d1a9;
  font-size: 17px;
  letter-spacing: 0.16em;
}

.invitation-code button {
  padding: 5px 9px;
  border: 1px solid rgba(216, 187, 128, 0.2);
  border-radius: 5px;
  background: transparent;
  color: #bda77f;
  cursor: pointer;
  font: inherit;
  font-size: 10px;
}

.invitation-code small {
  color: #626d84;
  font-size: 9px;
  grid-column: 1 / -1;
}

.dialog-primary {
  display: grid;
  width: 100%;
  min-height: 44px;
  margin-top: 24px;
  padding: 0 18px;
  place-items: center;
  border: 1px solid rgba(216, 187, 128, 0.5);
  border-radius: 999px;
  background: #d8bb80;
  color: #171526;
  cursor: pointer;
  font: inherit;
  font-size: 11px;
  letter-spacing: 0.08em;
  transition: background 200ms ease, border-color 200ms ease;
}

.dialog-primary:hover {
  border-color: #e4c994;
  background: #e4c994;
}

.dialog-primary:disabled {
  cursor: wait;
  opacity: 0.6;
}

.mode-switch {
  display: grid;
  margin-top: 28px;
  padding: 4px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 999px;
  background: rgba(2, 5, 12, 0.38);
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.mode-switch button {
  min-height: 34px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #737e95;
  cursor: pointer;
  font: inherit;
  font-size: 10px;
}

.mode-switch button.is-active {
  background: rgba(216, 187, 128, 0.1);
  color: #d8c6a4;
}

.space-form {
  display: flex;
  margin-top: 24px;
  flex-direction: column;
  gap: 17px;
}

.space-form label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.space-form label > span {
  color: #9da6ba;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.space-form label small {
  color: #59647b;
  font-size: 9px;
}

.space-form input,
.space-form textarea {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  outline: none;
  background: rgba(2, 5, 12, 0.42);
  color: #d9deea;
  font: inherit;
  font-size: 11px;
  resize: vertical;
  transition: border-color 180ms ease;
}

.space-form input:focus,
.space-form textarea:focus {
  border-color: rgba(216, 187, 128, 0.38);
}

.space-form input::placeholder,
.space-form textarea::placeholder {
  color: #515d74;
}

@media (max-width: 480px) {
  .space-dialog-content {
    padding: 24px;
  }

  .dialog-heading h2 {
    font-size: 22px;
  }
}
</style>
