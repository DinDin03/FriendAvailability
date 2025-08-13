import friend from '@/assets/gmail_groups.png'
import bell from '@/assets/Bell.png'
import eyecon from '@/assets/eyecon.png'
import dashboard from '@/assets/Dashboard.png'
import { ArrowRight } from 'lucide-react'

const cards = [
    {
        id: 1,
        img: friend,
        title: "Friends and Circles",
        description: "See, talk and make plans with your friends and circles",
        link: "/",
    },
    {
        id: 2,
        img: bell,
        title: "Instant Updates",
        description: "Get notifications for upcoming events so you never miss out",
        link: "/",
    },
    {
        id: 3,
        img: eyecon,
        title: "Availability",
        description: "See everyone's availability through a simple click",
        link: "/",
    },
]

export const Calendar = () => {
    return (
        <section id="calendar" className="relative pt-44 flex flex-col items-center justify-center">
            <h1 className='text-3xl font-bold'>Fun to use, easy to love</h1>
            <p className='mt-6'>Make planning fun again with our reactive dashboard</p>
            <button className='button mt-6'>Learn more</button>
            
            {/* <div className='mt-8 w-265 h-100 bg-gray-100 border-gray-400 rounded-4xl flex flex-col items-center justify-center'>
                Julian's a bitch ass idiot and didn't give me all the components
            </div> */}
            <img 
                    src={dashboard} 
                    alt="dashboard"
                    className='mt-8 w-265 border-2 border-gray-100 rounded-4xl card-hover'
                />
            

            <div className='grid grid-cols-3 gap-12 pt-10 px-60'>
                {cards.map((card, key) => (
                    <div
                        key={key}
                        className="flex flex-col items-center justify-center group border border-gray-200 rounded-[60px] overflow-hidden card-hover shadow-xs p-8 w-80"    
                     >
                    <img 
                        src={card.img} 
                        alt="card image" 
                        className="size-30 object-cover my-10" 
                    />
                    <h1 className="text-xl font-semibold mt-1">{card.title}</h1>
                    <p className="mt-6 mb-16">{card.description}</p>
                    <a 
                        href={card.link}
                        className='flex flex-row text-blue-500 items-center justify-start w-full'
                    >
                        Try it out <ArrowRight className='ml-2 text-blue-500'/>
                    </a>
                </div>
                ))}
            </div>
        </section>
    );
};