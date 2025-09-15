// src/components/frontoffice/auth/signup-form.tsx
import React from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Link, useNavigate } from "react-router-dom";
import { useProvideAuth } from "@/hooks/shared/useProvideAuth";

interface Signup2Props {
  heading?: string;
  buttonText?: string;
  signupText?: string;
}

const Signup = ({
  heading = "Create an account",
  buttonText = "Create Account",
  signupText = "Already a user?",
}: Signup2Props) => {
  const navigate = useNavigate();
  const { signup, loading } = useProvideAuth();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const firstname = String(fd.get("firstname") || "");
    const lastname = String(fd.get("lastname") || "");
    const email = String(fd.get("email") || "");
    const password = String(fd.get("password") || "");
    const confirm = String(fd.get("confirmPassword") || "");

    if (password !== confirm) {
      alert("Passwords do not match");
      return;
    }

    try {
      await signup({ firstname, lastname, email, password });
      alert("Account created. Please login.");
      navigate("/login");
    } catch (err) {
      console.error("Signup failed", err);
      alert("Signup failed");
    }
  };

  return (
    <section className="bg-muted h-screen">
      <div className="flex h-full items-center justify-center">
        <div className="flex flex-col items-center gap-6 lg:justify-start">
          <div className="min-w-sm border-muted bg-background flex w-full max-w-sm flex-col items-center gap-y-4 rounded-md border px-6 py-6 shadow-md">
            {heading && <h1 className="text-xl font-semibold">{heading}</h1>}
            <form className="flex flex-col w-full gap-4 mt-4" onSubmit={handleSubmit}>
              <div className="flex w-full flex-row gap-4">
                <div className="flex flex-col w-1/2 gap-2">
                  <Label>First Name</Label>
                  <Input name="firstname" type="text" placeholder="First Name" className="text-sm" required />
                </div>
                <div className="flex flex-col w-1/2 gap-2">
                  <Label>Last Name</Label>
                  <Input name="lastname" type="text" placeholder="Last Name" className="text-sm" required />
                </div>
              </div>
              <div className="flex w-full flex-col gap-2">
                <Label>Email</Label>
                <Input name="email" type="email" placeholder="Email" className="text-sm" required />
              </div>

              <div className="flex w-full flex-col gap-2">
                <Label>Password</Label>
                <Input name="password" type="password" placeholder="Password" className="text-sm" required />
              </div>
              <div className="flex w-full flex-col gap-2">
                <Label>Confirm Password</Label>
                <Input name="confirmPassword" type="password" placeholder="Confirm Password" className="text-sm" required />
              </div>
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? "Creating account..." : buttonText}
              </Button>
            </form>

            <div className="text-muted-foreground flex justify-center gap-1 text-sm mt-2">
              <p>{signupText}</p>
              <Link to="/login" className="text-primary font-medium hover:underline">Login</Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export { Signup };
