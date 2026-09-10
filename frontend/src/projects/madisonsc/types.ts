export type LatestWeek = {
  year: number | null;
  week: number | null;
};

export type PickRecord = {
  wins: number;
  losses: number;
  ties: number;
};

export type WeeklyPick = {
  team: string;
  underdog: boolean | null;
  line: number;
  result: 'win' | 'loss' | 'tie' | null;
};

export type WeeklyContestant = {
  contestantId: string;
  name: string;
  rank: number;
  cumulativeWinPercentage: number;
  cumulativeRecord: PickRecord;
  weeklyRecord: PickRecord;
  picks: WeeklyPick[];
};

export type WeeklyPicksResponse = {
  year: number;
  week: number;
  seasonLabel: string;
  availableYears: number[];
  contestants: WeeklyContestant[];
};

export type YearlyPicksResponse = {
  year: number;
  seasonLabel: string;
  weeks: Array<{
    week: number;
    contestants: WeeklyContestant[];
  }>;
};
