import placeholder from "@/assets/placeholder.webp"

export const Hero = () => {

    const items = [
        { id: 1, img: placeholder },
        { id: 2, img: placeholder },
        { id: 3, img: placeholder },
        { id: 4, img: placeholder },
    ];
    return (
        <section 
            id="hero" 
            className="relative min-h-screen flex flex-col items-center justify-center px-4"
        >
            <div className="grid md:grid-cols-4 gap-8">
                {items.map((item, key) => (
                    <div 
                        key={key}
                        className="group bg-card rounded-lg overflow-hidden shadow-xs card-hover"
                    >
                        <img src={item.img} alt="image" className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
                    </div>
                ))}
            </div>
        </section>
    )
}