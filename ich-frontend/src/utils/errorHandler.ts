import { ElMessage, ElNotification } from 'element-plus'
import axios, { AxiosError } from 'axios'

interface ErrorResponse {
    code: number
    msg: string
    data?: any
}

interface ErrorDetail {
    title: string
    message: string
    suggestion: string
    type: 'error' | 'warning' | 'info'
}

class GlobalErrorHandler {
    private static instance: GlobalErrorHandler

    private constructor() {}

    public static getInstance(): GlobalErrorHandler {
        if (!GlobalErrorHandler.instance) {
            GlobalErrorHandler.instance = new GlobalErrorHandler()
        }
        return GlobalErrorHandler.instance
    }

    public handleError(error: any, context: string = '操作'): void {
        console.error(`[${context}] 错误:`, error)

        if (axios.isAxiosError(error)) {
            this.handleAxiosError(error, context)
        } else if (error instanceof Error) {
            this.handleSystemError(error, context)
        } else {
            this.handleUnknownError(error, context)
        }
    }

    private handleAxiosError(error: AxiosError, context: string): void {
        const response = error.response as any
        const request = error.request

        if (response) {
            this.handleResponseError(response, context)
        } else if (request) {
            this.handleNetworkError(context)
        } else {
            this.handleRequestSetupError(error, context)
        }
    }

    private handleResponseError(response: any, context: string): void {
        const status = response.status
        const data = response.data as ErrorResponse

        const errorDetail = this.getErrorDetailByStatus(status, data, context)
        this.showErrorNotification(errorDetail)
    }

    private getErrorDetailByStatus(status: number, data: ErrorResponse, context: string): ErrorDetail {
        switch (status) {
            case 400:
                return {
                    title: `${context}失败`,
                    message: data.msg || '请求参数错误',
                    suggestion: '请检查输入信息是否正确，然后重试',
                    type: 'error'
                }
            case 401:
                return {
                    title: '认证失败',
                    message: data.msg || '用户名或密码错误',
                    suggestion: '请检查用户名和密码是否正确，或尝试找回密码',
                    type: 'error'
                }
            case 403:
                return {
                    title: '权限不足',
                    message: data.msg || '您没有权限执行此操作',
                    suggestion: '请联系管理员获取相应权限',
                    type: 'warning'
                }
            case 404:
                return {
                    title: '资源不存在',
                    message: data.msg || '请求的资源不存在',
                    suggestion: '请检查请求地址是否正确',
                    type: 'error'
                }
            case 429:
                return {
                    title: '请求过于频繁',
                    message: data.msg || '操作过于频繁，请稍后再试',
                    suggestion: '请等待片刻后再进行操作',
                    type: 'warning'
                }
            case 500:
                return {
                    title: '服务器错误',
                    message: data.msg || '服务器内部错误',
                    suggestion: '请稍后重试，如果问题持续，请联系技术支持',
                    type: 'error'
                }
            case 502:
            case 503:
            case 504:
                return {
                    title: '服务不可用',
                    message: '服务器暂时无法响应',
                    suggestion: '请检查网络连接，稍后重试',
                    type: 'error'
                }
            default:
                return {
                    title: `${context}失败`,
                    message: data.msg || `未知错误 (状态码: ${status})`,
                    suggestion: '请稍后重试，如果问题持续，请联系技术支持',
                    type: 'error'
                }
        }
    }

    private handleNetworkError(context: string): void {
        const errorDetail: ErrorDetail = {
            title: '网络连接失败',
            message: '无法连接到服务器',
            suggestion: '请检查网络连接是否正常，然后重试',
            type: 'error'
        }
        this.showErrorNotification(errorDetail)
    }

    private handleRequestSetupError(error: AxiosError, context: string): void {
        const errorDetail: ErrorDetail = {
            title: `${context}失败`,
            message: '请求配置错误',
            suggestion: '请检查请求参数是否正确',
            type: 'error'
        }
        this.showErrorNotification(errorDetail)
    }

    private handleSystemError(error: Error, context: string): void {
        const errorDetail: ErrorDetail = {
            title: `${context}失败`,
            message: error.message || '系统错误',
            suggestion: '请刷新页面重试，如果问题持续，请联系技术支持',
            type: 'error'
        }
        this.showErrorNotification(errorDetail)
    }

    private handleUnknownError(error: any, context: string): void {
        const errorDetail: ErrorDetail = {
            title: `${context}失败`,
            message: '未知错误',
            suggestion: '请刷新页面重试，如果问题持续，请联系技术支持',
            type: 'error'
        }
        this.showErrorNotification(errorDetail)
    }

    private showErrorNotification(detail: ErrorDetail): void {
        ElNotification({
            title: detail.title,
            message: this.formatErrorMessage(detail),
            type: detail.type,
            duration: 5000,
            showClose: true,
            dangerouslyUseHTMLString: true
        })
    }

    private formatErrorMessage(detail: ErrorDetail): string {
        return `
            <div style="line-height: 1.8;">
                <div style="margin-bottom: 8px; font-size: 14px;">${detail.message}</div>
                <div style="color: #909399; font-size: 12px;">
                    <strong>💡 建议：</strong>${detail.suggestion}
                </div>
            </div>
        `
    }

    public showSimpleError(message: string, suggestion: string = ''): void {
        ElNotification({
            title: '操作失败',
            message: this.formatErrorMessage({
                title: '操作失败',
                message,
                suggestion: suggestion || '请稍后重试',
                type: 'error'
            }),
            type: 'error',
            duration: 5000,
            showClose: true,
            dangerouslyUseHTMLString: true
        })
    }

    public showSuccess(message: string): void {
        ElMessage.success(message)
    }

    public showWarning(message: string): void {
        ElMessage.warning(message)
    }

    public showInfo(message: string): void {
        ElMessage.info(message)
    }
}

export const errorHandler = GlobalErrorHandler.getInstance()

export default errorHandler
