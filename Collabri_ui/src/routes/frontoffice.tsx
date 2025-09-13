import { Routes, Route } from "react-router-dom";
import Home from "@/pages/frontoffice/home";
import Login from "@/pages/frontoffice/auth/login";
import Signup from "@/pages/frontoffice/auth/signup";


export default function FrontofficeApp() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Signup />} />
    </Routes>
  );
}
