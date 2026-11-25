export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface SystemHealth {
  status: string
  service: string
  version: string
  scope: string
  timestamp: string
}

export interface AuthUser {
  id: number
  email: string
  nickname: string
  avatarFileId: number | null
}

export interface TokenResponse {
  token: string
  tokenType: string
  expiresAt: string
  user: AuthUser
}

export interface UserProfileInput {
  nickname: string
  avatarFileId: number | null
}

export interface CoupleMember {
  userId: number
  nickname: string
  role: 'OWNER' | 'PARTNER'
  joinedAt: string
}

export interface CoupleSpace {
  id: number
  name: string
  anniversaryDate: string | null
  description: string | null
  role: 'OWNER' | 'PARTNER'
  memberCount: number
  members: CoupleMember[]
}

export interface Invitation {
  code: string
  status: string
  expiresAt: string
}

export interface FileInfo {
  id: number
  originalName: string
  mimeType: string
  sizeBytes: number
  width: number | null
  height: number | null
  url: string
}

export interface MemoryMedia {
  id: number
  fileId: number
  sortOrder: number
  file: FileInfo
}

export interface Memory {
  id: number
  title: string
  content: string | null
  eventDate: string
  locationName: string | null
  latitude: number | null
  longitude: number | null
  starType: string
  creatorId: number
  creatorName: string
  createdAt: string
  updatedAt: string
  tags: string[]
  media: MemoryMedia[]
}

export interface MemoryInput {
  title: string
  content: string
  eventDate: string
  locationName: string
  latitude: number | null
  longitude: number | null
  starType: string
  tags: string[]
  mediaFileIds: number[]
}
