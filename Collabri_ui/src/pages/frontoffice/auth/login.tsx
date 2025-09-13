import logincover from "@/assets/login-cover.png";
import { LoginForm } from "@/components/frontoffice/auth/login-form";
import { Footer } from "@/components/frontoffice/layout/footer";
import { Navbar } from "@/components/frontoffice/layout/navbar";

export default function LoginPage() {
  return (
    <>
      <Navbar />
      <div className="grid min-h-svh lg:grid-cols-2">
        <div className="flex flex-col gap-4 p-6 md:p-10">
          <div className="flex flex-1 items-center justify-center">
            <div className="w-full max-w-xs">
              <LoginForm />
            </div>
          </div>
        </div>

        {/* ensure cover sits behind via z-0 */}
        <div className="bg-muted relative hidden lg:block z-0">
          <img
            src={logincover}
            alt="Image"
            className="absolute inset-0 h-full w-full object-cover dark:brightness-[0.8] z-0"
          />
        </div>
      </div>
      <Footer/>
    </>
  );
}
