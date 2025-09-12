import { BrowserRouter, Routes, Route } from 'react-router-dom'

import { ThemeProvider } from "@/components/theme-provider";
import FrontofficeApp from "./routes/frontoffice";
import BackofficeApp from "./routes/backoffice";

export default function App() {
  return (
    <>
      <ThemeProvider defaultTheme="dark" storageKey="vite-ui-theme">
        <BrowserRouter>
          <Routes>
            <Route path="/*" element={<FrontofficeApp />} />
            <Route path="/admin/*" element={<BackofficeApp />} />
          </Routes>
        </BrowserRouter>

      </ThemeProvider>

    </>
  )
}

