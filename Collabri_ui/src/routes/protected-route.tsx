// src/routes/protected-route.tsx
import { Navigate } from "react-router-dom";
import { useAuth } from "@/context/AuthProvider";

export default function ProtectedRoute({ children }: { children: JSX.Element }) {
    const { user, initializing } = useAuth();

    if (initializing) return 
    <div>Loading...</div>;
    if (!user)
         return <Navigate to="/login" replace />;

    return children;
}
