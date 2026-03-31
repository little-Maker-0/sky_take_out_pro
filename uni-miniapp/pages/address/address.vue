<template>
  <view style="padding: 24rpx;">
    <view class="card" style="margin-bottom: 20rpx;">
      <view style="display:flex;justify-content:space-between;align-items:center;">
        <view>
          <view style="font-weight: 600;">地址管理</view>
          <view class="muted" style="margin-top: 8rpx;">对接 `/user/addressBook/*`</view>
        </view>
        <view class="btn-primary" style="font-size: 26rpx;" @click="openAdd">新增</view>
      </view>
    </view>

    <view v-for="a in list" :key="a.id" class="card" style="margin-bottom: 16rpx;">
      <view style="display:flex;justify-content:space-between;align-items:flex-start;gap:16rpx;">
        <view style="flex:1;">
          <view style="font-weight:600;">
            {{ a.consignee }} {{ a.phone }}
            <text v-if="a.isDefault===1" style="margin-left:12rpx;color:#E95F3C;">默认</text>
          </view>
          <view class="muted" style="margin-top: 8rpx;">
            {{ a.provinceName || '' }}{{ a.cityName || '' }}{{ a.districtName || '' }}{{ a.detail || '' }}
          </view>
        </view>
        <view style="display:flex;flex-direction:column;gap:12rpx;">
          <view class="btn-primary" style="background:#666;font-size:26rpx;" @click="makeDefault(a)">设为默认</view>
          <view class="btn-primary" style="background:#333;font-size:26rpx;" @click="remove(a)">删除</view>
        </view>
      </view>
    </view>

    <view v-if="list.length===0" class="muted">暂无地址，请新增</view>

    <!-- 简化：用弹窗收集最少字段 -->
    <view v-if="showAdd" style="position:fixed;left:0;top:0;right:0;bottom:0;background:rgba(0,0,0,.45);display:flex;align-items:flex-end;">
      <view style="background:#fff;width:100%;border-top-left-radius:16rpx;border-top-right-radius:16rpx;padding:24rpx;">
        <view style="font-weight:600;margin-bottom:16rpx;">新增地址</view>
        <input v-model="form.consignee" placeholder="收货人" style="background:#f6f6f6;border-radius:12rpx;padding:18rpx;margin-bottom:12rpx;" />
        <input v-model="form.phone" placeholder="手机号" style="background:#f6f6f6;border-radius:12rpx;padding:18rpx;margin-bottom:12rpx;" />
        <input v-model="form.detail" placeholder="详细地址（如：xx路xx号）" style="background:#f6f6f6;border-radius:12rpx;padding:18rpx;margin-bottom:12rpx;" />
        <view class="muted" style="font-size:24rpx;margin-bottom:16rpx;">省/市/区字段此处省略，可按你业务再补齐</view>
        <view style="display:flex;gap:16rpx;">
          <view class="btn-primary" style="flex:1;background:#333;" @click="closeAdd">取消</view>
          <view class="btn-primary" style="flex:1;" @click="submitAdd">保存</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { ensureLogin } from '../../utils/auth'
import { getAddressList, addAddress, deleteAddress, setDefaultAddress } from '../../api/address'

export default {
  data() {
    return {
      list: [],
      showAdd: false,
      form: { consignee: '', phone: '', detail: '' }
    }
  },
  async onShow() {
    await this.refresh()
  },
  methods: {
    async refresh() {
      await ensureLogin().catch(() => {})
      const list = await getAddressList()
      this.list = Array.isArray(list) ? list : []
    },
    openAdd() {
      this.form = { consignee: '', phone: '', detail: '' }
      this.showAdd = true
    },
    closeAdd() {
      this.showAdd = false
    },
    async submitAdd() {
      if (!this.form.consignee || !this.form.phone || !this.form.detail) {
        uni.showToast({ title: '请填写收货人/手机号/详细地址', icon: 'none' })
        return
      }
      await addAddress(this.form)
      this.showAdd = false
      await this.refresh()
    },
    async remove(a) {
      await deleteAddress(a.id)
      await this.refresh()
    },
    async makeDefault(a) {
      await setDefaultAddress(a.id)
      await this.refresh()
    }
  }
}
</script>

