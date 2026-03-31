import { request } from '../utils/request'

export function getDishListByCategory(categoryId) {
  return request({
    url: '/user/dish/list',
    method: 'GET',
    data: { categoryId }
  })
}

