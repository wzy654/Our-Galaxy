<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const feature = computed(() => route.name === 'time-capsule'
  ? {
      eyebrow: 'TIME CAPSULE',
      title: '写给未来的信',
      description: '让现在的心意，在未来某一天被打开。',
    }
  : {
      eyebrow: 'AI LOVE ASSISTANT',
      title: 'AI 情感助手',
      description: '让每一段故事都能继续生长。',
    })
</script>

<template>
  <section class="coming-soon" aria-labelledby="coming-soon-title">
    <div class="coming-orbit" aria-hidden="true"><span /></div>
    <p class="coming-kicker">{{ feature.eyebrow }}</p>
    <h1 id="coming-soon-title">{{ feature.title }}</h1>
    <p class="coming-description">{{ feature.description }}</p>
    <div class="coming-status"><span /> 正在准备中 · 即将开放</div>
    <RouterLink class="coming-back" to="/">回到我们的宇宙 <span aria-hidden="true">↗</span></RouterLink>
  </section>
</template>

<style scoped lang="scss">
.coming-soon {
  position: relative;
  display: flex;
  width: min(720px, calc(100% - 40px));
  min-height: calc(100svh - 190px);
  margin: 0 auto;
  padding: 80px 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.coming-orbit {
  position: relative;
  display: grid;
  width: 86px;
  height: 86px;
  margin-bottom: 34px;
  place-items: center;
  border: 1px solid rgba(216, 187, 128, 0.16);
  border-radius: 50%;
}

.coming-orbit::after {
  position: absolute;
  width: 116px;
  height: 38px;
  border: 1px solid rgba(216, 187, 128, 0.12);
  border-radius: 50%;
  content: '';
  transform: rotate(-24deg);
}

.coming-orbit span {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 25%, #ead7ae, #93764b);
  box-shadow: 0 0 25px rgba(216, 187, 128, 0.14);
}

.coming-kicker {
  margin: 0 0 18px;
  color: #8f8268;
  font-size: 9px;
  letter-spacing: 0.28em;
}

h1 {
  margin: 0;
  color: #eee7da;
  font-family: 'Songti SC', 'STSong', 'Noto Serif CJK SC', Georgia, serif;
  font-size: clamp(38px, 6vw, 58px);
  font-weight: 400;
  letter-spacing: 0.08em;
}

.coming-description {
  margin: 22px 0 0;
  color: #7d869c;
  font-size: 13px;
  letter-spacing: 0.06em;
  line-height: 1.9;
}

.coming-status {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 32px;
  color: #8f8268;
  font-size: 10px;
  letter-spacing: 0.1em;
}

.coming-status span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #a28b62;
}

.coming-back {
  display: inline-flex;
  margin-top: 44px;
  padding-bottom: 6px;
  align-items: center;
  gap: 14px;
  border-bottom: 1px solid rgba(216, 187, 128, 0.2);
  color: #c5b28e;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-decoration: none;
  transition: color 200ms ease, border-color 200ms ease;
}

.coming-back:hover {
  border-color: rgba(216, 187, 128, 0.5);
  color: #e1cda6;
}

@media (max-width: 480px) {
  .coming-soon {
    min-height: calc(100svh - 170px);
  }

  .coming-description {
    max-width: 270px;
    font-size: 12px;
  }
}
</style>
