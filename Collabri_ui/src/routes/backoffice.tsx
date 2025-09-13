import { Routes, Route } from "react-router-dom";
import Dashboard from "@/pages/backoffice/dashboard/dashboard";

export default function BackofficeApp() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      {/* add other backoffice routes here */}
    </Routes>
  );
}
