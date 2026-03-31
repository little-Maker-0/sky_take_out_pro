<script>
import { ensureLogin } from './utils/auth'

export default {
  onLaunch() {
    // 启动时尝试静默登录，失败不阻塞页面渲染
    ensureLogin().catch(() => {})
  }
}
</script>

<style lang="scss">
@import "./uni.scss";
</style>

