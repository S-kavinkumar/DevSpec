import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Terminal, LogOut, User } from 'lucide-react';

export default function Navbar() {
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Developer';

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    navigate('/login');
  };

  return (
    <nav className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-dark-700 bg-dark-900/80">
      <div className="flex items-center space-x-3 cursor-pointer" onClick={() => navigate('/')}>
        <div className="p-2 bg-gradient-to-tr from-cyan-500 to-indigo-600 rounded-lg shadow-lg shadow-cyan-500/20">
          <Terminal className="w-5 h-5 text-dark-900" strokeWidth={2.5} />
        </div>
        <span className="text-lg font-bold tracking-wider bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">
          DEVSPEC
        </span>
      </div>

      <div className="flex items-center space-x-6">
        <Link to="/profile" className="flex items-center space-x-2 px-3 py-1.5 rounded-full bg-dark-800 border border-dark-700 hover:border-cyan-500/30 text-sm text-slate-300 transition-all cursor-pointer">
          <User className="w-4 h-4 text-cyan-400" />
          <span className="font-medium">{username}</span>
        </Link>
        <button
          onClick={handleLogout}
          className="flex items-center space-x-2 text-sm text-slate-400 hover:text-rose-500 transition-colors"
        >
          <LogOut className="w-4 h-4" />
          <span>Sign Out</span>
        </button>
      </div>
    </nav>
  );
}
