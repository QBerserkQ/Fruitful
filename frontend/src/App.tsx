import { BrowserRouter, Routes, Route} from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import {AuthProvider} from "./context/AuthContext.tsx";
import {DashboardPage} from "./pages/DashboardPage.tsx";
import {ErrorPage} from "./pages/ErrorPage.tsx";

function App() {
  return (
      <BrowserRouter>
        <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/" element={<DashboardPage />} />
              <Route path="*" element={<ErrorPage />} />
            </Routes>
        </AuthProvider>
      </BrowserRouter>

  );
}

export default App;