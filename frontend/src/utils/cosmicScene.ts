import * as THREE from 'three'

type Disposable = { dispose: () => void }
type Rgb = readonly [number, number, number]

interface StarLayerConfig {
  count: number
  z: number
  depth: number
  size: readonly [number, number]
  opacity: number
  speed: number
  drift: number
  seed: number
}

interface StarLayer {
  points: THREE.Points
  material: THREE.ShaderMaterial
  geometry: THREE.BufferGeometry
  baseRotation: number
  speed: number
  drift: number
  phase: number
}

interface ViewportSprite {
  sprite: THREE.Sprite
  x: number
  y: number
  z: number
  widthRatio: number
  heightRatio: number
  galaxy: boolean
}

interface CosmicScene {
  renderer: THREE.WebGLRenderer
  scene: THREE.Scene
  camera: THREE.PerspectiveCamera
  resources: Set<Disposable>
  layers: StarLayer[]
  viewportSprites: ViewportSprite[]
  compact: boolean
  frameInterval: number
  elapsed: number
  aspect: number
}

const STAR_LAYERS: readonly StarLayerConfig[] = [
  { count: 920, z: -36, depth: 14, size: [0.72, 1.5], opacity: 0.42, speed: 0.0016, drift: 0.08, seed: 0x51a7c3 },
  { count: 360, z: -18, depth: 10, size: [0.9, 1.9], opacity: 0.32, speed: -0.0022, drift: 0.12, seed: 0x7bd91e },
  { count: 120, z: -3, depth: 7, size: [1.2, 2.5], opacity: 0.24, speed: 0.003, drift: 0.16, seed: 0x2f86ab },
]

const STAR_VERTEX_SHADER = `
  uniform float uTime;
  uniform float uPixelRatio;
  uniform float uOpacity;
  attribute float aSize;
  attribute float aPhase;
  attribute float aBrightness;
  attribute vec3 aColor;
  varying vec3 vColor;
  varying float vAlpha;

  void main() {
    vec3 transformed = position;
    transformed.x += sin(uTime * 0.035 + aPhase) * 0.035;
    transformed.y += cos(uTime * 0.028 + aPhase * 1.3) * 0.025;
    vec4 viewPosition = modelViewMatrix * vec4(transformed, 1.0);
    gl_Position = projectionMatrix * viewPosition;
    gl_PointSize = clamp(
      aSize * uPixelRatio * (52.0 / max(1.0, -viewPosition.z)),
      uPixelRatio,
      4.2 * uPixelRatio
    );
    float twinkle = 0.88 + 0.12 * sin(uTime * (0.42 + aBrightness * 0.24) + aPhase);
    vColor = aColor;
    vAlpha = uOpacity * aBrightness * twinkle;
  }
`

const STAR_FRAGMENT_SHADER = `
  varying vec3 vColor;
  varying float vAlpha;

  void main() {
    float radius = length(gl_PointCoord - vec2(0.5));
    if (radius > 0.5) discard;
    float glow = 1.0 - smoothstep(0.0, 0.5, radius);
    float core = 1.0 - smoothstep(0.0, 0.16, radius);
    float alpha = (glow * glow * 0.82 + core * 0.18) * vAlpha;
    gl_FragColor = vec4(vColor, alpha);
    #include <colorspace_fragment>
  }
`

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

function isCompactDevice() {
  const cores = navigator.hardwareConcurrency || 8
  return window.innerWidth < 768 || cores <= 4
}

function particleQuality() {
  const cores = navigator.hardwareConcurrency || 8
  const widthFactor = window.innerWidth < 768 ? 0.56 : 1
  const coreFactor = cores <= 2 ? 0.62 : cores <= 4 ? 0.78 : 1
  return widthFactor * coreFactor
}

function viewportSize(camera: THREE.PerspectiveCamera, z: number) {
  const distance = camera.position.z - z
  const height = 2 * Math.tan(THREE.MathUtils.degToRad(camera.fov * 0.5)) * distance
  return { width: height * camera.aspect, height }
}

function paintGlow(
  context: CanvasRenderingContext2D,
  x: number,
  y: number,
  radius: number,
  color: Rgb,
  alpha: number,
) {
  const rgb = color.map((channel) => Math.round(channel * 255)).join(', ')
  const gradient = context.createRadialGradient(x, y, 0, x, y, radius)
  gradient.addColorStop(0, `rgba(${rgb}, ${alpha})`)
  gradient.addColorStop(0.38, `rgba(${rgb}, ${alpha * 0.52})`)
  gradient.addColorStop(1, `rgba(${rgb}, 0)`)
  context.fillStyle = gradient
  context.fillRect(x - radius, y - radius, radius * 2, radius * 2)
}

function canvasTexture(canvas: HTMLCanvasElement, resources: Set<Disposable>) {
  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  texture.minFilter = THREE.LinearFilter
  texture.magFilter = THREE.LinearFilter
  texture.generateMipmaps = false
  resources.add(texture)
  return texture
}

function nebulaTexture(seed: number, colors: readonly Rgb[], size: number, resources: Set<Disposable>) {
  const random = createRandom(seed)
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas 2D unavailable')

  context.globalCompositeOperation = 'lighter'
  for (let index = 0; index < 18; index += 1) {
    const angle = random() * Math.PI * 2
    const distance = Math.sqrt(random()) * size * 0.18
    const x = size * 0.5 + Math.cos(angle) * distance
    const y = size * 0.5 + Math.sin(angle) * distance * 0.72
    const radius = size * (0.12 + random() * 0.2)
    paintGlow(context, x, y, radius, colors[Math.floor(random() * colors.length)], 0.035 + random() * 0.055)
  }
  paintGlow(context, size * 0.5, size * 0.5, size * 0.38, colors[0], 0.025)
  return canvasTexture(canvas, resources)
}

function galaxyTexture(seed: number, warm: boolean, compact: boolean, resources: Set<Disposable>) {
  const random = createRandom(seed)
  const width = compact ? 192 : 256
  const height = width / 2
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Canvas 2D unavailable')

  const colors: readonly [Rgb, Rgb] = warm
    ? [[0.95, 0.7, 0.38], [0.68, 0.46, 0.24]]
    : [[0.56, 0.68, 0.95], [0.34, 0.43, 0.7]]

  context.globalCompositeOperation = 'lighter'
  for (let arm = 0; arm < 2; arm += 1) {
    for (let index = 0; index < 30; index += 1) {
      const progress = (index + 1) / 31
      const angle = arm * Math.PI + progress * 4.6
      const radius = 6 + progress * width * 0.37
      const x = width * 0.5 + Math.cos(angle) * radius
      const y = height * 0.5 + Math.sin(angle) * radius * 0.38
      paintGlow(context, x, y, width * (0.018 + random() * 0.022), colors[index % 2], 0.025 + random() * 0.025)
    }
  }
  paintGlow(context, width * 0.5, height * 0.5, width * 0.055, colors[0], 0.18)
  paintGlow(context, width * 0.5, height * 0.5, width * 0.13, colors[1], 0.05)
  return canvasTexture(canvas, resources)
}

function addSprite(
  current: CosmicScene,
  texture: THREE.Texture,
  options: Omit<ViewportSprite, 'sprite'> & { opacity: number; rotation: number; additive?: boolean },
) {
  const material = new THREE.SpriteMaterial({
    map: texture,
    transparent: true,
    opacity: options.opacity,
    depthWrite: false,
    depthTest: false,
    blending: options.additive ? THREE.AdditiveBlending : THREE.NormalBlending,
    toneMapped: false,
  })
  material.rotation = options.rotation
  current.resources.add(material)

  const sprite = new THREE.Sprite(material)
  current.scene.add(sprite)
  current.viewportSprites.push({
    sprite,
    x: options.x,
    y: options.y,
    z: options.z,
    widthRatio: options.widthRatio,
    heightRatio: options.heightRatio,
    galaxy: options.galaxy,
  })
}

function updateViewportSprites(current: CosmicScene) {
  for (const item of current.viewportSprites) {
    const viewport = viewportSize(current.camera, item.z)
    const width = item.galaxy
      ? Math.min(viewport.width * item.widthRatio, viewport.height * item.heightRatio)
      : viewport.width * item.widthRatio
    const height = item.galaxy ? width * 0.46 : viewport.height * item.heightRatio
    item.sprite.position.set(item.x * viewport.width * 0.5, item.y * viewport.height * 0.5, item.z)
    item.sprite.scale.set(width, height, 1)
  }
}

function starGeometry(camera: THREE.PerspectiveCamera, config: StarLayerConfig, quality: number) {
  const random = createRandom(config.seed)
  const count = Math.max(40, Math.round(config.count * quality))
  const viewport = viewportSize(camera, config.z)
  const spreadX = Math.max(viewport.width * 0.62, 16)
  const spreadY = Math.max(viewport.height * 0.6, 14)
  const clusters = Array.from({ length: 7 }, () => ({
    x: (random() * 2 - 1) * spreadX * 0.72,
    y: (random() * 2 - 1) * spreadY * 0.7,
    radiusX: spreadX * (0.1 + random() * 0.18),
    radiusY: spreadY * (0.12 + random() * 0.2),
  }))
  const positions = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  const phases = new Float32Array(count)
  const brightness = new Float32Array(count)
  const colors = new Float32Array(count * 3)

  for (let star = 0; star < count; star += 1) {
    let x: number
    let y: number
    if (random() < 0.78) {
      const cluster = clusters[Math.floor(random() * clusters.length)]
      const angle = random() * Math.PI * 2
      const radius = Math.pow(random(), 1.65)
      x = cluster.x + Math.cos(angle) * radius * cluster.radiusX
      y = cluster.y + Math.sin(angle) * radius * cluster.radiusY
    } else {
      x = (random() * 2 - 1) * spreadX
      y = (random() * 2 - 1) * spreadY
    }

    const offset = star * 3
    positions[offset] = x
    positions[offset + 1] = y
    positions[offset + 2] = config.z + (random() - 0.5) * config.depth
    sizes[star] = THREE.MathUtils.lerp(config.size[0], config.size[1], Math.pow(random(), 1.4))
    phases[star] = random() * Math.PI * 2
    const centerDistance = Math.hypot(x / (spreadX * 0.42), y / (spreadY * 0.48))
    brightness[star] = (0.3 + Math.pow(random(), 1.7) * 0.7) * (centerDistance < 1 ? 0.55 : 1)

    const color: Rgb = random() > 0.96
      ? [0.9, 0.68, 0.38]
      : random() > 0.68
        ? [0.42, 0.56, 0.86]
        : [0.72, 0.78, 0.92]
    colors[offset] = color[0]
    colors[offset + 1] = color[1]
    colors[offset + 2] = color[2]
  }

  const geometry = new THREE.BufferGeometry()
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('aSize', new THREE.BufferAttribute(sizes, 1))
  geometry.setAttribute('aPhase', new THREE.BufferAttribute(phases, 1))
  geometry.setAttribute('aBrightness', new THREE.BufferAttribute(brightness, 1))
  geometry.setAttribute('aColor', new THREE.BufferAttribute(colors, 3))
  geometry.computeBoundingSphere()
  return geometry
}

function clearStarLayers(current: CosmicScene) {
  for (const layer of current.layers) {
    current.scene.remove(layer.points)
    layer.geometry.dispose()
    layer.material.dispose()
    current.resources.delete(layer.geometry)
    current.resources.delete(layer.material)
  }
  current.layers = []
}

function buildStarLayers(current: CosmicScene) {
  clearStarLayers(current)
  const quality = particleQuality()
  current.layers = STAR_LAYERS.map((config) => {
    const geometry = starGeometry(current.camera, config, quality)
    const material = new THREE.ShaderMaterial({
      uniforms: {
        uTime: { value: current.elapsed },
        uPixelRatio: { value: current.renderer.getPixelRatio() },
        uOpacity: { value: config.opacity },
      },
      vertexShader: STAR_VERTEX_SHADER,
      fragmentShader: STAR_FRAGMENT_SHADER,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      depthTest: false,
      toneMapped: false,
    })
    current.resources.add(geometry)
    current.resources.add(material)

    const points = new THREE.Points(geometry, material)
    const baseRotation = createRandom(config.seed + 17)() * Math.PI * 2
    points.rotation.z = baseRotation
    current.scene.add(points)
    return {
      points,
      material,
      geometry,
      baseRotation,
      speed: config.speed,
      drift: config.drift,
      phase: createRandom(config.seed + 31)() * Math.PI * 2,
    }
  })
}

function populateScene(current: CosmicScene) {
  const size = current.compact ? 192 : 256
  const blue = nebulaTexture(0x1b43a7, [[0.16, 0.25, 0.52], [0.29, 0.2, 0.49], [0.12, 0.31, 0.58]], size, current.resources)
  const violet = nebulaTexture(0x72c4e9, [[0.31, 0.2, 0.5], [0.17, 0.22, 0.48], [0.24, 0.28, 0.57]], size, current.resources)
  const warm = nebulaTexture(0x91af37, [[0.58, 0.39, 0.18], [0.48, 0.3, 0.14], [0.64, 0.47, 0.24]], size, current.resources)

  addSprite(current, blue, { x: -0.38, y: 0.3, z: -18, widthRatio: 0.72, heightRatio: 0.58, opacity: 0.17, rotation: 0.18, galaxy: false })
  addSprite(current, violet, { x: 0.42, y: -0.12, z: -30, widthRatio: 0.78, heightRatio: 0.62, opacity: 0.13, rotation: -0.24, galaxy: false })
  addSprite(current, warm, { x: -0.28, y: -0.43, z: -12, widthRatio: 0.34, heightRatio: 0.28, opacity: 0.065, rotation: -0.35, galaxy: false })

  const coolGalaxy = galaxyTexture(0x34d921, false, current.compact, current.resources)
  const warmGalaxy = galaxyTexture(0x87e5b3, true, current.compact, current.resources)
  addSprite(current, coolGalaxy, { x: 0.57, y: 0.36, z: -48, widthRatio: 0.16, heightRatio: 0.17, opacity: 0.14, rotation: 0.35, additive: true, galaxy: true })
  addSprite(current, warmGalaxy, { x: -0.6, y: -0.3, z: -42, widthRatio: 0.16, heightRatio: 0.17, opacity: 0.095, rotation: -0.45, additive: true, galaxy: true })
  buildStarLayers(current)
  updateViewportSprites(current)
}

function disposeResources(current: CosmicScene) {
  const disposables = new Set<Disposable>(current.resources)
  current.scene.traverse((object) => {
    const renderable = object as THREE.Object3D & {
      geometry?: THREE.BufferGeometry
      material?: THREE.Material | THREE.Material[]
    }
    if (renderable.geometry) disposables.add(renderable.geometry)
    if (Array.isArray(renderable.material)) renderable.material.forEach((material) => disposables.add(material))
    else if (renderable.material) disposables.add(renderable.material)
  })
  for (const resource of disposables) {
    try {
      resource.dispose()
    } catch {}
  }
  current.resources.clear()
  current.scene.clear()
}

export function mountCosmicScene(host: HTMLDivElement, onReady: () => void): () => void {
  let renderer: THREE.WebGLRenderer | null = null
  let current: CosmicScene | null = null
  let motionQuery: MediaQueryList | null = null
  let reducedMotion = false
  let animationFrame: number | null = null
  let lastFrameTime = 0
  let disposed = false
  const pointerTarget = new THREE.Vector2()
  let scrollTarget = 0

  const render = (delta = 0) => {
    if (!current || disposed || document.hidden) return
    if (delta > 0) {
      current.elapsed += delta
      const smoothing = 1 - Math.exp(-delta * 2.1)
      current.camera.position.x = THREE.MathUtils.lerp(current.camera.position.x, pointerTarget.x, smoothing)
      current.camera.position.y = THREE.MathUtils.lerp(current.camera.position.y, pointerTarget.y + scrollTarget, smoothing)
    }
    for (const layer of current.layers) {
      layer.material.uniforms.uTime.value = current.elapsed
      layer.points.rotation.z = layer.baseRotation + current.elapsed * layer.speed
      layer.points.position.x = Math.sin(current.elapsed * 0.025 + layer.phase) * layer.drift
      layer.points.position.y = Math.cos(current.elapsed * 0.02 + layer.phase) * layer.drift * 0.6
    }
    current.renderer.render(current.scene, current.camera)
    onReady()
  }

  const animate = (now: number) => {
    animationFrame = null
    if (!current || disposed || reducedMotion || document.hidden) return
    const sinceLastFrame = lastFrameTime === 0 ? current.frameInterval : now - lastFrameTime
    if (sinceLastFrame + 0.25 < current.frameInterval) {
      animationFrame = requestAnimationFrame(animate)
      return
    }
    const delta = Math.min(sinceLastFrame / 1000, 0.05)
    lastFrameTime = now - (sinceLastFrame % current.frameInterval)
    render(delta)
    animationFrame = requestAnimationFrame(animate)
  }

  const startAnimation = () => {
    if (animationFrame !== null || !current || reducedMotion || document.hidden || disposed) return
    lastFrameTime = 0
    animationFrame = requestAnimationFrame(animate)
  }

  const stopAnimation = () => {
    if (animationFrame !== null) cancelAnimationFrame(animationFrame)
    animationFrame = null
    lastFrameTime = 0
  }

  const resize = () => {
    if (!current) return
    const width = Math.max(1, window.innerWidth)
    const height = Math.max(1, window.innerHeight)
    const compact = isCompactDevice()
    const aspect = width / height
    const rebuildStars = compact !== current.compact || Math.abs(aspect - current.aspect) > 0.35
    current.compact = compact
    current.aspect = aspect
    current.frameInterval = 1000 / (compact ? 24 : 30)
    current.camera.aspect = aspect
    current.camera.updateProjectionMatrix()
    current.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, compact ? 1.25 : 1.5))
    current.renderer.setSize(width, height, false)
    if (rebuildStars) buildStarLayers(current)
    for (const layer of current.layers) layer.material.uniforms.uPixelRatio.value = current.renderer.getPixelRatio()
    updateViewportSprites(current)
    lastFrameTime = 0
    render()
  }

  const handlePointerMove = (event: PointerEvent) => {
    if (reducedMotion || document.hidden) return
    pointerTarget.set(
      (event.clientX / Math.max(1, window.innerWidth) - 0.5) * 0.7,
      (0.5 - event.clientY / Math.max(1, window.innerHeight)) * 0.45,
    )
  }

  const handleScroll = () => {
    if (reducedMotion || document.hidden) return
    scrollTarget = THREE.MathUtils.clamp(window.scrollY / Math.max(1, window.innerHeight), -1, 1.5) * 0.32
  }

  const handleVisibilityChange = () => {
    lastFrameTime = 0
    if (document.hidden) stopAnimation()
    else if (reducedMotion) render()
    else startAnimation()
  }

  const handleMotionChange = (event: MediaQueryListEvent) => {
    reducedMotion = event.matches
    pointerTarget.set(0, 0)
    scrollTarget = 0
    if (reducedMotion) {
      stopAnimation()
      if (current) {
        current.elapsed = 0
        current.camera.position.set(0, 0, current.camera.position.z)
        render()
      }
    } else {
      startAnimation()
    }
  }

  const detachListeners = () => {
    window.removeEventListener('resize', resize)
    window.removeEventListener('pointermove', handlePointerMove)
    window.removeEventListener('scroll', handleScroll)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    motionQuery?.removeEventListener('change', handleMotionChange)
    motionQuery = null
  }

  const cleanup = () => {
    if (disposed) return
    disposed = true
    stopAnimation()
    detachListeners()
    if (current) {
      disposeResources(current)
      current.renderer.dispose()
      try {
        current.renderer.forceContextLoss()
      } catch {}
      current.renderer.domElement.remove()
      current = null
    }
    renderer = null
  }

  try {
    renderer = new THREE.WebGLRenderer({ alpha: true, antialias: false, powerPreference: 'low-power' })
    const scene = new THREE.Scene()
    const camera = new THREE.PerspectiveCamera(56, 1, 0.1, 180)
    camera.position.set(0, 0, 32)
    const compact = isCompactDevice()
    current = {
      renderer,
      scene,
      camera,
      resources: new Set(),
      layers: [],
      viewportSprites: [],
      compact,
      frameInterval: 1000 / (compact ? 24 : 30),
      elapsed: 0,
      aspect: window.innerWidth / Math.max(1, window.innerHeight),
    }

    renderer.setClearColor(0x000000, 0)
    renderer.outputColorSpace = THREE.SRGBColorSpace
    renderer.toneMapping = THREE.NoToneMapping
    renderer.domElement.setAttribute('aria-hidden', 'true')
    host.appendChild(renderer.domElement)

    motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    reducedMotion = motionQuery.matches
    resize()
    populateScene(current)
    resize()
    window.addEventListener('resize', resize, { passive: true })
    window.addEventListener('pointermove', handlePointerMove, { passive: true })
    window.addEventListener('scroll', handleScroll, { passive: true })
    document.addEventListener('visibilitychange', handleVisibilityChange)
    motionQuery.addEventListener('change', handleMotionChange)

    if (reducedMotion) render()
    else startAnimation()
  } catch {
    cleanup()
  }

  return cleanup
}
