import { AuthProvider } from "@/contexts/AuthContext";
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Home } from "./pages/Home";
import { Dashboard } from './pages/Dashboard';
import { ALTDashboard } from './pages/ALTDashboard';
import { Chat } from './pages/Chat';
import { CheckEmail } from './pages/CheckEmail';
import { EmailVerified } from './pages/EmailVerified';
import { EmailVerificationFailed } from './pages/EmailVerificationFailed';
import { Toaster } from 'react-hot-toast';
import './App.css'

function App() {
  return (
    <div>
      <AuthProvider>
        <Toaster position="bottom-right"/>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/dashboard" element={<Dashboard />}></Route>
            <Route path="/chat" element={<Chat />}></Route>
            <Route path="/check-email" element={<CheckEmail />}></Route>
            <Route path="/email-verified" element={<EmailVerified />}></Route>
            <Route path="/email-verification-failed" element={<EmailVerificationFailed />}></Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>

    </div>
  );
}

export default App
