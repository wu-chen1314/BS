const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const hasWindow = typeof window !== 'undefined'

const resolveApiOrigin = () => {
    const configuredOrigin = (import.meta.env.VITE_API_ORIGIN || '').trim()
    if (configuredOrigin) {
        return trimTrailingSlash(configuredOrigin)
    }

    if (!hasWindow) {
        return ''
    }

    const { protocol, hostname, port, origin } = window.location
    if (!port || port === '80' || port === '443' || port === '8080') {
        return trimTrailingSlash(origin)
    }

    return `${protocol}//${hostname}:8080`
}

const resolveWsOrigin = () => {
    const configuredOrigin = (import.meta.env.VITE_WS_ORIGIN || '').trim()
    if (configuredOrigin) {
        return trimTrailingSlash(configuredOrigin)
    }

    if (!hasWindow) {
        return ''
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const { hostname, port } = window.location
    if (!port || port === '80' || port === '443' || port === '8080') {
        return `${protocol}//${hostname}${port ? `:${port}` : ''}`
    }

    return `${protocol}//${hostname}:8080`
}

export const apiOrigin = resolveApiOrigin()
export const apiBaseUrl = apiOrigin ? `${apiOrigin}/api` : '/api'

export const buildApiUrl = (path: string) => {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    return `${apiBaseUrl}${normalizedPath}`
}

export const buildStaticUrl = (path?: string | null) => {
    if (!path) {
        return ''
    }
    if (/^https?:\/\//i.test(path)) {
        return path
    }
    if (path.startsWith('//')) {
        const protocol = hasWindow ? window.location.protocol : 'https:'
        return `${protocol}${path}`
    }
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    return apiOrigin ? `${apiOrigin}${normalizedPath}` : normalizedPath
}

export const buildWsUrl = (path: string) => {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`
    const wsOrigin = resolveWsOrigin()
    return wsOrigin ? `${wsOrigin}${normalizedPath}` : normalizedPath
}

export const getAuthHeaders = () => {
    if (!hasWindow) {
        return {}
    }

    const userStr = window.sessionStorage.getItem('user')
    if (!userStr) {
        return {}
    }

    try {
        const user = JSON.parse(userStr)
        return user?.token ? { Authorization: `Bearer ${user.token}` } : {}
    } catch (error) {
        console.error('Failed to parse user session', error)
        return {}
    }
}