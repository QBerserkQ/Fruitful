import {BrowserRouter, Routes, Route, Navigate} from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import {AuthProvider} from "./context/AuthContext.tsx";
import {DashboardPage} from "./pages/DashboardPage.tsx";
import {ErrorPage} from "./pages/ErrorPage.tsx";
import ProtectedRoute from "./security/ProtectedRoute.tsx";
import AppLayout from "./components/AppLayout.tsx";
import SessionsSettings from "./pages/settings/SessionsSettings.tsx";
import AccountSettings from "./pages/settings/AccountSettings.tsx";

function App() {
  return (
      <BrowserRouter>
        <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="*" element={<ErrorPage />} />

              <Route element={<ProtectedRoute />}>
                  <Route element={<AppLayout />}>
                      <Route path="/" element={<DashboardPage />} />

                      <Route path="/settings" element={<Navigate to="/settings/account" replace />} />
                      <Route path="/settings/account" element={<AccountSettings />} />
                      <Route path="/settings/sessions" element={<SessionsSettings />} />
                  </Route>
              </Route>

            </Routes>
        </AuthProvider>
      </BrowserRouter>
  );
}

export default App;