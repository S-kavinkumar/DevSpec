import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { 
  User as UserIcon, Mail, Shield, Key, BarChart3, 
  Settings, Award, Clock, DollarSign, Bell, Check, Inbox
} from 'lucide-react';

export default function UserProfile() {
  const [profile, setProfile] = useState(null);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Profile update form state
  const [email, setEmail] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [updateMessage, setUpdateMessage] = useState('');
  const [updateError, setUpdateError] = useState('');

  const fetchProfileAndNotifications = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      const headers = { Authorization: `Bearer ${token}` };
      
      const profileRes = await axios.get('http://localhost:8080/api/profile', { headers });
      setProfile(profileRes.data);
      setEmail(profileRes.data.email);

      const notifRes = await axios.get('http://localhost:8080/api/notifications', { headers });
      setNotifications(notifRes.data);

    } catch (err) {
      console.error(err);
      setError('Failed to load user profile statistics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileAndNotifications();
  }, []);

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setUpdateMessage('');
    setUpdateError('');

    if (newPassword && newPassword !== confirmPassword) {
      setUpdateError('New passwords do not match');
      return;
    }

    const token = localStorage.getItem('token');
    try {
      const payload = { email };
      if (newPassword) {
        payload.currentPassword = currentPassword;
        payload.newPassword = newPassword;
      }

      await axios.put('http://localhost:8080/api/profile', payload, {
        headers: { Authorization: `Bearer ${token}` }
      });

      setUpdateMessage('Profile details updated successfully!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      fetchProfileAndNotifications();
    } catch (err) {
      setUpdateError(err.response?.data || 'Failed to update credentials.');
    }
  };

  const handleMarkNotifRead = async (id) => {
    const token = localStorage.getItem('token');
    try {
      await axios.post(`http://localhost:8080/api/notifications/${id}/read`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      fetchProfileAndNotifications();
    } catch (err) {
      console.error(err);
    }
  };

  const handleMarkAllRead = async () => {
    const token = localStorage.getItem('token');
    try {
      await axios.post('http://localhost:8080/api/notifications/read-all', {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      fetchProfileAndNotifications();
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-dark-900">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400" />
      </div>
    );
  }

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto">
      {/* Top Header */}
      <div className="mb-8 flex items-center space-x-3">
        <div className="p-2.5 bg-cyan-500/10 text-cyan-400 rounded-lg">
          <UserIcon className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-white tracking-wide">Account Profile</h1>
          <p className="text-xs text-dark-500 mt-1">Review statistics, update settings, and manage security logs</p>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center space-x-3 text-rose-500 text-sm">
          <span>{error}</span>
        </div>
      )}

      {/* Profile Overview Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="glass-card p-5 rounded-xl border border-dark-700/50 flex flex-col justify-between">
          <div className="flex items-center justify-between text-dark-500">
            <span className="text-[10px] font-bold uppercase tracking-widest">Total Uploads</span>
            <BarChart3 className="w-4 h-4 text-cyan-400" />
          </div>
          <span className="text-3xl font-black text-white mt-4">{profile?.totalProjectsUploaded || 0}</span>
        </div>

        <div className="glass-card p-5 rounded-xl border border-dark-700/50 flex flex-col justify-between">
          <div className="flex items-center justify-between text-dark-500">
            <span className="text-[10px] font-bold uppercase tracking-widest">Reports Compiled</span>
            <Inbox className="w-4 h-4 text-indigo-400" />
          </div>
          <span className="text-3xl font-black text-white mt-4">{profile?.totalReportsGenerated || 0}</span>
        </div>

        <div className="glass-card p-5 rounded-xl border border-dark-700/50 flex flex-col justify-between">
          <div className="flex items-center justify-between text-dark-500">
            <span className="text-[10px] font-bold uppercase tracking-widest">Average Quality Score</span>
            <Award className="w-4 h-4 text-emerald-400" />
          </div>
          <span className="text-3xl font-black text-emerald-400 mt-4">{profile?.averageProjectScore || 0}%</span>
        </div>

        <div className="glass-card p-5 rounded-xl border border-dark-700/50 flex flex-col justify-between">
          <div className="flex items-center justify-between text-dark-500">
            <span className="text-[10px] font-bold uppercase tracking-widest">AI Audit Cost</span>
            <DollarSign className="w-4 h-4 text-purple-400" />
          </div>
          <span className="text-3xl font-black text-white mt-4">${profile?.totalAiCost || '0.00'}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Profile Settings (Left) */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-6 lg:col-span-2">
          <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
            <Settings className="w-4 h-4 text-cyan-400" />
            <span>Profile Settings</span>
          </h2>
          
          <form onSubmit={handleUpdateProfile} className="space-y-4">
            {updateMessage && (
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400 text-xs font-semibold">
                {updateMessage}
              </div>
            )}
            {updateError && (
              <div className="p-3 bg-rose-500/10 border border-rose-500/20 rounded-xl text-rose-400 text-xs font-semibold">
                {updateError}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-dark-500 uppercase">Username</label>
                <input
                  type="text"
                  disabled
                  value={profile?.username || ''}
                  className="w-full bg-dark-800/40 border border-dark-700/80 rounded-lg py-2 px-3 text-xs text-dark-500 cursor-not-allowed"
                />
              </div>
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-dark-500 uppercase">Email Address</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white"
                />
              </div>
            </div>

            <div className="border-t border-dark-800/80 pt-4 space-y-4">
              <span className="block text-[10px] font-bold text-dark-500 uppercase tracking-widest flex items-center space-x-1.5">
                <Key className="w-3.5 h-3.5" />
                <span>Change Password (Optional)</span>
              </span>
              
              <div className="space-y-3">
                <div className="space-y-1">
                  <label className="text-[10px] font-bold text-dark-500 uppercase">Current Password</label>
                  <input
                    type="password"
                    placeholder="Enter current password..."
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white"
                  />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-dark-500 uppercase">New Password</label>
                    <input
                      type="password"
                      placeholder="Enter new password..."
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-dark-500 uppercase">Confirm New Password</label>
                    <input
                      type="password"
                      placeholder="Confirm new password..."
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white"
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="submit"
                className="px-5 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-dark-900 font-bold text-xs shadow-lg shadow-cyan-500/10 active:scale-95 transition-all"
              >
                Save Profile
              </button>
            </div>
          </form>
        </div>

        {/* Notifications Center (Right) */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700 flex flex-col h-[400px]">
          <div className="flex items-center justify-between border-b border-dark-800 pb-3 mb-4">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
              <Bell className="w-4 h-4 text-cyan-400" />
              <span>Notification Logs</span>
            </h2>
            {notifications.some(n => !n.isRead) && (
              <button
                onClick={handleMarkAllRead}
                className="text-[10px] text-cyan-400 hover:text-cyan-300 font-semibold"
              >
                Mark all read
              </button>
            )}
          </div>
          
          <div className="flex-1 overflow-y-auto space-y-3 pr-2 scrollbar-thin">
            {notifications.length > 0 ? notifications.map(notif => (
              <div 
                key={notif.id} 
                className={`p-3 border rounded-xl flex items-start justify-between gap-3 ${
                  notif.isRead 
                    ? 'bg-dark-850/30 border-dark-800 text-slate-400' 
                    : 'bg-cyan-500/5 border-cyan-500/10 text-slate-200'
                }`}
              >
                <div className="space-y-1">
                  <p className="text-xs font-medium leading-relaxed">{notif.message}</p>
                  <span className="block text-[9px] text-dark-500">{new Date(notif.createdAt).toLocaleTimeString()}</span>
                </div>
                {!notif.isRead && (
                  <button
                    onClick={() => handleMarkNotifRead(notif.id)}
                    className="p-1 rounded bg-dark-800 text-slate-400 hover:text-cyan-400 hover:bg-dark-750 transition-colors"
                  >
                    <Check className="w-3 h-3" />
                  </button>
                )}
              </div>
            )) : (
              <div className="h-full flex flex-col items-center justify-center text-center text-dark-500 py-10">
                <Bell className="w-10 h-10 mb-2 opacity-30" />
                <span className="text-xs">No notifications captured.</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
