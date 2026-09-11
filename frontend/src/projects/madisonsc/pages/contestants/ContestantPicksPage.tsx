import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getContestantPicks } from '../../api';
import NavigationBanner from '../../components/NavigationBanner';
import type { ContestantPicksResponse, PickRecord } from '../../types';
import '../../styles.css';

function formatRecord(record: PickRecord) {
  return `${record.wins}-${record.losses}-${record.ties}`;
}

function recordColor(record: PickRecord) {
  const completedPicks = record.wins + record.losses + record.ties;
  if (completedPicks === 0) return 'never-picked';

  const scoreNumerator = (2 * record.wins) + record.ties;
  const fiftyPercentNumerator = completedPicks;
  if (scoreNumerator > fiftyPercentNumerator) return 'above-fifty';
  if (scoreNumerator < fiftyPercentNumerator) return 'below-fifty';
  return 'exactly-fifty';
}

function RecordCell({
  record,
  label,
  desktopOnly = false,
  colorCoded = false,
}: {
  record: PickRecord;
  label: string;
  desktopOnly?: boolean;
  colorCoded?: boolean;
}) {
  const formattedRecord = formatRecord(record);
  return (
    <td
      className={`contestant-team-record${colorCoded ? ` ${recordColor(record)}` : ''}${desktopOnly ? ' contestant-year-desktop-only' : ''}`}
      aria-label={`${label}: ${formattedRecord}`}
    >
      {formattedRecord}
    </td>
  );
}

export default function ContestantPicksPage() {
  const { contestant = '' } = useParams();
  const [data, setData] = useState<ContestantPicksResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setData(null);
    setError(null);
    setIsLoading(true);

    getContestantPicks(contestant, controller.signal)
      .then(setData)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return;
        setError(requestError instanceof Error ? requestError.message : 'Unable to load contestant picks.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, [contestant]);

  return (
    <main className="msc-page msc-contestant-picks-page">
      {isLoading && <p className="msc-status" role="status">Loading contestant picks…</p>}
      {error && <p className="msc-status msc-error" role="alert">{error}</p>}

      {data && (
        <>
          <NavigationBanner
            segments={[{ label: 'Contestants', emphasized: true }, { label: data.name }]}
            separator=">>"
          />

          <div className="contestant-team-table-scroll">
            <table className="contestant-team-table">
              <colgroup>
                <col className="contestant-team-column" />
                <col className="contestant-record-column" />
                {data.years.map((year, index) => (
                  <col
                    className={index >= 5 ? 'contestant-year-desktop-only contestant-record-column' : 'contestant-record-column'}
                    key={year}
                  />
                ))}
              </colgroup>
              <thead>
                <tr>
                  <th scope="col" aria-label="Team" />
                  <th scope="col" aria-label="All-Time Record" />
                  {data.years.map((year, index) => (
                    <th
                      className={index >= 5 ? 'contestant-year-desktop-only' : undefined}
                      scope="col"
                      key={year}
                    >
                      <Link to={`/madisonsc/picks/${year}`}>Year {year}</Link>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.teams.map((team) => (
                  <tr key={team.team}>
                    <th scope="row">
                      <span className="contestant-team-identity">
                        <img
                          src={`/madisonsc/img/logo/${encodeURIComponent(team.team)}.gif`}
                          alt=""
                        />
                        <span>{team.team}</span>
                      </span>
                    </th>
                    <RecordCell
                      record={team.cumulativeRecord}
                      label={`${team.team} all-time record`}
                      colorCoded
                    />
                    {team.yearlyRecords.map(({ year, record }, index) => (
                      <RecordCell
                        record={record}
                        label={`${team.team} Year ${year} record`}
                        desktopOnly={index >= 5}
                        key={year}
                      />
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </main>
  );
}
