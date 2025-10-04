import { useAuth } from '@/contexts/AuthContext';
import { SquarePen, ChevronDown, Plus, MessageSquare, Calendar } from 'lucide-react';
import ProfilePlaceholder from '@/assets/3D-avatars/1.png'
import placeholder1 from '@/assets/gmail_groups.png'
import placeholder2 from '@/assets/3D-avatars/9.png'
import placeholder3 from "@/assets/3D-avatars/16.png"
import placeholder4 from "@/assets/3D-avatars/10.png"

const itemUpdates = [
    {
        img: placeholder1, 
        description: `You've been added to "Meetups"`
    },
    {
        img: ProfilePlaceholder, 
        description: `Dineth just updated their availability!`
    },
    {
        img: placeholder2, 
        description: `Samantha added you as a friend!`
    },
];

const friendsItems = [
    {
        img: ProfilePlaceholder, 
        name: "Dineth",
        messageLink: "/dashboard/messages1",
        calendarLink: "/dashboard/calendar1",  
    },
    {
        img: placeholder2, 
        name: "Samantha", 
        messageLink: "/dashboard/messages2",
        calendarLink: "/dashboard/calendar2",  
    },
    {
        img: placeholder3, 
        name: "Leo", 
        messageLink: "/dashboard/messages3",
        calendarLink: "/dashboard/calendar3",  
    },
    {
        img: placeholder4, 
        name: "Quan", 
        messageLink: "/dashboard/messages4",
        calendarLink: "/dashboard/calendar4",  
    },
];

export const Profile = () => {
    const { user } = useAuth();
    return (
        <section className="flex flex-col pt-30">
            <div className='flex flex-row justify-start px-6 gap-10 h-[20%]'>
                <img src={ProfilePlaceholder} alt="profile-picture" className="lg:size-40 size-28"/>
                <div className="flex flex-col items-start justify-start gap-4">
                    <span className="text-2xl font-bold text-primary">
                        {user?.name || 'UserNoName'}
                    </span>
                    <span className='flex flex-row gap-3 items-center'>
                        <SquarePen size={20}/>edit profile
                    </span>
                    <span className='pt-4 flex flex-col'>
                        25 friends
                        <span>
                            2 circles
                        </span>
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
                    {itemUpdates.map((item, key) => (
                        <span 
                            key={key}
                            className='flex justify-start items-center py-1.5'
                        >
                            <img src={item.img} className='w-8 h-8'/>
                            <p className='ml-8'>{item.description}</p>
                        </span> 
                    ))}
                </div>
            </div>
            <hr className='my-7 border-1 border-gray-300/30'/>
            <div className='flex flex-col px-10'>
                <span className='flex justify-between items-center pb-5'>
                    <h1 className='text-xl font-bold'>Friends</h1>
                    <Plus size={30}/>
                </span>
                <div className='flex flex-col'> 
                    {friendsItems.map((item, key) => (
                        <span 
                            key={key}
                            className='flex justify-between items-center py-7'
                        >
                            <div className='flex items-center gap-8'>
                                <img src={item.img} className='w-10 h-10' />
                                <p className='font-bold'>{item.name}</p>
                            </div>

                            {/* right group: icons */}
                            <div className='flex items-center gap-7'>
                                <a
                                    href={item.messageLink}
                                    className='cursor-default duration-100 hover:scale-110 hover:text-blue-700'
                                >
                                    <MessageSquare />
                                </a>
                                <a
                                    href={item.calendarLink}
                                    className='cursor-default duration-100 hover:scale-110 hover:text-blue-700'
                                >
                                <Calendar />
                                </a>
                            </div>
                        </span> 
                    ))}
                </div>
            </div>
        </section>
    )
}