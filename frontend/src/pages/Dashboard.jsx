import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';
import { Calendar, Users, MessageCircle, Settings, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

export const Dashboard = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await logout();
            toast.success("Logged out successfully!")
            navigate("/");
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    return (
        <>
            <div className='h-screen flex'>
                {/* LEFT */}
                <div className='w-1/4 bg-red-500'>
                    l
                </div>
                <div className='w-2/4 bg-blue-400'>
                    right
                    <div className='h-1/4 bg-purple-400'>

                    </div>
                </div>
                <div className='w-1/4 bg-green-400'>
                    r
                </div>
            </div>
        </>
        
    );
};