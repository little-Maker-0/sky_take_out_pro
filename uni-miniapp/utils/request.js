import { CONFIG } from './config'
import { getToken } from './storage'

function normalizeUrl(url) {
  if (!url) return CONFIG.baseURL
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  const base = CONFIG.baseURL.replace(/\/$/, '')
  const path = url.startsWith('/') ? url : `/${url}`
  return `${base}${path}`
}

export function request({ url, method = 'GET', data, header = {} }) {
  const token = getToken()
  const finalHeader = {
    'content-type': 'application/json',
    ...header
  }
  if (token) finalHeader.authentication = token

  return new Promise((resolve, reject) => {
    uni.request({
      url: normalizeUrl(url),
      method,
      data,
      header: finalHeader,
      success(res) {
        const { statusCode, data: body } = res
        if (statusCode < 200 || statusCode >= 300) {
          uni.showToast({ title: `网络错误(${statusCode})`, icon: 'none' })
          reject(res)
          return
        }

        // 适配后端 Result<T>：{code,msg,data}
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 1) {
            resolve(body.data)
          } else {
            uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
            reject(body)
          }
          return
        }

        resolve(body)
      },
      fail(err) {
        uni.showToast({ title: '请求失败，请检查后端地址', icon: 'none' })
        reject(err)
      }
    })
  })
}

