import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';
import { Calendar, Users, MessageCircle, Settings, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

{/* Components */}
import { DashboardNav } from '@/dashboard/DashboardNav';
import { Circles } from '@/dashboard/Circles';
import { FriendsCalendar } from '@/dashboard/FriendsCalendar';
import { Availability } from '@/dashboard/Availability';
import { Profile } from '@/dashboard/Profile';

export const ALTDashboard = () => {
    const { user, logout, isLoading } = useAuth();
    const navigate = useNavigate();

    if (isLoading) {
        return <div className='p-8'>Loading...</div>;
    }

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
            <DashboardNav />
            <main className="h-screen flex bg-gray-50/75 divide-x-2 divide-solid divide-gray-300/30">
                <div className='w-[25%]'>
                    <Circles/>
                </div>
                <div className='w-[50%] flex flex-col h-full divide-y-2 divide-gray-300/30'>
                    <div className='flex-[8.5]'>
                        <FriendsCalendar/>
                    </div>

                    <div className='flex-[1.5]'>
                        <Availability/>
                    </div>
                </div>
                <div className='w-[25%]'>
                    <Profile/>
                </div>
            </main>
        </>
    );
};