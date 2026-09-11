export interface SurvivorContestant {
  name: string;
  pick: string;
  selectionExists: boolean;
}

export interface SurvivorWeek {
  number: number;
  label: string;
  current: boolean;
  contestants: SurvivorContestant[];
}

export interface SurvivorResponse {
  spreadsheetTitle: string;
  weeks: SurvivorWeek[];
}
