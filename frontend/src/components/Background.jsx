import placeholder from "@/assets/placeholder.webp"
import { useState, useEffect } from "react";

export const Background = () => {
    const [scroll, setScroll] = useState(0);

    useEffect(() => {
      const handleScroll = () => {
        setScroll(Math.min(window.scrollY, 300)); // change second argument to affect how far the images move
      };
      window.addEventListener("scroll", handleScroll);
      return () => window.removeEventListener("scroll", handleScroll);
    }, []);
  
    const maxTranslate = 200;
    const leftTranslate = -((scroll / 300) * maxTranslate);
    const rightTranslate = (scroll / 300) * maxTranslate;
  
    return (
      <>
        <div
          className="fixed left-0 top-0 h-full z-0 pointer-events-none"
          style={{
            transform: `translateX(${leftTranslate}px)`,
            transition: "transform 0.2s",
          }}
        >
          <img
            src={placeholder}
            alt="left background"
            className="h-full object-cover"
          />
        </div>
        <div
          className="fixed right-0 top-0 h-full z-0 pointer-events-none"
          style={{
            transform: `translateX(${rightTranslate}px)`,
            transition: "transform 0.2s",
          }}
        >
          <img
            src={placeholder}
            alt="right background"
            className="h-full object-cover"
          />
        </div>
      </>
    );
}