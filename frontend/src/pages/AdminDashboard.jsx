import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { 
  Users, Files, BarChart3, Shield, Activity, HelpCircle, 
  Trash2, Award, Clock, Terminal, AlertTriangle, Play, RefreshCw, 
  Download, HardDrive, Cpu, ShieldCheck
} from 'lucide-react';
import API_BASE_URL from "../config/api";

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [aiStats, setAiStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [activeJobs, setActiveJobs] = useState([]);
  const [failures, setFailures] = useState([]);
  const [logs, setLogs] = useState([]);
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [activeTab, setActiveTab] = useState('overview'); // overview, users, jobs, logs, health, aiCost

  const fetchData = async () => {
    setLoading(true);
    setError('');
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      const headers = { Authorization: `Bearer ${token}` };
      
      const statsRes = await axios.get('${API_BASE_URL}/api/admin/analytics', { headers });
      setStats(statsRes.data);

      const aiRes = await axios.get('${API_BASE_URL}/api/admin/ai-analytics', { headers });
      setAiStats(aiRes.data);

      const usersRes = await axios.get('${API_BASE_URL}/api/admin/users', { headers });
      setUsers(usersRes.data);

      const jobsRes = await axios.get('${API_BASE_URL}/api/admin/active-jobs', { headers });
      setActiveJobs(jobsRes.data);

      const failRes = await axios.get('${API_BASE_URL}/api/admin/failures', { headers });
      setFailures(failRes.data);

      const logRes = await axios.get('${API_BASE_URL}/api/admin/logs', { headers });
      setLogs(logRes.data);

      const healthRes = await axios.get('${API_BASE_URL}/api/admin/health', { headers });
      setHealth(healthRes.data);

    } catch (err) {
      console.error(err);
      setError('Access denied or server connection failed. Admins only.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleRoleChange = async (userId, currentRole) => {
    const token = localStorage.getItem('token');
    const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN';
    if (!window.confirm(`Are you sure you want to change user role to ${newRole}?`)) return;

    try {
      await axios.put(`${API_BASE_URL}/api/admin/users/${userId}/role`, { role: newRole }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      fetchData();
    } catch (err) {
      alert(err.response?.data || 'Failed to update user role');
    }
  };

  const handleDeleteUser = async (userId) => {
    const token = localStorage.getItem('token');
    if (!window.confirm('Are you sure you want to delete this user? This cannot be undone.')) return;

    try {
      await axios.delete(`${API_BASE_URL}/api/admin/users/${userId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      fetchData();
    } catch (err) {
      alert(err.response?.data || 'Failed to delete user');
    }
  };

  const handleDownloadBackup = async () => {
    const token = localStorage.getItem('token');
    try {
      const response = await axios.get('${API_BASE_URL}/api/admin/backup/download', {
        headers: { Authorization: `Bearer ${token}` },
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `devspec-backup-${new Date().toISOString().split('T')[0]}.zip`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to download system backup archive.');
    }
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-dark-900">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-1 p-8 bg-dark-900 flex flex-col items-center justify-center text-center">
        <AlertTriangle className="w-16 h-16 text-rose-500 mb-4 animate-bounce" />
        <h1 className="text-xl font-bold text-white mb-2">Administrative Privilege Required</h1>
        <p className="text-xs text-dark-500 max-w-sm">{error}</p>
      </div>
    );
  }

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto">
      {/* Top Header */}
      <div className="mb-8 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-wide flex items-center space-x-2">
            <Shield className="w-6 h-6 text-cyan-400" />
            <span>DEVSPEC Admin Console</span>
          </h1>
          <p className="text-xs text-dark-500 mt-1">Manage users, audit AI logs, track platform metrics, and download backups.</p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={fetchData}
            className="p-2.5 bg-dark-850 hover:bg-dark-800 border border-dark-700 rounded-lg text-slate-300 transition-colors flex items-center justify-center"
            title="Refresh System Data"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            onClick={handleDownloadBackup}
            className="flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-dark-900 font-bold text-xs shadow-lg transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Generate Backup</span>
          </button>
        </div>
      </div>

      {/* Metrics summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-7 gap-4 mb-8">
        {[
          { label: 'Registered Users', val: stats?.registeredUsers || 0, color: 'text-white' },
          { label: 'Active Users', val: stats?.activeUsers || 0, color: 'text-cyan-400' },
          { label: 'Projects Uploaded', val: stats?.projectsUploaded || 0, color: 'text-white' },
          { label: 'Reports Generated', val: stats?.reportsGenerated || 0, color: 'text-white' },
          { label: 'Total AI Requests', val: stats?.totalAiRequests || 0, color: 'text-indigo-400' },
          { label: 'Avg Analysis (s)', val: stats?.averageAnalysisTime || '0', color: 'text-emerald-400' },
          { label: 'Failed Runs', val: stats?.failedAnalyses || 0, color: 'text-rose-500' }
        ].map((item, idx) => (
          <div key={idx} className="glass-card p-4 rounded-xl border border-dark-700/50 flex flex-col justify-between">
            <span className="text-[9px] font-bold text-dark-500 uppercase tracking-widest">{item.label}</span>
            <span className={`text-2xl font-black mt-2 ${item.color}`}>{item.val}</span>
          </div>
        ))}
      </div>

      {/* Control Tabs */}
      <div className="flex border-b border-dark-700 mb-8 overflow-x-auto">
        {[
          { id: 'overview', label: 'Platform Overview', icon: BarChart3 },
          { id: 'users', label: 'User Manager', icon: Users },
          { id: 'jobs', label: `Active Pipelines (${activeJobs.length})`, icon: Play },
          { id: 'aiCost', label: 'AI Cost Audit', icon: ShieldCheck },
          { id: 'health', label: 'System Health', icon: Activity },
          { id: 'logs', label: 'Live Logs', icon: Terminal }
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center space-x-2 px-5 py-3 border-b-2 font-bold text-xs transition-colors whitespace-nowrap ${
              activeTab === tab.id
                ? 'border-cyan-500 text-cyan-400'
                : 'border-transparent text-slate-400 hover:text-white'
            }`}
          >
            <tab.icon className="w-3.5 h-3.5" />
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Tab Contents */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 animate-fade-in">
          {/* General Platform Stats */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-6">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">Analysis Quality Parameters</h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="p-4 bg-dark-800/40 rounded-xl border border-dark-700/50">
                <span className="text-[10px] text-dark-500 font-semibold block uppercase">Success Rate</span>
                <span className="text-xl font-bold text-white">{stats?.successRate || 100}%</span>
              </div>
              <div className="p-4 bg-dark-800/40 rounded-xl border border-dark-700/50">
                <span className="text-[10px] text-dark-500 font-semibold block uppercase">Average Review Score</span>
                <span className="text-xl font-bold text-cyan-400">{stats?.averageProjectScore || 0}%</span>
              </div>
            </div>
            <div className="p-4 bg-dark-800/40 rounded-xl border border-dark-700/50">
              <span className="text-[10px] text-dark-500 font-semibold block uppercase mb-2">Technology Distribution</span>
              <div className="space-y-2">
                {stats?.technologyDistribution && Object.entries(stats.technologyDistribution).map(([tech, count]) => (
                  <div key={tech} className="flex justify-between text-xs text-slate-300">
                    <span>{tech} uploads</span>
                    <span className="font-bold">{count}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Failed Analysis Log */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center space-x-2">
              <AlertTriangle className="w-4 h-4 text-rose-500" />
              <span>Failed Pipeline Runs ({failures.length})</span>
            </h2>
            <div className="space-y-3 max-h-[300px] overflow-y-auto pr-2">
              {failures.length > 0 ? failures.map(fail => (
                <div key={fail.id} className="p-3 bg-rose-500/5 border border-rose-500/10 rounded-xl text-xs space-y-1">
                  <div className="flex justify-between font-semibold text-rose-400">
                    <span>{fail.project?.name || 'Workspace'}</span>
                    <span>{fail.stage}</span>
                  </div>
                  <p className="text-[10px] text-dark-500">{new Date(fail.createdAt).toLocaleString()} | ID: {fail.analysisId}</p>
                </div>
              )) : (
                <p className="text-xs text-dark-500 text-center py-10">No execution failures detected.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'users' && (
        <div className="glass-panel rounded-2xl border border-dark-700 overflow-hidden animate-fade-in">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-dark-700 bg-dark-800/50 text-[10px] font-bold text-dark-500 uppercase tracking-widest">
                <th className="px-6 py-4">Username</th>
                <th className="px-6 py-4">Email</th>
                <th className="px-6 py-4">Role</th>
                <th className="px-6 py-4">Created Date</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-dark-700/50 text-slate-300 text-xs">
              {users.map(u => (
                <tr key={u.id} className="hover:bg-dark-800/20 transition-colors">
                  <td className="px-6 py-4 font-semibold text-white">{u.username}</td>
                  <td className="px-6 py-4">{u.email}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      u.role === 'ADMIN' ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20' : 'bg-dark-700 text-slate-400'
                    }`}>
                      {u.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">{new Date(u.createdAt).toLocaleDateString()}</td>
                  <td className="px-6 py-4 text-right space-x-2">
                    <button
                      onClick={() => handleRoleChange(u.id, u.role)}
                      className="px-2.5 py-1 rounded bg-dark-800 hover:bg-dark-750 text-cyan-400 font-semibold text-[10px] border border-dark-700 hover:border-cyan-500/30 transition-all"
                    >
                      Toggle Privilege
                    </button>
                    <button
                      onClick={() => handleDeleteUser(u.id)}
                      className="p-1.5 rounded bg-rose-500/10 hover:bg-rose-500/25 border border-rose-500/20 text-rose-400 transition-all inline-flex items-center justify-center align-middle"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'jobs' && (
        <div className="glass-panel p-6 rounded-2xl border border-dark-700 animate-fade-in space-y-4">
          <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">Active Jobs in Analysis Pool ({activeJobs.length})</h2>
          <div className="space-y-4">
            {activeJobs.length > 0 ? activeJobs.map(job => (
              <div key={job.id} className="p-4 bg-dark-800/40 border border-dark-700/50 rounded-2xl flex items-center justify-between">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-white">{job.project?.name || 'Cloned Workspace'}</span>
                    <span className="text-[10px] px-2 py-0.5 rounded bg-cyan-500/10 text-cyan-400 font-bold animate-pulse">{job.status}</span>
                  </div>
                  <p className="text-xs text-dark-500">Stage: {job.stage} | ID: {job.analysisId}</p>
                </div>
                <div className="text-xs text-dark-500 text-right">
                  <span>Started: {new Date(job.createdAt).toLocaleTimeString()}</span>
                </div>
              </div>
            )) : (
              <div className="text-center py-20 text-dark-500 text-xs">No active pipeline runs executing.</div>
            )}
          </div>
        </div>
      )}

      {activeTab === 'aiCost' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 animate-fade-in">
          {/* Stats Column */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-6">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">AI Integration Audit</h2>
            <div className="space-y-4">
              <div className="p-4 bg-dark-800/40 rounded-xl border border-dark-700/50">
                <span className="text-[10px] text-dark-500 font-semibold block uppercase">Total Accrued Cost</span>
                <span className="text-2xl font-black text-white">${aiStats?.totalCost || '0.00'}</span>
              </div>
              <div className="p-4 bg-dark-800/40 rounded-xl border border-dark-700/50">
                <span className="text-[10px] text-dark-500 font-semibold block uppercase">Total Tokens Used</span>
                <span className="text-xl font-bold text-indigo-400">{(aiStats?.totalTokensUsed || 0).toLocaleString()}</span>
              </div>
            </div>
            
            <div className="p-4 bg-dark-800/40 rounded-xl border border-dark-700/50 space-y-2">
              <span className="text-[10px] text-dark-500 font-semibold block uppercase mb-1">Calls by Provider</span>
              {aiStats?.providerCallsDistribution && Object.entries(aiStats.providerCallsDistribution).map(([prov, count]) => (
                <div key={prov} className="flex justify-between text-xs text-slate-300">
                  <span>{prov}</span>
                  <span className="font-bold">{count} calls</span>
                </div>
              ))}
            </div>
          </div>

          {/* AI Usage Logs */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 lg:col-span-2">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-4">Recent AI Queries</h2>
            <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2 text-xs">
              {aiStats?.logs && aiStats.logs.length > 0 ? aiStats.logs.map(log => (
                <div key={log.id} className="p-3 bg-dark-800/40 border border-dark-700/50 rounded-xl flex items-center justify-between">
                  <div className="space-y-1">
                    <span className="font-semibold text-slate-200">{log.operation} ({log.provider})</span>
                    <p className="text-[10px] text-dark-500">{new Date(log.createdAt).toLocaleString()} | User: {log.username || 'unknown'}</p>
                  </div>
                  <div className="text-right space-y-1 min-w-[100px]">
                    <span className="font-bold text-cyan-400 block">${log.costEstimate.toFixed(5)}</span>
                    <span className="text-[10px] text-slate-400 block">{log.tokensUsed} tokens | {(log.requestTimeMs / 1000.0).toFixed(1)}s</span>
                  </div>
                </div>
              )) : (
                <p className="text-center py-20 text-dark-500">No AI usage logged yet.</p>
              )}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'health' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-fade-in">
          {/* Disk space */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 flex flex-col justify-between h-48">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-white uppercase tracking-wider">Disk Storage</span>
              <HardDrive className="w-5 h-5 text-cyan-400" />
            </div>
            <div>
              <span className="text-3xl font-black text-white">{health?.diskFreeGb || 0} GB</span>
              <span className="block text-xs text-dark-500 mt-1">Available space (Used: {health?.diskUsedPercent || 0}%)</span>
            </div>
          </div>

          {/* Memory Usage */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 flex flex-col justify-between h-48">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-white uppercase tracking-wider">JVM Memory</span>
              <Cpu className="w-5 h-5 text-indigo-400" />
            </div>
            <div>
              <span className="text-3xl font-black text-indigo-400">{health?.memoryUsedMb || 0} MB</span>
              <span className="block text-xs text-dark-500 mt-1">Used of {health?.memoryTotalMb || 0}MB ({health?.memoryUsedPercent || 0}%)</span>
            </div>
          </div>

          {/* Core System */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 flex flex-col justify-between h-48">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-white uppercase tracking-wider">System Load</span>
              <Activity className="w-5 h-5 text-emerald-400" />
            </div>
            <div>
              <span className="text-3xl font-black text-emerald-400">{health?.cpuSystemLoad || '0.0'}</span>
              <span className="block text-xs text-dark-500 mt-1">System load average | Active threads: {health?.activeThreads || 0}</span>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'logs' && (
        <div className="glass-panel p-6 rounded-2xl border border-dark-700 animate-fade-in space-y-4">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">Application Server Logs (/logs/application.log)</h2>
            <span className="text-[10px] text-dark-500">Showing last 150 entries</span>
          </div>
          <div className="p-4 bg-black/40 border border-dark-700 rounded-xl font-mono text-[10px] text-emerald-400/90 h-[450px] overflow-y-auto space-y-1 scrollbar-thin">
            {logs.length > 0 ? logs.map((line, idx) => (
              <div key={idx} className="whitespace-pre-wrap">{line}</div>
            )) : (
              <div className="text-center py-20 text-dark-500">Log file currently empty or inaccessible.</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
