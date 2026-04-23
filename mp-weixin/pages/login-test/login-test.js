Page({
  data: {
    loginResult: '',
    userInfo: {}
  },

  onLoad: function() {
    this.checkLoginStatus();
  },

  // 检查登录状态
  checkLoginStatus: function() {
    wx.getStorage({ key: 'userInfo', success: (res) => {
      this.setData({ userInfo: res.data });
    }});
  },

  // 测试登录
  testLogin: function() {
    const that = this;
    
    // 模拟登录过程
    wx.login({
      success: (loginRes) => {
        console.log('登录成功', loginRes);
        
        // 模拟获取用户信息
        wx.getUserInfo({
          success: (userRes) => {
            console.log('获取用户信息成功', userRes);
            
            // 模拟登录请求
            wx.request({
              url: 'http://localhost/user/user/login',
              method: 'POST',
              data: {
                code: loginRes.code
              },
              success: (res) => {
                console.log('登录请求成功', res);
                
                if (res.data.code === 1) {
                  // 登录成功，保存用户信息和token
                  const userInfoWithToken = {
                    ...userRes.userInfo,
                    token: res.data.data.token
                  };
                  wx.setStorage({ key: 'userInfo', data: userInfoWithToken });
                  that.setData({
                    loginResult: '登录成功',
                    userInfo: userInfoWithToken
                  });
                  wx.showToast({ title: '登录成功', icon: 'success' });
                } else {
                  that.setData({ loginResult: '登录失败: ' + res.data.msg });
                  wx.showToast({ title: '登录失败', icon: 'none' });
                }
              },
              fail: (err) => {
                console.log('登录请求失败', err);
                that.setData({ loginResult: '登录请求失败: ' + err.errMsg });
                wx.showToast({ title: '网络请求失败', icon: 'none' });
              }
            });
          },
          fail: (err) => {
            console.log('获取用户信息失败', err);
            that.setData({ loginResult: '获取用户信息失败: ' + err.errMsg });
            wx.showToast({ title: '获取用户信息失败', icon: 'none' });
          }
        });
      },
      fail: (err) => {
        console.log('登录失败', err);
        that.setData({ loginResult: '登录失败: ' + err.errMsg });
        wx.showToast({ title: '登录失败', icon: 'none' });
      }
    });
  },

  // 清除登录状态
  clearLogin: function() {
    wx.removeStorage({ key: 'userInfo' });
    this.setData({
      loginResult: '登录状态已清除',
      userInfo: {}
    });
    wx.showToast({ title: '登录状态已清除', icon: 'success' });
  }
});