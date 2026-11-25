<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MemoryGalaxyScene from '@/components/memories/MemoryGalaxyScene.vue'
import MemoryFormDialog from '@/components/memories/MemoryFormDialog.vue'
import MemoryDetailPanel from '@/components/memories/MemoryDetailPanel.vue'
import { cancelWarp } from '@/utils/warpNavigation'
import { fetchCurrentSpace } from '@/api/spaces'
import { deleteMemory, fetchMemories } from '@/api/memories'
import type { Memory } from '@/types/api'

const memories = ref<Memory[]>([])
const hasSpace = ref(false)
const loading = ref(false)
const refreshing = ref(false)
const error = ref('')
const search = ref('')
const indexOpen = ref(false)
const selectedId = ref<number | null>(null)
const detailOpen = ref(false)
const formOpen = ref(false)
const editingMemory = ref<Memory | null>(null)
const sceneRef = ref<InstanceType<typeof MemoryGalaxyScene>>()
let requestId = 0

const filteredMemories = computed(() => {
  const keyword = search.value.trim().toLocaleLowerCase()
  const list = keyword
    ? memories.value.filter((memory) => [
        memory.title,
        memory.content || '',
        memory.eventDate,
        memory.locationName || '',
        memory.creatorName,
        ...memory.tags,
      ].join(' ').toLocaleLowerCase().includes(keyword))
    : memories.value
  return [...list].sort((a, b) => b.eventDate.localeCompare(a.eventDate))
})

const selectedMemory = computed(() => memories.value.find((memory) => memory.id === selectedId.value) || null)
const planetCount = computed(() => memories.value.filter((memory) => memory.media.length > 0).length)
const tagCount = computed(() => new Set(memories.value.flatMap((memory) => memory.tags)).size)

async function load(initial = false) {
  const currentRequest = ++requestId
  if (initial) loading.value = true
  else refreshing.value = true
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
    if (currentRequest === requestId) {
      loading.value = false
      refreshing.value = false
    }
  }
}

function selectMemory(memoryId: number) {
  selectedId.value = memoryId
  detailOpen.value = true
}

function closeDetail() {
  detailOpen.value = false
  selectedId.value = null
}

function openCreate() {
  editingMemory.value = null
  formOpen.value = true
}

function openEdit(memory: Memory) {
  editingMemory.value = memory
  detailOpen.value = false
  selectedId.value = null
  formOpen.value = true
}

async function handleSaved() {
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

onMounted(() => load(true))
onBeforeUnmount(() => {
  requestId += 1
  cancelWarp()
})
</script>

<template>
  <main class="memories-universe">
    <div class="universe-backdrop" aria-hidden="true" />
    <div class="universe-vignette" aria-hidden="true" />

    <MemoryGalaxyScene
      v-if="hasSpace && !loading && !error && memories.length"
      ref="sceneRef"
      :memories="memories"
      :selected-id="selectedId"
      @select="selectMemory"
    />

    <header v-if="hasSpace" class="galaxy-intro">
      <div>
        <p><span /> MEMORY GALAXY</p>
        <h1>共同回忆星图</h1>
        <small>每一颗星星，都是你们共同经过的一段时光。</small>
      </div>
      <div class="galaxy-stats" aria-label="回忆统计">
        <div><strong>{{ memories.length }}</strong><span>回忆星辰</span></div>
        <div><strong>{{ planetCount }}</strong><span>影像星球</span></div>
        <div><strong>{{ tagCount }}</strong><span>共同标签</span></div>
      </div>
    </header>

    <div v-if="loading" class="galaxy-state" role="status">
      <span class="state-orbit" />
      <h2>正在读取回忆星光</h2>
      <p>属于你们的宇宙，正在缓缓展开。</p>
    </div>

    <div v-else-if="error" class="galaxy-state" role="alert">
      <span class="state-mark">!</span>
      <h2>星光暂时迷路了</h2>
      <p>{{ error }}</p>
      <el-button @click="load(true)">重新连接星图</el-button>
    </div>

    <div v-else-if="!hasSpace" class="galaxy-state">
      <span class="state-star">✦</span>
      <h2>宇宙还没有创建</h2>
      <p>先创建或加入情侣空间，再开始记录共同回忆。</p>
      <RouterLink to="/space">前往我们的空间 <span>↗</span></RouterLink>
    </div>

    <template v-else>
      <aside class="memory-index" :class="{ 'is-open': indexOpen }" aria-label="回忆索引">
        <div class="index-heading">
          <div>
            <span>STAR INDEX</span>
            <h2>回忆索引</h2>
          </div>
          <button type="button" class="index-close" aria-label="关闭回忆索引" @click="indexOpen = false">×</button>
        </div>

        <label class="index-search">
          <span aria-hidden="true">⌕</span>
          <input v-model="search" type="search" placeholder="搜索标题、地点或标签" />
        </label>

        <div class="index-summary">
          <span>{{ filteredMemories.length }} / {{ memories.length }} 颗星辰</span>
          <span v-if="refreshing">同步中…</span>
        </div>

        <div class="index-list">
          <button
            v-for="memory in filteredMemories"
            :key="memory.id"
            type="button"
            class="index-item"
            :class="{ 'is-selected': selectedId === memory.id }"
            @click="selectMemory(memory.id)"
          >
            <span class="index-star" :class="`star-${memory.starType.toLowerCase()}`">
              {{ memory.media.length ? '◉' : '✦' }}
            </span>
            <span class="index-copy">
              <strong>{{ memory.title }}</strong>
              <small>{{ memory.eventDate }} · {{ memory.locationName || memory.tags[0] || '私人回忆' }}</small>
            </span>
            <span class="index-arrow">↗</span>
          </button>

          <div v-if="!filteredMemories.length" class="index-empty">
            <span>✦</span>
            <p>{{ search ? '没有找到对应的回忆星辰' : '这里还没有回忆' }}</p>
          </div>
        </div>

        <button v-if="memories.length" type="button" class="index-create" @click="openCreate">
          <span>＋</span> 记录新回忆
        </button>
      </aside>

      <button
        v-if="!memories.length"
        type="button"
        class="empty-create"
        @click="openCreate"
      >
        点亮第一段回忆 <span>↗</span>
      </button>

      <div v-if="memories.length" class="galaxy-toolbar">
        <button type="button" aria-label="放大星图" @click="sceneRef?.zoom(0.84)">＋</button>
        <button type="button" aria-label="缩小星图" @click="sceneRef?.zoom(1.18)">−</button>
        <span />
        <button type="button" aria-label="重置星图视角" @click="sceneRef?.resetView()">⌖</button>
      </div>

      <button
        v-if="memories.length"
        type="button"
        class="index-toggle"
        :aria-expanded="indexOpen"
        @click="indexOpen = !indexOpen"
      >
        <span>☰</span> 回忆索引
      </button>

      <div v-if="memories.length" class="galaxy-hint" aria-hidden="true">
        <span>拖动旋转</span><i /> <span>滚轮缩放</span><i /> <span>点击星辰打开回忆</span>
      </div>

      <div class="galaxy-coordinate" aria-hidden="true">
        <span>PRIVATE UNIVERSE</span>
        <small>只属于两个人的时空坐标</small>
      </div>
    </template>

    <MemoryDetailPanel
      v-model="detailOpen"
      :memory="selectedMemory"
      @edit="openEdit"
      @delete="confirmDelete"
    />
    <MemoryFormDialog v-model="formOpen" :memory="editingMemory" @saved="handleSaved" />
  </main>
</template>

<style scoped lang="scss">
.memories-universe {
  position: relative;
  min-height: 100svh;
  overflow: hidden;
  background: #040711;
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
    radial-gradient(circle at 12% 16%, rgba(59, 72, 123, 0.2), transparent 30%),
    radial-gradient(circle at 76% 68%, rgba(72, 50, 91, 0.16), transparent 34%),
    radial-gradient(circle at 44% 38%, rgba(27, 71, 85, 0.09), transparent 30%),
    #040711;
}

.universe-backdrop::before {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 8% 18%, rgba(255, 255, 255, 0.7) 0 1px, transparent 1.5px),
    radial-gradient(circle at 72% 12%, rgba(224, 202, 160, 0.6) 0 1px, transparent 1.5px),
    radial-gradient(circle at 31% 78%, rgba(169, 191, 235, 0.55) 0 1px, transparent 1.5px),
    radial-gradient(circle at 88% 56%, rgba(255, 255, 255, 0.5) 0 1px, transparent 1.5px);
  background-size: 173px 149px, 227px 191px, 263px 211px, 311px 257px;
  content: '';
  opacity: 0.35;
}

.universe-vignette {
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(3, 5, 12, 0.62), transparent 22%, transparent 76%, rgba(3, 5, 12, 0.58)),
    radial-gradient(ellipse at center, transparent 40%, rgba(2, 4, 10, 0.48) 100%);
}

.galaxy-intro {
  position: absolute;
  top: 126px;
  left: max(32px, calc((100vw - 1280px) / 2));
  z-index: 5;
  display: flex;
  align-items: flex-end;
  gap: 48px;
  pointer-events: none;
}

.galaxy-intro p {
  display: flex;
  margin: 0 0 12px;
  align-items: center;
  color: #9b8969;
  gap: 9px;
  font-size: 9px;
  letter-spacing: 0.22em;
}

.galaxy-intro p span {
  width: 20px;
  height: 1px;
  background: #9b8969;
}

.galaxy-intro h1 {
  margin: 0;
  color: #ece4d6;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: clamp(30px, 3vw, 42px);
  font-weight: 400;
  letter-spacing: 0.08em;
  text-shadow: 0 2px 24px rgba(0, 0, 0, 0.5);
}

.galaxy-intro small {
  display: block;
  margin-top: 12px;
  color: #778198;
  font-size: 11px;
  font-weight: 300;
  letter-spacing: 0.04em;
}

.galaxy-stats {
  display: flex;
  padding-bottom: 3px;
  gap: 26px;
}

.galaxy-stats div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.galaxy-stats strong {
  color: #cbb790;
  font-family: Georgia, serif;
  font-size: 19px;
  font-weight: 400;
}

.galaxy-stats span {
  color: #626c83;
  font-size: 8px;
  letter-spacing: 0.08em;
}

.galaxy-state {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 5;
  display: flex;
  width: min(430px, calc(100% - 40px));
  padding: 48px 32px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(8, 12, 24, 0.72);
  box-shadow: 0 24px 90px rgba(0, 0, 0, 0.3);
  flex-direction: column;
  text-align: center;
  transform: translate(-50%, -50%);
  backdrop-filter: blur(18px);
}

.galaxy-state h2 {
  margin: 18px 0 10px;
  color: #e9e0d0;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 25px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.galaxy-state p {
  margin: 0;
  color: #818ba2;
  font-size: 12px;
  line-height: 1.8;
}

.galaxy-state a {
  display: inline-flex;
  margin-top: 24px;
  align-items: center;
  color: #d8bb80;
  gap: 8px;
  font-size: 12px;
  text-decoration: none;
}

.state-star {
  color: #dec28d;
  font-size: 48px;
  text-shadow: 0 0 34px rgba(216, 187, 128, 0.42);
}

.state-mark {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  border: 1px solid rgba(201, 143, 143, 0.4);
  border-radius: 50%;
  color: #d59c9c;
  font-family: Georgia, serif;
  font-size: 22px;
}

.state-orbit {
  width: 48px;
  height: 48px;
  border: 1px solid rgba(216, 187, 128, 0.14);
  border-top-color: rgba(216, 187, 128, 0.85);
  border-radius: 50%;
  animation: state-orbit 1.3s linear infinite;
}

.memory-index {
  position: absolute;
  top: 126px;
  right: max(32px, calc((100vw - 1280px) / 2));
  bottom: 30px;
  z-index: 6;
  display: flex;
  width: 296px;
  min-height: 0;
  padding: 22px 18px 18px;
  border: 1px solid rgba(164, 184, 220, 0.1);
  border-radius: 14px;
  background: linear-gradient(160deg, rgba(17, 22, 39, 0.76), rgba(7, 10, 20, 0.86));
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.24);
  flex-direction: column;
  backdrop-filter: blur(18px);
}

.index-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.index-heading span {
  color: #746b5b;
  font-size: 8px;
  letter-spacing: 0.2em;
}

.index-heading h2 {
  margin: 7px 0 0;
  color: #e1d8c8;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: 19px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

.index-close {
  display: none;
  padding: 0;
  border: 0;
  background: transparent;
  color: #9aa4b9;
  font-size: 22px;
  cursor: pointer;
}

.index-search {
  display: flex;
  height: 38px;
  margin-top: 18px;
  padding: 0 11px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 8px;
  background: rgba(3, 6, 14, 0.5);
  gap: 8px;
}

.index-search span {
  color: #747e94;
  font-size: 17px;
}

.index-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #d2d7e3;
  font: inherit;
  font-size: 10px;
}

.index-search input::placeholder {
  color: #596278;
}

.index-summary {
  display: flex;
  margin: 12px 1px 8px;
  justify-content: space-between;
  color: #5f697f;
  font-size: 8px;
  letter-spacing: 0.04em;
}

.index-list {
  min-height: 0;
  margin: 0 -6px;
  padding: 0 6px 8px;
  overflow-y: auto;
  scrollbar-color: rgba(216, 187, 128, 0.2) transparent;
  scrollbar-width: thin;
}

.index-item {
  display: flex;
  width: 100%;
  padding: 12px 8px;
  align-items: center;
  border: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.045);
  background: transparent;
  color: inherit;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  transition: background 180ms ease;
}

.index-item:hover,
.index-item.is-selected {
  border-radius: 7px;
  background: rgba(216, 187, 128, 0.055);
}

.index-star {
  display: grid;
  width: 30px;
  height: 30px;
  flex: none;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 50%;
  color: #c9d9ff;
  font-size: 13px;
  text-shadow: 0 0 12px currentColor;
}

.star-golden {
  color: #f2cf8c;
}

.star-rose {
  color: #efa6b9;
}

.star-blue {
  color: #78d9ef;
}

.index-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 5px;
}

.index-copy strong {
  overflow: hidden;
  color: #c8cede;
  font-size: 11px;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.index-copy small {
  overflow: hidden;
  color: #667188;
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.index-arrow {
  color: #75664c;
  font-size: 12px;
  opacity: 0;
  transition: opacity 180ms ease;
}

.index-item:hover .index-arrow,
.index-item.is-selected .index-arrow {
  opacity: 1;
}

.index-empty {
  display: flex;
  padding: 38px 10px;
  align-items: center;
  color: #656f86;
  flex-direction: column;
  gap: 12px;
  text-align: center;
}

.index-empty span {
  color: #9b8969;
}

.index-empty p {
  margin: 0;
  font-size: 10px;
}

.index-create {
  display: flex;
  width: 100%;
  height: 38px;
  margin-top: 10px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(216, 187, 128, 0.18);
  border-radius: 8px;
  background: rgba(216, 187, 128, 0.055);
  color: #c7b28d;
  gap: 8px;
  font-size: 10px;
  letter-spacing: 0.06em;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease;
}

.index-create:hover {
  border-color: rgba(216, 187, 128, 0.38);
  background: rgba(216, 187, 128, 0.09);
}

.empty-create {
  position: absolute;
  bottom: 84px;
  left: 50%;
  z-index: 5;
  display: inline-flex;
  padding: 13px 22px;
  align-items: center;
  border: 1px solid rgba(216, 187, 128, 0.3);
  border-radius: 8px;
  background: rgba(216, 187, 128, 0.08);
  color: #dcc59d;
  gap: 12px;
  font-size: 12px;
  cursor: pointer;
  transform: translateX(-50%);
}

.galaxy-toolbar {
  position: absolute;
  bottom: 32px;
  left: max(32px, calc((100vw - 1280px) / 2));
  z-index: 6;
  display: flex;
  height: 38px;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(8, 11, 22, 0.7);
  backdrop-filter: blur(12px);
}

.galaxy-toolbar button {
  width: 38px;
  height: 36px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #a6afc2;
  font-size: 17px;
  cursor: pointer;
}

.galaxy-toolbar button:hover {
  color: #e2c995;
}

.galaxy-toolbar span {
  width: 1px;
  height: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.galaxy-hint {
  position: absolute;
  bottom: 43px;
  left: calc(50% + 80px);
  z-index: 5;
  display: flex;
  align-items: center;
  color: #59647b;
  gap: 10px;
  font-size: 8px;
  letter-spacing: 0.06em;
  transform: translateX(-50%);
  pointer-events: none;
}

.galaxy-hint i {
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: #75664c;
}

.galaxy-coordinate {
  position: absolute;
  right: 348px;
  bottom: 37px;
  z-index: 5;
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 5px;
  pointer-events: none;
}

.galaxy-coordinate span {
  color: #665e50;
  font-size: 7px;
  letter-spacing: 0.2em;
}

.galaxy-coordinate small {
  color: #475166;
  font-size: 8px;
}

.index-toggle {
  display: none;
}

@keyframes state-orbit {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .galaxy-intro {
    gap: 26px;
  }

  .galaxy-stats {
    gap: 18px;
  }

  .galaxy-hint {
    display: none;
  }

  .galaxy-coordinate {
    display: none;
  }
}

@media (max-width: 768px) {
  .galaxy-intro {
    top: 104px;
    right: 20px;
    left: 20px;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
  }

  .galaxy-intro h1 {
    font-size: 29px;
  }

  .galaxy-intro small {
    max-width: 230px;
    font-size: 9px;
    line-height: 1.7;
  }

  .galaxy-stats {
    padding-top: 5px;
    gap: 14px;
  }

  .galaxy-stats div:nth-child(3) {
    display: none;
  }

  .galaxy-stats strong {
    font-size: 16px;
  }

  .memory-index {
    top: auto;
    right: 16px;
    bottom: 16px;
    left: 16px;
    width: auto;
    height: min(64svh, 560px);
    opacity: 0;
    transform: translateY(24px);
    transition: opacity 240ms ease, transform 240ms ease;
    pointer-events: none;
  }

  .memory-index.is-open {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }

  .index-close {
    display: block;
  }

  .index-toggle {
    position: absolute;
    right: 20px;
    bottom: 26px;
    z-index: 7;
    display: inline-flex;
    height: 40px;
    padding: 0 14px;
    align-items: center;
    border: 1px solid rgba(216, 187, 128, 0.22);
    border-radius: 8px;
    background: rgba(8, 11, 22, 0.82);
    color: #cbb991;
    gap: 8px;
    font-size: 10px;
    backdrop-filter: blur(12px);
  }

  .galaxy-toolbar {
    bottom: 26px;
    left: 20px;
  }

  .galaxy-state {
    top: 48%;
    padding: 38px 24px;
  }

  .empty-create {
    bottom: 74px;
  }
}

@media (max-width: 480px) {
  .galaxy-intro {
    top: 96px;
  }

  .galaxy-intro p {
    margin-bottom: 9px;
    font-size: 7px;
  }

  .galaxy-intro h1 {
    font-size: 25px;
  }

  .galaxy-intro small {
    max-width: 205px;
    font-size: 8px;
  }

  .galaxy-stats {
    gap: 12px;
  }

  .galaxy-stats div:nth-child(2) {
    display: none;
  }

  .galaxy-stats strong {
    font-size: 15px;
  }

  .galaxy-stats span {
    font-size: 7px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .state-orbit {
    animation: none;
  }

  .memory-index,
  .index-item,
  .index-arrow,
  .index-create {
    transition: none;
  }
}
</style>
