<template>
  <view style="padding: 24rpx;">
    <view class="card" style="margin-bottom: 20rpx;">
      <view style="display:flex;justify-content:space-between;align-items:center;">
        <view>
          <view style="font-weight: 600;">购物车</view>
          <view class="muted" style="margin-top: 8rpx;">对接 `/user/shoppingCart/*`</view>
        </view>
        <view style="display:flex;gap:16rpx;">
          <view class="btn-primary" style="font-size: 26rpx;" @click="refresh">刷新</view>
          <view class="btn-primary" style="font-size: 26rpx;background:#333;" @click="clean">清空</view>
        </view>
      </view>
    </view>

    <view class="card">
      <view v-if="loading" class="muted">加载中...</view>

      <view v-for="item in list" :key="item.id" style="display:flex;justify-content:space-between;align-items:center;padding: 18rpx 0;border-bottom: 1px solid #f3f3f3;">
        <view style="flex:1;">
          <view style="font-weight: 500;">{{ item.name }}</view>
          <view class="muted" style="margin-top: 6rpx;font-size: 24rpx;">{{ item.dishFlavor || '' }}</view>
          <view style="margin-top: 8rpx; color:#E95F3C; font-weight: 600;">¥ {{ (item.amount / 100).toFixed(2) }}</view>
        </view>
        <view style="display:flex;align-items:center;gap:12rpx;">
          <view class="btn-primary" style="width: 64rpx; padding: 10rpx 0;background:#666;" @click="sub(item)">-</view>
          <view style="min-width: 40rpx;text-align:center;">{{ item.number }}</view>
          <view class="btn-primary" style="width: 64rpx; padding: 10rpx 0;" @click="add(item)">+</view>
        </view>
      </view>

      <view v-if="!loading && list.length === 0" class="muted">购物车为空</view>
    </view>

    <view class="btn-primary" style="width:100%;margin-top:20rpx;" @click="goCheckout">去结算</view>
  </view>
</template>

<script>
import { ensureLogin } from '../../utils/auth'
import { getCartList, addToCart, subFromCart, cleanCart } from '../../api/cart'

export default {
  data() {
    return { list: [], loading: false }
  },
  async onShow() {
    await this.refresh()
  },
  methods: {
    async refresh() {
      await ensureLogin().catch(() => {})
      this.loading = true
      try {
        const list = await getCartList()
        this.list = Array.isArray(list) ? list : []
      } finally {
        this.loading = false
      }
    },
    async add(item) {
      await addToCart({ dishId: item.dishId, setmealId: item.setmealId, dishFlavor: item.dishFlavor })
      await this.refresh()
    },
    async sub(item) {
      await subFromCart({ dishId: item.dishId, setmealId: item.setmealId, dishFlavor: item.dishFlavor })
      await this.refresh()
    },
    async clean() {
      await cleanCart()
      await this.refresh()
    },
    goCheckout() {
      uni.navigateTo({ url: '/pages/checkout/checkout' })
    }
  }
}
</script>

