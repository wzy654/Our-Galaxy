<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useSystemStore } from '@/stores/system'

const props = defineProps<{
  authenticated: boolean
  spaceName: string | null
  hasSpace: boolean
  hasAnniversary: boolean
  daysTogether: number | null
  memoryCount: number | null
  nextAnniversaryDays: number | null
  loading: boolean
  error: string
}>()

const emit = defineEmits<{
  retry: []
}>()

const system = useSystemStore()
const isDev = import.meta.env.DEV

const description = computed(() => {
  if (props.loading) return '正在读取属于你们的宇宙微光…'
  if (!props.authenticated) return '登录后，这里会收藏你们宇宙的微光。'
  if (!props.hasSpace) return '创建或加入空间后，每一段相伴都会在这里留下刻度。'
  return `${props.spaceName} 的微光，正在这里被收藏。`
})

function display(value: number | null): string {
  return value === null ? '—' : value.toLocaleString('zh-CN')
}

function note(fallback: string): string {
  if (!props.authenticated) return '登录后可见'
  return fallback
}

onMounted(() => {
  if (isDev) void system.loadHealth()
})
</script>

<template>
  <section id="universe-archive" class="archive-section" aria-labelledby="archive-title">
    <div class="section-heading">
      <div>
        <p class="section-kicker"><span /> OUR UNIVERSE ARCHIVE</p>
        <h2 id="archive-title">我们的宇宙档案</h2>
      </div>
      <p class="section-description">{{ description }}</p>
    </div>

    <div class="archive-grid" :class="{ 'is-loading': loading }" aria-live="polite" :aria-busy="loading">
      <div class="archive-stat">
        <span class="stat-number">01</span>
        <div class="stat-value">
          <strong>{{ display(daysTogether) }}</strong>
          <span v-if="daysTogether !== null">天</span>
        </div>
        <h3>相伴天数</h3>
        <p>{{ note(hasAnniversary ? '自空间纪念日起' : '待设置纪念日') }}</p>
      </div>

      <div class="archive-stat">
        <span class="stat-number">02</span>
        <div class="stat-value">
          <strong>{{ display(memoryCount) }}</strong>
          <span v-if="memoryCount !== null">段</span>
        </div>
        <h3>共同回忆</h3>
        <p>{{ note(hasSpace ? '已收藏的星光' : '先创建或加入空间') }}</p>
      </div>

      <div class="archive-stat">
        <span class="stat-number">03</span>
        <div class="stat-value">
          <strong>—</strong>
        </div>
        <h3>解锁胶囊 <small>即将开放</small></h3>
        <p>写给未来的心意</p>
      </div>

      <div class="archive-stat">
        <span class="stat-number">04</span>
        <div class="stat-value">
          <strong>{{ display(nextAnniversaryDays) }}</strong>
          <span v-if="nextAnniversaryDays !== null">天</span>
        </div>
        <h3>下一纪念日</h3>
        <p>{{ note(hasAnniversary ? '值得再次庆祝' : '待设置纪念日') }}</p>
      </div>
    </div>

    <div v-if="error" class="archive-error" role="status">
      <span>{{ error }}</span>
      <button type="button" @click="emit('retry')">重新读取</button>
    </div>

    <div v-if="isDev" class="dev-status" :class="{ 'is-error': system.error }" aria-live="polite">
      <span class="dev-status-dot" />
      <span>{{ system.loading ? '正在连接后端' : system.error ? '后端连接异常' : '开发服务已连接' }}</span>
      <span v-if="system.health" class="dev-version">{{ system.health.service }} · {{ system.health.version }}</span>
      <button v-if="system.error" type="button" @click="system.loadHealth">重试</button>
    </div>
  </section>
</template>

<style scoped lang="scss">
.archive-section {
  position: relative;
  padding: 108px 0 112px;
  scroll-margin-top: 24px;
}

.section-heading {
  display: flex;
  margin-bottom: 54px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
}

.section-kicker {
  display: flex;
  gap: 10px;
  align-items: center;
  margin: 0 0 17px;
  color: #8f8268;
  font-size: 9px;
  letter-spacing: 0.24em;
}

.section-kicker span {
  width: 18px;
  height: 1px;
  background: #8f8268;
}

h2 {
  margin: 0;
  color: #e9e2d5;
  font-family: 'Songti SC', 'STSong', 'Noto Serif CJK SC', Georgia, serif;
  font-size: clamp(28px, 3vw, 40px);
  font-weight: 400;
  letter-spacing: 0.08em;
}

.section-description {
  max-width: 330px;
  margin: 0 0 4px;
  color: #737c93;
  font-size: 12px;
  font-weight: 300;
  letter-spacing: 0.04em;
  line-height: 1.9;
  text-align: right;
}

.archive-grid {
  display: grid;
  border-top: 1px solid rgba(216, 187, 128, 0.17);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  grid-template-columns: repeat(4, 1fr);
}

.archive-stat {
  position: relative;
  min-width: 0;
  padding: 28px 28px 30px;
  transition: background 300ms ease;
}

.archive-stat + .archive-stat {
  border-left: 1px solid rgba(255, 255, 255, 0.06);
}

.archive-stat:hover {
  background: linear-gradient(180deg, rgba(216, 187, 128, 0.025), transparent);
}

.stat-number {
  display: block;
  margin-bottom: 34px;
  color: #5e576f;
  font-family: Georgia, serif;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.stat-value {
  display: flex;
  min-height: 66px;
  align-items: baseline;
  gap: 8px;
  color: #d8bb80;
}

.stat-value strong {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(38px, 4vw, 54px);
  font-weight: 400;
  letter-spacing: -0.025em;
  line-height: 1;
}

.stat-value > span {
  color: #8f8268;
  font-size: 11px;
}

.archive-stat h3 {
  display: flex;
  margin: 19px 0 0;
  align-items: center;
  gap: 8px;
  color: #c8cbd6;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

.archive-stat h3 small {
  padding: 3px 6px;
  border: 1px solid rgba(216, 187, 128, 0.14);
  border-radius: 3px;
  color: #80745e;
  font-size: 8px;
  font-weight: 400;
  letter-spacing: 0.04em;
}

.archive-stat p {
  margin: 9px 0 0;
  color: #5f687f;
  font-size: 10px;
  letter-spacing: 0.04em;
}

.archive-grid.is-loading .stat-value strong {
  color: #6f6680;
}

.archive-error {
  display: flex;
  padding: 14px 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: #c99494;
  font-size: 11px;
}

.archive-error button,
.dev-status button {
  padding: 4px 8px;
  border: 0;
  background: transparent;
  color: #b9a77f;
  cursor: pointer;
  font: inherit;
}

.dev-status {
  display: flex;
  width: fit-content;
  margin-top: 22px;
  align-items: center;
  gap: 7px;
  color: #525b71;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 9px;
  letter-spacing: 0.02em;
}

.dev-status-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #719b84;
}

.dev-status.is-error .dev-status-dot {
  background: #b87878;
}

.dev-version {
  color: #444d62;
}

@media (max-width: 900px) {
  .archive-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .archive-stat:nth-child(3) {
    border-left: 0;
  }

  .archive-stat:nth-child(n + 3) {
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }
}

@media (max-width: 640px) {
  .archive-section {
    padding: 78px 0 82px;
  }

  .section-heading {
    margin-bottom: 36px;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .section-description {
    max-width: 280px;
    text-align: left;
  }

  .archive-stat {
    padding: 22px 18px 24px;
  }

  .stat-number {
    margin-bottom: 25px;
  }

  .stat-value {
    min-height: 48px;
  }

  .stat-value strong {
    font-size: 38px;
  }

  .archive-stat h3 {
    font-size: 11px;
  }

  .archive-stat h3 small {
    display: none;
  }

  .dev-version {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .archive-stat {
    transition: none;
  }
}
</style>
