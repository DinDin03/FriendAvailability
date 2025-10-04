import {cn} from '@/lib/utils'
import Logo from '@/assets/logo.png'
import { Menu, X, Search, LogOut, MessageCircle } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { chatService } from '@/services/chatService';

const navItems = [
    { name: "Messages", href: "/chat", icon: MessageCircle },
];

export const DashboardNav = () => {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0);

    const { user, logout } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (!user) navigate("/");
    }, [user, navigate]);

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 10)
        }

        window.addEventListener("scroll", handleScroll)
        return () => window.removeEventListener("scroll", handleScroll)
    }, []);

    // Load unread message count
    useEffect(() => {
        const loadUnreadCount = async () => {
            if (!user?.id) return;

            try {
                const response = await chatService.getUserChatRooms(user.id, { page: 0, size: 0 });
                const rooms = response.chatRooms || [];
                const total = rooms.reduce((sum, room) => sum + (room.unreadCount || 0), 0);
                setUnreadCount(total);
            } catch (error) {
                console.error('Failed to load unread count:', error);
            }
        };

        loadUnreadCount();

        // Refresh every 30 seconds
        const interval = setInterval(loadUnreadCount, 30000);
        return () => clearInterval(interval);
    }, [user?.id]);

    const handleLogout = async () => {
        try {
            await logout();
            toast.success("Logged out successfully!")
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    return (
        <>
            <nav
                className={cn(
                    "bg-white fixed w-full z-50 transition-all duration-300", 
                    isScrolled ? "py-1.5 bg-background/80 backdrop-blur-md shadow-xs" : "py-2 shadow-xs"
                )}
            >
                <div className="container flex items-center justify-between">
                    <a
                        className="flex items-center"
                        href="/dashboard"
                    >
                        <span className="relative flex items-center z-10">
                            <img src={Logo} alt="Logo" className="lg:size-18 size-10" />
                            <span className="lg:text-3xl text-xl font-bold text-primary transition-colors">Link Up</span>
                        </span>
                    </a>

                    {/* desktop nav */}
                    <div className="hidden md:flex space-x-6 items-center">
                            <Search size={24} className="cursor-pointer duration-300 hover:scale-110 hover:text-blue-700"/>
                            <button
                                onClick={() => navigate('/chat')}
                                className='cursor-pointer duration-300 hover:scale-110 hover:text-purple-700 relative'
                                title="Messages"
                            >
                                <MessageCircle size={24}/>
                                {unreadCount > 0 && (
                                    <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                                        {unreadCount > 9 ? '9+' : unreadCount}
                                    </span>
                                )}
                            </button>
                            <button
                                onClick={handleLogout}
                                className='cursor-pointer duration-300 hover:scale-110 hover:text-blue-700'
                                title="Logout"
                            >
                                <LogOut size={24}/>
                            </button>
                    </div>
                
                    {/* mobile nav */}
                    <button 
                        onClick={() => setIsMenuOpen((prev) => !prev)} 
                        className="md:hidden p-2 text-foreground z-50"
                        aria-label={isMenuOpen ? "Close Menu" : "Open Menu"}
                    >
                        {isMenuOpen ? <X size={24} className="text-primary"/> : <Menu size={24} className="text-primary"/>}
                    </button>

                    <div 
                        className={cn(
                            "fixed inset-0 bg-background/95 backdrop-blur-md z-40 flex flex-col items-center justify-center",
                            "transition-all duration-300 md:hidden",
                            isMenuOpen 
                                ? "opacity-100 pointer-events-auto" 
                                : "opacity-0 pointer-events-none"
                        )}
                    >
                        <div className="flex flex-col space-y-8 text-xl">
                            {navItems.map((item, key) => (
                                <a 
                                    key={key} 
                                    href={item.href} 
                                    className="text-foreground text-2xl hover:text-primary transition-colors duration-300"
                                    onClick={() => setIsMenuOpen(false)}
                                >
                                    {item.name}
                                </a>
                            ))}
                        </div>
                    </div>

                </div>
            </nav>
        </>
    )
}