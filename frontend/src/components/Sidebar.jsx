import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutGrid, Upload, History, Activity, Settings, User, Shield } from 'lucide-react';

export default function Sidebar() {
  const role = localStorage.getItem('role') || 'USER';
  
  const links = [
    { to: '/', label: 'Dashboard', icon: LayoutGrid },
    { to: '/upload', label: 'Upload Project', icon: Upload },
    { to: '/history', label: 'Review History', icon: History },
    { to: '/profile', label: 'My Profile', icon: User },
    { to: '/system', label: 'System Monitor', icon: Activity },
    { to: '/settings', label: 'Settings', icon: Settings },
  ];

  if (role === 'ADMIN') {
    links.push({ to: '/admin', label: 'Admin Panel', icon: Shield });
  }

  return (
    <aside className="w-64 border-r border-dark-700 bg-dark-900/40 p-6 flex flex-col justify-between">
      <div className="space-y-6">
        <span className="text-xs font-semibold tracking-widest text-dark-500 uppercase px-3">
          Navigation
        </span>
        <nav className="space-y-2">
          {links.map((link) => {
            const Icon = link.icon;
            return (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  `flex items-center space-x-3 px-3 py-3 rounded-lg text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-gradient-to-r from-cyan-900/30 to-indigo-900/10 text-cyan-400 border-l-2 border-cyan-500 font-semibold'
                      : 'text-slate-400 hover:text-white hover:bg-dark-800'
                  }`
                }
              >
                <Icon className="w-5 h-5" />
                <span>{link.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </div>
      
      <div className="p-4 rounded-xl bg-dark-800/60 border border-dark-700/50">
        <span className="block text-xs font-bold text-cyan-400 uppercase tracking-wide">Version 1.0</span>
        <span className="block text-[10px] text-dark-500 mt-1">Static Analysis & AI Review Engine</span>
      </div>
    </aside>
  );
}
