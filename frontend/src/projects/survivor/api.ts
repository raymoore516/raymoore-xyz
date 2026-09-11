import type { SurvivorResponse } from './types';

export async function getSurvivorLeague(signal: AbortSignal): Promise<SurvivorResponse> {
  const response = await fetch('/api/survivor', { signal, cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`);
  }
  return response.json() as Promise<SurvivorResponse>;
}
