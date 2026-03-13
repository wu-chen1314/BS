import request from "@/utils/request";
import type { InheritorQuery, InheritorRecord } from "@/types/inheritor";

export const fetchInheritorPage = (params: InheritorQuery) =>
  request.get("/inheritors/page", {
    params,
  });

export const saveInheritor = (payload: InheritorRecord) =>
  payload.id ? request.put("/inheritors/update", payload) : request.post("/inheritors/add", payload);

export const deleteInheritor = (id: number) => request.delete(`/inheritors/delete/${id}`);

export const batchDeleteInheritors = (ids: number[]) =>
  request.post("/inheritors/delete/batch", {
    ids,
  });

export const fetchProjectOptions = () => request.get("/projects/list");

