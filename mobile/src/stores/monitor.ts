import { defineStore } from 'pinia'

export interface ChannelShortcut {
  deviceId: string
  channelId: string
  name: string
  deviceName?: string
  lastViewedAt: number
}

const FAVORITES_KEY = 'wvp_mobile_favorites'
const RECENTS_KEY = 'wvp_mobile_recents'

function read(key: string): ChannelShortcut[] {
  const value = uni.getStorageSync(key)
  return Array.isArray(value) ? value : []
}

export const useMonitorStore = defineStore('monitor', {
  state: () => ({
    favorites: [] as ChannelShortcut[],
    recents: [] as ChannelShortcut[],
  }),
  actions: {
    restore() {
      this.favorites = read(FAVORITES_KEY)
      this.recents = read(RECENTS_KEY)
    },
    isFavorite(deviceId: string, channelId: string) {
      return this.favorites.some((item) => item.deviceId === deviceId && item.channelId === channelId)
    },
    toggleFavorite(channel: ChannelShortcut) {
      const index = this.favorites.findIndex((item) => item.deviceId === channel.deviceId && item.channelId === channel.channelId)
      if (index >= 0) this.favorites.splice(index, 1)
      else this.favorites.unshift(channel)
      uni.setStorageSync(FAVORITES_KEY, this.favorites)
    },
    addRecent(channel: ChannelShortcut) {
      this.recents = [channel, ...this.recents.filter((item) => item.deviceId !== channel.deviceId || item.channelId !== channel.channelId)].slice(0, 10)
      uni.setStorageSync(RECENTS_KEY, this.recents)
    },
  },
})
