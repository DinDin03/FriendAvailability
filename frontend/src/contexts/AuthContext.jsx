import { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';

const AuthContext = createContext();

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export const AuthProvider = ({ children }) => {

    const [user, setUser] = useState(authService.getUser());
    const [isAuthenticated, setIsAuthenticated] = useState(authService.isUserAuthenticated());
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const initializeAuth = async () => {
        try {
            // Ask authService to restore state from localStorage
            authService.initializeAuth();

            if (authService.isUserAuthenticated()) {
            try {
                const currentUser = await authService.getCurrentUser();
                setUser(currentUser);
                setIsAuthenticated(true);
            } catch (error) {
                console.log('Token invalid, logging out');
                await logout();
            }
            }
        } finally {
            setIsLoading(false);
        }
        };

        initializeAuth();
    }, []);

    const login = async (email, password) => {
        const response = await authService.login(email, password);
        setUser(authService.getUser());
        setIsAuthenticated(authService.isUserAuthenticated());
        return response;
    };

    const register = (userData) => authService.register(userData);

    const logout = async () => {
        await authService.logout();
        setUser(null);
        setIsAuthenticated(false);
    };

    const updateUser = (updatedUser) => {
        setUser(updatedUser);
        localStorage.setItem('user', JSON.stringify(updatedUser)); // still needed for persistence
    };

    const value = {
        user,
        isAuthenticated,
        isLoading,
        login,
        register,
        logout,
        updateUser
    };

    return <AuthContext.Provider value={value}>
                {isLoading ? <div className="p-8">Loading auth...</div> : children}
            </AuthContext.Provider>;
    };
