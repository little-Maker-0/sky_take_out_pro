<template>
  <view style="padding: 24rpx;">
    <view class="card" style="margin-bottom: 20rpx;">
      <view style="font-weight: 600;">确认下单</view>
      <view class="muted" style="margin-top: 8rpx;">对接 `/user/order/submit`，金额等字段先做最小实现</view>
    </view>

    <view class="card" style="margin-bottom: 20rpx;">
      <view style="display:flex;justify-content:space-between;align-items:center;">
        <view>
          <view style="font-weight: 600;">收货地址</view>
          <view class="muted" style="margin-top: 8rpx;">
            {{ addressText }}
          </view>
        </view>
        <view class="btn-primary" style="font-size: 26rpx;" @click="goAddress">选择/管理</view>
      </view>
    </view>

    <view class="card" style="margin-bottom: 20rpx;">
      <view style="font-weight: 600; margin-bottom: 12rpx;">购物车</view>
      <view v-for="item in cart" :key="item.id" style="display:flex;justify-content:space-between;padding: 12rpx 0;border-bottom: 1px solid #f3f3f3;">
        <view>{{ item.name }} x {{ item.number }}</view>
        <view>¥ {{ (item.amount / 100).toFixed(2) }}</view>
      </view>
      <view v-if="cart.length===0" class="muted">购物车为空</view>
      <view style="margin-top: 12rpx; display:flex;justify-content:space-between;font-weight:600;">
        <view>合计</view>
        <view style="color:#E95F3C;">¥ {{ totalYuan }}</view>
      </view>
    </view>

    <view class="btn-primary" @click="submit" style="width:100%;">提交订单（最小实现）</view>
  </view>
</template>

<script>
import { ensureLogin } from '../../utils/auth'
import { getDefaultAddress } from '../../api/address'
import { getCartList } from '../../api/cart'
import { submitOrder } from '../../api/order'

export default {
  data() {
    return { address: null, cart: [] }
  },
  computed: {
    addressText() {
      if (!this.address) return '未选择地址'
      const a = this.address
      return `${a.consignee || ''} ${a.phone || ''} ${a.provinceName || ''}${a.cityName || ''}${a.districtName || ''}${a.detail || ''}`
    },
    totalFen() {
      return this.cart.reduce((sum, it) => sum + (it.amount || 0) * (it.number || 0), 0)
    },
    totalYuan() {
      return (this.totalFen / 100).toFixed(2)
    }
  },
  async onShow() {
    await this.refresh()
  },
  methods: {
    async refresh() {
      await ensureLogin().catch(() => {})
      try {
        this.address = await getDefaultAddress()
      } catch (e) {
        this.address = null
      }
      const list = await getCartList()
      this.cart = Array.isArray(list) ? list : []
    },
    goAddress() {
      uni.navigateTo({ url: '/pages/address/address' })
    },
    async submit() {
      if (!this.address || !this.address.id) {
        uni.showToast({ title: '请先设置默认地址', icon: 'none' })
        return
      }
      if (!this.cart.length) {
        uni.showToast({ title: '购物车为空', icon: 'none' })
        return
      }

      // 最小可用：后端会以购物车为准生成订单明细；这里按 DTO 填关键字段
      const payload = {
        addressBookId: this.address.id,
        payMethod: 1,
        remark: '',
        estimatedDeliveryTime: null,
        deliveryStatus: 1,
        tablewareNumber: 0,
        tablewareStatus: 1,
        packAmount: 0,
        amount: Number(this.totalYuan)
      }

      const res = await submitOrder(payload)
      uni.showToast({ title: '下单成功', icon: 'none' })
      // res 通常包含 orderId / orderNumber 等
      console.log('submitOrder result:', res)
    }
  }
}
</script>

