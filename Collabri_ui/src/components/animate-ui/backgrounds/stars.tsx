// components/animate-ui/backgrounds/stars.tsx
"use client";

import * as React from "react";
import {
  type HTMLMotionProps,
  motion,
  type SpringOptions,
  type Transition,
  useMotionValue,
  useSpring,
} from "motion/react";
import { cn } from "@/lib/utils";
import { useTheme } from "@/components/theme-provider";

type StarLayerProps = HTMLMotionProps<"div"> & {
  count: number;
  size: number;
  transition: Transition;
  starColor: string;
};

function generateStars(count: number, starColor: string) {
  const shadows: string[] = [];
  for (let i = 0; i < count; i++) {
    const x = Math.floor(Math.random() * 4000) - 2000;
    const y = Math.floor(Math.random() * 4000) - 2000;
    // box-shadow format: offset-x offset-y blur color
    // we keep blur small (0) to create dot-like stars; if you want glow use "0 0 2px color"
    shadows.push(`${x}px ${y}px 0 ${starColor}`);
  }
  return shadows.join(", ");
}

function StarLayer({
  count = 1000,
  size = 1,
  transition = { repeat: Infinity, duration: 50, ease: "linear" },
  starColor = "#fff",
  className,
  ...props
}: StarLayerProps) {
  const [boxShadow, setBoxShadow] = React.useState<string>("");

  React.useEffect(() => {
    setBoxShadow(generateStars(count, starColor));
  }, [count, starColor]);

  return (
    <motion.div
      data-slot="star-layer"
      animate={{ y: [0, -2000] }}
      transition={transition}
      className={cn("absolute top-0 left-0 w-full h-[2000px]", className)}
      {...props}
    >
      <div
        className="absolute bg-transparent rounded-full"
        style={{
          width: `${size}px`,
          height: `${size}px`,
          boxShadow: boxShadow,
        }}
      />
      <div
        className="absolute bg-transparent rounded-full top-[2000px]"
        style={{
          width: `${size}px`,
          height: `${size}px`,
          boxShadow: boxShadow,
        }}
      />
    </motion.div>
  );
}

type StarsBackgroundProps = React.ComponentProps<"div"> & {
  factor?: number;
  speed?: number;
  transition?: SpringOptions;
  starColor?: string; // color used in dark mode (default: white)
  starColorLight?: string; // color used in light mode (default: soft black)
  hideInLight?: boolean; // if true, stars hidden in light mode (default false)
};

function StarsBackground({
  children,
  className,
  factor = 0.05,
  speed = 50,
  transition = { stiffness: 50, damping: 20 },
  starColor = "#ffffff",
  starColorLight = "rgba(0,0,0,0.65)",
  hideInLight = false,
  ...props
}: StarsBackgroundProps) {
  const offsetX = useMotionValue(1);
  const offsetY = useMotionValue(1);

  const springX = useSpring(offsetX, transition);
  const springY = useSpring(offsetY, transition);

  // use theme-provider to detect theme
  const { theme } = useTheme();

  // client-side dark detection (keeps SSR safe)
  const [isDark, setIsDark] = React.useState<boolean>(() => {
    if (typeof window !== "undefined") {
      return document.documentElement.classList.contains("dark");
    }
    return theme === "dark";
  });

  React.useEffect(() => {
    if (typeof theme !== "undefined") {
      setIsDark(theme === "dark");
    } else if (typeof window !== "undefined") {
      setIsDark(document.documentElement.classList.contains("dark"));
    }
  }, [theme]);

  const handleMouseMove = React.useCallback(
    (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
      const centerX = window.innerWidth / 2;
      const centerY = window.innerHeight / 2;
      const newOffsetX = -(e.clientX - centerX) * factor;
      const newOffsetY = -(e.clientY - centerY) * factor;
      offsetX.set(newOffsetX);
      offsetY.set(newOffsetY);
    },
    [offsetX, offsetY, factor]
  );

  // decide whether to render stars at all in light mode
  const renderStars = !(hideInLight && !isDark);

  // background: dark radial in dark mode, transparent in light mode
  const bgClass = isDark
    ? "bg-[radial-gradient(ellipse_at_bottom,_#262626_0%,_#000_100%)]"
    : "bg-transparent";

  // choose the effective star color (dark mode uses starColor, light uses starColorLight)
  const effectiveStarColor = isDark ? starColor : starColorLight;

  return (
    <div
      data-slot="stars-background"
      className={cn("relative size-full overflow-hidden", bgClass, className)}
      onMouseMove={handleMouseMove}
      {...props}
    >
      <motion.div style={{ x: springX, y: springY }}>
        {renderStars && (
          <>
            <StarLayer
              count={1000}
              size={1}
              transition={{ repeat: Infinity, duration: speed, ease: "linear" }}
              starColor={effectiveStarColor}
            />
            <StarLayer
              count={400}
              size={2}
              transition={{
                repeat: Infinity,
                duration: speed * 2,
                ease: "linear",
              }}
              starColor={effectiveStarColor}
            />
            <StarLayer
              count={200}
              size={3}
              transition={{
                repeat: Infinity,
                duration: speed * 3,
                ease: "linear",
              }}
              starColor={effectiveStarColor}
            />
          </>
        )}
      </motion.div>

      {children}
    </div>
  );
}

export {
  StarLayer,
  StarsBackground,
  type StarLayerProps,
  type StarsBackgroundProps,
};
