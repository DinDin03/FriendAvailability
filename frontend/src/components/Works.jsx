import calendar1 from "@/assets/Calendar-amico/Calendar-amico-1.png"
import calendar2 from "@/assets/Calendar-amico/Calendar-amico-2.png"
import calendar3 from "@/assets/Calendar-amico/Calendar-amico-3.png"
import Logo from '@/assets/logo.png'
import CalendarImage from '@/assets/friends-calendar.png'
import { Mouse, MousePointer } from 'lucide-react'

export const Works = () => {

    const cards = [
        {
            id: 1,
            img: calendar1,
            title: "1. Connect With Friends",
            description: "Create your profile and circles with friends using email or by sharing your unique LinkUp code.",
        },
        {
            id: 2,
            img: calendar2,
            title: "2. Share Your Availability",
            description: "Select when you're free with our interactive calendar and sync with your Google Calendar for automatic updates.",
        },
        {
            id: 3,
            img: calendar3,
            title: "3. Find Common Time",
            description: "Our smart algorithm finds common free time slots among your friend group, making sure meetings can happen with ease.",
        },
    ]
    return (
        <section id="works" className="relative pt-36 flex flex-col items-center justify-center">
            <div className="container">
                <h1 className="text-3xl font-bold">How it works</h1>
                <p className="text-lg mt-4 ">Get from chaos to coordinated in three simple steps.</p>
            </div>

            <div className="grid grid-cols-3 gap-12 pt-10 px-60">
                {cards.map((card, key) => (
                    <div
                        key={key}
                        className="group border border-gray-200 rounded-[60px] overflow-hidden card-hover shadow-xs p-8 w-80"    
                    >
                        <img 
                            src={card.img} 
                            alt="card image" 
                            className="w-full h-60 object-cover my-10" 
                        />
                        <h1 className="text-xl font-semibold mt-1">{card.title}</h1>
                        <p className="mt-6 mb-16">{card.description}</p>
                    </div>
                ))}
            </div>

            <div className="mt-36 flex flex-row rounded-[100px] bg-primary p-12 h-full w-[1250px]">
                <div className='flex-1'>
                    <div className='bg-white size-26 rounded-full flex items-center justify-center'>
                        <img src={Logo} className='size-20'/>
                    </div> 
                    <div className='flex flex-col items-start justify-start pl-25 pt-20'>
                        <h1 
                            className="text-3xl text-background font-bold flex flex-row items-center justify-center"
                        >
                            Click, view, plan <MousePointer className='ml-4 size-7'/>
                        </h1>
                        <p className='text-background mt-6 text-left'>
                            With our interactive calendar, you can view everyone's availability by simply clicking on the date.
                        </p>
                        <div className='flex flex-row items-center justify-start mt-10 gap-6'>
                            <button className='bg-white font-bold button text-primary'>
                                Get started
                            </button>
                            <button className='text-background'>
                                Learn more
                            </button>
                        </div>
                        
                    </div>
                </div>    
                <div className='p-10 grid grid-cols-1 grid-rows-1 card-hover'>
                    <img src={CalendarImage} className='z-10 w-full h-100 object-contain col-start-1 row-start-1'/>
                    <img src={CalendarImage} className='w-full h-100 object-contain col-start-1 row-start-1 rotate-10 translate-x-10'/>
                </div> 
            </div>
        </section>
    )
}