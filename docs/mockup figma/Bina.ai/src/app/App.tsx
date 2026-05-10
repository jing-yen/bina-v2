import { RouterProvider } from 'react-router';
import { router } from './routes';
import ExportPage from './ExportPage';

export default function App() {
  // Export Route - Check first before any other logic
  if (window.location.pathname === '/export') {
    return <ExportPage />;
  }

  return <RouterProvider router={router} />;
}
