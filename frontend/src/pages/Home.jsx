import { Navbar } from "@/components/Navbar";
import { Hero } from "@/components/Hero";
import { Background } from "@/components/Background";
import { Works } from "@/components/Works";

export const Home = () => {
    return (
        <>
            <Background />
            <Navbar />
            <Hero />      
            <Works />
        </>
    )
}
