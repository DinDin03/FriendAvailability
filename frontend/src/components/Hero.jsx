import placeholder from "@/assets/placeholder.webp"

export const Hero = () => {

    const items = [
        { id: 1, img: placeholder },
        { id: 2, img: placeholder },
        { id: 3, img: placeholder },
        { id: 4, img: placeholder },
        { id: 5, img: placeholder },
        { id: 6, img: placeholder },
        { id: 7, img: placeholder },
        { id: 8, img: placeholder },
        { id: 9, img: placeholder },
        { id: 10, img: placeholder },
        { id: 11, img: placeholder },
        { id: 12, img: placeholder },
    ];
    return (
        <section 
            id="hero" 
            className="relative min-h-screen flex flex-col items-center justify-start pt-40"
        >
            <div className="grid md:grid-cols-4 gap-8 w-200">
                {items.map((item, key) => (
                    <div 
                        key={key}
                        className="group bg-card rounded-3xl overflow-hidden shadow-xs card-hover"
                    >
                        <img src={item.img} alt="image" className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
                    </div>
                ))}
            </div>
            <div className="flex flex-col items-center justify-center mt-20">
                <h1 className="text-4xl font-bold">
                    Find your perfect time, <span className="text-primary">together</span>
                </h1>
                <p className="text-lg mt-10 text-center w-105">
                    No more endless texts, no more missed hangouts, just seamless plans.
                </p>
            </div>
        </section>
    )
}