import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import errorHandler from './errorHandler'
import { requestCache, generateCacheKey } from './cache'
import { apiBaseUrl } from './url'

const service = axios.create({
    baseURL: apiBaseUrl,
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

        if (config.method === 'get' && config.url?.includes('/region-category/')) {
            const cacheKey = generateCacheKey(config.url || '', config.params)
            config.headers['X-Cache-Key'] = cacheKey
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
            const config = response.config
            if (config.method === 'get' && config.headers['X-Cache-Key']) {
                const cacheKey = config.headers['X-Cache-Key'] as string
                requestCache.set(cacheKey, res)
            }
            return response
        }

        errorHandler.showSimpleError(res.msg || 'Operation failed')
        return Promise.reject(new Error(res.msg || 'Operation failed'))
    },
    (error: AxiosError) => {
        const config = error.config as InternalAxiosRequestConfig & { headers: Record<string, string> }
        if (config && config.method === 'get' && config.headers['X-Cache-Key']) {
            const cacheKey = config.headers['X-Cache-Key']
            const cachedData = requestCache.get(cacheKey)

            if (cachedData) {
                return Promise.resolve({ data: cachedData } as AxiosResponse)
            }
        }

        errorHandler.handleError(error, 'Request')
        return Promise.reject(error)
    }
)

export default service