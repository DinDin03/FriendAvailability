import { SquarePen, Plus } from "lucide-react"

export const Circles = () => {
    return (
        <section className="pt-30 px-12">
            <div className="flex flex-row justify-between">
                <h1 className="text-2xl">Circles</h1>
                <div className="flex gap-3">
                    <SquarePen/>
                    <Plus/>
                </div>
            </div>
            { /* Placeholder for circles group chat thing */ }
            <a 
                className="flex flex-row justify-between w-full h-20 border-1 mt-5 p-5 rounded-3xl card-hover"
                href="/"
            >
                <h1 className="text-xl font-bold">The Best Group</h1>
            </a>
            
        </section>
    )
}