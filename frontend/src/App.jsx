import { AuthProvider } from "@/contexts/AuthContext";
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Home } from "./pages/Home";
import { Dashboard } from './pages/Dashboard';
import { ALTDashboard } from './pages/ALTDashboard';
import { Chat } from './pages/Chat';
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
          </Routes>
        </BrowserRouter>
      </AuthProvider>

    </div>
  );
}

export default App
