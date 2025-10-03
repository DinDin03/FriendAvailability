import { Navbar } from "@/components/Navbar";
import { Hero } from "@/components/Hero";
import { Background } from "@/components/Background";
import { Works } from "@/components/Works";
import { Calendar } from "@/components/Calendar";
import { Circle } from "@/components/Circle"
import { Footer } from "@/components/Footer";

export const Home = () => {
    return (
        <>
            <Background />
            <Navbar />
            <Hero />
            <Works />
            <Calendar />
            <Circle />
            <Footer />
        </>
    )
}
