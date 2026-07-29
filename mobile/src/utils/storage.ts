const KEYS = { token: 'wvp_token', username: 'wvp_username', serverId: 'wvp_server_id' } as const

export const storage = {
  get: (key: keyof typeof KEYS) => uni.getStorageSync(KEYS[key]) as string,
  set: (key: keyof typeof KEYS, value: string) => uni.setStorageSync(KEYS[key], value),
  remove: (key: keyof typeof KEYS) => uni.removeStorageSync(KEYS[key]),
  clearAuth() {
    this.remove('token')
    this.remove('username')
    this.remove('serverId')
  },
}
