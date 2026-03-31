import { request } from '../utils/request'

// type: 1 菜品分类；2 套餐分类（按后端约定）
export function getCategoryList(type = 1) {
  return request({
    url: '/user/category/list',
    method: 'GET',
    data: { type }
  })
}

