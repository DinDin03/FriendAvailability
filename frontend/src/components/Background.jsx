import herobackground from "@/assets/herobackground.png"
import { useState, useEffect } from "react";

export const Background = () => {
    const [scroll, setScroll] = useState(0);

    useEffect(() => {
      const handleScroll = () => {
        setScroll(window.scrollY); // change second argument to affect how far the images move
      };
      window.addEventListener("scroll", handleScroll);
      return () => window.removeEventListener("scroll", handleScroll);
    }, []);
  
    const maxTranslate = 100;
    const leftTranslate = -((scroll / 100) * maxTranslate);
    const rightTranslate = (scroll / 100) * maxTranslate;

    const changeOpacity = 1 - Math.abs(leftTranslate + 100) / maxTranslate * 0.2;
  
    return (
      <>
        <div
          className="fixed left-0 top-0 h-full w-full z-0 pointer-events-none -ml-125"
          style={{
            transform: `translateX(${leftTranslate}px)`,
            opacity: scroll > 250 ? changeOpacity : 1,
            transition: "transform 0.4s",
          }}
        >
          <img
            src={herobackground}
            alt="left background"
            className="h-full object-cover object-left"
          />
        </div>
        <div
          className="fixed right-0 top-0 h-full z-0 pointer-events-none -mr-130"
          style={{
            transform: `translateX(${rightTranslate}px)`,
            opacity: scroll > 250 ? changeOpacity : 1,
            transition: "transform 0.4s",
          }}
        >
          <img
            src={herobackground}
            alt="right background"
            className="h-full object-cover"
          />
        </div>
      </>
    );
}