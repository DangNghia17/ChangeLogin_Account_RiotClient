import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { I18nProvider } from "./i18n/I18nContext";
import { ThemeProvider } from "./theme/ThemeContext";
import { ToastProvider } from "./components/Toast";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <ThemeProvider>
      <I18nProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </I18nProvider>
    </ThemeProvider>
  </React.StrictMode>,
);
