import { request } from '../utils/request'

export function submitOrder(payload) {
  return request({
    url: '/user/order/submit',
    method: 'POST',
    data: payload
  })
}

