import {cn} from '@/lib/utils'
import Logo from '@/assets/logo.png'
import { Menu, X, Search } from 'lucide-react';
import { useState, useEffect } from 'react';

{/* Functions */}

const navItems = [
    {name: "Log In", href: "/login"},
    {name: "Sign Up", href: "/signup"},
];

export const Navbar = () => {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isSignUpOpen, setIsSignUpOpen] = useState(false);

    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [errors, setErrors] = useState({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 10)
        }

        window.addEventListener("scroll", handleScroll)
        return () => window.removeEventListener("scroll", handleScroll)
    }, []);

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
                <div 
                    className="bg-white rounded-4xl p-8 max-w-md w-full mx-4 shadow-xl"
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="flex items-center mb-6 relative">
                         <h2 className="text-2xl font-bold text-gray-900 flex-1 text-center">Join LinkUp</h2>
                         <button
                             onClick={() => setIsSignUpOpen(false)}
                             className="text-gray-400 hover:text-gray-600 transition-colors absolute right-0"
                         >
                             <X size={24} />
                         </button>
                     </div>
                    
                    {/* sign up form content */}

                    {/*Full Name */ }
                    {/*Email*/}
                    {/*Password*/}
                    {/*Confirm password*/}
                    {/*Sign in with google*/}

                    <form 
                        className="space-y-4"

                    >
                        <div>
                            <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                                Full Name
                            </label>
                            <input
                                type="text"
                                id="signupName"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Enter your full name"
                                required
                            />
                        </div>

                        <div>
                            <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                                Email
                            </label>
                            <input
                                type="email"
                                id="signupEmail"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Enter your email"
                                required
                            />
                        </div>
                        
                        <div>
                            <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                                Password
                            </label>
                            <input
                                type="password"
                                id="signupPassword"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Create a strong password"
                                required
                            />
                        </div>

                        <div>
                            <label className="text-left block text-sm font-medium text-gray-700 mb-1">
                                Confirm Password
                            </label>
                            <input
                                type="password"
                                id="confirmPassword"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                placeholder="Confirm your password"
                                required
                            />
                        </div>
                        
                        <button 
                            type="submit"
                            className="w-full bg-blue-600 text-white mt-4 py-2 px-4 rounded-md hover:bg-blue-700 transition-colors"
                        >
                            Create Account
                        </button>
                    </form>
                </div>
            </div>

            <nav
                className={cn(
                    "bg-white fixed w-full z-50 transition-all duration-300", 
                    isScrolled ? "py-3 bg-background/80 backdrop-blur-md shadow-xs" : "py-4.5"
                )}
            >
                <div className="container flex items-center justify-between">
                    <a
                        className="flex items-center"
                        href="#hero"
                    >
                        <span className="relative flex items-center z-10">
                            <img src={Logo} alt="Logo" className="lg:size-18 size-10" />
                            <span className="lg:text-3xl text-xl font-bold text-primary transition-colors">Link Up</span>
                        </span>
                    </a>

                    {/* desktop nav */}
                    <div className="hidden md:flex space-x-18 items-center">
                            <a
                                href="login"
                                className="font-semibold tracking-wide text-foreground hover:text-neutral-700 transition-colors duration-300"
                            >
                                Log In
                            </a>
                            <button
                                onClick={() => setIsSignUpOpen((prev) => !prev)}
                                className=" text-gray-50 button"
                            >
                                Sign Up
                            </button>
                            <Search size={24}/>
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