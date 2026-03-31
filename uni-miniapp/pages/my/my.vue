<template>
  <view style="padding: 24rpx;">
    <view class="card" style="margin-bottom: 20rpx;">
      <view style="font-weight: 600;">我的</view>
      <view class="muted" style="margin-top: 8rpx;">登录态与 token 状态</view>
    </view>

    <view class="card" style="margin-bottom: 20rpx;">
      <view class="muted">Token（前 24 位）</view>
      <view style="margin-top: 10rpx; word-break: break-all;">{{ tokenPreview }}</view>
      <view style="display:flex;gap:16rpx;margin-top: 18rpx;">
        <view class="btn-primary" @click="relogin">重新登录</view>
        <view class="btn-primary" style="background:#333;" @click="doLogout">退出登录</view>
      </view>
    </view>

    <view class="card">
      <view class="muted">后端地址</view>
      <view style="margin-top: 10rpx;">{{ baseURL }}</view>
      <view class="muted" style="margin-top: 16rpx;">如需真机访问，请改 `utils/config.js` 为局域网 IP</view>
    </view>
  </view>
</template>

<script>
import { CONFIG } from '../../utils/config'
import { ensureLogin, logout } from '../../utils/auth'
import { getToken } from '../../utils/storage'

export default {
  data() {
    return { baseURL: CONFIG.baseURL, token: '' }
  },
  onShow() {
    this.token = getToken()
  },
  computed: {
    tokenPreview() {
      if (!this.token) return '(未登录)'
      return `${this.token.slice(0, 24)}...`
    }
  },
  methods: {
    async relogin() {
      await ensureLogin({ force: true })
      this.token = getToken()
      uni.showToast({ title: '登录完成', icon: 'none' })
    },
    doLogout() {
      logout()
      this.token = ''
      uni.showToast({ title: '已退出', icon: 'none' })
    }
  }
}
</script>

