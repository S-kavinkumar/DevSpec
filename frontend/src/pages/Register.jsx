import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { Terminal, Lock, User, Mail, AlertCircle } from 'lucide-react';
import API_BASE_URL from "../config/api";

export default function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await axios.post('${API_BASE_URL}/api/auth/register', {
        username,
        email,
        password,
      });

      localStorage.setItem('token', res.data.token);
      localStorage.setItem('username', res.data.username);
      localStorage.setItem('role', res.data.role);
      navigate('/');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to sign up. Username or email might be taken.');
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
          <h2 className="text-2xl font-bold text-white tracking-wide">Get Started</h2>
          <p className="text-sm text-dark-500 mt-1">Create a new developer profile</p>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-lg bg-rose-500/10 border border-rose-500/20 flex items-center space-x-2 text-rose-500 text-sm">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleRegister} className="space-y-5 relative">
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
                placeholder="git_developer"
              />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-slate-300 tracking-wider uppercase block">Email Address</label>
            <div className="relative">
              <Mail className="absolute left-3 top-3.5 w-5 h-5 text-dark-500" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-3 pl-10 pr-4 text-sm text-white placeholder-dark-500 transition-colors"
                placeholder="developer@example.com"
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
                placeholder="Min. 6 characters"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 px-4 rounded-lg bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-dark-900 font-bold text-sm tracking-wide shadow-lg shadow-cyan-500/10 hover:shadow-cyan-400/20 active:scale-95 transition-all disabled:opacity-50 disabled:pointer-events-none"
          >
            {loading ? 'Creating Profile...' : 'Sign Up'}
          </button>
        </form>

        <div className="mt-8 text-center text-sm text-dark-500">
          Already registered?{' '}
          <Link to="/login" className="text-cyan-400 hover:underline font-semibold transition-colors">
            Sign In
          </Link>
        </div>
      </div>
    </div>
  );
}
