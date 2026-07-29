import { unreadMessageCount } from '@/api/message'

export async function refreshMessageBadge() {
  try {
    const count = await unreadMessageCount()
    if (count > 0) {
      uni.setTabBarBadge({ index: 2, text: count > 99 ? '99+' : String(count) })
    } else {
      uni.removeTabBarBadge({ index: 2 })
    }
    return count
  } catch {
    return 0
  }
}
