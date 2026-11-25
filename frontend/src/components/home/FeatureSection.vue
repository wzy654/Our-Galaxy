<script setup lang="ts">
import { useRouter } from 'vue-router'
import { navigateWithWarp } from '@/utils/warpNavigation'

const router = useRouter()

function handleFeatureClick(to: string, event: MouseEvent) {
  if (to !== '/memories') return
  event.preventDefault()
  void navigateWithWarp(router, to)
}

const features = [
  {
    number: '01',
    title: 'Memory Galaxy',
    subtitle: '共同回忆星图',
    description: '收藏一起经历的每一个瞬间。',
    to: '/memories',
    icon: 'galaxy',
    available: true,
  },
  {
    number: '02',
    title: 'Time Capsule',
    subtitle: '写给未来的信',
    description: '让现在的心意，在未来某一天被打开。',
    to: '/time-capsule',
    icon: 'capsule',
    available: false,
  },
  {
    number: '03',
    title: 'AI Love Assistant',
    subtitle: 'AI 情感助手',
    description: '让每一段故事都能继续生长。',
    to: '/ai-assistant',
    icon: 'assistant',
    available: false,
  },
]
</script>

<template>
  <section class="features-section" aria-labelledby="features-title">
    <div class="section-heading">
      <div>
        <p class="section-kicker"><span /> CONTINUE YOUR STORY</p>
        <h2 id="features-title">让故事继续生长</h2>
      </div>
      <p class="section-description">从此刻出发，把平凡的日子写进属于两个人的宇宙。</p>
    </div>

    <nav class="feature-grid" aria-label="宇宙功能">
      <RouterLink
        v-for="feature in features"
        :key="feature.to"
        class="feature-item"
        :to="feature.to"
        @click.capture="handleFeatureClick(feature.to, $event)"
      >
        <div class="feature-meta">
          <span class="feature-number">{{ feature.number }}</span>
          <span class="feature-status" :class="{ 'is-available': feature.available }">
            {{ feature.available ? '已开放' : '即将开放' }}
          </span>
        </div>

        <div class="feature-symbol" aria-hidden="true">
          <svg v-if="feature.icon === 'galaxy'" viewBox="0 0 72 72" fill="none">
            <ellipse cx="36" cy="36" rx="29" ry="12" transform="rotate(-24 36 36)" />
            <ellipse cx="36" cy="36" rx="23" ry="20" transform="rotate(38 36 36)" />
            <circle class="symbol-core" cx="36" cy="36" r="6" />
            <circle class="symbol-moon" cx="59" cy="23" r="2.5" />
            <circle class="symbol-star" cx="17" cy="48" r="1.5" />
          </svg>
          <svg v-else-if="feature.icon === 'capsule'" viewBox="0 0 72 72" fill="none">
            <rect x="14" y="23" width="44" height="31" rx="4" transform="rotate(-7 14 23)" />
            <path d="M16 26L36 41L56 25" />
            <path class="symbol-orbit" d="M9 49C20 63 52 67 63 47" />
            <circle class="symbol-star" cx="55" cy="14" r="2" />
            <path class="symbol-spark" d="M18 11V19M14 15H22" />
          </svg>
          <svg v-else viewBox="0 0 72 72" fill="none">
            <path class="symbol-core" d="M36 12C38 27 45 34 60 36C45 38 38 45 36 60C34 45 27 38 12 36C27 34 34 27 36 12Z" />
            <ellipse class="symbol-orbit" cx="36" cy="36" rx="29" ry="14" transform="rotate(-32 36 36)" />
            <circle class="symbol-moon" cx="57" cy="20" r="2.5" />
            <circle class="symbol-star" cx="16" cy="53" r="1.5" />
          </svg>
        </div>

        <div class="feature-copy">
          <p class="feature-title">{{ feature.title }}</p>
          <h3>{{ feature.subtitle }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
        </div>

        <span class="feature-arrow" aria-hidden="true">↗</span>
      </RouterLink>
    </nav>
  </section>
</template>

<style scoped lang="scss">
.features-section {
  padding: 30px 0 120px;
}

.section-heading {
  display: flex;
  margin-bottom: 48px;
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

.feature-grid {
  display: grid;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  grid-template-columns: repeat(3, 1fr);
}

.feature-item {
  position: relative;
  display: flex;
  isolation: isolate;
  min-width: 0;
  min-height: 330px;
  padding: 28px 30px 32px;
  overflow: hidden;
  flex-direction: column;
  color: inherit;
  text-decoration: none;
  transition: background 400ms ease, transform 400ms ease;
}

.feature-item + .feature-item {
  border-left: 1px solid rgba(255, 255, 255, 0.07);
}

.feature-item::before {
  position: absolute;
  top: -1px;
  right: 0;
  left: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(216, 187, 128, 0.75), transparent);
  content: '';
  opacity: 0;
  transform: scaleX(0.4);
  transition: opacity 400ms ease, transform 400ms ease;
}

.feature-item::after {
  position: absolute;
  z-index: -1;
  top: -90px;
  left: 10%;
  width: 80%;
  height: 260px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(216, 187, 128, 0.06), transparent 68%);
  content: '';
  opacity: 0;
  transition: opacity 400ms ease;
}

.feature-item:hover {
  background: rgba(216, 187, 128, 0.018);
  transform: translateY(-3px);
}

.feature-item:hover::before,
.feature-item:hover::after {
  opacity: 1;
}

.feature-item:hover::before {
  transform: scaleX(1);
}

.feature-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feature-number {
  color: #665d73;
  font-family: Georgia, serif;
  font-size: 11px;
  letter-spacing: 0.12em;
}

.feature-status {
  color: #5f586d;
  font-size: 9px;
  letter-spacing: 0.08em;
}

.feature-status.is-available {
  color: #897c63;
}

.feature-symbol {
  display: grid;
  width: 76px;
  height: 76px;
  margin: 35px 0 28px;
  place-items: center;
  color: #a99778;
  transition: color 350ms ease, transform 500ms ease;
}

.feature-symbol svg {
  width: 72px;
  height: 72px;
  overflow: visible;
  stroke: currentColor;
  stroke-width: 0.8;
}

.feature-symbol .symbol-core {
  fill: rgba(216, 187, 128, 0.1);
}

.feature-symbol .symbol-moon {
  fill: #b9a27b;
  stroke: none;
}

.feature-symbol .symbol-star {
  fill: #8f98b1;
  stroke: none;
}

.feature-symbol .symbol-orbit {
  opacity: 0.45;
  stroke-dasharray: 2 4;
}

.feature-symbol .symbol-spark {
  opacity: 0.7;
}

.feature-item:hover .feature-symbol {
  color: #d8bb80;
  transform: rotate(-4deg) scale(1.04);
}

.feature-title {
  margin: 0 0 9px;
  color: #8a8fa2;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 11px;
  letter-spacing: 0.1em;
}

.feature-copy h3 {
  margin: 0;
  color: #ddd7cb;
  font-family: 'Songti SC', 'STSong', 'Noto Serif CJK SC', Georgia, serif;
  font-size: 21px;
  font-weight: 400;
  letter-spacing: 0.08em;
}

.feature-description {
  max-width: 250px;
  margin: 13px 0 0;
  color: #687188;
  font-size: 11px;
  font-weight: 300;
  letter-spacing: 0.04em;
  line-height: 1.9;
}

.feature-arrow {
  position: absolute;
  right: 28px;
  bottom: 30px;
  color: #736650;
  font-size: 16px;
  opacity: 0;
  transform: translate(-5px, 5px);
  transition: opacity 350ms ease, transform 350ms ease;
}

.feature-item:hover .feature-arrow {
  opacity: 1;
  transform: translate(0, 0);
}

@media (max-width: 820px) {
  .features-section {
    padding-bottom: 88px;
  }

  .section-heading {
    margin-bottom: 34px;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .section-description {
    max-width: 300px;
    text-align: left;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .feature-item {
    display: grid;
    min-height: 0;
    padding: 26px 12px 28px;
    grid-template-columns: 72px 1fr;
    gap: 0 24px;
    align-items: center;
  }

  .feature-item + .feature-item {
    border-top: 1px solid rgba(255, 255, 255, 0.07);
    border-left: 0;
  }

  .feature-meta {
    position: absolute;
    top: 22px;
    right: 8px;
    left: 108px;
    pointer-events: none;
  }

  .feature-number {
    margin-right: auto;
  }

  .feature-symbol {
    width: 72px;
    height: 72px;
    margin: 0;
    grid-row: span 2;
  }

  .feature-copy {
    padding-top: 22px;
  }

  .feature-title {
    margin-bottom: 7px;
  }

  .feature-copy h3 {
    font-size: 19px;
  }

  .feature-description {
    margin-top: 9px;
  }

  .feature-arrow {
    right: 10px;
    bottom: 24px;
    opacity: 0.55;
    transform: none;
  }
}

@media (max-width: 480px) {
  .features-section {
    padding-top: 10px;
  }

  .feature-item {
    padding-inline: 4px;
    grid-template-columns: 60px 1fr;
    gap: 0 17px;
  }

  .feature-symbol {
    width: 60px;
    height: 60px;
  }

  .feature-symbol svg {
    width: 60px;
    height: 60px;
  }

  .feature-meta {
    left: 81px;
  }

  .feature-copy h3 {
    font-size: 18px;
  }

  .feature-description {
    font-size: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .feature-item,
  .feature-item::before,
  .feature-item::after,
  .feature-symbol,
  .feature-arrow {
    transition: none;
  }

  .feature-item:hover {
    transform: none;
  }

  .feature-item:hover .feature-symbol {
    transform: none;
  }
}
</style>
