import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getYearlyPicks } from '../../api';
import PicksNavigationBanner from '../../components/PicksNavigationBanner';
import type { PickRecord, WeeklyContestant, WeeklyPick, YearlyPicksResponse } from '../../types';
import '../../styles.css';

const pickSlots = Array.from({ length: 5 }, (_, index) => index);

function formatRecord(record: PickRecord) {
  return `${record.wins}-${record.losses}-${record.ties}`;
}

function resultLabel(result: WeeklyPick['result']) {
  if (result == null) return 'Pending';
  return result[0].toUpperCase() + result.slice(1);
}

function rankMedal(rank: number) {
  if (rank === 1) return '🥇 ';
  if (rank === 2) return '🥈 ';
  if (rank === 3) return '🥉 ';
  return '';
}

function ContestantRow({ contestant }: { contestant: WeeklyContestant }) {
  return (
    <tr>
      <th scope="row" title={contestant.name}>
        {rankMedal(contestant.rank)}{contestant.name}
      </th>
      <td className="year-record-cell" aria-label={`Cumulative record ${formatRecord(contestant.cumulativeRecord)}`}>
        {formatRecord(contestant.cumulativeRecord)}
      </td>
      <td className="year-record-cell" aria-label={`Weekly record ${formatRecord(contestant.weeklyRecord)}`}>
        {formatRecord(contestant.weeklyRecord)}
      </td>
      {pickSlots.map((slot) => {
        const pick = contestant.picks[slot];
        if (!pick) {
          return <td className="year-pick-cell empty" aria-label={`Pick ${slot + 1}: empty`} key={slot}>—</td>;
        }

        const result = resultLabel(pick.result);
        return (
          <td
            className={`year-pick-cell ${pick.result ?? 'pending'}`}
            aria-label={`${pick.team}, ${result}`}
            title={`${pick.team} — ${result}`}
            key={`${slot}-${pick.team}`}
          >
            {pick.team}
          </td>
        );
      })}
    </tr>
  );
}

export default function YearlyPicksPage() {
  const params = useParams();
  const year = Number(params.year);
  const validRoute = Number.isInteger(year) && year > 0;
  const [data, setData] = useState<YearlyPicksResponse | null>(null);
  const [isLoading, setIsLoading] = useState(validRoute);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!validRoute) return;

    const controller = new AbortController();
    setData(null);
    setError(null);
    setIsLoading(true);

    getYearlyPicks(year, controller.signal)
      .then(setData)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return;
        setError(requestError instanceof Error ? requestError.message : 'Unable to load picks.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, [validRoute, year]);

  if (!validRoute) {
    return (
      <main className="msc-page msc-yearly-picks-page">
        <h1>Madison SC</h1>
        <p className="msc-status msc-error" role="alert">Choose a positive year.</p>
      </main>
    );
  }

  return (
    <main className="msc-page msc-yearly-picks-page">
      <PicksNavigationBanner year={year} />

      {year === 12 && (
        <aside className="reyna-memorial" role="note">
          Year 12 has been suspended in loving memory of Reyna
        </aside>
      )}

      {isLoading && <p className="msc-status" role="status">Loading picks…</p>}
      {error && <p className="msc-status msc-error" role="alert">{error}</p>}

      {data && (
        <section className="year-week-grid" aria-label={`Year ${year} weekly snapshots`}>
          {data.weeks.map((week) => (
            <article className="year-week-card" key={week.week}>
              <h2>
                <Link to={`/madisonsc/picks/${year}/${week.week}`}>Week {week.week}</Link>
              </h2>
              {week.contestants.length === 0 ? (
                <p className="year-week-empty">No picks found</p>
              ) : (
                <div className="year-week-table-scroll">
                  <table className="year-week-table">
                    <colgroup>
                      <col className="year-name-column" />
                      <col className="year-record-column" />
                      <col className="year-record-column" />
                      {pickSlots.map((slot) => <col className="year-pick-column" key={slot} />)}
                    </colgroup>
                    <thead>
                      <tr>
                        <th scope="col" aria-label="Contestant" />
                        <th scope="col" title="Cumulative record">Total</th>
                        <th scope="col" title="Weekly record">Week</th>
                        {pickSlots.map((slot) => <th scope="col" key={slot}>P{slot + 1}</th>)}
                      </tr>
                    </thead>
                    <tbody>
                      {week.contestants.map((contestant) => (
                        <ContestantRow contestant={contestant} key={contestant.contestantId} />
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </article>
          ))}
        </section>
      )}
    </main>
  );
}
