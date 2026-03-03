import axios, { AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import errorHandler from './errorHandler'

const service = axios.create({
    baseURL: 'http://localhost:8080/api',
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
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

service.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data

        if (res.code === 200) {
            return response
        } else {
            errorHandler.showSimpleError(res.msg || '操作失败')
            return Promise.reject(new Error(res.msg || '操作失败'))
        }
    },
    (error: AxiosError) => {
        errorHandler.handleError(error, '请求')
        return Promise.reject(error)
    }
)

export default service
