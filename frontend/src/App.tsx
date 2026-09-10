import HomePage from './app/pages/HomePage';
import HamburgerMenu from './app/components/HamburgerMenu';
import RootPage from './projects/madisonsc/pages/RootPage';
import LatestWeekPage from './projects/madisonsc/pages/picks/LatestWeekPage';
import WeeklyPicksPage from './projects/madisonsc/pages/picks/WeeklyPicksPage';
import YearlyPicksPage from './projects/madisonsc/pages/picks/YearlyPicksPage';
import { BrowserRouter, Route, Routes } from 'react-router-dom';

function NotFoundPage() {
  return (
    <main>
      <h1>Page not found</h1>
      <p>The page you requested does not exist.</p>
    </main>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <HamburgerMenu />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/madisonsc" element={<RootPage />} />
        <Route path="/madisonsc/picks/latest" element={<LatestWeekPage />} />
        <Route path="/madisonsc/picks/:year" element={<YearlyPicksPage />} />
        <Route path="/madisonsc/picks/:year/:week" element={<WeeklyPicksPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
