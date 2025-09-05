import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Home } from "./pages/Home";
import { Login } from "./pages/Login";
import Dashboard from "./pages/Dashboard"; // Add this import
import './App.css'

function App() {
    return (
        <div>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/dashboard" element={<Dashboard />} /> {/* Add this route */}
                </Routes>
            </BrowserRouter>
        </div>
    );
}

export default App