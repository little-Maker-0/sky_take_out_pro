import { request } from '../utils/request'

export function getCartList() {
  return request({
    url: '/user/shoppingCart/list',
    method: 'GET'
  })
}

export function addToCart({ dishId, setmealId, dishFlavor }) {
  return request({
    url: '/user/shoppingCart/add',
    method: 'POST',
    data: { dishId, setmealId, dishFlavor }
  })
}

export function subFromCart({ dishId, setmealId, dishFlavor }) {
  return request({
    url: '/user/shoppingCart/sub',
    method: 'POST',
    data: { dishId, setmealId, dishFlavor }
  })
}

export function cleanCart() {
  return request({
    url: '/user/shoppingCart/clean',
    method: 'DELETE'
  })
}

