import request from "@/utils/request";
import type { UserQuery, UserRecord } from "@/types/user-management";

export const fetchUserPage = (params: UserQuery) =>
  request.get("/users/page", {
    params,
  });

export const saveUser = (payload: UserRecord) =>
  payload.id ? request.put("/users/update", payload) : request.post("/users/add", payload);

export const deleteUser = (id: number) => request.delete(`/users/delete/${id}`);

export const resetUserPassword = (id: number) => request.put(`/users/reset-password/${id}`);
