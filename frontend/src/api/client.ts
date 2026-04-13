import { ApiError } from "./types";

let token: string | null = localStorage.getItem("jwt");

export function setToken(newToken: string | null): void {
    token = newToken;
    if (newToken) localStorage.setItem("jwt", newToken);
    else localStorage.removeItem("jwt")
}

export function getToken(): string | null {
    return token;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
   const headers: Record<string, string> = {
    "Content-Type": "application/json"
   };

   if(token) headers["Authorization"] = `Bearer ${token}`;

   const response = await fetch(`http://localhost:9000/v1${path}`, {
    ...options,
    headers,
   });

   if(response.status === 204) return undefined as T;
   if(!response.ok) throw new ApiError(response.status, await response.text());
   return response.json() as Promise<T>;
}

export function get<T>(path: string): Promise<T> {
    return request(path);
}

export function post<TBody, TResponse>(path: string, body?: TBody): Promise<TResponse> {
    return request(path, {method: 'POST', body: body ? JSON.stringify(body) : undefined});
}

export function remove<T>(path: string): Promise<T> {
    return request(path, {method: 'DELETE'});
}
