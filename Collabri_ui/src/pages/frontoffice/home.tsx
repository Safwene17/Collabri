
import { Navbar } from "@/components/frontoffice/layout/navbar"
import { Footer } from "@/components/frontoffice/layout/footer"
import { Hero } from "@/components/frontoffice/hero"
import heroImage from "@/assets/hero-image.jpg"
import { Stats } from "@/components/frontoffice/stats"
import { Services } from "@/components/frontoffice/services"
import { Faq } from "@/components/frontoffice/faq"
import {Feature} from "@/components/frontoffice/feature"
import { Cta } from "@/components/frontoffice/cta"
import { StarsBackground } from "@/components/animate-ui/backgrounds/stars"
export default function home() {

  return (
    <>

      <Navbar />
      <StarsBackground >
        <Hero
          heading="Welcome "
          subheading="to Collabri"
          description="Collabri helps teams collaborate seamlessly and manage tasks efficiently. Organize your projects, assign tasks, and achieve more together."
          image={{ src: heroImage, alt: "Collabri hero" }}
          buttons={{
            primary: { text: "Get Started", url: "/signup" },
            secondary: { text: "Learn More", url: "/about" },
          }}
        />
      </StarsBackground>
      <Stats />
      <StarsBackground>
      <Services />
      </StarsBackground>
      <Feature />
            <StarsBackground>

      <Faq />
            </StarsBackground>

      <Cta />
      <Footer />

    </>
  )
}
