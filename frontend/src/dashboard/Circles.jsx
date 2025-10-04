import {cn} from '@/lib/utils'
import { useState } from 'react'
import { SquarePen, Plus, Bell, MessageSquare, ChevronDown, ChevronUp } from "lucide-react"
import placeholder2 from '@/assets/3D-avatars/9.png'
import placeholder3 from "@/assets/3D-avatars/16.png"
import placeholder4 from "@/assets/3D-avatars/10.png"

const maxCircleName = 20;
const maxEventName = 14;

const circles = [
    {
        circleName: "The Best Group",
        circleEvents: `"BBQ timeeeeeeeeeeeeeeeee"`,
        remainingDays: 2,
        circleChat: "/",
        circleMembers: [
            {name: "Dineth", icon: placeholder2},
            {name: "Quan", icon: placeholder3},
            {name: "Samantha", icon: placeholder4},
            {name: "Samantha", icon: placeholder4},
            {name: "Samantha", icon: placeholder4},
            {name: "Samantha", icon: placeholder4},
        ]
    },
    {
        circleName: "Gang",
        circleEvents: null,
        remainingDays: null,
        circleChat: "/",
        circleMembers: [
            {name: "Dineth", icon: placeholder2},
            {name: "Quan", icon: placeholder3},
            {name: "Samantha", icon: placeholder4},
        ]
    },
    {
        circleName: "Gang",
        circleEvents: "House party",
        remainingDays: 1,
        circleChat: "/",
        circleMembers: [
            {name: "Dineth", icon: placeholder2},
            {name: "Quan", icon: placeholder3},
        ]
    },
    {
        circleName: "Gang",
        circleEvents: "Beach time",
        remainingDays: 0,
        circleChat: "/",
        circleMembers: [
            {name: "Dineth", icon: placeholder2},
            {name: "Quan", icon: placeholder3},
        ]
    },
];

export const Circles = () => {
    const [expanded, setExpanded] = useState({});
    return (
        <section className="pt-30 px-6 flex flex-col h-full flex-1 overflow-y-auto space-y-4 pr-2">
            <nav className="flex flex-row justify-between items-center">
                <h1 className="text-2xl">Circles</h1>
                <div className="flex gap-3">
                    <SquarePen/>
                    <Plus/>
                </div>
            </nav>
            { /* Placeholder for circles group chat thing */ }
            {circles.map((circle, key) => (
                <div 
                    key={key}
                    className='bg-white rounded-2xl shadow p-4'
                >
                    <a 
                        className="flex flex-row justify-between items-center w-full h-18 
                                    border-1 my-2 p-4 rounded-3xl card-hover"
                        href={circle.circleChat}
                    >
                        <h1 
                            className="text-xl font-bold"
                        >
                            {
                                circle.circleName.length > maxCircleName 
                                    ? circle.circleName.slice(0, maxCircleName) + "..." 
                                    : circle.circleName
                            }
                        </h1>
                        <div className="flex flex-row items-center">
                            <div 
                                className={cn("flex flex-row", circle.circleMembers.length > 3 ? "-space-x-4" : "gap-2")}
                            >
                                {circle.circleMembers.slice(0,3).map((member, i) => (
                                    <img 
                                        key={i}
                                        src={member.icon}
                                        className="h-10 w-10"
                                    />
                                ))}
                                
                            </div>
                            <span 
                                className="ml-2 font-extrabold"
                            >
                                {circle.circleMembers.length > 3 ? `+${circle.circleMembers.length-3}` : ""}
                            </span>
                        </div>
                    </a>
                    { /* Events */}
                    <div className="mt-4 px-4 bg-gray-700 rounded-full w-full h-12 flex 
                                    flex-row items-center justify-start gap-2">
                        <Bell className="text-white"/>
                        <h1 
                            className="text-white font-bold"
                        >
                            <span>Upcoming events: </span>
                            { /* No events */}
                            {!circle.circleEvents && <span>No events</span>}
                            { /* Event in 1 day */}
                            {
                                circle.circleEvents 
                                    && circle.remainingDays != 0 
                                    && <span>
                                        {
                                            circle.circleEvents.length > maxEventName 
                                                ? circle.circleEvents.slice(0, maxEventName) + "..." 
                                                : circle.circleEvents} in {circle.remainingDays} {circle.remainingDays < 2 ? 'day' : 'days'
                                        }
                                        </span>
                            }
                            { /* Event today */}
                            {
                                circle.circleEvents 
                                    && circle.remainingDays == 0 
                                    && <span>
                                        {
                                            circle.circleEvents.length > maxEventName
                                                ? circle.circleEvents.slice(0, maxEventName) + "..."
                                                : circle.circleEvents
                                        } today!
                                        </span>
                            }
                        </h1>
                    </div>
                    { /* Members */ }
                    <div className='flex flex-row ml-6 my-2 items-center justify-between'>
                        <button
                            onClick={() => 
                                setExpanded(prev => ({
                                    ...prev,
                                    [key]: !prev[key],
                                }))
                            }
                            className='z-50'
                        >
                            {expanded[key] ? <ChevronUp/> : <ChevronDown/>}
                        </button>
                        <div className='flex flex-row items-center justify-center gap-2'>
                            <a 
                                className='bg-gray-300 rounded-full p-3 duration-300 hover:scale-110'
                                href={circle.circleChat}
                            >
                                <MessageSquare/>
                            </a>
                            
                            <button className='flex items-center button h-12 px-6'>
                                <p>Find common time</p>
                            </button>
                        </div>
                    </div>
                    <div className={`overflow-hidden transition-all duration-300 ${
                        expanded[key] ? "max-h-40" : "max-h-0"
                    }`}>
                        <div className='max-h-40 overflow-y-auto'>
                            {circle.circleMembers.map((member, i) => (
                                <div className='flex flex-row items-center justify-between px-5 py-2'>
                                    <div className='flex flex-row items-center gap-4'>
                                        <img
                                            key={i}
                                            src={member.icon}
                                            alt={member.name}
                                            className='w-10 h-10 rounded-full'
                                        />
                                        <h1 className='font-bold'>
                                            {member.name}
                                        </h1>
                                    </div>
                                    <p>
                                        Yes
                                    </p>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            ))}
        </section>
    )
}