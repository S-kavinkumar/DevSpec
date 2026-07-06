import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { 
  Files, BarChart3, Clock, AlertCircle, PlusCircle, ArrowRight, GitBranch, 
  ShieldCheck, CheckSquare, Sparkles, HelpCircle, Layers, Activity 
} from 'lucide-react';
import { 
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  PieChart, Pie, Cell, AreaChart, Area, Legend
} from 'recharts';
import API_BASE_URL from "../config/api";

const COLORS = ['#06b6d4', '#10b981', '#f43f5e', '#f59e0b', '#6366f1', '#ec4899'];

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDashboard = async () => {
      const token = localStorage.getItem('token');
      if (!token) return;

      try {
        const res = await axios.get('${API_BASE_URL}/api/reports/dashboard', {
          headers: { Authorization: `Bearer ${token}` }
        });
        setStats(res.data);
      } catch (err) {
        console.error(err);
        setError('Failed to fetch dashboard analysis metrics.');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-dark-900">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400" />
      </div>
    );
  }

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto">
      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center space-x-3 text-rose-500">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Hero Welcome Banner */}
      <div className="mb-8 p-6 rounded-2xl bg-gradient-to-r from-dark-800 to-dark-900 border border-dark-700/50 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white tracking-wide">Developer Analytics Studio</h1>
          <p className="text-sm text-dark-500 mt-1">Review source structures, executing tests and generating automated AI reports.</p>
        </div>
        <button
          onClick={() => navigate('/upload')}
          className="flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-dark-900 font-semibold text-sm shadow-lg shadow-cyan-500/10 transition-all hover:scale-105 active:scale-95"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Analyze Project</span>
        </button>
      </div>

      {/* Project Health Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-5 gap-4 mb-8">
        {[
          { key: 'Excellent', label: 'Excellent (90-100)', color: 'text-emerald-400 bg-emerald-500/5 border-emerald-500/10' },
          { key: 'Good', label: 'Good (80-89)', color: 'text-cyan-400 bg-cyan-500/5 border-cyan-500/10' },
          { key: 'Average', label: 'Average (70-79)', color: 'text-indigo-400 bg-indigo-500/5 border-indigo-500/10' },
          { key: 'Needs Improvement', label: 'Needs Improvement (50-69)', color: 'text-amber-400 bg-amber-500/5 border-amber-500/10' },
          { key: 'Critical', label: 'Critical (<50)', color: 'text-rose-500 bg-rose-500/5 border-rose-500/10' }
        ].map(item => (
          <div key={item.key} className={`border rounded-xl p-4 flex items-center justify-between ${item.color}`}>
            <div>
              <span className="block text-[10px] font-bold uppercase tracking-wider opacity-80">{item.label}</span>
              <span className="block text-2xl font-black mt-1">{stats?.healthDistribution?.[item.key] || 0}</span>
            </div>
            <ShieldCheck className="w-5 h-5 opacity-70" />
          </div>
        ))}
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-6 mb-8">
        <div className="glass-card p-5 rounded-xl">
          <span className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block mb-2">Total Projects</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-white">{stats?.totalProjects || 0}</span>
            <span className="text-xs text-dark-500">creations</span>
          </div>
        </div>

        <div className="glass-card p-5 rounded-xl">
          <span className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block mb-2">Reviewed Projects</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-cyan-400">{stats?.projectsReviewed || 0}</span>
            <span className="text-xs text-dark-500">audits</span>
          </div>
        </div>

        <div className="glass-card p-5 rounded-xl">
          <span className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block mb-2">Avg overall Score</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-white">{stats?.averageProjectScore || 0}%</span>
            <span className="text-xs text-dark-500">average</span>
          </div>
        </div>

        <div className="glass-card p-5 rounded-xl">
          <span className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block mb-2">Avg Security Score</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-emerald-400">{stats?.averageSecurityScore || 0}%</span>
            <span className="text-xs text-emerald-400">safe</span>
          </div>
        </div>

        <div className="glass-card p-5 rounded-xl">
          <span className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block mb-2">Avg Test Pass Rate</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-indigo-400">{stats?.averageTestPassRate || 0}%</span>
            <span className="text-xs text-indigo-400">passed</span>
          </div>
        </div>

        <div className="glass-card p-5 rounded-xl">
          <span className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block mb-2">Avg Run Duration</span>
          <div className="flex items-baseline space-x-2">
            <span className="text-3xl font-extrabold text-purple-400">{stats?.averageAnalysisDuration || 0}s</span>
            <span className="text-xs text-purple-400">seconds</span>
          </div>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        
        {/* Score Trend (Line/Area) */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider flex items-center space-x-2">
            <Activity className="w-4 h-4 text-cyan-400" />
            <span>Project Review Score Trend</span>
          </h2>
          <div className="h-64">
            {stats?.projectReviewTrend && stats.projectReviewTrend.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={stats.projectReviewTrend}>
                  <defs>
                    <linearGradient id="scoreColor" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#06b6d4" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#06b6d4" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid stroke="#1e293b" vertical={false} />
                  <XAxis dataKey="date" stroke="#64748b" fontSize={10} tickLine={false} />
                  <YAxis stroke="#64748b" fontSize={10} tickLine={false} domain={[0, 100]} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#0f172a', borderColor: '#1e293b', borderRadius: '8px' }}
                    labelClassName="text-slate-400 text-xs font-semibold"
                    itemStyle={{ color: '#22d3ee', fontSize: '12px' }}
                  />
                  <Area type="monotone" dataKey="score" stroke="#06b6d4" strokeWidth={2} fillOpacity={1} fill="url(#scoreColor)" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-dark-500 text-xs">No trend data available</div>
            )}
          </div>
        </div>

        {/* Tech Stack Distribution */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider flex items-center space-x-2">
            <Layers className="w-4 h-4 text-cyan-400" />
            <span>Technology Stack Distribution</span>
          </h2>
          <div className="h-64">
            {stats?.technologyDistribution && stats.technologyDistribution.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stats.technologyDistribution} layout="vertical">
                  <CartesianGrid stroke="#1e293b" horizontal={false} />
                  <XAxis type="number" stroke="#64748b" fontSize={10} tickLine={false} />
                  <YAxis dataKey="name" type="category" stroke="#64748b" fontSize={10} tickLine={false} width={80} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#0f172a', borderColor: '#1e293b', borderRadius: '8px' }}
                    itemStyle={{ color: '#22d3ee', fontSize: '12px' }}
                  />
                  <Bar dataKey="value" fill="#6366f1" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center text-dark-500 text-xs">No technology data available</div>
            )}
          </div>
        </div>

        {/* Score Distribution (Pie) */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">Score Distribution</h2>
          <div className="h-64 flex items-center justify-center">
            {stats?.scoreDistribution && stats.scoreDistribution.some(d => d.value > 0) ? (
              <div className="w-full h-full flex flex-col md:flex-row items-center justify-around">
                <div className="w-48 h-48">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={stats.scoreDistribution}
                        cx="50%"
                        cy="50%"
                        innerRadius={50}
                        outerRadius={75}
                        paddingAngle={5}
                        dataKey="value"
                      >
                        {stats.scoreDistribution.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="space-y-2 mt-4 md:mt-0">
                  {stats.scoreDistribution.map((entry, index) => (
                    <div key={index} className="flex items-center space-x-3 text-xs">
                      <div className="w-3 h-3 rounded" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                      <span className="text-slate-400 font-semibold">{entry.name}:</span>
                      <span className="text-white font-bold">{entry.value} reports</span>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="text-dark-500 text-xs">No score distribution data available</div>
            )}
          </div>
        </div>

        {/* Severity Distribution */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">Static Issues Severity</h2>
          <div className="h-64 flex items-center justify-center">
            {stats?.severityDistribution && stats.severityDistribution.some(d => d.value > 0) ? (
              <div className="w-full h-full flex flex-col md:flex-row items-center justify-around">
                <div className="w-48 h-48">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={stats.severityDistribution}
                        cx="50%"
                        cy="50%"
                        innerRadius={50}
                        outerRadius={75}
                        paddingAngle={5}
                        dataKey="value"
                      >
                        {stats.severityDistribution.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={COLORS[(index + 2) % COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="space-y-2 mt-4 md:mt-0">
                  {stats.severityDistribution.map((entry, index) => (
                    <div key={index} className="flex items-center space-x-3 text-xs">
                      <div className="w-3 h-3 rounded" style={{ backgroundColor: COLORS[(index + 2) % COLORS.length] }} />
                      <span className="text-slate-400 font-semibold">{entry.name}:</span>
                      <span className="text-white font-bold">{entry.value} occurrences</span>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="text-dark-500 text-xs">No issues recorded</div>
            )}
          </div>
        </div>

      </div>

      {/* Lists Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Recent Reports */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">Recent Audits</h2>
          {stats?.recentReports && stats.recentReports.length > 0 ? (
            <div className="space-y-4">
              {stats.recentReports.map((r) => (
                <div 
                  key={r.reportId}
                  onClick={() => navigate(`/report/${r.projectId}`)}
                  className="p-3 bg-dark-800/40 hover:bg-dark-800/80 border border-dark-700/50 rounded-lg flex items-center justify-between cursor-pointer transition-colors"
                >
                  <div>
                    <span className="block text-xs font-semibold text-white truncate max-w-[120px]">{r.projectName}</span>
                    <span className="block text-[9px] text-dark-500 mt-1">{new Date(r.date).toLocaleDateString()} ({r.version})</span>
                  </div>
                  <span className="text-xs font-extrabold text-cyan-400 bg-cyan-400/5 px-2 py-1 rounded border border-cyan-400/10">{r.overallScore}%</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-dark-500 text-xs text-center py-10">No recent audits</div>
          )}
        </div>

        {/* Most Common Issues */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">Top Common Issues</h2>
          {stats?.topCommonIssues && stats.topCommonIssues.length > 0 ? (
            <div className="space-y-4">
              {stats.topCommonIssues.map((issue, idx) => (
                <div key={idx} className="flex items-center justify-between p-3 bg-dark-800/20 border border-dark-700/30 rounded-lg">
                  <span className="text-xs text-slate-300 truncate max-w-[180px]">{issue.title}</span>
                  <span className="text-xs font-semibold text-rose-400 bg-rose-400/5 px-2 py-0.5 rounded border border-rose-500/10">{issue.count} files</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-dark-500 text-xs text-center py-10">Zero code defects detected</div>
          )}
        </div>

        {/* Recent AI Suggestions */}
        <div className="glass-panel p-6 rounded-2xl border border-dark-700">
          <h2 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">Recent AI Insights</h2>
          {stats?.recentSuggestions && stats.recentSuggestions.length > 0 ? (
            <div className="space-y-4">
              {stats.recentSuggestions.map((sug, idx) => (
                <div key={idx} className="p-3 bg-dark-800/20 border border-dark-700/30 rounded-lg text-xs space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold text-cyan-400">{sug.projectName}</span>
                    <span className="text-[9px] font-bold text-amber-500 uppercase">{sug.severity}</span>
                  </div>
                  <p className="font-bold text-white truncate">{sug.title}</p>
                  <p className="text-dark-500 text-[10px] line-clamp-2">{sug.description}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-dark-500 text-xs text-center py-10">No insights available</div>
          )}
        </div>

      </div>
    </div>
  );
}
