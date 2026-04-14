import { get } from './client';
import { AppStatus } from './types';

export function fetchHealth(): Promise<AppStatus> {
  return get<AppStatus>('/healthcheck');
}
