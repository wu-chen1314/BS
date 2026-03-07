const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const normalizePath = (value: string) => (value.startsWith('/') ? value : `/${value}`)

const isAbsoluteUrl = (value: string) => /^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')

const rawApiBase = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || '/api'
const rawServerBase = (import.meta.env.VITE_SERVER_BASE_URL as string | undefined)?.trim() || ''
const rawWsBase = (import.meta.env.VITE_WS_BASE_URL as string | undefined)?.trim() || ''

export const API_BASE_URL = trimTrailingSlash(rawApiBase) || '/api'

export const SERVER_BASE_URL = trimTrailingSlash(
    rawServerBase || (/^https?:\/\//i.test(API_BASE_URL) && API_BASE_URL.endsWith('/api') ? API_BASE_URL.slice(0, -4) : '')
)

export const DEFAULT_AVATAR_URL = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

export const toApiUrl = (path: string) => {
    if (isAbsoluteUrl(path)) {
        return path
    }
    if (path.startsWith('/api/')) {
        return API_BASE_URL === '/api' ? path : `${API_BASE_URL}${path.slice(4)}`
    }
    return `${API_BASE_URL}${normalizePath(path)}`
}

export const toServerUrl = (path?: string | null) => {
    if (!path) {
        return ''
    }
    if (isAbsoluteUrl(path)) {
        return path
    }
    const normalized = normalizePath(path)
    return SERVER_BASE_URL ? `${SERVER_BASE_URL}${normalized}` : normalized
}

export const toWsUrl = (path: string) => {
    if (/^wss?:\/\//i.test(path)) {
        return path
    }
    const normalized = normalizePath(path)
    if (rawWsBase) {
        return `${trimTrailingSlash(rawWsBase)}${normalized}`
    }
    if (SERVER_BASE_URL && /^https?:\/\//i.test(SERVER_BASE_URL)) {
        return `${SERVER_BASE_URL.replace(/^http/i, 'ws')}${normalized}`
    }
    if (typeof window !== 'undefined') {
        return `${window.location.origin.replace(/^http/i, 'ws')}${normalized}`
    }
    return normalized
}