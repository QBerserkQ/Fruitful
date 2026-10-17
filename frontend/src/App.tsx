import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import {AuthProvider} from "./context/AuthContext.tsx";

function App() {
  return (
      <BrowserRouter>
        <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </AuthProvider>
      </BrowserRouter>

  );
}

export default App;