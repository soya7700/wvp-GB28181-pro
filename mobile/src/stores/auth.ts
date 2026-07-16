import { defineStore } from 'pinia'
import md5 from 'blueimp-md5'
import { login, logout } from '@/api/user'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', {
  state: () => ({ token: '', username: '', serverId: '' }),
  getters: { loggedIn: (state) => Boolean(state.token) },
  actions: {
    restore() {
      this.token = storage.get('token') || ''
      this.username = storage.get('username') || ''
      this.serverId = storage.get('serverId') || ''
    },
    async signIn(username: string, password: string) {
      const result = await login(username.trim(), md5(password))
      this.token = result.accessToken
      this.username = result.username
      this.serverId = result.serverId
      storage.set('token', result.accessToken)
      storage.set('username', result.username)
      storage.set('serverId', result.serverId)
    },
    async signOut() {
      try { await logout() } finally {
        storage.clearAuth()
        this.$reset()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    },
    requireAuth() {
      this.restore()
      if (!this.loggedIn) {
        uni.reLaunch({ url: '/pages/login/index' })
        return false
      }
      return true
    },
  },
})
