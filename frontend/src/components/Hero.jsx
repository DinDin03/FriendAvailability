import {cn} from '@/lib/utils'
import { useState, useEffect } from 'react';
import { SignupForm } from '@/components/SignupForm.jsx';
import { authService } from '@/services/authService';
import { useAuth } from "@/contexts/AuthContext";
import { GoogleLogin, useGoogleLogin } from "@react-oauth/google";
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';


import avatar1 from "@/assets/3D-avatars/1.png"
import avatar2 from "@/assets/3D-avatars/9.png"
import avatar3 from "@/assets/3D-avatars/10.png"
import avatar4 from "@/assets/3D-avatars/16.png"
import avatar5 from "@/assets/3D-avatars/26.png"
import herobackground from "@/assets/herobackground.png"

export const Hero = () => {
    const navigate = useNavigate();
    const { updateUser } = useAuth();

    const handleGoogleSuccess = async (credentialResponse) => {
        try {
            const userData = await authService.googleLogin(credentialResponse);
            if (userData) {
                updateUser(userData)
                const userFirstName = userData.name.split(' ')[0];
                toast.success(`Welcome back, ${userFirstName || "User"}!`);
                navigate("/dashboard");
            }
        } catch (error) {
            console.log('Google sign-in failed, ', error)
            toast.error(error.message || "Google sign-in failed.")
        }
    }
    
    const handleGoogleError = (error) => {
        console.error("Google login failed:", error);
        toast.error("Google sign-in failed. Please try again.")
    }

    const googleLogin = useGoogleLogin({
        onSuccess: async credentialResponse => {
            console.log(credentialResponse)
        },
        onError: () => toast.error("Google login failed."),
        flow: "auth-code"  
    });

    const [isSignUpOpen, setIsSignUpOpen] = useState(false);
    const avatars = [
        { 
            id: 1, 
            img1: avatar1,
            img2: avatar2,
            img3: avatar3,
            img4: avatar4,
            img5: avatar5,
        },
    ];

    useEffect(() => {
        if (isSignUpOpen) {
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = 'unset';
        }

        return () => {
            document.body.style.overflow = 'unset';
        };
    }, [isSignUpOpen]);

    return (
        <>
                {/* Sign up module */}
            <div 
                className={cn(
                    "fixed inset-0 bg-gray-500/50 backdrop-blur-md z-[60] flex flex-col items-center justify-center",
                    "transition-all duration-300",
                    isSignUpOpen 
                        ? "opacity-100 pointer-events-auto" 
                        : "opacity-0 pointer-events-none"
                )}
                onClick={() => setIsSignUpOpen(false)}
            >
                <SignupForm onClose={() => setIsSignUpOpen(false)}/>
            </div>
            <section 
                id="hero" 
                className="relative min-h-screen flex flex-col items-center lg:pt-40 pt-25 px-4 w-full"
            >
                <div className="grid grid-cols-4 lg:gap-8 gap-x-4 gap-y-2">
                    {Array.from({ length: 12 }).map((_, i) => (
                        avatars.map((avatar, key) => (
                            <div 
                                key={`${i}-${key}`}
                                className={cn(
                                    "p-1 w-22 h-full rounded-2xl gap-x-1",
                                    "lg:w-full lg:h-full lg:p-1 lg:rounded-4xl", 
                                    "grayscale grid grid-cols-3 group bg-card overflow-hidden shadow-xs card-hover transition-color")}
                            >
                                {[avatar.img1, avatar.img2, avatar.img3, avatar.img4, avatar.img5].map((image, key) => (
                                    <img 
                                        key={key}
                                        src={image} 
                                        alt="image" 
                                        className="size-6 sm:size-10 lg:m-2 m-1 object-cover transition-transform duration-300 group-hover:scale-110" 
                                    />
                                ))}
                            </div>
                        ))
                    ))}
                </div>
                <img src={herobackground} alt="herobackground" className="md:hidden -mt-25"/>
                <div className="md:flex flex-col items-center justify-center lg:mt-20 w-full max-w-4xl">
                    <h1 className="text-2xl lg:text-4xl font-bold text-center">
                        Find your perfect time, <span className="text-primary">together.</span>
                    </h1>
                    <p className="text-lg mt-10 text-center">
                        No more endless texts, no more missed hangouts, just seamless plans.
                    </p>
                </div>
                
                <div className='flex lg:flex-row flex-col items-center justify-center gap-4 mt-10'>
                    {/* <GoogleLogin
                        onSuccess={handleGoogleSuccess}
                        onError={handleGoogleError}
                        useOneTap
                        size="large"
                        shape="pill"
                        render={({ onClick, disabled }) => (
                            <button 
                                className='border-2 border-primary button'
                                onClick={onClick}
                                disabled={disabled}
                            >
                                Login with Google
                            </button>
                        )}
                    /> */}

                    <button 
                        className='border-2 border-primary button'
                        onClick={googleLogin}
                    >
                        Login with Google
                    </button>

                    <button 
                        className='border-2 border-primary bg-white text-primary button w-60'
                        onClick={() => setIsSignUpOpen((prev) => !prev)}
                    >
                        Sign Up
                    </button>
                </div>
            </section>
        </>
    )
}