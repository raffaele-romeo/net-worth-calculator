import { post, setToken } from './client';
import { User } from './types';

export async function createUser(user: User): Promise<string> {
  const token = await post<User, string>('/auth/users', user);
  setToken(token);

  return token;
}

export async function loginUser(user: User): Promise<string> {
  const token = await post<User, string>('/auth/login', user);
  setToken(token);

  return token;
}

export async function logoutUser(): Promise<void> {
  await post<void, void>('/auth/logout');
  setToken(null);
}
