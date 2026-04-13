import { get, post, remove } from "./client";
import { Asset, CreateAssetRequest } from "./types";


export function getAsset(): Promise<Asset[]> {
    return get<Asset[]>("/assets");
}

export function createAsset(asset: CreateAssetRequest): Promise<void> {
    return post<CreateAssetRequest, void>("/assets", asset);
}

export function deleteAsset(id: number): Promise<void> {
    return remove(`/assets/${id}`);
}