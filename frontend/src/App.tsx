import { Counter } from "./components/Counter";
import StatusBadge from "./components/StatusBadge";
import { get } from "./api/client";
import { AppStatus } from "./api/types";
import { useState } from "react";

export default function App() {
  const [appStatus, setAppStatus] = useState<AppStatus | null>(null);
  
  return (
    <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
      <button onClick={ healthcheck }>HealthCheck</button>
      {appStatus && <p>Postgres: {appStatus.postgres ? "up" : "down"}</p>}
    </div>
  );

  async function healthcheck() {
    const status = await get<AppStatus>("/healthcheck");
    setAppStatus(status);
  }
}



