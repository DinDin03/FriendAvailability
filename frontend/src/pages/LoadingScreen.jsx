import Logo from '@/assets/logo.png'
import { useState, useEffect } from 'react';

export const LoadingScreen = () => {
    const [dots, setDots] = useState("");

    useEffect(() => {
        const interval = setInterval(() => {
            setDots(prev => (prev.length === 3 ? "" : prev + "."));
        }, 500);

        return clearInterval(interval);
    }, []);
    
    return (
        <div className="flex flex-row justify-center items-center gap-3 min-h-screen font-bold">
            <img src={Logo} alt="Logo" className="lg:size-36 size-24 animate-bounce"/>
            <h1 className="text-5xl">Loading Linkups{dots}</h1>
        </div>
    )
}