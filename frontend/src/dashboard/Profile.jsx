import { useAuth } from '@/contexts/AuthContext';
import { SquarePen } from 'lucide-react';
import placeholder from '@/assets/3D-avatars/1.png'

export const Profile = () => {
    const { user } = useAuth();
    return (
        <section className="pt-30">
            <div className='flex flex-row justify-center gap-10'>
                <img src={placeholder} alt="profile-picture" className="lg:size-40 size-28"/>
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
        </section>
    )
}