import { Link } from 'react-router-dom';

type PicksNavigationBannerProps = {
  year: number;
  week?: number;
};

function yearLabel(year: number) {
  const seasonStartYear = year + 2013;
  return `Year ${year} (${seasonStartYear}-${seasonStartYear + 1})`;
}

export default function PicksNavigationBanner({ year, week }: PicksNavigationBannerProps) {
  const label = yearLabel(year);

  return (
    <nav className="picks-navigation-banner" aria-label="Madison SC picks navigation">
      <h1>
        <strong>Madison SC</strong>
        <span className="picks-navigation-separator" aria-hidden="true">»</span>
        {week == null ? (
          <span aria-current="page">{label}</span>
        ) : (
          <>
            <Link to={`/madisonsc/picks/${year}`}>{label}</Link>
            <span className="picks-navigation-separator" aria-hidden="true">»</span>
            <span aria-current="page">Week {week}</span>
          </>
        )}
      </h1>
    </nav>
  );
}
