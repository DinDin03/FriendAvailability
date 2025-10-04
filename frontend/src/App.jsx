import { AuthProvider } from "@/contexts/AuthContext";
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { Toaster } from 'react-hot-toast';
import './App.css'

{ /* Pages */ }
import { Home } from "./pages/Home";
import { NotFound } from "@/pages/NotFound";
import { ALTDashboard } from './pages/ALTDashboard';
import { Chat } from './pages/Chat';

function App() {
  return (
    <div>
      <AuthProvider>
        <Toaster position="top-center"/>
        <BrowserRouter>
          <Routes>
            { /* public routes */}
            <Route path="/" element={<Home />} />
            <Route path="/notfound" element={<NotFound/>}/>

            { /* protected routes */}
            <Route element={<ProtectedRoute />}>
              <Route path="/dashboard/*" element={<ALTDashboard/>}/>
              <Route path="/chat" element={<Chat/>}/>
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>

    </div>
  );
}

export default App
