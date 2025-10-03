import { useAuth } from '@/contexts/AuthContext';
import placeholder from '@/assets/placeholder.webp'

export const Profile = () => {
    const { user, logout } = useAuth();
    return (
        <section className="pt-30">
            <div className='flex flex-row justify-center gap-2'>
                <img
                    href={placeholder}
                    alt="profile-picture"
                />
            </div>
        </section>
    )
}