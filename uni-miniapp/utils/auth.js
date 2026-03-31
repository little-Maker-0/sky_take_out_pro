import { request } from './request'
import { getToken, setToken, clearToken } from './storage'

export async function loginByWeChatCode(code) {
  const data = await request({
    url: '/user/user/login',
    method: 'POST',
    data: { code }
  })
  if (data && data.token) {
    setToken(data.token)
    return data.token
  }
  throw new Error('登录失败：未返回 token')
}

export async function ensureLogin({ force = false } = {}) {
  if (!force && getToken()) return getToken()

  // #ifdef MP-WEIXIN
  const loginRes = await new Promise((resolve, reject) => {
    wx.login({
      timeout: 8000,
      success: resolve,
      fail: reject
    })
  })
  if (!loginRes || !loginRes.code) throw new Error('wx.login 未返回 code')
  return await loginByWeChatCode(loginRes.code)
  // #endif

  // 其它端先不做
}

export function logout() {
  clearToken()
}

