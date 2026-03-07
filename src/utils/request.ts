import axios, { AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import errorHandler from './errorHandler'
import { API_BASE_URL } from './url'

const service = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json;charset=UTF-8'
    }
})

service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userStr = sessionStorage.getItem('user')
        if (userStr) {
            const user = JSON.parse(userStr)
            config.headers['Authorization'] = `Bearer ${user.token || ''}`
        }
        return config
    },
    (error: AxiosError) => {
        console.error('Request error:', error)
        return Promise.reject(error)
    }
)

service.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data

        if (res.code === 200) {
            return response
        }

        errorHandler.showSimpleError(res.msg || 'Operation failed')
        return Promise.reject(new Error(res.msg || 'Operation failed'))
    },
    (error: AxiosError) => {
        errorHandler.handleError(error, 'Request')
        return Promise.reject(error)
    }
)

export default service