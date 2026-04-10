// This is the entry point — the first file that runs.
// Open LEARNING_GUIDE.md and start with Step 1!

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App"

// Step 1: Your first React component
// A component is just a function that returns JSX (HTML-like syntax).
// Replace the contents of this function with your own JSX!

// This mounts your App component into the <div id="root"> in index.html
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
