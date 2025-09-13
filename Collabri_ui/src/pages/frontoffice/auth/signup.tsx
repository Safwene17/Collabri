import { Signup } from "@/components/frontoffice/auth/signup-form";
import { Navbar } from "@/components/frontoffice/layout/navbar";
import { Footer } from "@/components/frontoffice/layout/footer";

export default function SignupPage() {
  return (
    <>
    <Navbar />
    <main className="min-h-screen flex items-center justify-center bg-muted py-8 px-4">
      <Signup  />
    </main>
    <Footer />
    </>
  );
}
