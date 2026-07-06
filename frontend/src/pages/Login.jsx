import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { Terminal, Lock, User, AlertCircle } from 'lucide-react';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await axios.post('http://localhost:8080/api/auth/login', {
        username,
        password,
      });

      localStorage.setItem('token', res.data.token);
      localStorage.setItem('username', res.data.username);
      localStorage.setItem('role', res.data.role);
      navigate('/');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to sign in. Verify your username and password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-900 px-4">
      <div className="max-w-md w-full glass-panel rounded-2xl p-8 shadow-2xl relative overflow-hidden">
        {/* Glow effect */}
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-cyan-500/10 rounded-full blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl" />

        <div className="flex flex-col items-center mb-8 relative">
          <div className="p-3 bg-gradient-to-tr from-cyan-500 to-indigo-600 rounded-xl shadow-lg shadow-cyan-500/20 mb-3">
            <Terminal className="w-6 h-6 text-dark-900" strokeWidth={2.5} />
          </div>
          <h2 className="text-2xl font-bold text-white tracking-wide">Welcome to DEVSPEC</h2>
          <p className="text-sm text-dark-500 mt-1">Sign in to review your projects</p>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-center space-x-2 text-rose-500 text-sm">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-5 relative">
          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-300 tracking-wider uppercase block">Username</label>
            <div className="relative">
              <User className="absolute left-3 top-3.5 w-5 h-5 text-dark-500" />
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-3 pl-10 pr-4 text-sm text-white placeholder-dark-500 transition-colors"
                placeholder="developer_admin"
              />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-300 tracking-wider uppercase block">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-3.5 w-5 h-5 text-dark-500" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-3 pl-10 pr-4 text-sm text-white placeholder-dark-500 transition-colors"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 rounded-lg bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-dark-900 font-bold text-sm tracking-wide shadow-lg shadow-cyan-500/10 hover:shadow-cyan-400/20 active:scale-95 transition-all disabled:opacity-50 disabled:pointer-events-none"
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div className="mt-8 text-center text-sm text-dark-500">
          New here?{' '}
          <Link to="/register" className="text-cyan-400 hover:underline font-semibold transition-colors">
            Create an account
          </Link>
        </div>
      </div>
    </div>
  );
}
