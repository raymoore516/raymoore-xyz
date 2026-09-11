import { useEffect, useState } from 'react';
import { getSurvivorLeague } from '../api';
import type { SurvivorResponse } from '../types';
import '../styles.css';

export default function SurvivorPage() {
  const [data, setData] = useState<SurvivorResponse | null>(null);
  const [selectedWeek, setSelectedWeek] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    getSurvivorLeague(controller.signal)
      .then((response) => {
        setData(response);
        setSelectedWeek(response.weeks.at(-1)?.number ?? null);
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return;
        setError(requestError instanceof Error ? requestError.message : 'Unable to load Survivor League.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, []);

  const week = data?.weeks.find(({ number }) => number === selectedWeek);

  return (
    <main className="survivor-page">
      <header className="survivor-heading">
        <h1>Survivor League</h1>
        <p>Selections are hidden until Sunday</p>
      </header>

      {isLoading && <p className="survivor-status" role="status">Loading picks…</p>}
      {error && <p className="survivor-status survivor-error" role="alert">{error}</p>}
      {data && data.weeks.length === 0 && (
        <section className="survivor-status" aria-labelledby="no-weeks-heading">
          <h2 id="no-weeks-heading">No picks found</h2>
          <p>The spreadsheet does not contain a populated week yet.</p>
        </section>
      )}

      {data && week && (
        <section className="survivor-picks" aria-label="Survivor selections">
          <select
            id="survivor-week"
            className="survivor-week-picker"
            aria-label="Select week"
            value={selectedWeek ?? ''}
            onChange={(event) => setSelectedWeek(Number(event.target.value))}
          >
            {data.weeks.map((availableWeek) => (
              <option key={availableWeek.number} value={availableWeek.number}>
                {availableWeek.label}
              </option>
            ))}
          </select>

          <div className="survivor-table-frame">
            <table>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Pick</th>
                </tr>
              </thead>
              <tbody>
                {week.contestants.length === 0 ? (
                  <tr>
                    <td colSpan={2} className="survivor-empty-row">No active contestants found.</td>
                  </tr>
                ) : week.contestants.map((contestant, index) => (
                  <tr key={`${contestant.name}-${index}`}>
                    <th scope="row">{contestant.name}</th>
                    <td className={contestant.pick || contestant.selectionExists ? undefined : 'survivor-empty-pick'}>
                      {week.current && contestant.selectionExists ? (
                        <span className="survivor-selection-exists" aria-label="Selection submitted">
                          <svg aria-hidden="true" viewBox="0 0 20 20">
                            <circle cx="10" cy="10" r="9" />
                            <path d="m5.75 10.25 2.75 2.75 5.75-6" />
                          </svg>
                        </span>
                      ) : contestant.pick || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </main>
  );
}
