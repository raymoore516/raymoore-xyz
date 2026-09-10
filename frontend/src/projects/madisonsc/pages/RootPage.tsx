import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getRootSummary } from '../api';
import type { PickRecord, RootResponse } from '../types';
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

function rankMedal(rank: number) {
  if (rank === 1) return '🥇 ';
  if (rank === 2) return '🥈 ';
  if (rank === 3) return '🥉 ';
  return '';
}

export default function RootPage() {
  const [data, setData] = useState<RootResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    getRootSummary(controller.signal)
      .then(setData)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return;
        setError(requestError instanceof Error ? requestError.message : 'Unable to load Madison SC.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, []);

  return (
    <main className="msc-page msc-root-page">
      <header className="msc-root-heading">
        <h1>Madison SC</h1>
      </header>

      {isLoading && <p className="msc-status" role="status">Loading standings…</p>}
      {error && <p className="msc-status msc-error" role="alert">{error}</p>}
      {data && data.years.length === 0 && (
        <section className="msc-empty-state" aria-labelledby="no-years-heading">
          <h2 id="no-years-heading">No picks found</h2>
          <p>There are no Madison SC competition years in the database yet.</p>
        </section>
      )}

      {data && data.years.length > 0 && (
        <section className="msc-year-summary-grid" aria-label="Madison SC final standings by year">
          {data.years.map((year) => (
            <article className="msc-year-summary-card" key={year.year}>
              <header>
                <h2>
                  <Link to={`/madisonsc/picks/${year.year}`}>Year {year.year}</Link>
                </h2>
                <p>{year.seasonLabel}</p>
              </header>
              <p className="msc-year-summary-cutoff">Final standings through Week {year.latestWeek}</p>
              <ol className="msc-final-standings">
                {year.standings.map((contestant) => (
                  <li key={contestant.contestantId}>
                    <span className="msc-standing-rank" aria-label={`Rank ${contestant.rank}`}>
                      #{contestant.rank}
                    </span>
                    <span className="msc-standing-name">{rankMedal(contestant.rank)}{contestant.name}</span>
                    <span
                      className="msc-standing-record"
                      aria-label={`Cumulative record ${formatRecord(contestant.cumulativeRecord)}, ${formatPercentage(contestant.cumulativeWinPercentage)}`}
                    >
                      {formatRecord(contestant.cumulativeRecord)} · {formatPercentage(contestant.cumulativeWinPercentage)}
                    </span>
                  </li>
                ))}
              </ol>
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
