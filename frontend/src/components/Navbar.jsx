import {cn} from '@/lib/utils'
import Logo from '@/assets/logo.png'
import { Menu, X, Search } from 'lucide-react';
import { useState, useEffect } from 'react';

const navItems = [
    {name: "Log In", href: "/login"},
    {name: "Sign Up", href: "/signup"},
];

export const Navbar = () => {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 10)
        }

        window.addEventListener("scroll", handleScroll)
        return () => window.removeEventListener("scroll", handleScroll)
    }, []);

    return (
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
                        <img src={Logo} alt="Logo" className="size-18" />
                        <span className="text-3xl font-bold text-primary transition-colors">Link Up</span>
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
                        <a
                            href="signup"
                            className=" text-gray-50 button"
                        >
                            Sign Up
                        </a>
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
                                className="text-foreground hover:text-primary transition-colors duration-300"
                                onClick={() => setIsMenuOpen(false)}
                            >
                                {item.name}
                            </a>
                        ))}
                    </div>
                </div>

            </div>
        </nav>
    )
}