<template>
  <view style="padding: 24rpx;">
    <view class="card" style="margin-bottom: 20rpx;">
      <view style="display:flex;justify-content:space-between;align-items:center;">
        <view>
          <view style="font-weight: 600;">点餐</view>
          <view class="muted" style="margin-top: 8rpx;">选择分类后展示菜品，点击 + 加入购物车</view>
        </view>
        <view class="btn-primary" style="font-size: 26rpx;" @click="refreshAll">刷新</view>
      </view>
    </view>

    <view class="card" style="margin-bottom: 20rpx;">
      <view style="display:flex;gap:16rpx;flex-wrap:wrap;">
        <view
          v-for="c in categories"
          :key="c.id"
          @click="selectCategory(c)"
          :style="{
            padding: '14rpx 18rpx',
            borderRadius: '999rpx',
            background: c.id === selectedCategoryId ? '#E95F3C' : '#f2f2f2',
            color: c.id === selectedCategoryId ? '#fff' : '#333'
          }"
        >
          {{ c.name }}
        </view>
      </view>
      <view v-if="categories.length === 0" class="muted" style="margin-top: 16rpx;">暂无分类</view>
    </view>

    <view class="card">
      <view style="font-weight: 600; margin-bottom: 12rpx;">菜品</view>
      <view v-if="loading" class="muted">加载中...</view>

      <view v-for="d in dishes" :key="d.id" style="display:flex;justify-content:space-between;align-items:center;padding: 18rpx 0;border-bottom: 1px solid #f3f3f3;">
        <view style="flex:1; padding-right: 16rpx;">
          <view style="font-weight: 500;">{{ d.name }}</view>
          <view class="muted" style="margin-top: 6rpx; font-size: 24rpx;">{{ d.description || '' }}</view>
          <view style="margin-top: 8rpx; color:#E95F3C; font-weight: 600;">¥ {{ (d.price / 100).toFixed(2) }}</view>
        </view>
        <view class="btn-primary" style="width: 76rpx; padding: 10rpx 0;" @click="addDish(d)">+</view>
      </view>

      <view v-if="!loading && dishes.length === 0" class="muted">该分类暂无菜品</view>
    </view>
  </view>
</template>

<script>
import { ensureLogin } from '../../utils/auth'
import { getCategoryList } from '../../api/category'
import { getDishListByCategory } from '../../api/dish'
import { addToCart } from '../../api/cart'

export default {
  data() {
    return {
      categories: [],
      selectedCategoryId: null,
      dishes: [],
      loading: false
    }
  },
  async onShow() {
    await this.refreshAll()
  },
  methods: {
    async refreshAll() {
      await ensureLogin().catch(() => {})
      await this.loadCategories()
    },
    async loadCategories() {
      this.loading = true
      try {
        const list = await getCategoryList(1)
        this.categories = Array.isArray(list) ? list : []
        if (!this.selectedCategoryId && this.categories.length) {
          this.selectedCategoryId = this.categories[0].id
        }
        if (this.selectedCategoryId) {
          await this.loadDishes(this.selectedCategoryId)
        } else {
          this.dishes = []
        }
      } finally {
        this.loading = false
      }
    },
    async selectCategory(c) {
      this.selectedCategoryId = c.id
      await this.loadDishes(c.id)
    },
    async loadDishes(categoryId) {
      this.loading = true
      try {
        const list = await getDishListByCategory(categoryId)
        this.dishes = Array.isArray(list) ? list : []
      } finally {
        this.loading = false
      }
    },
    async addDish(d) {
      await ensureLogin().catch(() => {})
      await addToCart({ dishId: d.id })
      uni.showToast({ title: '已加入购物车', icon: 'none' })
    }
  }
}
</script>

