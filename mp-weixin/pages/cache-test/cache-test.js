Page({
  data: {
    categories: [],
    dishId: '',
    dishListResult: '',
    dishListTime: 0,
    dishDetailResult: '',
    dishDetailTime: 0,
    cartResult: '',
    cartCount: 0,
    clearCacheResult: ''
  },

  onLoad: function() {
    this.getCategories();
  },

  // 获取分类列表
  getCategories: function() {
    wx.request({
      url: 'http://localhost/user/category/list',
      method: 'GET',
      success: (res) => {
        if (res.data.code === 1) {
          this.setData({
            categories: res.data.data
          });
        }
      }
    });
  },

  // 测试菜品列表缓存
  testDishListCache: function(e) {
    const categoryId = e.currentTarget.dataset.categoryId;
    const startTime = Date.now();
    
    wx.request({
      url: 'http://localhost/user/dish/list',
      method: 'GET',
      data: {
        categoryId: categoryId
      },
      success: (res) => {
        const endTime = Date.now();
        const responseTime = endTime - startTime;
        
        if (res.data.code === 1) {
          this.setData({
            dishListResult: `成功获取${res.data.data.length}个菜品`,
            dishListTime: responseTime
          });
        } else {
          this.setData({
            dishListResult: '获取失败',
            dishListTime: responseTime
          });
        }
      }
    });
  },

  // 输入菜品ID
  onDishIdInput: function(e) {
    this.setData({
      dishId: e.detail.value
    });
  },

  // 测试菜品详情缓存
  testDishDetailCache: function() {
    const dishId = this.data.dishId;
    if (!dishId) {
      wx.showToast({
        title: '请输入菜品ID',
        icon: 'none'
      });
      return;
    }

    const startTime = Date.now();
    wx.request({
      url: `http://localhost/user/dish/${dishId}`,
      method: 'GET',
      success: (res) => {
        const endTime = Date.now();
        const responseTime = endTime - startTime;
        
        if (res.data.code === 1) {
          this.setData({
            dishDetailResult: `成功获取菜品: ${res.data.data.name}`,
            dishDetailTime: responseTime
          });
        } else {
          this.setData({
            dishDetailResult: '获取失败',
            dishDetailTime: responseTime
          });
        }
      }
    });
  },

  // 测试购物车缓存
  testShoppingCartCache: function() {
    wx.request({
      url: 'http://localhost/user/shoppingCart/list',
      method: 'GET',
      success: (res) => {
        if (res.data.code === 1) {
          this.setData({
            cartResult: '成功获取购物车数据',
            cartCount: res.data.data.length
          });
        } else {
          this.setData({
            cartResult: '获取失败',
            cartCount: 0
          });
        }
      }
    });
  },

  // 清理缓存
  clearCache: function() {
    wx.clearStorageSync();
    this.setData({
      clearCacheResult: '缓存清理成功',
      dishListResult: '',
      dishListTime: 0,
      dishDetailResult: '',
      dishDetailTime: 0,
      cartResult: '',
      cartCount: 0
    });
    wx.showToast({
      title: '缓存清理成功',
      icon: 'success'
    });
  }
});