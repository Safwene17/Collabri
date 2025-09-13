"use client";

import { MenuIcon } from "lucide-react";

import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { Button } from "@/components/ui/button";
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
  navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { ModeToggle } from "@/components/mode-toggle";
import { Link } from "react-router-dom";

export function Navbar() {
  const features = [
    { title: "Dashboard", description: "Overview of your activity", href: "#" },
    { title: "Analytics", description: "Track your performance", href: "#" },
    { title: "Settings", description: "Configure your preferences", href: "#" },
    { title: "Integrations", description: "Connect with other tools", href: "#" },
    { title: "Storage", description: "Manage your files", href: "#" },
    { title: "Support", description: "Get help when needed", href: "#" },
  ];

  return (
    <section className="py-4">
  <div className="container max-w-7xl mx-auto px-6">
        {/* Add z-50 so navbar and its dropdowns sit above page content */}
        <nav className="relative z-50 flex items-center justify-between">
          {/* left: logo */}
          <Link to="/" className="flex items-center gap-2">
        <span className="text-2xl font-semibold tracking-tighter">Collabri</span>
          </Link>

          {/* center: nav menu (absolute centered on lg+) */}
      <NavigationMenu className="hidden lg:block absolute left-1/2 -translate-x-1/2">
            <NavigationMenuList className="mx-auto">
              <NavigationMenuItem>
                <NavigationMenuTrigger>Features</NavigationMenuTrigger>

                {/* ensure dropdown content is above everything */}
                <NavigationMenuContent className="z-50">
                  <div className="grid w-[600px] grid-cols-2 p-3">
                    {features.map((feature, index) => (
                      <NavigationMenuLink
                        href={feature.href}
                        key={index}
                        className="rounded-md p-3 transition-colors hover:bg-muted/70"
                      >
                        <div>
                          <p className="mb-1 font-semibold text-foreground">{feature.title}</p>
                          <p className="text-sm text-muted-foreground">{feature.description}</p>
                        </div>
                      </NavigationMenuLink>
                    ))}
                  </div>
                </NavigationMenuContent>
              </NavigationMenuItem>

              <NavigationMenuItem>
                <NavigationMenuLink href="#" className={navigationMenuTriggerStyle()}>
                  Products
                </NavigationMenuLink>
              </NavigationMenuItem>
              <NavigationMenuItem>
                <NavigationMenuLink href="#" className={navigationMenuTriggerStyle()}>
                  Resources
                </NavigationMenuLink>
              </NavigationMenuItem>
              <NavigationMenuItem>
                <NavigationMenuLink href="#" className={navigationMenuTriggerStyle()}>
                  Contact
                </NavigationMenuLink>
              </NavigationMenuItem>
            </NavigationMenuList>
          </NavigationMenu>

          {/* right: controls */}
      <div className="hidden items-center gap-4 lg:flex">
            <ModeToggle />
            <Button variant="outline">Sign in</Button>
            <Button>Start now</Button>
          </div>

          {/* mobile: sheet menu */}
          <Sheet>
            <SheetTrigger asChild className="lg:hidden">
              <Button variant="outline" size="icon">
                <MenuIcon className="h-4 w-4" />
              </Button>
            </SheetTrigger>
            <SheetContent side="top" className="max-h-screen overflow-auto">
              <SheetHeader>
                <SheetTitle>
                  <Link to="/" className="flex items-center gap-2">

                    <span className="text-2xl font-semibold tracking-tighter">Collabri</span>
                  </Link>
                </SheetTitle>
              </SheetHeader>

              <div className="flex flex-col p-4">
                <Accordion type="single" collapsible className="mt-4 mb-2">
                  <AccordionItem value="solutions" className="border-none">
                    <AccordionTrigger className="text-base hover:no-underline">Features</AccordionTrigger>
                    <AccordionContent>
                      <div className="grid md:grid-cols-2">
                        {features.map((feature, index) => (
                          <a
                            href={feature.href}
                            key={index}
                            className="rounded-md p-3 transition-colors hover:bg-muted/70"
                          >
                            <div>
                              <p className="mb-1 font-semibold text-foreground">{feature.title}</p>
                              <p className="text-sm text-muted-foreground">{feature.description}</p>
                            </div>
                          </a>
                        ))}
                      </div>
                    </AccordionContent>
                  </AccordionItem>
                </Accordion>

                <div className="flex flex-col gap-6">
                  <a href="#" className="font-medium">Templates</a>
                  <a href="#" className="font-medium">Blog</a>
                  <a href="#" className="font-medium">Pricing</a>
                </div>

                <div className="mt-6 flex flex-col gap-4">
                  <ModeToggle />
                  <Button variant="outline">Sign in</Button>
                  <Button>Start now</Button>
                </div>
              </div>
            </SheetContent>
          </Sheet>
        </nav>
      </div>
    </section>
  );
}
