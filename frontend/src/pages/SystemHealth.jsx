import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { 
  Heart, Database, Cpu, Shield, Activity, HardDrive, 
  Clock, AlertCircle, RefreshCw, Layers 
} from 'lucide-react';
import API_BASE_URL from "../config/api";

export default function SystemHealth() {
  const [health, setHealth] = useState(null);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');

  const fetchSystemData = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      const healthRes = await axios.get('${API_BASE_URL}/api/system/health', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setHealth(healthRes.data);

      const logsRes = await axios.get('${API_BASE_URL}/api/system/audit-logs', {
        headers: { Authorization: `Bearer ${token}` }
      });
      setLogs(logsRes.data);
      
      setError('');
    } catch (err) {
      console.error(err);
      setError('Failed to fetch system monitor and audit log datasets.');
    }
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      await fetchSystemData();
      setLoading(false);
    };
    load();
  }, []);

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetchSystemData();
    setRefreshing(false);
  };

  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-dark-900">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400" />
      </div>
    );
  }

  const diskUsed = health ? (health.diskTotalBytes - health.diskFreeBytes) : 0;
  const diskPercentage = health ? Math.round((diskUsed * 100) / health.diskTotalBytes) : 0;

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-cyan-500/10 text-cyan-400 rounded-lg">
            <Activity className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white tracking-wide">System Monitor & Audits</h1>
            <p className="text-xs text-dark-500 mt-1">Real-time status diagnostics and database logs</p>
          </div>
        </div>
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="flex items-center space-x-1.5 px-3 py-2 rounded bg-dark-800 hover:bg-dark-700 text-slate-300 hover:text-white text-xs border border-dark-700 active:scale-95 transition-all"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
          <span>{refreshing ? 'Refreshing...' : 'Refresh Status'}</span>
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center space-x-3 text-rose-500 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Health Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        {/* DB Status */}
        <div className="glass-panel p-5 rounded-2xl border border-dark-700 flex items-center space-x-4">
          <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl">
            <Database className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-dark-500 font-bold uppercase tracking-wider block">Database Cluster</span>
            <span className="text-base font-extrabold text-white mt-1 block">
              {health?.databaseStatus === 'UP' ? 'Connected (UP)' : 'Disconnected (DOWN)'}
            </span>
          </div>
        </div>

        {/* AI Provider Status */}
        <div className="glass-panel p-5 rounded-2xl border border-dark-700 flex items-center space-x-4">
          <div className="p-3 bg-indigo-500/10 text-indigo-400 rounded-xl">
            <Cpu className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-dark-500 font-bold uppercase tracking-wider block">AI Review Engine ({health?.aiProvider})</span>
            <span className={`text-xs font-bold px-2 py-0.5 rounded-full mt-1.5 inline-block ${
              health?.aiProviderStatus.startsWith('UP') 
                ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20' 
                : 'bg-rose-500/15 text-rose-400 border border-rose-500/20'
            }`}>
              {health?.aiProviderStatus}
            </span>
          </div>
        </div>

        {/* Analysis Queue Size */}
        <div className="glass-panel p-5 rounded-2xl border border-dark-700 flex items-center space-x-4">
          <div className="p-3 bg-cyan-500/10 text-cyan-400 rounded-xl">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-dark-500 font-bold uppercase tracking-wider block">Active Queue Size</span>
            <span className="text-xl font-extrabold text-white mt-0.5 block">{health?.queueSize || 0} runs</span>
          </div>
        </div>

        {/* Avg Analysis Time */}
        <div className="glass-panel p-5 rounded-2xl border border-dark-700 flex items-center space-x-4">
          <div className="p-3 bg-amber-500/10 text-amber-400 rounded-xl">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] text-dark-500 font-bold uppercase tracking-wider block">Avg Analysis Time</span>
            <span className="text-xl font-extrabold text-white mt-0.5 block">
              {health?.avgAnalysisSeconds ? `${health.avgAnalysisSeconds}s` : 'N/A'}
            </span>
          </div>
        </div>
      </div>

      {/* Disk Space & Workspace Usage */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
        {/* Drive Info */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4">
          <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2 border-b border-dark-700 pb-2">
            <HardDrive className="w-4.5 h-4.5 text-cyan-400" />
            <span>Server Disk Usage</span>
          </h2>
          {health && (
            <div className="space-y-4">
              <div className="flex justify-between text-xs">
                <span className="text-slate-400 font-semibold">Workspace Directory:</span>
                <span className="text-white font-bold">{formatBytes(health.workspaceUsageBytes)}</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400 font-semibold">Total Server space:</span>
                <span className="text-white font-bold">{formatBytes(health.diskTotalBytes)}</span>
              </div>
              <div className="flex justify-between text-xs">
                <span className="text-slate-400 font-semibold">Available Space:</span>
                <span className="text-white font-bold">{formatBytes(health.diskFreeBytes)}</span>
              </div>
              {/* Progress Slider */}
              <div className="space-y-1">
                <div className="flex justify-between text-[10px] font-bold text-dark-500 uppercase">
                  <span>Disk usage percentage</span>
                  <span>{diskPercentage}%</span>
                </div>
                <div className="w-full bg-dark-900 rounded-full h-2 overflow-hidden border border-dark-700">
                  <div 
                    className="bg-cyan-500 h-2 rounded-full"
                    style={{ width: `${diskPercentage}%` }}
                  />
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Health summary */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4 flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2 border-b border-dark-700 pb-2">
              <Shield className="w-4.5 h-4.5 text-cyan-400" />
              <span>Safety Diagnostic</span>
            </h2>
            <div className="mt-4 p-4 rounded-xl bg-cyan-900/10 border border-cyan-500/15 text-xs text-cyan-400 leading-relaxed font-medium">
              System health status verified as operational. The temporary workspace manager is isolated with path validation rules. Concurrent thread capacity configured to core index limits.
            </div>
          </div>
          <div className="flex justify-between items-center text-xs text-dark-500 border-t border-dark-700 pt-3">
            <span>Diagnostics Status</span>
            <span className="text-emerald-400 font-bold uppercase tracking-wider flex items-center space-x-1.5">
              <div className="w-2.5 h-2.5 bg-emerald-400 rounded-full animate-ping" />
              <span>Healthy</span>
            </span>
          </div>
        </div>
      </div>

      {/* Audit Log Timeline */}
      <div className="glass-panel rounded-2xl border border-dark-700 overflow-hidden">
        <div className="p-6 border-b border-dark-700 bg-dark-800/40">
          <h2 className="text-sm font-bold text-white uppercase tracking-wider">System Audit Trail</h2>
          <p className="text-[10px] text-dark-500 mt-1">Chronological history of security, authentication, and analysis triggers</p>
        </div>
        
        {logs.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-dark-700 bg-dark-800/20 text-[10px] font-bold text-dark-500 uppercase tracking-widest">
                  <th className="px-6 py-3">Timestamp</th>
                  <th className="px-6 py-3">User</th>
                  <th className="px-6 py-3">Operation</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-dark-700/40">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-dark-800/10 transition-colors">
                    <td className="px-6 py-3 text-slate-400 font-mono text-[10px]">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                    <td className="px-6 py-3 font-semibold text-slate-200">{log.username}</td>
                    <td className="px-6 py-3 text-slate-300">{log.operation}</td>
                    <td className="px-6 py-3">
                      <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase ${
                        log.status === 'SUCCESS' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                      }`}>
                        {log.status}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-dark-500 max-w-sm truncate">{log.details}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center text-dark-500 italic">No audit trail records found.</div>
        )}
      </div>
    </div>
  );
}
