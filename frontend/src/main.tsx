// This is the entry point — the first file that runs.
// Open LEARNING_GUIDE.md and start with Step 1!

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";

// Step 1: Your first React component
// A component is just a function that returns JSX (HTML-like syntax).
// Replace the contents of this function with your own JSX!

function App() {
  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <h1>Hello, Net Worth Calculator!</h1>
      <p>If you can see this, React is working. Open LEARNING_GUIDE.md for your first exercise.</p>
    </div>
  );
}

// This mounts your App component into the <div id="root"> in index.html
createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
