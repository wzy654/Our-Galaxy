<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import { uploadAvatar } from '@/api/files'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
}>()

const auth = useAuthStore()
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})
const form = reactive({
  nickname: '',
  avatarFileId: null as number | null,
})
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const submitting = ref(false)

watch(() => props.modelValue, (open) => {
  if (!open) return
  form.nickname = auth.user?.nickname || ''
  form.avatarFileId = auth.user?.avatarFileId || null
})

async function handleAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
    ElMessage.error('头像仅支持 JPEG、PNG 或 GIF')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('头像不能超过 10MB')
    return
  }

  uploading.value = true
  try {
    const uploaded = await uploadAvatar(file)
    form.avatarFileId = uploaded.id
    ElMessage.success('头像已更换，保存后生效')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '头像上传失败')
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (!form.nickname.trim()) {
    ElMessage.warning('请填写昵称')
    return
  }
  submitting.value = true
  try {
    await auth.updateProfile({
      nickname: form.nickname.trim(),
      avatarFileId: form.avatarFileId,
    })
    ElMessage.success('个人资料已保存')
    visible.value = false
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '个人资料保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    class="profile-dialog"
    width="min(420px, calc(100vw - 32px))"
    align-center
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    :show-close="false"
  >
    <form class="profile-content" @submit.prevent="save">
      <header class="profile-heading">
        <div>
          <p>PERSONAL ORBIT</p>
          <h2>个人资料</h2>
        </div>
        <button type="button" aria-label="关闭个人资料" @click="visible = false">×</button>
      </header>

      <div class="avatar-editor">
        <UserAvatar :file-id="form.avatarFileId" :name="form.nickname || auth.user?.nickname || '星'" :size="88" />
        <div>
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif" hidden @change="handleAvatar" />
          <button type="button" :disabled="uploading" @click="fileInput?.click()">
            {{ uploading ? '正在上传…' : '更换头像' }}
          </button>
          <button v-if="form.avatarFileId !== null" type="button" :disabled="uploading" @click="form.avatarFileId = null">
            移除头像
          </button>
          <small>JPEG / PNG / GIF，不超过 10MB</small>
        </div>
      </div>

      <label>
        <span>昵称</span>
        <input v-model="form.nickname" maxlength="64" placeholder="你的名字" />
      </label>
      <label>
        <span>邮箱</span>
        <input :value="auth.user?.email || ''" readonly />
      </label>

      <button type="submit" class="profile-save" :disabled="submitting || uploading">
        {{ submitting ? '正在保存…' : '保存资料' }}
      </button>
    </form>
  </el-dialog>
</template>

<style scoped lang="scss">
:global(.profile-dialog) {
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(216, 187, 128, 0.16);
  border-radius: 18px;
  background: rgba(9, 13, 25, 0.97);
  box-shadow: 0 32px 120px rgba(0, 0, 0, 0.52);
  backdrop-filter: blur(24px);
}

:global(.profile-dialog .el-dialog__header),
:global(.profile-dialog .el-dialog__body) {
  padding: 0;
  margin: 0;
}

.profile-content {
  display: flex;
  padding: 28px;
  flex-direction: column;
  gap: 20px;
}

.profile-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.profile-heading p {
  margin: 0 0 8px;
  color: #8f8268;
  font-size: 8px;
  letter-spacing: 0.24em;
}

.profile-heading h2 {
  margin: 0;
  color: #eee5d6;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 24px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.profile-heading > button {
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

.avatar-editor {
  display: flex;
  padding: 20px 0;
  align-items: center;
  border-block: 1px solid rgba(255, 255, 255, 0.06);
  gap: 20px;
}

.avatar-editor > div {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.avatar-editor button {
  padding: 7px 11px;
  border: 1px solid rgba(216, 187, 128, 0.2);
  border-radius: 999px;
  background: rgba(216, 187, 128, 0.05);
  color: #c9b48d;
  cursor: pointer;
  font: inherit;
  font-size: 10px;
}

.avatar-editor button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.avatar-editor small {
  width: 100%;
  color: #626d84;
  font-size: 9px;
}

.profile-content > label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.profile-content > label span {
  color: #9da6ba;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.profile-content > label input {
  width: 100%;
  padding: 11px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  outline: none;
  background: rgba(2, 5, 12, 0.42);
  color: #d9deea;
  font: inherit;
  font-size: 11px;
}

.profile-content > label input:focus {
  border-color: rgba(216, 187, 128, 0.38);
}

.profile-content > label input[readonly] {
  color: #707b91;
}

.profile-save {
  min-height: 44px;
  border: 1px solid rgba(216, 187, 128, 0.5);
  border-radius: 999px;
  background: #d8bb80;
  color: #171526;
  cursor: pointer;
  font: inherit;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.profile-save:disabled {
  cursor: wait;
  opacity: 0.6;
}

@media (max-width: 480px) {
  .profile-content {
    padding: 22px;
  }
}
</style>
