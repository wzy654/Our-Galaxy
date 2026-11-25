import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import type { Memory } from '@/types/api'

type Disposable = { dispose: () => void }

interface MemoryGalaxyOptions {
  onSelect: (memoryId: number) => void
  loadImage: (fileId: number) => Promise<Blob>
  onReady?: () => void
}

interface PlanetVisual {
  memoryId: number
  group: THREE.Group
  disc: THREE.Mesh<THREE.CircleGeometry, THREE.MeshBasicMaterial>
  glow: THREE.Sprite
  basePosition: THREE.Vector3
  phase: number
  speedX: number
  speedY: number
  orbitPhase: number
  size: number
}

export interface MemoryGalaxyHandle {
  updateMemories: (memories: Memory[]) => void
  setSelected: (memoryId: number | null) => void
  resetView: () => void
  zoom: (factor: number) => void
  dispose: () => void
}

interface StarLabel {
  button: HTMLButtonElement
  index: number
}

const MEMORY_VERTEX_SHADER = `
  uniform float uTime;
  uniform float uPixelRatio;
  attribute float aSize;
  attribute float aPhase;
  attribute float aSelected;
  attribute vec3 aColor;
  varying vec3 vColor;
  varying float vAlpha;

  void main() {
    vec4 viewPosition = modelViewMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * viewPosition;
    float pulse = 1.0 + sin(uTime * 1.8 + aPhase) * 0.08;
    float selected = 1.0 + aSelected * 0.75;
    gl_PointSize = clamp(aSize * uPixelRatio * pulse * selected * (58.0 / max(8.0, -viewPosition.z)), 4.0, 42.0 * uPixelRatio);
    vColor = aColor;
    vAlpha = 0.82 + aSelected * 0.18;
  }
`

const MEMORY_FRAGMENT_SHADER = `
  varying vec3 vColor;
  varying float vAlpha;

  void main() {
    vec2 uv = gl_PointCoord - vec2(0.5);
    float radius = length(uv);
    if (radius > 0.5) discard;
    float glow = 1.0 - smoothstep(0.0, 0.5, radius);
    float core = 1.0 - smoothstep(0.0, 0.13, radius);
    float flare = (1.0 - smoothstep(0.0, 0.055, abs(uv.x))) * (1.0 - smoothstep(0.0, 0.48, abs(uv.y)));
    flare += (1.0 - smoothstep(0.0, 0.055, abs(uv.y))) * (1.0 - smoothstep(0.0, 0.48, abs(uv.x)));
    float alpha = (glow * glow * 0.72 + core * 0.28 + flare * 0.16) * vAlpha;
    gl_FragColor = vec4(vColor, alpha);
    #include <colorspace_fragment>
  }
`

const STAR_COLORS: Record<string, THREE.Color> = {
  CLASSIC: new THREE.Color('#c9d9ff'),
  GOLDEN: new THREE.Color('#f2cf8c'),
  ROSE: new THREE.Color('#efa6b9'),
  BLUE: new THREE.Color('#78d9ef'),
}

const CLUSTER_CENTERS = [
  new THREE.Vector3(-9, 5, -2),
  new THREE.Vector3(8, 6, -5),
  new THREE.Vector3(-7, -6, 2),
  new THREE.Vector3(9, -5, -1),
]

function createRandom(seed: number) {
  let value = seed >>> 0
  return () => {
    value += 0x6d2b79f5
    let result = value
    result = Math.imul(result ^ (result >>> 15), result | 1)
    result ^= result + Math.imul(result ^ (result >>> 7), result | 61)
    return ((result ^ (result >>> 14)) >>> 0) / 4294967296
  }
}

function hashValue(value: number) {
  const result = Math.sin(value * 12.9898 + 78.233) * 43758.5453
  return result - Math.floor(result)
}

function isCompactDevice() {
  return window.innerWidth < 768 || (navigator.hardwareConcurrency || 8) <= 4
}

function createGlowTexture() {
  const canvas = document.createElement('canvas')
  canvas.width = 64
  canvas.height = 64
  const context = canvas.getContext('2d')!
  const gradient = context.createRadialGradient(32, 32, 0, 32, 32, 32)
  gradient.addColorStop(0, 'rgba(255,255,255,1)')
  gradient.addColorStop(0.12, 'rgba(255,255,255,0.95)')
  gradient.addColorStop(0.35, 'rgba(255,255,255,0.28)')
  gradient.addColorStop(1, 'rgba(255,255,255,0)')
  context.fillStyle = gradient
  context.fillRect(0, 0, 64, 64)
  return new THREE.CanvasTexture(canvas)
}

function createRingTexture() {
  const canvas = document.createElement('canvas')
  canvas.width = 128
  canvas.height = 128
  const context = canvas.getContext('2d')!
  const gradient = context.createRadialGradient(64, 64, 42, 64, 64, 62)
  gradient.addColorStop(0, 'rgba(216,187,128,0)')
  gradient.addColorStop(0.55, 'rgba(216,187,128,0.08)')
  gradient.addColorStop(0.72, 'rgba(244,224,184,0.9)')
  gradient.addColorStop(0.82, 'rgba(216,187,128,0.18)')
  gradient.addColorStop(1, 'rgba(216,187,128,0)')
  context.fillStyle = gradient
  context.fillRect(0, 0, 128, 128)
  return new THREE.CanvasTexture(canvas)
}

function createNebulaTexture(seed: number, colors: readonly string[]) {
  const random = createRandom(seed)
  const canvas = document.createElement('canvas')
  canvas.width = 256
  canvas.height = 256
  const context = canvas.getContext('2d')!
  context.globalCompositeOperation = 'lighter'

  for (let index = 0; index < 26; index += 1) {
    const angle = random() * Math.PI * 2
    const distance = Math.sqrt(random()) * 74
    const x = 128 + Math.cos(angle) * distance
    const y = 128 + Math.sin(angle) * distance * 0.68
    const radius = 28 + random() * 62
    const gradient = context.createRadialGradient(x, y, 0, x, y, radius)
    gradient.addColorStop(0, `${colors[index % colors.length]}22`)
    gradient.addColorStop(0.42, `${colors[index % colors.length]}12`)
    gradient.addColorStop(1, `${colors[index % colors.length]}00`)
    context.fillStyle = gradient
    context.fillRect(x - radius, y - radius, radius * 2, radius * 2)
  }

  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return texture
}

async function loadImageSource(blob: Blob): Promise<CanvasImageSource> {
  if ('createImageBitmap' in window) return createImageBitmap(blob)

  const url = URL.createObjectURL(blob)
  try {
    const image = new Image()
    image.src = url
    await image.decode()
    return image
  } finally {
    URL.revokeObjectURL(url)
  }
}

function createPlanetTexture(source: CanvasImageSource, compact: boolean) {
  const dimensions = source as { width: number; height: number }
  const width = source instanceof HTMLImageElement ? source.naturalWidth : dimensions.width
  const height = source instanceof HTMLImageElement ? source.naturalHeight : dimensions.height
  const canvas = document.createElement('canvas')
  canvas.width = compact ? 256 : 512
  canvas.height = compact ? 256 : 512
  const context = canvas.getContext('2d')!
  const scale = Math.max(canvas.width / width, canvas.height / height)
  const drawWidth = width * scale
  const drawHeight = height * scale
  context.drawImage(source, (canvas.width - drawWidth) / 2, (canvas.height - drawHeight) / 2, drawWidth, drawHeight)

  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  texture.needsUpdate = true
  return texture
}

function disposeObject(object: THREE.Object3D) {
  object.traverse((child) => {
    const renderable = child as THREE.Object3D & {
      geometry?: THREE.BufferGeometry
      material?: THREE.Material | THREE.Material[]
    }
    renderable.geometry?.dispose()
    if (Array.isArray(renderable.material)) renderable.material.forEach((material) => material.dispose())
    else renderable.material?.dispose()
  })
}

export function mountMemoryGalaxy(
  host: HTMLDivElement,
  labelHost: HTMLDivElement,
  initialMemories: Memory[],
  options: MemoryGalaxyOptions,
): MemoryGalaxyHandle {
  const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'high-performance' })
  renderer.setClearColor(0x000000, 0)
  renderer.outputColorSpace = THREE.SRGBColorSpace
  renderer.toneMapping = THREE.NoToneMapping
  renderer.domElement.setAttribute('aria-hidden', 'true')
  host.appendChild(renderer.domElement)

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(54, 1, 0.1, 180)
  camera.position.set(0, 0, 38)

  const controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.055
  controls.enablePan = false
  controls.minDistance = 13
  controls.maxDistance = 72
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.22
  controls.target.set(0, 0, 0)
  controls.saveState()

  const textures = new Set<Disposable>()
  const glowTexture = createGlowTexture()
  const ringTexture = createRingTexture()
  const nebulaTextures = [
    createNebulaTexture(0x41a7, ['#365a8c', '#50427e', '#2f6f78']),
    createNebulaTexture(0x92d1, ['#6f5a32', '#7b493c', '#3d537a']),
    createNebulaTexture(0x33c9, ['#2e7168', '#3b5688', '#667348']),
  ]
  textures.add(glowTexture)
  textures.add(ringTexture)
  nebulaTextures.forEach((texture) => textures.add(texture))

  const compact = isCompactDevice()
  const random = createRandom(0x71c9a2)
  const backgroundGeometry = new THREE.BufferGeometry()
  const backgroundCount = compact ? 520 : 980
  const backgroundPositions = new Float32Array(backgroundCount * 3)
  const backgroundColors = new Float32Array(backgroundCount * 3)
  for (let index = 0; index < backgroundCount; index += 1) {
    const offset = index * 3
    backgroundPositions[offset] = (random() - 0.5) * 100
    backgroundPositions[offset + 1] = (random() - 0.5) * 64
    backgroundPositions[offset + 2] = -12 - random() * 76
    const color = new THREE.Color(random() > 0.88 ? '#e6c58d' : random() > 0.55 ? '#88a8df' : '#c7d2e8')
    backgroundColors[offset] = color.r
    backgroundColors[offset + 1] = color.g
    backgroundColors[offset + 2] = color.b
  }
  backgroundGeometry.setAttribute('position', new THREE.BufferAttribute(backgroundPositions, 3))
  backgroundGeometry.setAttribute('color', new THREE.BufferAttribute(backgroundColors, 3))
  const backgroundMaterial = new THREE.PointsMaterial({
    size: compact ? 0.2 : 0.16,
    map: glowTexture,
    vertexColors: true,
    transparent: true,
    opacity: 0.72,
    depthWrite: false,
    blending: THREE.AdditiveBlending,
    sizeAttenuation: true,
  })
  const backgroundStars = new THREE.Points(backgroundGeometry, backgroundMaterial)
  scene.add(backgroundStars)

  const nebulaSettings = [
    { position: [-16, 8, -24] as const, scale: [46, 30, 1] as const, opacity: 0.2, texture: nebulaTextures[0] },
    { position: [18, -5, -28] as const, scale: [42, 28, 1] as const, opacity: 0.16, texture: nebulaTextures[1] },
    { position: [1, -17, -20] as const, scale: [38, 24, 1] as const, opacity: 0.12, texture: nebulaTextures[2] },
  ]
  for (const setting of nebulaSettings) {
    const material = new THREE.SpriteMaterial({
      map: setting.texture,
      transparent: true,
      opacity: setting.opacity,
      depthWrite: false,
      depthTest: false,
      blending: THREE.AdditiveBlending,
    })
    const sprite = new THREE.Sprite(material)
    sprite.position.set(setting.position[0], setting.position[1], setting.position[2])
    sprite.scale.set(setting.scale[0], setting.scale[1], setting.scale[2])
    scene.add(sprite)
  }

  const selectionMaterial = new THREE.SpriteMaterial({
    map: ringTexture,
    transparent: true,
    opacity: 0.9,
    depthWrite: false,
    depthTest: false,
    blending: THREE.AdditiveBlending,
  })
  const selectionSprite = new THREE.Sprite(selectionMaterial)
  selectionSprite.scale.set(2.6, 2.6, 1)
  selectionSprite.visible = false
  selectionSprite.renderOrder = 4
  scene.add(selectionSprite)

  let memories: Memory[] = []
  let memoryGroup = new THREE.Group()
  let memoryPoints: THREE.Points | null = null
  let memoryMaterial: THREE.ShaderMaterial | null = null
  let selectedId: number | null = null
  let disposed = false
  let loadGeneration = 0
  const planetVisuals: PlanetVisual[] = []
  const planetTextures = new Set<THREE.Texture>()
  let reducedMotion = false
  let animationFrame: number | null = null
  let lastFrameTime = 0
  let elapsed = 0
  let ready = false
  let rotateTimer: number | null = null
  const labels = new Map<number, StarLabel>()
  const raycaster = new THREE.Raycaster()
  raycaster.params.Points.threshold = 0.72
  const pointer = new THREE.Vector2()
  const projected = new THREE.Vector3()
  const pointerStart = new THREE.Vector2()
  const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  reducedMotion = motionQuery.matches
  controls.autoRotate = !reducedMotion
  controls.enableDamping = !reducedMotion
  scene.add(memoryGroup)

  function clearLabels() {
    for (const label of labels.values()) label.button.remove()
    labels.clear()
  }

  function createLabel(memory: Memory, index: number) {
    if (labels.has(memory.id)) return
    const button = document.createElement('button')
    button.type = 'button'
    button.className = 'memory-star-label'
    button.setAttribute('aria-label', `打开回忆：${memory.title}`)

    const title = document.createElement('span')
    title.className = 'label-title'
    title.textContent = memory.title
    const date = document.createElement('small')
    date.textContent = memory.eventDate
    button.append(title, date)
    button.addEventListener('click', (event) => {
      event.stopPropagation()
      options.onSelect(memory.id)
    })
    labelHost.appendChild(button)
    labels.set(memory.id, { button, index })
  }

  function updateLabels() {
    if (!memoryPoints) return
    const positions = memoryPoints.geometry.getAttribute('position') as THREE.BufferAttribute
    for (const label of labels.values()) {
      projected.fromBufferAttribute(positions, label.index)
      projected.project(camera)
      const visible = projected.z > -1 && projected.z < 1 && Math.abs(projected.x) < 1.08 && Math.abs(projected.y) < 1.08
      label.button.hidden = !visible
      if (!visible) continue
      const x = (projected.x * 0.5 + 0.5) * host.clientWidth
      const y = (-projected.y * 0.5 + 0.5) * host.clientHeight
      const depth = THREE.MathUtils.clamp(1 - (projected.z + 1) * 0.5, 0, 1)
      label.button.style.transform = `translate3d(${x}px, ${y}px, 0) translate(-50%, -50%)`
      label.button.style.opacity = `${0.42 + depth * 0.5}`
      label.button.style.zIndex = `${Math.round(depth * 10)}`
    }
  }

  function renderFrame() {
    if (disposed || document.hidden) return
    if (memoryMaterial) memoryMaterial.uniforms.uTime.value = elapsed
    if (selectionSprite.visible && !reducedMotion) {
      const pulse = 2.6 + Math.sin(elapsed * 2.2) * 0.18
      selectionSprite.scale.set(pulse, pulse, 1)
    }
    for (const planet of planetVisuals) {
      if (!reducedMotion) {
        planet.group.rotation.x = planet.phase + elapsed * planet.speedX
        planet.group.rotation.y = planet.phase * 0.7 + elapsed * planet.speedY
        planet.group.rotation.z = Math.sin(elapsed * 0.16 + planet.orbitPhase) * 0.28
        planet.group.position.set(
          planet.basePosition.x + Math.cos(elapsed * 0.32 + planet.orbitPhase) * 0.16,
          planet.basePosition.y + Math.sin(elapsed * 0.44 + planet.orbitPhase) * 0.28,
          planet.basePosition.z + Math.sin(elapsed * 0.26 + planet.orbitPhase) * 0.2,
        )
      }
      const selectedScale = planet.memoryId === selectedId ? 1.14 : 1
      planet.group.scale.setScalar(selectedScale)
    }
    renderer.render(scene, camera)
    updateLabels()
    if (!ready) {
      ready = true
      options.onReady?.()
    }
  }

  function stopAnimation() {
    if (animationFrame !== null) cancelAnimationFrame(animationFrame)
    animationFrame = null
    lastFrameTime = 0
  }

  function animate(now: number) {
    animationFrame = null
    if (disposed || reducedMotion || document.hidden) return
    const sinceLastFrame = lastFrameTime === 0 ? 33 : now - lastFrameTime
    if (sinceLastFrame + 0.25 < 33) {
      animationFrame = requestAnimationFrame(animate)
      return
    }
    elapsed += Math.min(sinceLastFrame / 1000, 0.05)
    lastFrameTime = now - (sinceLastFrame % 33)
    controls.update()
    renderFrame()
    animationFrame = requestAnimationFrame(animate)
  }

  function startAnimation() {
    if (animationFrame !== null || disposed || reducedMotion || document.hidden) return
    lastFrameTime = 0
    animationFrame = requestAnimationFrame(animate)
  }

  function scheduleRotation() {
    if (rotateTimer !== null) window.clearTimeout(rotateTimer)
    if (!reducedMotion) {
      rotateTimer = window.setTimeout(() => {
        controls.autoRotate = true
      }, 6500)
    }
  }

  function resize() {
    if (disposed) return
    const width = Math.max(1, host.clientWidth)
    const height = Math.max(1, host.clientHeight)
    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, compact ? 1.25 : 1.5))
    renderer.setSize(width, height, false)
    if (memoryMaterial) memoryMaterial.uniforms.uPixelRatio.value = renderer.getPixelRatio()
    renderFrame()
  }

  function createMemoryPlanet(
    memory: Memory,
    index: number,
    positions: Float32Array,
    generation: number,
  ) {
    const fileId = memory.media[0]?.fileId
    if (!fileId) return

    const basePosition = new THREE.Vector3().fromArray(positions, index * 3)
    const group = new THREE.Group()
    group.position.copy(basePosition)
    const phase = hashValue(memory.id + 31) * Math.PI * 2
    group.rotation.set(phase * 0.12, phase * 0.2, phase * 0.08)

    const geometry = new THREE.CircleGeometry(1, 48)
    const material = new THREE.MeshBasicMaterial({
      color: 0xffffff,
      transparent: true,
      opacity: 0,
      side: THREE.DoubleSide,
      depthWrite: false,
      depthTest: false,
    })
    const disc = new THREE.Mesh(geometry, material)
    disc.renderOrder = 3

    const glowMaterial = new THREE.SpriteMaterial({
      map: glowTexture,
      color: STAR_COLORS[memory.starType] || STAR_COLORS.CLASSIC,
      transparent: true,
      opacity: 0.22,
      depthWrite: false,
      depthTest: false,
      blending: THREE.AdditiveBlending,
    })
    const glow = new THREE.Sprite(glowMaterial)
    glow.renderOrder = 2
    group.add(glow, disc)
    memoryGroup.add(group)

    const size = 1.45 + Math.min(memory.media.length, 4) * 0.12 + hashValue(memory.id + 37) * 0.24
    disc.scale.setScalar(size)
    glow.scale.set(size * 3.2, size * 3.2, 1)
    planetVisuals.push({
      memoryId: memory.id,
      group,
      disc,
      glow,
      basePosition,
      phase,
      speedX: 0.07 + hashValue(memory.id + 41) * 0.06,
      speedY: (hashValue(memory.id + 43) > 0.5 ? 1 : -1) * (0.1 + hashValue(memory.id + 47) * 0.08),
      orbitPhase: phase,
      size,
    })

    void (async () => {
      try {
        const blob = await options.loadImage(fileId)
        const source = await loadImageSource(blob)
        try {
          if (disposed || generation !== loadGeneration) return
          const texture = createPlanetTexture(source, compact)
          if (disposed || generation !== loadGeneration) {
            texture.dispose()
            return
          }
          planetTextures.add(texture)
          disc.material.map = texture
          disc.material.opacity = 1
          disc.material.needsUpdate = true
          glow.material.opacity = 0.34
        } finally {
          if ('close' in source && typeof source.close === 'function') source.close()
        }
      } catch {}
    })()
  }

  function rebuildMemories(nextMemories: Memory[]) {
    loadGeneration += 1
    planetVisuals.length = 0
    for (const texture of planetTextures) texture.dispose()
    planetTextures.clear()
    scene.remove(memoryGroup)
    disposeObject(memoryGroup)
    memoryGroup = new THREE.Group()
    scene.add(memoryGroup)
    clearLabels()
    memories = nextMemories
    memoryPoints = null
    memoryMaterial = null
    selectionSprite.visible = false

    if (!memories.length) {
      renderFrame()
      return
    }

    const clusterCounts = [0, 0, 0, 0]
    const assignments = memories.map((memory) => {
      const cluster = Math.floor(hashValue(memory.id) * CLUSTER_CENTERS.length) % CLUSTER_CENTERS.length
      const index = clusterCounts[cluster]
      clusterCounts[cluster] += 1
      return { cluster, index }
    })
    const positions = new Float32Array(memories.length * 3)
    const sizes = new Float32Array(memories.length)
    const phases = new Float32Array(memories.length)
    const selected = new Float32Array(memories.length)
    const colors = new Float32Array(memories.length * 3)

    memories.forEach((memory, index) => {
      const assignment = assignments[index]
      const center = CLUSTER_CENTERS[assignment.cluster]
      const angle = assignment.index * 2.399963 + hashValue(memory.id + 3) * 0.5
      const radius = 1.1 + Math.sqrt(assignment.index + 1) * 1.65
      const offset = index * 3
      positions[offset] = center.x + Math.cos(angle) * radius * 1.12
      positions[offset + 1] = center.y + Math.sin(angle) * radius * 0.74
      positions[offset + 2] = center.z + (hashValue(memory.id + 9) - 0.5) * 9
      sizes[index] = 5.2 + Math.min(memory.media.length, 5) * 0.85 + hashValue(memory.id + 17) * 2.2
      phases[index] = hashValue(memory.id + 23) * Math.PI * 2
      const color = STAR_COLORS[memory.starType] || STAR_COLORS.CLASSIC
      colors[offset] = color.r
      colors[offset + 1] = color.g
      colors[offset + 2] = color.b
    })

    const geometry = new THREE.BufferGeometry()
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
    geometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
    geometry.setAttribute('aSelected', new THREE.BufferAttribute(selected, 1))
    geometry.setAttribute('aColor', new THREE.BufferAttribute(colors, 3))
    geometry.computeBoundingSphere()

    memoryMaterial = new THREE.ShaderMaterial({
      uniforms: {
        uTime: { value: elapsed },
        uPixelRatio: { value: renderer.getPixelRatio() },
      },
      vertexShader: MEMORY_VERTEX_SHADER,
      fragmentShader: MEMORY_FRAGMENT_SHADER,
      transparent: true,
      depthWrite: false,
      depthTest: false,
      blending: THREE.AdditiveBlending,
      toneMapped: false,
    })
    memoryPoints = new THREE.Points(geometry, memoryMaterial)
    memoryPoints.renderOrder = 2
    memoryGroup.add(memoryPoints)

    const linePositions: number[] = []
    for (let index = 0; index < memories.length - 1; index += 1) {
      const current = new THREE.Vector3().fromArray(positions, index * 3)
      const next = new THREE.Vector3().fromArray(positions, (index + 1) * 3)
      if (current.distanceTo(next) > 24) continue
      linePositions.push(...current.toArray(), ...next.toArray())
    }
    if (linePositions.length) {
      const lineGeometry = new THREE.BufferGeometry()
      lineGeometry.setAttribute('position', new THREE.Float32BufferAttribute(linePositions, 3))
      const lineMaterial = new THREE.LineBasicMaterial({
        color: 0x7890b8,
        transparent: true,
        opacity: 0.13,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      })
      memoryGroup.add(new THREE.LineSegments(lineGeometry, lineMaterial))
    }

    const labelLimit = compact ? 8 : 18
    const stride = Math.max(1, Math.ceil(memories.length / labelLimit))
    memories.forEach((memory, index) => {
      if (index % stride === 0) createLabel(memory, index)
      createMemoryPlanet(memory, index, positions, loadGeneration)
    })

    setSelected(selectedId)
    renderFrame()
  }

  function setSelected(memoryId: number | null) {
    selectedId = memoryId
    for (const label of labels.values()) label.button.classList.toggle('is-selected', memories[label.index]?.id === memoryId)

    if (!memoryPoints || memoryId === null) {
      selectionSprite.visible = false
      if (memoryPoints) {
        const attribute = memoryPoints.geometry.getAttribute('aSelected') as THREE.BufferAttribute
        attribute.array.fill(0)
        attribute.needsUpdate = true
      }
      renderFrame()
      return
    }

    const index = memories.findIndex((memory) => memory.id === memoryId)
    if (index < 0) {
      selectionSprite.visible = false
      renderFrame()
      return
    }

    createLabel(memories[index], index)
    const attribute = memoryPoints.geometry.getAttribute('aSelected') as THREE.BufferAttribute
    attribute.array.fill(0)
    attribute.setX(index, 1)
    attribute.needsUpdate = true
    const position = memoryPoints.geometry.getAttribute('position') as THREE.BufferAttribute
    selectionSprite.position.fromBufferAttribute(position, index)
    selectionSprite.visible = true
    for (const label of labels.values()) label.button.classList.toggle('is-selected', label.index === index)
    renderFrame()
  }

  function pickMemory(event: PointerEvent) {
    if (!memoryPoints || !memories.length) return
    const rect = renderer.domElement.getBoundingClientRect()
    pointer.set(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1,
    )
    raycaster.setFromCamera(pointer, camera)
    const intersection = raycaster.intersectObject(memoryPoints, false)[0]
    if (intersection?.index !== undefined && memories[intersection.index]) {
      options.onSelect(memories[intersection.index].id)
    }
  }

  function handlePointerDown(event: PointerEvent) {
    pointerStart.set(event.clientX, event.clientY)
    controls.autoRotate = false
    renderer.domElement.classList.add('is-dragging')
  }

  function handlePointerUp(event: PointerEvent) {
    renderer.domElement.classList.remove('is-dragging')
    if (pointerStart.distanceTo(new THREE.Vector2(event.clientX, event.clientY)) < 6) pickMemory(event)
    scheduleRotation()
  }

  function handleVisibilityChange() {
    if (document.hidden) stopAnimation()
    else if (!reducedMotion) startAnimation()
    else renderFrame()
  }

  function handleMotionChange(event: MediaQueryListEvent) {
    reducedMotion = event.matches
    controls.autoRotate = !reducedMotion
    controls.enableDamping = !reducedMotion
    if (reducedMotion) stopAnimation()
    else startAnimation()
    renderFrame()
  }

  function resetView() {
    controls.reset()
    controls.autoRotate = !reducedMotion
    renderFrame()
  }

  function zoom(factor: number) {
    const offset = camera.position.clone().sub(controls.target)
    const distance = THREE.MathUtils.clamp(offset.length() * factor, controls.minDistance, controls.maxDistance)
    offset.setLength(distance)
    camera.position.copy(controls.target).add(offset)
    controls.update()
    renderFrame()
  }

  function dispose() {
    if (disposed) return
    disposed = true
    stopAnimation()
    if (rotateTimer !== null) window.clearTimeout(rotateTimer)
    resizeObserver.disconnect()
    controls.dispose()
    clearLabels()
    window.removeEventListener('resize', resize)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    motionQuery.removeEventListener('change', handleMotionChange)
    renderer.domElement.removeEventListener('pointerdown', handlePointerDown)
    renderer.domElement.removeEventListener('pointerup', handlePointerUp)
    disposeObject(scene)
    for (const texture of planetTextures) texture.dispose()
    planetTextures.clear()
    for (const texture of textures) texture.dispose()
    textures.clear()
    renderer.dispose()
    try {
      renderer.forceContextLoss()
    } catch {}
    renderer.domElement.remove()
  }

  const resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(host)
  window.addEventListener('resize', resize, { passive: true })
  document.addEventListener('visibilitychange', handleVisibilityChange)
  renderer.domElement.addEventListener('pointerdown', handlePointerDown)
  renderer.domElement.addEventListener('pointerup', handlePointerUp)
  motionQuery.addEventListener('change', handleMotionChange)
  controls.addEventListener('start', () => {
    controls.autoRotate = false
    if (rotateTimer !== null) window.clearTimeout(rotateTimer)
  })
  controls.addEventListener('end', scheduleRotation)
  controls.addEventListener('change', () => {
    if (reducedMotion) renderFrame()
  })

  resize()
  rebuildMemories(initialMemories)
  if (!reducedMotion) startAnimation()

  return {
    updateMemories: rebuildMemories,
    setSelected,
    resetView,
    zoom,
    dispose,
  }
}
