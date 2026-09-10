import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getWeeklyPicks } from '../api';
import PicksNavigationBanner from '../components/PicksNavigationBanner';
import type { PickRecord, WeeklyPick, WeeklyPicksResponse } from '../types';
import '../styles.css';

function formatRecord(record: PickRecord) {
  return `${record.wins}-${record.losses}-${record.ties}`;
}

function formatPercentage(value: number) {
  return new Intl.NumberFormat(undefined, {
    style: 'percent',
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  }).format(value);
}

function formatSpread(pick: WeeklyPick) {
  if (pick.line === 0) return `${pick.team} PK`;
  if (pick.underdog == null) return `${pick.team} (invalid spread)`;
  return `${pick.team} ${pick.underdog ? '+' : '-'}${pick.line}`;
}

function resultLabel(result: WeeklyPick['result']) {
  if (result == null) return 'TBD';
  return result.toUpperCase();
}

function rankMedal(rank: number) {
  if (rank === 1) return '🥇 ';
  if (rank === 2) return '🥈 ';
  if (rank === 3) return '🥉 ';
  return '';
}

export default function WeeklyPicksPage() {
  const params = useParams();
  const year = Number(params.year);
  const week = Number(params.week);
  const validRoute = Number.isInteger(year) && year > 0 && Number.isInteger(week) && week >= 1 && week <= 18;
  const [data, setData] = useState<WeeklyPicksResponse | null>(null);
  const [isLoading, setIsLoading] = useState(validRoute);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!validRoute) return;

    const controller = new AbortController();
    setData(null);
    setError(null);
    setIsLoading(true);

    getWeeklyPicks(year, week, controller.signal)
      .then(setData)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return;
        setError(requestError instanceof Error ? requestError.message : 'Unable to load picks.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, [validRoute, week, year]);

  if (!validRoute) {
    return (
      <main className="msc-page msc-weekly-picks-page">
        <h1>Madison SC</h1>
        <p className="msc-status msc-error" role="alert">Choose a positive year and a week from 1 through 18.</p>
      </main>
    );
  }

  return (
    <main className="msc-page msc-weekly-picks-page">
      <PicksNavigationBanner year={year} week={week} />

      {year === 12 && (
        <aside className="reyna-memorial" role="note">
          Year 12 has been suspended in loving memory of Reyna
        </aside>
      )}

      {isLoading && <p className="msc-status" role="status">Loading picks…</p>}
      {error && <p className="msc-status msc-error" role="alert">{error}</p>}
      {data && data.contestants.length === 0 && (
        <section className="msc-empty-state" aria-labelledby="no-picks-heading">
          <h2 id="no-picks-heading">No picks found</h2>
          <p>No picks were submitted for Year {year}, Week {week}.</p>
        </section>
      )}

      {data && data.contestants.length > 0 && (
        <section className="contestant-list" aria-label={`Year ${year}, week ${week} standings`}>
          {data.contestants.map((contestant) => (
            <article className="contestant-card" key={contestant.contestantId}>
              <header className="contestant-header">
                <div className="contestant-identity">
                  <h2>{rankMedal(contestant.rank)}{contestant.name}</h2>
                  <span className="cumulative-record">
                    {formatRecord(contestant.cumulativeRecord)} · {formatPercentage(contestant.cumulativeWinPercentage)}
                  </span>
                </div>
                <span className="weekly-record">
                  <strong>Weekly Record:</strong> {formatRecord(contestant.weeklyRecord)}
                </span>
              </header>

              <div className="contestant-picks">
                {contestant.picks.length === 0 ? (
                  <p className="no-contestant-picks">No picks found</p>
                ) : contestant.picks.map((pick) => (
                  <div className={`pick-card ${pick.result ?? 'pending'}`} key={pick.team}>
                    <strong className="pick-spread">{formatSpread(pick)}</strong>
                    <span className="team-logo-frame">
                      <img
                        className="team-logo"
                        src={`/madisonsc/img/logo/${encodeURIComponent(pick.team)}.gif`}
                        alt=""
                      />
                    </span>
                    <span className="pick-result">{resultLabel(pick.result)}</span>
                  </div>
                ))}
              </div>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
