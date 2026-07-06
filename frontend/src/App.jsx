import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Upload from './pages/Upload';
import History from './pages/History';
import ReportDetails from './pages/ReportDetails';
import CompareReports from './pages/CompareReports';
import SystemHealth from './pages/SystemHealth';
import Settings from './pages/Settings';
import AdminDashboard from './pages/AdminDashboard';
import UserProfile from './pages/UserProfile';

// Layout wrapper for pages containing navigation bars
function DashboardLayout({ children }) {
  return (
    <div className="min-h-screen flex flex-col bg-dark-900 text-slate-100">
      <Navbar />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar />
        <main className="flex-1 flex overflow-hidden">
          {children}
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected Dashboard Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <Dashboard />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/upload"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <Upload />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/history"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <History />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/report/:projectId"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <ReportDetails />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/compare/:projectId"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <CompareReports />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/system"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <SystemHealth />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <Settings />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <UserProfile />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <DashboardLayout>
                <AdminDashboard />
              </DashboardLayout>
            </ProtectedRoute>
          }
        />

        {/* Catch-all fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}
