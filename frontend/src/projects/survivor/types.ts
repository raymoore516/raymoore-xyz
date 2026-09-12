export type SurvivorPickStatus = 'PENDING' | 'SURVIVAL' | 'ELIMINATION' | 'ELIMINATED';

export interface SurvivorContestant {
  name: string;
  pick: string;
  selectionExists: boolean;
  status: SurvivorPickStatus;
  eliminationWeek: number | null;
}

export interface SurvivorWeek {
  number: number;
  label: string;
  picksHidden: boolean;
  contestants: SurvivorContestant[];
}

export interface SurvivorResponse {
  spreadsheetTitle: string;
  weeks: SurvivorWeek[];
}
