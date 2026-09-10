import type { LatestWeek, RootResponse, WeeklyPicksResponse, YearlyPicksResponse } from './types';

const root = '/api/madisonsc';

async function getJson<T>(path: string, signal: AbortSignal): Promise<T> {
  const response = await fetch(path, { signal });
  if (!response.ok) {
    throw new Error(`Request failed (${response.status})`);
  }
  return response.json() as Promise<T>;
}

export function getRootSummary(signal: AbortSignal): Promise<RootResponse> {
  return getJson(root, signal);
}

export function getLatestWeek(signal: AbortSignal): Promise<LatestWeek> {
  return getJson(`${root}/picks/latest`, signal);
}

export function getWeeklyPicks(year: number, week: number, signal: AbortSignal): Promise<WeeklyPicksResponse> {
  return getJson(`${root}/picks/${year}/${week}`, signal);
}

export function getYearlyPicks(year: number, signal: AbortSignal): Promise<YearlyPicksResponse> {
  return getJson(`${root}/picks/${year}`, signal);
}
