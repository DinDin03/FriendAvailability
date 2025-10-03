import { AuthProvider } from "@/contexts/AuthContext";
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Home } from "./pages/Home";
import { Dashboard } from './pages/Dashboard';
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { ALTDashboard } from './pages/ALTDashboard';
import { Toaster } from 'react-hot-toast';
import './App.css'
import { NotFound } from "@/pages/NotFound";

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
              <Route path="/dashboard" element={<ALTDashboard/>}/>
              
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>

    </div>
  );
}

export default App
