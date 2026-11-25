<script setup lang="ts">
import { warpState } from '@/utils/warpNavigation'

const streaks = Array.from({ length: 84 }, (_, index) => ({
  style: {
    '--angle': `${index * 137.508 + (index % 7) * 3.2}deg`,
    '--delay': `${(index % 12) * 0.035}s`,
    '--duration': `${0.72 + (index % 9) * 0.045}s`,
    '--length': `${70 + (index * 37) % 190}px`,
    '--alpha': `${0.28 + (index % 6) * 0.1}`,
  } as Record<string, string>,
}))
</script>

<template>
  <div
    v-if="warpState.active"
    class="warp-transition"
    :class="{ 'is-arriving': warpState.arriving }"
    role="status"
    aria-live="polite"
  >
    <div class="warp-depth" aria-hidden="true" />
    <div class="warp-core" aria-hidden="true" />
    <div class="warp-streaks" aria-hidden="true">
      <i v-for="(streak, index) in streaks" :key="index" :style="streak.style" />
    </div>
    <div class="warp-caption">
      <span>MEMORY GALAXY</span>
      <strong>正在穿越回忆星海</strong>
      <small>每一颗星星，都是你们的一段时光</small>
    </div>
  </div>
</template>

<style scoped>
.warp-transition {
  position: fixed;
  z-index: 9999;
  inset: 0;
  display: grid;
  overflow: hidden;
  place-items: center;
  background: #02030a;
  animation: warp-overlay-in 180ms ease both;
}

.warp-transition.is-arriving {
  pointer-events: none;
  animation: warp-overlay-out 650ms ease both;
}

.warp-depth {
  position: absolute;
  inset: -20%;
  background:
    radial-gradient(circle at center, rgba(111, 143, 214, 0.2), transparent 12%),
    radial-gradient(circle at center, transparent 24%, rgba(31, 42, 89, 0.24) 48%, transparent 72%);
  animation: warp-depth 900ms ease-in both;
}

.warp-core {
  position: absolute;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: radial-gradient(circle, #fff 0 4%, #bcd8ff 12%, rgba(111, 143, 214, 0.42) 30%, transparent 70%);
  filter: blur(0.4px);
  animation: warp-core 900ms ease-in both;
}

.warp-streaks {
  position: absolute;
  inset: 0;
}

.warp-streaks i {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 1px;
  height: var(--length);
  background: linear-gradient(180deg, transparent, rgba(220, 233, 255, var(--alpha)), #fff);
  box-shadow: 0 0 6px rgba(173, 205, 255, 0.4);
  opacity: 0;
  transform-origin: 50% 0;
  animation: warp-streak var(--duration) var(--delay) cubic-bezier(0.2, 0.7, 0.3, 1) both;
}

.warp-caption {
  position: relative;
  z-index: 2;
  display: flex;
  padding: 24px;
  flex-direction: column;
  align-items: center;
  color: #d8c9ad;
  text-align: center;
  animation: warp-caption 900ms ease both;
}

.warp-caption span {
  color: #8f8268;
  font-size: 9px;
  letter-spacing: 0.32em;
}

.warp-caption strong {
  margin-top: 14px;
  color: #f1e9da;
  font-family: 'Songti SC', 'STSong', Georgia, serif;
  font-size: clamp(22px, 3vw, 34px);
  font-weight: 400;
  letter-spacing: 0.12em;
}

.warp-caption small {
  margin-top: 12px;
  color: #737c93;
  font-size: 10px;
  letter-spacing: 0.1em;
}

@keyframes warp-overlay-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes warp-overlay-out {
  from { opacity: 1; }
  to { opacity: 0; }
}

@keyframes warp-depth {
  from { opacity: 0.4; transform: scale(0.72); }
  to { opacity: 1; transform: scale(1.25); }
}

@keyframes warp-core {
  0% { opacity: 0; transform: scale(0.2); }
  35% { opacity: 1; transform: scale(1.4); }
  100% { opacity: 0.9; transform: scale(5.5); }
}

@keyframes warp-streak {
  0% { opacity: 0; transform: rotate(var(--angle)) translateY(-4vh) scaleY(0.05); }
  18% { opacity: var(--alpha); }
  100% { opacity: 0; transform: rotate(var(--angle)) translateY(-82vh) scaleY(1.35); }
}

@keyframes warp-caption {
  0% { opacity: 0; transform: translateY(8px); }
  25%, 70% { opacity: 1; transform: translateY(0); }
  100% { opacity: 0; transform: translateY(-5px); }
}

@media (prefers-reduced-motion: reduce) {
  .warp-transition,
  .warp-transition * {
    animation: none !important;
  }
}
</style>
