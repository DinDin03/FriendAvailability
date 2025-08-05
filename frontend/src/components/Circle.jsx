import friendsCircle from "@/assets/Circles/circles-1.png"
import friendsGroup from "@/assets/Circles/friends-expanded-1.png"
import friendsChat from "@/assets/Circles/friends-chat-1.png"

export const Circle = () => {
    return (
        <section id="circle" className="relative pt-12 flex flex-col items-center justify-center">
            <div className="bg-zinc-800 rounded-full size-200 p-30 flex flex-col items-center justify-start">
                <h1 className="text-3xl font-bold text-white">All your friends, one circle</h1>
                    <p className="text-white m-10 w-80">
                        Create or join circles where friends can make plans and chat together on the go.
                    </p>
                    <button className="button">Get started</button>
                    <div className="flex flex-row items-center justify-center gap-8">
                         <img src={friendsCircle} alt="circle" className="h-93 w-90 object-contain card-hover"/>
                         <img src={friendsGroup} alt="circle" className="h-100 w-90 object-contain card-hover"/>
                         <img src={friendsChat} alt="circle" className="h-100 w-93 object-contain card-hover"/>
                     </div>
            </div>
        </section>
    )
    
}