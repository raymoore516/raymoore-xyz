import { Fragment } from 'react';
import { Link } from 'react-router-dom';

type NavigationSegment = {
  label: string;
  to?: string;
  emphasized?: boolean;
};

type NavigationBannerProps = {
  segments: NavigationSegment[];
  separator?: string;
};

export default function NavigationBanner({ segments, separator = '»' }: NavigationBannerProps) {
  return (
    <nav className="navigation-banner" aria-label="Madison SC navigation">
      <h1>
        <Link to="/madisonsc">Madison SC</Link>
        {segments.map((segment, index) => (
          <Fragment key={`${segment.label}-${index}`}>
            <span className="navigation-banner-separator" aria-hidden="true">{separator}</span>
            {segment.to ? (
              <Link to={segment.to}>{segment.label}</Link>
            ) : (
              <span
                className={segment.emphasized ? 'navigation-banner-emphasis' : undefined}
                aria-current={index === segments.length - 1 ? 'page' : undefined}
              >
                {segment.label}
              </span>
            )}
          </Fragment>
        ))}
      </h1>
    </nav>
  );
}
