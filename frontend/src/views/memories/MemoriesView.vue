<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MemoryGalaxyScene from '@/components/memories/MemoryGalaxyScene.vue'
import MemoryFormDialog from '@/components/memories/MemoryFormDialog.vue'
import MemoryDetailPanel from '@/components/memories/MemoryDetailPanel.vue'
import SpaceSetupDialog from '@/components/SpaceSetupDialog.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import UserProfileDialog from '@/components/UserProfileDialog.vue'
import { cancelWarp } from '@/utils/warpNavigation'
import { fetchCurrentSpace } from '@/api/spaces'
import { deleteMemory, fetchMemories, fetchMemory } from '@/api/memories'
import { useAuthStore } from '@/stores/auth'
import type { Memory } from '@/types/api'

const router = useRouter()
const auth = useAuthStore()
const memories = ref<Memory[]>([])
const hasSpace = ref(false)
const loading = ref(false)
const error = ref('')
const selectedId = ref<number | null>(null)
const detailMemory = ref<Memory | null>(null)
const detailOpen = ref(false)
const formOpen = ref(false)
const spaceDialogOpen = ref(false)
const profileDialogOpen = ref(false)
const editingMemory = ref<Memory | null>(null)
const sceneRef = ref<InstanceType<typeof MemoryGalaxyScene>>()
let requestId = 0
let detailRequestId = 0

const selectedMemory = computed(
  () => detailMemory.value || memories.value.find((memory) => memory.id === selectedId.value) || null,
)

async function load(initial = false) {
  const currentRequest = ++requestId
  if (initial) loading.value = true
  error.value = ''
  try {
    const space = await fetchCurrentSpace()
    if (currentRequest !== requestId) return
    hasSpace.value = Boolean(space)
    memories.value = space ? await fetchMemories() : []
    if (currentRequest !== requestId) return
    if (selectedId.value && !memories.value.some((memory) => memory.id === selectedId.value)) {
      closeDetail()
    }
  } catch (cause) {
    if (currentRequest === requestId) error.value = cause instanceof Error ? cause.message : '回忆加载失败'
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

async function selectMemory(memoryId: number) {
  const currentRequest = ++detailRequestId
  selectedId.value = memoryId
  detailMemory.value = memories.value.find((memory) => memory.id === memoryId) || null
  detailOpen.value = true
  try {
    const memory = await fetchMemory(memoryId)
    if (currentRequest !== detailRequestId || selectedId.value !== memoryId) return
    detailMemory.value = memory
  } catch (cause) {
    if (currentRequest === detailRequestId) {
      ElMessage.error(cause instanceof Error ? cause.message : '回忆详情加载失败')
    }
  }
}

function closeDetail() {
  detailRequestId += 1
  detailOpen.value = false
  selectedId.value = null
  detailMemory.value = null
}

function handleDetailVisibility(visible: boolean) {
  if (!visible) closeDetail()
}

function openCreate() {
  editingMemory.value = null
  formOpen.value = true
}

function openEdit(memory: Memory) {
  editingMemory.value = memory
  closeDetail()
  formOpen.value = true
}

async function handleSaved() {
  await load()
}

async function handleSpaceChanged() {
  await load()
}

async function confirmDelete(memory: Memory) {
  try {
    await ElMessageBox.confirm(`确定删除“${memory.title}”吗？此操作会隐藏这段回忆。`, '删除回忆', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteMemory(memory.id)
    ElMessage.success('回忆已删除')
    if (selectedId.value === memory.id) closeDetail()
    await load()
  } catch (cause) {
    if (cause !== 'cancel' && cause !== 'close') {
      ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
    }
  }
}

function returnHome() {
  void router.push('/')
}

onMounted(() => load(true))
onBeforeUnmount(() => {
  requestId += 1
  detailRequestId += 1
  cancelWarp()
})
</script>

<template>
  <main class="memories-universe">
    <div class="universe-backdrop" aria-hidden="true" />
    <div class="universe-vignette" aria-hidden="true" />

    <MemoryGalaxyScene
      v-if="hasSpace && !loading && !error"
      ref="sceneRef"
      :memories="memories"
      :selected-id="selectedId"
      @select="selectMemory"
    />

    <div v-if="loading" class="galaxy-state" role="status">
      <span class="state-orbit" />
    </div>

    <div v-else-if="error" class="galaxy-state" role="alert">
      <span class="state-mark">!</span>
      <p>{{ error }}</p>
      <button type="button" @click="load(true)">重新连接</button>
    </div>

    <div v-else-if="!hasSpace" class="galaxy-state">
      <span class="state-star">✦</span>
      <p>先建立或进入你们的宇宙</p>
      <button type="button" @click="spaceDialogOpen = true">开始</button>
    </div>

    <div v-else-if="!memories.length" class="empty-universe">
      <span class="empty-orbit" aria-hidden="true"><i /></span>
      <p>这里还很安静</p>
      <button type="button" @click="openCreate">点亮第一段回忆</button>
    </div>

    <nav v-if="!loading && !error" class="galaxy-controls" aria-label="星图操作">
      <button type="button" aria-label="返回宇宙首页" title="返回首页" @click="returnHome">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M19 12H5M11 6L5 12L11 18" />
        </svg>
      </button>
      <button type="button" aria-label="宇宙与邀请" title="宇宙与邀请" @click="spaceDialogOpen = true">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="9" cy="12" r="5" />
          <circle cx="15" cy="12" r="5" />
        </svg>
      </button>
      <span aria-hidden="true" />
      <button v-if="hasSpace" type="button" class="create-control" aria-label="记录新回忆" title="记录新回忆" @click="openCreate">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M12 5V19M5 12H19" />
        </svg>
      </button>
      <button v-if="hasSpace && memories.length" type="button" aria-label="重置星图视角" title="重置视角" @click="sceneRef?.resetView()">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="12" cy="12" r="6" />
          <circle cx="12" cy="12" r="1.5" />
          <path d="M12 2V4M12 20V22M2 12H4M20 12H22" />
        </svg>
      </button>
      <button type="button" class="profile-control" aria-label="个人资料" title="个人资料" @click="profileDialogOpen = true">
        <UserAvatar :file-id="auth.user?.avatarFileId || null" :name="auth.user?.nickname || '星'" :size="22" />
      </button>
    </nav>

    <MemoryDetailPanel
      :model-value="detailOpen"
      :memory="selectedMemory"
      @update:model-value="handleDetailVisibility"
      @edit="openEdit"
      @delete="confirmDelete"
    />
    <MemoryFormDialog v-model="formOpen" :memory="editingMemory" @saved="handleSaved" />
    <SpaceSetupDialog v-model="spaceDialogOpen" @changed="handleSpaceChanged" />
    <UserProfileDialog v-model="profileDialogOpen" />
  </main>
</template>

<style scoped lang="scss">
.memories-universe {
  position: relative;
  height: 100svh;
  overflow: hidden;
  background: #03050b;
  color: #d8ddea;
}

.universe-backdrop,
.universe-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.universe-backdrop {
  background:
    radial-gradient(circle at 18% 22%, rgba(45, 63, 108, 0.14), transparent 34%),
    radial-gradient(circle at 78% 70%, rgba(73, 46, 86, 0.12), transparent 36%),
    #03050b;
}

.universe-vignette {
  z-index: 1;
  background: radial-gradient(ellipse at center, transparent 46%, rgba(1, 3, 8, 0.4) 100%);
}

.galaxy-state {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 5;
  display: flex;
  width: min(360px, calc(100% - 48px));
  align-items: center;
  flex-direction: column;
  color: #8993aa;
  gap: 18px;
  text-align: center;
  transform: translate(-50%, -50%);
}

.galaxy-state p {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.galaxy-state button,
.empty-universe button {
  padding: 9px 16px;
  border: 1px solid rgba(216, 187, 128, 0.25);
  border-radius: 999px;
  background: rgba(216, 187, 128, 0.05);
  color: #c9b48d;
  cursor: pointer;
  font: inherit;
  font-size: 10px;
  letter-spacing: 0.06em;
}

.state-star {
  color: #dec28d;
  font-size: 42px;
  text-shadow: 0 0 34px rgba(216, 187, 128, 0.4);
}

.state-mark {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid rgba(201, 143, 143, 0.35);
  border-radius: 50%;
  color: #d59c9c;
  font-family: Georgia, serif;
  font-size: 18px;
}

.state-orbit {
  width: 42px;
  height: 42px;
  border: 1px solid rgba(216, 187, 128, 0.12);
  border-top-color: rgba(216, 187, 128, 0.75);
  border-radius: 50%;
  animation: state-orbit 1.3s linear infinite;
}

.empty-universe {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 4;
  display: flex;
  align-items: center;
  flex-direction: column;
  color: #707c94;
  gap: 15px;
  text-align: center;
  transform: translate(-50%, -50%);
}

.empty-universe p {
  margin: 0;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 15px;
  letter-spacing: 0.12em;
}

.empty-orbit {
  position: relative;
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border: 1px solid rgba(216, 187, 128, 0.16);
  border-radius: 50%;
}

.empty-orbit::after {
  position: absolute;
  width: 66px;
  height: 20px;
  border: 1px solid rgba(216, 187, 128, 0.13);
  border-radius: 50%;
  content: '';
  transform: rotate(-24deg);
}

.empty-orbit i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbb790;
  box-shadow: 0 0 22px rgba(216, 187, 128, 0.3);
}

.galaxy-controls {
  position: absolute;
  bottom: max(24px, env(safe-area-inset-bottom));
  left: 50%;
  z-index: 6;
  display: flex;
  padding: 5px;
  align-items: center;
  border: 1px solid rgba(190, 205, 232, 0.09);
  border-radius: 999px;
  background: rgba(5, 8, 16, 0.44);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  gap: 3px;
  opacity: 0.42;
  transform: translateX(-50%);
  transition: opacity 300ms ease, border-color 300ms ease, background 300ms ease;
  backdrop-filter: blur(16px);
}

.galaxy-controls:hover,
.galaxy-controls:focus-within {
  border-color: rgba(216, 187, 128, 0.18);
  background: rgba(5, 8, 16, 0.72);
  opacity: 1;
}

.galaxy-controls button {
  display: grid;
  width: 36px;
  height: 36px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #8994ab;
  cursor: pointer;
  transition: color 200ms ease, background 200ms ease;
}

.galaxy-controls button:hover {
  background: rgba(216, 187, 128, 0.08);
  color: #e2cfaa;
}

.galaxy-controls button.create-control {
  background: rgba(216, 187, 128, 0.11);
  color: #d8c29b;
}

.galaxy-controls svg {
  width: 17px;
  height: 17px;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.2;
}

.galaxy-controls > span {
  width: 1px;
  height: 14px;
  margin: 0 3px;
  background: rgba(255, 255, 255, 0.08);
}

@keyframes state-orbit {
  to { transform: rotate(360deg); }
}

@media (hover: none) {
  .galaxy-controls {
    opacity: 0.72;
  }
}

@media (prefers-reduced-motion: reduce) {
  .state-orbit {
    animation: none;
  }

  .galaxy-controls,
  .galaxy-controls button {
    transition: none;
  }
}
</style>
