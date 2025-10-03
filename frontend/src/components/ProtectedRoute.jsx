import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

export const ProtectedRoute = () => {
    const { user } = useAuth();
    const location = useLocation();

    // Render the protected component
    return user ? <Outlet /> : <Navigate to="/" state={{ from: location }} replace />;
};