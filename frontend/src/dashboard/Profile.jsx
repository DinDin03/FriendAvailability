import { useAuth } from '@/contexts/AuthContext';
import { useState, useEffect } from 'react';
import { SquarePen, ChevronDown, Plus, MessageSquare, Calendar, Loader, User } from 'lucide-react';
import { activityService } from '@/services/activityService';
import { friendService } from '@/services/friendService';
import { chatService } from '@/services/chatService';
import { getActivityIcon, formatActivityMessage } from '@/lib/activityFormatters';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import ProfilePlaceholder from '@/assets/3D-avatars/1.png'

export const Profile = () => {
    const { user } = useAuth();
    const navigate = useNavigate();

    // Activity state
    const [activities, setActivities] = useState([]);
    const [isLoadingActivities, setIsLoadingActivities] = useState(true);
    const [activityError, setActivityError] = useState(null);

    // Friends state
    const [friends, setFriends] = useState([]);
    const [isLoadingFriends, setIsLoadingFriends] = useState(true);
    const [friendsError, setFriendsError] = useState(null);
    const [friendsStats, setFriendsStats] = useState({ totalFriends: 0, totalCircles: 0 });

    // Load activities on mount
    useEffect(() => {
        const loadActivities = async () => {
            if (!user?.id) return;

            try {
                setIsLoadingActivities(true);
                setActivityError(null);

                const response = await activityService.getActivityFeed(user.id, {
                    limit: 5, // Only show 5 most recent
                    sort: 'recent',
                    dateRange: 'week' // Last week's activities
                });

                setActivities(response.activities || []);
            } catch (error) {
                console.error('Failed to load activities:', error);
                setActivityError(error.message);
            } finally {
                setIsLoadingActivities(false);
            }
        };

        loadActivities();

        // Refresh every minute
        const interval = setInterval(loadActivities, 60000);
        return () => clearInterval(interval);
    }, [user?.id]);

    // Load friends on mount
    useEffect(() => {
        const loadFriends = async () => {
            if (!user?.id) return;

            try {
                setIsLoadingFriends(true);
                setFriendsError(null);

                // Get friends list (limit to 4 for display)
                const response = await friendService.getFriends(user.id, {
                    page: 1,
                    limit: 4,
                    sort: 'recent'
                });

                // Filter only accepted friends
                const acceptedFriends = (response.friends || []).filter(f => f.status === 'ACCEPTED');
                setFriends(acceptedFriends);

                // Get stats
                const stats = await friendService.getFriendshipStatistics(user.id);
                setFriendsStats({
                    totalFriends: stats.totalFriends || 0,
                    totalCircles: stats.totalCircles || 0
                });

            } catch (error) {
                console.error('Failed to load friends:', error);
                setFriendsError(error.message);
            } finally {
                setIsLoadingFriends(false);
            }
        };

        loadFriends();
    }, [user?.id]);

    // Handle message friend
    const handleMessageFriend = async (friendId) => {
        try {
            // Create or get existing private chat
            const chatRoom = await chatService.createPrivateChat(user.id, friendId);

            // Navigate to chat page and open this room
            navigate(`/chat?roomId=${chatRoom.id}`);
        } catch (error) {
            console.error('Failed to open chat:', error);
            toast.error('Failed to open chat');
        }
    };

    return (
        <section className="flex flex-col pt-30">
            <div className='flex flex-row justify-start px-6 gap-10 h-[20%]'>
                <img src={ProfilePlaceholder} alt="profile-picture" className="lg:size-40 size-28"/>
                <div className="flex flex-col items-start justify-start gap-4">
                    <span className="text-2xl font-bold text-primary">
                        {user?.name || 'UserNoName'}
                    </span>
                    <span className='flex flex-row gap-3 items-center cursor-pointer hover:text-blue-600 transition-colors'>
                        <SquarePen size={20}/>edit profile
                    </span>
                    <span className='pt-4 flex flex-col text-sm text-gray-600'>
                        {isLoadingFriends ? (
                            <Loader className='w-4 h-4 animate-spin'/>
                        ) : (
                            <>
                                <span className='font-semibold text-gray-900'>
                                    {friendsStats.totalFriends} {friendsStats.totalFriends === 1 ? 'friend' : 'friends'}
                                </span>
                                <span>
                                    {friendsStats.totalCircles} {friendsStats.totalCircles === 1 ? 'circle' : 'circles'}
                                </span>
                            </>
                        )}
                    </span>
                </div>
            </div>
            <hr className='my-8 border-1 border-gray-300/30'/>
            <div className='flex flex-col px-10'>
                <span className='flex justify-between items-center pb-5'>
                    <h1 className='text-xl font-bold'>Updates</h1>
                    <ChevronDown size={30}/>
                </span>
                <div className='flex flex-col'>
                    {/* Loading State */}
                    {isLoadingActivities && (
                        <div className='flex justify-center items-center py-8'>
                            <Loader className='w-6 h-6 animate-spin text-gray-400'/>
                        </div>
                    )}

                    {/* Error State */}
                    {activityError && !isLoadingActivities && (
                        <div className='text-sm text-red-600 py-2'>
                            Failed to load updates
                        </div>
                    )}

                    {/* Empty State */}
                    {!isLoadingActivities && !activityError && activities.length === 0 && (
                        <div className='text-sm text-gray-500 py-2'>
                            No recent updates
                        </div>
                    )}

                    {/* Activities List */}
                    {!isLoadingActivities && activities.length > 0 && activities.map((activity, key) => {
                        const ActivityIcon = getActivityIcon(activity.type);
                        return (
                            <span
                                key={activity.id || key}
                                className='flex justify-start items-center py-2 hover:bg-gray-50 rounded-lg px-2 transition-colors'
                            >
                                <div className='w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0'>
                                    <ActivityIcon size={16} className='text-blue-600'/>
                                </div>
                                <p className='ml-4 text-sm text-gray-700'>{formatActivityMessage(activity)}</p>
                            </span>
                        );
                    })}
                </div>
            </div>
            <hr className='my-7 border-1 border-gray-300/30'/>
            <div className='flex flex-col px-10'>
                <span className='flex justify-between items-center pb-5'>
                    <h1 className='text-xl font-bold'>Friends</h1>
                    <button
                        className='hover:scale-110 transition-transform cursor-pointer'
                        title='Add friend'
                    >
                        <Plus size={30}/>
                    </button>
                </span>

                {/* Loading State */}
                {isLoadingFriends && (
                    <div className='flex justify-center items-center py-8'>
                        <Loader className='w-6 h-6 animate-spin text-gray-400'/>
                    </div>
                )}

                {/* Error State */}
                {friendsError && !isLoadingFriends && (
                    <div className='text-sm text-red-600 py-2'>
                        Failed to load friends
                    </div>
                )}

                {/* Empty State */}
                {!isLoadingFriends && !friendsError && friends.length === 0 && (
                    <div className='text-sm text-gray-500 py-8 text-center'>
                        <User className='w-12 h-12 mx-auto mb-2 text-gray-300'/>
                        <p>No friends yet</p>
                        <p className='text-xs mt-1'>Add friends to see them here</p>
                    </div>
                )}

                {/* Friends List */}
                {!isLoadingFriends && friends.length > 0 && (
                    <div className='flex flex-col'>
                        {friends.map((friend, key) => (
                            <span
                                key={friend.id || key}
                                className='flex justify-between items-center py-7 hover:bg-gray-50 rounded-lg px-2 transition-colors'
                            >
                                <div className='flex items-center gap-8'>
                                    <div className='w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center'>
                                        <User size={20} className='text-blue-600'/>
                                    </div>
                                    <p className='font-bold'>{friend.friendName}</p>
                                </div>

                                {/* Action Icons */}
                                <div className='flex items-center gap-7'>
                                    <button
                                        onClick={() => handleMessageFriend(friend.friendId)}
                                        className='cursor-pointer duration-100 hover:scale-110 hover:text-purple-600 transition-colors'
                                        title='Send message'
                                    >
                                        <MessageSquare size={20}/>
                                    </button>
                                    <button
                                        className='cursor-pointer duration-100 hover:scale-110 hover:text-blue-600 transition-colors'
                                        title='View calendar'
                                    >
                                        <Calendar size={20}/>
                                    </button>
                                </div>
                            </span>
                        ))}
                    </div>
                )}

                {/* View All Link */}
                {!isLoadingFriends && friends.length > 0 && friendsStats.totalFriends > 4 && (
                    <div className='text-center mt-4'>
                        <button className='text-sm text-blue-600 hover:text-blue-700 font-medium'>
                            View all {friendsStats.totalFriends} friends
                        </button>
                    </div>
                )}
            </div>
        </section>
    )
}