import HomePage from './app/pages/HomePage';
import LatestWeekPage from './projects/madisonsc/pages/LatestWeekPage';
import WeeklyPicksPage from './projects/madisonsc/pages/WeeklyPicksPage';
import YearlyPicksPage from './projects/madisonsc/pages/YearlyPicksPage';
import NavigationBar from './app/components/NavigationBar';
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
      <NavigationBar />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/madisonsc" element={<LatestWeekPage />} />
        <Route path="/madisonsc/picks/:year" element={<YearlyPicksPage />} />
        <Route path="/madisonsc/picks/:year/:week" element={<WeeklyPicksPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
