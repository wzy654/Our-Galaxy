<script setup lang="ts">
import { useRouter } from 'vue-router'
import { navigateWithWarp } from '@/utils/warpNavigation'

defineProps<{
  authenticated: boolean
}>()

const router = useRouter()

function enterGalaxy(event: MouseEvent) {
  event.preventDefault()
  void navigateWithWarp(router, '/memories')
}
</script>

<template>
  <section class="hero" aria-labelledby="hero-title">
    <div class="orbital-system" aria-hidden="true">
      <span class="orbit orbit-outer" />
      <span class="orbit orbit-inner" />
      <span class="planet planet-warm" />
      <span class="planet planet-cool" />
      <span class="orbit-particle" />
    </div>

    <div class="hero-content">
      <p class="eyebrow">
        <span /> A PRIVATE UNIVERSE FOR TWO <span />
      </p>
      <h1 id="hero-title">我们的宇宙<span class="title-dot">.</span></h1>
      <p class="hero-subtitle">记录过去，陪伴现在，创造未来。</p>
      <p class="hero-description">把共同经历收藏成星光，让每一个平凡日子都有迹可循。</p>

      <div class="hero-actions">
        <button v-if="authenticated" type="button" class="primary-action" @click="enterGalaxy">
          进入回忆星海 <span aria-hidden="true">↗</span>
        </button>
        <template v-else>
          <RouterLink class="primary-action" to="/register">
            创建我们的宇宙 <span aria-hidden="true">↗</span>
          </RouterLink>
          <RouterLink class="secondary-action" to="/login">已有账号，登录</RouterLink>
        </template>
      </div>

      <p class="hero-note"><span /> 只属于两个人的私人数字空间</p>
    </div>

    <a class="scroll-cue" href="#universe-archive">
      <span>探索我们的宇宙</span>
      <i aria-hidden="true">↓</i>
    </a>
  </section>
</template>

<style scoped lang="scss">
.hero {
  position: relative;
  display: grid;
  min-height: calc(100svh - 92px);
  padding: 90px 24px 76px;
  place-items: center;
  isolation: isolate;
  text-align: center;
}

.hero::before {
  position: absolute;
  z-index: -1;
  width: min(760px, 90vw);
  height: 560px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(9, 12, 28, 0.12) 20%, rgba(9, 12, 28, 0.52) 62%, transparent 75%);
  content: '';
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 2;
  display: flex;
  max-width: 900px;
  flex-direction: column;
  align-items: center;
}

.eyebrow {
  display: flex;
  gap: 14px;
  align-items: center;
  margin: 0 0 32px;
  color: #a9a38f;
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.32em;
  line-height: 1.6;
  animation: reveal 1.2s 120ms both;
}

.eyebrow span {
  width: 30px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(216, 187, 128, 0.65));
}

.eyebrow span:last-child {
  transform: rotate(180deg);
}

h1 {
  margin: 0;
  color: #f5efe3;
  font-family: 'Songti SC', 'STSong', 'Noto Serif CJK SC', 'Source Han Serif SC', Georgia, serif;
  font-size: clamp(68px, 8.2vw, 126px);
  font-weight: 400;
  letter-spacing: 0.055em;
  line-height: 1.08;
  text-shadow: 0 8px 60px rgba(0, 0, 0, 0.28);
  text-wrap: balance;
  animation: reveal 1.4s 220ms both;
}

.title-dot {
  display: inline-block;
  margin-left: 0.02em;
  color: #d8bb80;
  font-family: Georgia, serif;
  font-size: 0.82em;
  transform: translateY(-0.04em);
}

.hero-subtitle {
  margin: 30px 0 0;
  color: #d8c9ad;
  font-family: 'Songti SC', 'STSong', 'Noto Serif CJK SC', Georgia, serif;
  font-size: clamp(17px, 1.65vw, 23px);
  font-weight: 400;
  letter-spacing: 0.2em;
  line-height: 1.8;
  animation: reveal 1.3s 420ms both;
}

.hero-description {
  max-width: 540px;
  margin: 18px 0 0;
  color: #8d95aa;
  font-size: 13px;
  font-weight: 300;
  letter-spacing: 0.08em;
  line-height: 2;
  animation: reveal 1.3s 540ms both;
}

.hero-actions {
  display: flex;
  margin-top: 38px;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  animation: reveal 1.3s 660ms both;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  min-height: 46px;
  padding: 0 24px;
  align-items: center;
  justify-content: center;
  gap: 18px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-decoration: none;
  transition: border-color 240ms ease, background 240ms ease, color 240ms ease, transform 240ms ease, box-shadow 240ms ease;
}

.primary-action {
  border: 1px solid #d8bb80;
  background: #d8bb80;
  box-shadow: 0 8px 30px rgba(216, 187, 128, 0.08);
  color: #171526;
}

.primary-action:hover {
  background: #e4c994;
  box-shadow: 0 10px 36px rgba(216, 187, 128, 0.16);
  transform: translateY(-2px);
}

.primary-action span {
  font-size: 15px;
  transition: transform 240ms ease;
}

.primary-action:hover span {
  transform: translate(2px, -2px);
}

.secondary-action {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(13, 17, 34, 0.24);
  color: #b6bdd0;
}

.secondary-action:hover {
  border-color: rgba(216, 187, 128, 0.35);
  background: rgba(216, 187, 128, 0.05);
  color: #e6d7b8;
  transform: translateY(-2px);
}

.hero-note {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 22px 0 0;
  color: #626b82;
  font-size: 10px;
  letter-spacing: 0.12em;
  animation: reveal 1.3s 760ms both;
}

.hero-note span {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #8e7c5b;
}

.orbital-system {
  position: absolute;
  z-index: 0;
  top: 48%;
  left: 50%;
  width: min(690px, 92vw);
  height: 480px;
  opacity: 0;
  pointer-events: none;
  transform: translate(-50%, -50%);
  animation: cosmic-reveal 2.8s 200ms forwards;
}

.orbit {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid rgba(216, 187, 128, 0.13);
  border-radius: 50%;
  transform: translate(-50%, -50%) rotate(-17deg);
}

.orbit-outer {
  width: 100%;
  height: 68%;
  border-color: rgba(184, 191, 216, 0.1);
}

.orbit-inner {
  width: 68%;
  height: 44%;
  border-color: rgba(216, 187, 128, 0.12);
  transform: translate(-50%, -50%) rotate(22deg);
}

.planet {
  position: absolute;
  border-radius: 50%;
  animation: drift 9s ease-in-out infinite alternate;
}

.planet-warm {
  top: 29%;
  left: 17%;
  width: 10px;
  height: 10px;
  background: radial-gradient(circle at 32% 28%, #f3ddb0, #a6814c 70%);
  box-shadow: 0 0 22px rgba(216, 187, 128, 0.28);
}

.planet-cool {
  right: 15%;
  bottom: 28%;
  width: 7px;
  height: 7px;
  background: radial-gradient(circle at 32% 28%, #c9d7ee, #647a9e 72%);
  box-shadow: 0 0 18px rgba(130, 157, 204, 0.25);
  animation-delay: -4s;
}

.orbit-particle {
  position: absolute;
  top: 18%;
  right: 28%;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(244, 231, 202, 0.7);
  box-shadow: 0 0 10px rgba(244, 231, 202, 0.28);
}

.scroll-cue {
  position: absolute;
  bottom: 26px;
  left: 50%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  color: #69718a;
  font-size: 9px;
  letter-spacing: 0.18em;
  text-decoration: none;
  transform: translateX(-50%);
  animation: reveal 1.4s 1s both;
  transition: color 220ms ease;
}

.scroll-cue:hover {
  color: #c5b28e;
}

.scroll-cue i {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  font-size: 12px;
  font-style: normal;
  animation: cue 2.8s ease-in-out infinite;
}

@keyframes reveal {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cosmic-reveal {
  from { opacity: 0; transform: translate(-50%, -50%) scale(0.96); }
  to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
}

@keyframes drift {
  from { transform: translateY(-4px); }
  to { transform: translateY(5px); }
}

@keyframes cue {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(4px); }
}

@media (max-width: 768px) {
  .hero {
    min-height: calc(100svh - 76px);
    padding: 74px 20px 68px;
  }

  h1 {
    font-size: clamp(58px, 12vw, 92px);
  }

  .orbital-system {
    height: 400px;
  }
}

@media (max-width: 480px) {
  .hero {
    padding-inline: 16px;
  }

  .eyebrow {
    gap: 9px;
    margin-bottom: 27px;
    font-size: 8px;
    letter-spacing: 0.19em;
  }

  .eyebrow span {
    width: 18px;
  }

  h1 {
    font-size: clamp(48px, 15vw, 68px);
    letter-spacing: 0.035em;
  }

  .hero-subtitle {
    margin-top: 25px;
    font-size: 15px;
    letter-spacing: 0.1em;
  }

  .hero-description {
    max-width: 300px;
    font-size: 12px;
    letter-spacing: 0.04em;
  }

  .hero-actions {
    width: 100%;
    margin-top: 32px;
  }

  .primary-action,
  .secondary-action {
    min-height: 44px;
    padding-inline: 20px;
  }

  .orbital-system {
    top: 46%;
    width: 118vw;
    height: 340px;
  }

  .scroll-cue {
    bottom: 18px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero *,
  .hero::before {
    animation: none !important;
    transition: none !important;
  }

  .orbital-system {
    opacity: 0.8;
  }
}
</style>
