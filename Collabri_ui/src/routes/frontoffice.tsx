import { Routes, Route } from "react-router-dom";
import Home from "@/app/frontoffice/home";
import Login from "@/app/frontoffice/auth/login";
export default function FrontofficeApp() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
    </Routes>
  );
}
