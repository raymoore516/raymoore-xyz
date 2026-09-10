import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { getLatestWeek } from '../../api';
import type { LatestWeek } from '../../types';
import '../../styles.css';

export default function LatestWeekPage() {
  const [latestWeek, setLatestWeek] = useState<LatestWeek | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    getLatestWeek(controller.signal)
      .then(setLatestWeek)
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === 'AbortError') return;
        setError(requestError instanceof Error ? requestError.message : 'Unable to find the latest picks.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });

    return () => controller.abort();
  }, []);

  if (latestWeek?.year != null && latestWeek.week != null) {
    return <Navigate to={`/madisonsc/picks/${latestWeek.year}/${latestWeek.week}`} replace />;
  }

  return (
    <main className="msc-page">
      <h1>Madison SC</h1>
      {isLoading && <p className="msc-status" role="status">Finding the latest picks…</p>}
      {error && <p className="msc-status msc-error" role="alert">{error}</p>}
      {!isLoading && !error && (
        <section className="msc-empty-state" aria-labelledby="no-picks-heading">
          <h2 id="no-picks-heading">No picks found</h2>
          <p>There are no Madison SC picks in the database yet.</p>
        </section>
      )}
    </main>
  );
}
