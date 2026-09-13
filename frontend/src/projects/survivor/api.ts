import type { SurvivorResponse } from './types';

export async function getSurvivorLeague(signal: AbortSignal, refresh: boolean): Promise<SurvivorResponse> {
  const url = refresh ? '/api/survivor?refresh=true' : '/api/survivor';
  const response = await fetch(url, { signal, cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`);
  }
  return response.json() as Promise<SurvivorResponse>;
}
