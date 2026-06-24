import { app } from 'electron'
import fs from 'node:fs'
import path from 'node:path'
import { randomUUID } from 'node:crypto'
import type { ServerProfile } from '../../shared/types'

interface StoreData {
  profiles: ServerProfile[]
  activeProfileId: string | null
  tunEnabled: boolean
}

const FILE_NAME = 'profiles.json'

function getFilePath(): string {
  return path.join(app.getPath('userData'), FILE_NAME)
}

function readStore(): StoreData {
  try {
    const raw = fs.readFileSync(getFilePath(), 'utf-8')
    const data = JSON.parse(raw) as StoreData
    if (!Array.isArray(data.profiles)) {
      return { profiles: [], activeProfileId: null, tunEnabled: false }
    }
    return {
      profiles: data.profiles,
      activeProfileId: data.activeProfileId ?? null,
      tunEnabled: data.tunEnabled ?? false,
    }
  } catch {
    return { profiles: [], activeProfileId: null, tunEnabled: false }
  }
}

function writeStore(data: StoreData): void {
  const filePath = getFilePath()
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8')
}

export function getProfiles(): ServerProfile[] {
  return readStore().profiles
}

export function getProfile(id: string): ServerProfile | undefined {
  return readStore().profiles.find((p) => p.id === id)
}

export function getActiveProfileId(): string | null {
  return readStore().activeProfileId
}

export function setActiveProfileId(id: string | null): void {
  const data = readStore()
  data.activeProfileId = id
  writeStore(data)
}

export function getTunEnabled(): boolean {
  return readStore().tunEnabled
}

export function setTunEnabled(enabled: boolean): void {
  const data = readStore()
  data.tunEnabled = enabled
  writeStore(data)
}

export function addProfile(profile: Omit<ServerProfile, 'id'>): ServerProfile {
  const data = readStore()
  const newProfile: ServerProfile = { ...profile, id: randomUUID() }
  data.profiles.push(newProfile)
  if (!data.activeProfileId) {
    data.activeProfileId = newProfile.id
  }
  writeStore(data)
  return newProfile
}

export function removeProfile(id: string): void {
  const data = readStore()
  data.profiles = data.profiles.filter((p) => p.id !== id)
  if (data.activeProfileId === id) {
    data.activeProfileId = data.profiles[0]?.id ?? null
  }
  writeStore(data)
}

export function updateProfile(id: string, updates: Partial<Omit<ServerProfile, 'id'>>): ServerProfile | null {
  const data = readStore()
  const index = data.profiles.findIndex((p) => p.id === id)
  if (index === -1) return null
  data.profiles[index] = { ...data.profiles[index], ...updates }
  writeStore(data)
  return data.profiles[index]
}
