import { BrowserRouter, Route, Routes } from "react-router";
import { get } from "./api/client";
import { AppStatus } from "./api/types";
import { useState } from "react";
import LoginPage from "./pages/LoginPage";
import { ProtectedRoute } from "./components/ProtectedRoute";
import DashboardPage from "./pages/DashboardPage";
import AssetsPage from "./pages/AssetsPage";
import TransactionsPage from "./pages/TransactionsPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/assets" element={<AssetsPage />} />
            <Route path="/transactions" element={<TransactionsPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}



