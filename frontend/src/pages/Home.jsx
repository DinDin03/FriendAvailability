import { Navbar } from "@/components/Navbar";
import { Hero } from "@/components/Hero";
import { Background } from "@/components/Background";
import { Works } from "@/components/Works";
import { Calendar } from "@/components/Calendar";
import { Circle } from "@/components/Circle"
import { Footer } from "../components/Footer";
import { ApiTest } from "../components/ApiTest";

export const Home = () => {
    return (
        <>
            <Background />
            <Navbar />
            {/* API Test Component - Testing backend connection */}
            <div className="py-12 bg-gray-100">
                <div className="container mx-auto">
                    <h2 className="text-3xl font-bold text-center mb-8">Backend Connection Test</h2>
                    <ApiTest />
                </div>
            </div>
            <Hero />
            <Works />
            <Calendar />
            <Circle />
            <Footer />
        </>
    )
}
