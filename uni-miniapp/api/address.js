import { request } from '../utils/request'

export function getAddressList() {
  return request({ url: '/user/addressBook/list', method: 'GET' })
}

export function getDefaultAddress() {
  return request({ url: '/user/addressBook/default', method: 'GET' })
}

export function addAddress(address) {
  return request({ url: '/user/addressBook', method: 'POST', data: address })
}

export function updateAddress(address) {
  return request({ url: '/user/addressBook', method: 'PUT', data: address })
}

export function deleteAddress(id) {
  return request({ url: '/user/addressBook', method: 'DELETE', data: { id } })
}

export function setDefaultAddress(id) {
  return request({ url: '/user/addressBook/default', method: 'PUT', data: { id } })
}

