import avatar1 from "@/assets/3D-avatars/1.png"
import avatar2 from "@/assets/3D-avatars/9.png"
import avatar3 from "@/assets/3D-avatars/10.png"
import avatar4 from "@/assets/3D-avatars/16.png"
import avatar5 from "@/assets/3D-avatars/26.png"

export const Hero = () => {

    const avatars = [
        { 
            id: 1, 
            img1: avatar1,
            img2: avatar2,
            img3: avatar3,
            img4: avatar4,
            img5: avatar5,
        },
    ];
    return (
        <section 
            id="hero" 
            className="relative min-h-screen flex flex-col items-center justify-start pt-40"
        >
            <div className="grid md:grid-cols-4 gap-8 w-200">
                {Array.from({ length: 12 }).map((_, i) => (
                    avatars.map((avatar, key) => (
                        <div 
                            key={`${i}-${key}`}
                            className="grayscale grid grid-cols-3 w-full h-full group bg-card rounded-4xl overflow-hidden shadow-xs card-hover transition-color p-1"
                        >
                            {[avatar.img1, avatar.img2, avatar.img3, avatar.img4, avatar.img5].map((image, key) => (
                                <img 
                                    key={key}
                                    src={image} 
                                    alt="image" 
                                    className="size-10 m-2 object-cover transition-transform duration-300 group-hover:scale-110" 
                                />
                            ))}
                        </div>
                    ))
                ))}
            </div>
            <div className="flex flex-col items-center justify-center mt-20">
                <h1 className="text-4xl font-bold">
                    Find your perfect time, <span className="text-primary">together.</span>
                </h1>
                <p className="text-lg mt-10 text-center w-105">
                    No more endless texts, no more missed hangouts, just seamless plans.
                </p>
            </div>
        </section>
    )
}