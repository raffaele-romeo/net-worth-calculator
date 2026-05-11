import { ApiError } from './types';

let token: string | null = localStorage.getItem('jwt');

export function setToken(newToken: string | null): void {
  token = newToken;
  if (newToken) localStorage.setItem('jwt', newToken);
  else localStorage.removeItem('jwt');
}

export function getToken(): string | null {
  return token;
}

/**
 * Decode the `exp` claim (unix seconds) from a JWT.
 * Returns `null` if the token is malformed or has no `exp`.
 */
export function getTokenExp(jwt: string): number | null {
  try {
    const parts = jwt.split('.');
    if (parts.length < 3 || !parts[1]) return null;
    const payload = JSON.parse(atob(parts[1]));
    return typeof payload.exp === 'number' ? payload.exp : null;
  } catch {
    return null;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (token) headers['Authorization'] = `Bearer ${token}`;

  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/v1';
  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401 || (response.status === 403 && token)) {
    setToken(null);
    window.location.href = '/login';
    throw new ApiError(response.status, 'Session expired');
  }

  if (!response.ok) throw new ApiError(response.status, await response.text());
  const text = await response.text();
  return text ? JSON.parse(text) : (undefined as T);
}

export function get<T>(path: string): Promise<T> {
  return request(path);
}

export function post<TBody, TResponse>(path: string, body?: TBody): Promise<TResponse> {
  return request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
}

export function remove<T>(path: string): Promise<T> {
  return request(path, { method: 'DELETE' });
}
