package com.example.demo.request;

import java.util.List;

/**
 * 批量删除请求对象
 * 支持两种格式：
 * 1. 数组格式：[1, 2, 3]
 * 2. 对象格式：{ "ids": [1, 2, 3] }
 */
public class BatchDeleteRequest {
    
    private List<Long> ids;
    
    public BatchDeleteRequest() {
    }
    
    public BatchDeleteRequest(List<Long> ids) {
        this.ids = ids;
    }
    
    public List<Long> getIds() {
        return ids;
    }
    
    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
