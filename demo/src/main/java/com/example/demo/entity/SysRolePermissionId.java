package com.example.demo.entity;

import java.io.Serializable;
import java.util.Objects;

public class SysRolePermissionId implements Serializable {
    private Long roleId;  // 角色ID
    private Long permId;  // 权限ID

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermId() {
        return permId;
    }

    public void setPermId(Long permId) {
        this.permId = permId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysRolePermissionId that = (SysRolePermissionId) o;
        return Objects.equals(roleId, that.roleId) && Objects.equals(permId, that.permId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, permId);
    }
    // Getters, Setters, hashCode, equals
}
