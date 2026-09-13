import { Link } from 'react-router-dom';

export default function HomePage() {
  return (
    <main>
      <h1>Hello World</h1>
      <p>This site is a constant work in progress...</p>
      <p>Are you looking for the <Link to="/survivor">Survivor</Link> page?</p>
    </main>
  );
}
