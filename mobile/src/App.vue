<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'

onLaunch(() => {
  const auth = useAuthStore()
  auth.restore()
  if (auth.loggedIn) import('@/utils/message-badge').then(({ refreshMessageBadge }) => refreshMessageBadge())
})
</script>

<style>
page { min-height: 100%; color: #172033; background: #f4f6fa; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
button::after { border: 0; }
</style>
