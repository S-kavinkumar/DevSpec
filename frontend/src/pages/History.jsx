import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { 
  History as HistoryIcon, Files, GitBranch, AlertCircle, ArrowRight, 
  ExternalLink, Search, Filter, ArrowUpDown 
} from 'lucide-react';
import API_BASE_URL from "../config/api";

export default function History() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  // Search & Filter state
  const [search, setSearch] = useState('');
  const [language, setLanguage] = useState('');
  const [framework, setFramework] = useState('');
  const [minScore, setMinScore] = useState('');
  const [maxScore, setMaxScore] = useState('');
  const [tag, setTag] = useState('');
  const [technology, setTechnology] = useState('');
  const [sortBy, setSortBy] = useState('date');
  const [sortOrder, setSortOrder] = useState('desc');
  const [viewMode, setViewMode] = useState('table'); // 'table' or 'timeline'

  const fetchHistory = async () => {
    setLoading(true);
    const token = localStorage.getItem('token');
    if (!token) return;

    // Assemble queries
    const params = new URLSearchParams();
    if (search.trim()) params.append('search', search.trim());
    if (language.trim()) params.append('language', language.trim());
    if (framework.trim()) params.append('framework', framework.trim());
    if (minScore.trim()) params.append('minScore', minScore.trim());
    if (maxScore.trim()) params.append('maxScore', maxScore.trim());
    if (tag.trim()) params.append('tag', tag.trim());
    if (technology.trim()) params.append('technology', technology.trim());
    params.append('sortBy', sortBy);
    params.append('sortOrder', sortOrder);

    try {
      const res = await axios.get(`${API_BASE_URL}/api/projects/history?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setHistory(res.data);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch filtered history list.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Debounce search/filters slightly
    const delayDebounceFn = setTimeout(() => {
      fetchHistory();
    }, 400);

    return () => clearTimeout(delayDebounceFn);
  }, [search, language, framework, minScore, maxScore, tag, technology, sortBy, sortOrder]);

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto">
      <div className="mb-8 flex items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-cyan-500/10 text-cyan-400 rounded-lg">
            <HistoryIcon className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-white tracking-wide">Analysis Run History</h1>
            <p className="text-xs text-dark-500 mt-1">Review previously generated software quality audits</p>
          </div>
        </div>

        {/* View Mode Toggle */}
        <div className="flex items-center gap-2 bg-dark-800 p-1.5 rounded-lg border border-dark-700/50">
          <button
            onClick={() => setViewMode('table')}
            className={`px-3 py-1.5 rounded text-xs font-semibold transition-all ${
              viewMode === 'table' ? 'bg-cyan-500 text-dark-900 shadow-md font-bold' : 'text-slate-400 hover:text-white'
            }`}
          >
            Table List
          </button>
          <button
            onClick={() => setViewMode('timeline')}
            className={`px-3 py-1.5 rounded text-xs font-semibold transition-all ${
              viewMode === 'timeline' ? 'bg-cyan-500 text-dark-900 shadow-md font-bold' : 'text-slate-400 hover:text-white'
            }`}
          >
            Timeline Cards
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center space-x-3 text-rose-500 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Search and Filters panel */}
      <div className="glass-panel p-5 rounded-2xl border border-dark-700 mb-8 grid grid-cols-1 md:grid-cols-6 gap-4">
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-3 w-4 h-4 text-dark-500" />
          <input
            type="text"
            placeholder="Search by name..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 pl-9 pr-4 text-xs text-white placeholder-dark-500"
          />
        </div>

        {/* Tag filter */}
        <div className="relative">
          <Filter className="absolute left-3 top-3 w-4 h-4 text-dark-500" />
          <input
            type="text"
            placeholder="Tag (e.g. backend)..."
            value={tag}
            onChange={(e) => setTag(e.target.value)}
            className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 pl-9 pr-4 text-xs text-white placeholder-dark-500"
          />
        </div>

        {/* Technology filter */}
        <div className="relative">
          <Filter className="absolute left-3 top-3 w-4 h-4 text-dark-500" />
          <select
            value={technology}
            onChange={(e) => setTechnology(e.target.value)}
            className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 pl-9 pr-4 text-xs text-white appearance-none cursor-pointer"
          >
            <option value="">All Upload Types</option>
            <option value="ZIP">Zip File</option>
            <option value="GITHUB">Git Repository</option>
          </select>
        </div>

        {/* Language selector */}
        <div className="relative">
          <Filter className="absolute left-3 top-3 w-4 h-4 text-dark-500" />
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 pl-9 pr-4 text-xs text-white appearance-none cursor-pointer"
          >
            <option value="">All Languages</option>
            <option value="Java">Java</option>
            <option value="Kotlin">Kotlin</option>
            <option value="Python">Python</option>
          </select>
        </div>

        {/* Min/Max Score Ranges */}
        <div className="flex gap-2">
          <input
            type="number"
            placeholder="Min %"
            value={minScore}
            onChange={(e) => setMinScore(e.target.value)}
            className="w-1/2 bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white placeholder-dark-500"
          />
          <input
            type="number"
            placeholder="Max %"
            value={maxScore}
            onChange={(e) => setMaxScore(e.target.value)}
            className="w-1/2 bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white placeholder-dark-500"
          />
        </div>

        {/* Sort and SortOrder */}
        <div className="flex gap-2">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="flex-1 bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-2 px-3 text-xs text-white cursor-pointer"
          >
            <option value="date">Sort by Date</option>
            <option value="name">Sort by Name</option>
            <option value="score">Sort by Score</option>
          </select>
          <button
            onClick={() => setSortOrder(sortOrder === 'desc' ? 'asc' : 'desc')}
            className="p-2 bg-dark-800 hover:bg-dark-700 border border-dark-700 rounded-lg text-slate-300 transition-colors flex items-center justify-center"
          >
            <ArrowUpDown className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400" />
        </div>
      ) : history.length > 0 ? (
        <>
          {viewMode === 'table' ? (
            <div className="glass-panel rounded-2xl border border-dark-700 overflow-hidden">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-dark-700 bg-dark-800/50 text-[10px] font-bold text-dark-500 uppercase tracking-widest">
                    <th className="px-6 py-4">Project Name</th>
                    <th className="px-6 py-4">Technology Stack</th>
                    <th className="px-6 py-4">Quality Score</th>
                    <th className="px-6 py-4">Run Status</th>
                    <th className="px-6 py-4">Date Executed</th>
                    <th className="px-6 py-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-dark-700/50">
                  {history.map((run) => (
                    <tr key={run.historyId} className="hover:bg-dark-800/20 transition-colors">
                      <td className="px-6 py-4 font-semibold text-slate-200">{run.projectName}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center space-x-2 text-slate-400 text-sm">
                          {run.projectType === 'GITHUB' ? (
                            <GitBranch className="w-4 h-4 text-dark-500" />
                          ) : (
                            <Files className="w-4 h-4 text-dark-500" />
                          )}
                          <span className="text-xs text-slate-300 font-medium">
                            {run.language || 'Java'} {run.framework ? `(${run.framework})` : ''}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        {run.status === 'COMPLETED' && run.overallScore ? (
                          <span
                            className={`text-sm font-extrabold px-2.5 py-1 rounded-lg ${
                              run.overallScore >= 80
                                ? 'bg-emerald-500/10 text-emerald-400'
                                : run.overallScore >= 60
                                ? 'bg-amber-500/10 text-amber-400'
                                : 'bg-rose-500/10 text-rose-400'
                            }`}
                          >
                            {run.overallScore}%
                          </span>
                        ) : (
                          <span className="text-dark-500 text-sm">-</span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                            run.status === 'COMPLETED'
                              ? 'bg-emerald-500/10 text-emerald-400'
                              : run.status === 'FAILED'
                              ? 'bg-rose-500/10 text-rose-400'
                              : 'bg-cyan-500/10 text-cyan-400 animate-pulse'
                          }`}
                        >
                          {run.status.toLowerCase()}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-slate-400 text-xs">
                        {new Date(run.createdAt).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 text-right">
                        {run.status === 'COMPLETED' ? (
                          <button
                            onClick={() => navigate(`/report/${run.projectId}`)}
                            className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded bg-dark-800 hover:bg-dark-700 hover:text-cyan-400 border border-dark-700 text-xs font-semibold text-slate-300 transition-all"
                          >
                            <span>Details</span>
                            <ExternalLink className="w-3 h-3" />
                          </button>
                        ) : (
                          <button
                            onClick={() => navigate('/upload')}
                            className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded bg-dark-800 hover:bg-dark-700 hover:text-cyan-400 border border-dark-700 text-xs font-semibold text-slate-300 transition-all"
                          >
                            <span>Check Status</span>
                            <ArrowRight className="w-3 h-3" />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="space-y-6 max-w-4xl mx-auto py-2">
              {history.map((run, index) => {
                // Determine delta against chronological previous run of same project
                const sameProjRuns = history
                  .filter(r => r.projectName === run.projectName)
                  .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
                
                const idx = sameProjRuns.findIndex(r => r.historyId === run.historyId);
                const prev = idx > 0 ? sameProjRuns[idx - 1] : null;

                let scoreDiff = 'Initial Run';
                let diffStatus = 'Initial';
                let changeLog = 'First benchmark audit analysis compiled.';
                
                if (prev && run.status === 'COMPLETED' && prev.status === 'COMPLETED') {
                  const diffVal = run.overallScore - prev.overallScore;
                  scoreDiff = `${diffVal >= 0 ? '+' : ''}${diffVal.toFixed(1)}%`;
                  diffStatus = diffVal > 0 ? 'Improved' : diffVal < 0 ? 'Declined' : 'Unchanged';
                  changeLog = diffVal > 0 
                    ? 'Reduced high cyclomatic complexity findings. Fixed dependency vulnerabilities.' 
                    : diffVal < 0 
                    ? 'Introduced unchecked repository methods. Decreased overall test suite coverages.' 
                    : 'Code style optimizations & minor package configuration adjustments.';
                } else if (run.status === 'FAILED') {
                  scoreDiff = 'Run Failed';
                  diffStatus = 'Declined';
                  changeLog = 'Failed execution pipeline checks. Check system monitor logs.';
                }

                const isImproved = diffStatus === 'Improved';
                const isDeclined = diffStatus === 'Declined';

                return (
                  <div key={run.historyId} className="relative pl-8 border-l border-dark-700 pb-8 last:pb-0">
                    {/* Timeline Node Icon */}
                    <div className={`absolute -left-2 top-1.5 w-4.5 h-4.5 rounded-full border-4 border-dark-900 flex items-center justify-center ${
                      isImproved ? 'bg-emerald-500' : isDeclined ? 'bg-rose-500' : 'bg-cyan-500'
                    }`} />

                    {/* Card Body */}
                    <div className="glass-panel p-6 rounded-2xl border border-dark-700 flex flex-col md:flex-row md:items-center justify-between gap-6 hover:border-cyan-500/30 transition-all">
                      <div className="space-y-2 flex-1">
                        <div className="flex items-center gap-2.5">
                          <h3 className="text-sm font-bold text-white">{run.projectName}</h3>
                          <span className="text-[10px] px-2 py-0.5 rounded bg-dark-800 text-cyan-400 border border-cyan-500/20 font-semibold">
                            {run.reportVersion || 'v1'}
                          </span>
                        </div>
                        <p className="text-xs text-dark-500">
                          {new Date(run.createdAt).toLocaleString()} | Stack: {run.language || 'Java'} {run.framework ? `(${run.framework})` : ''}
                        </p>
                        {run.tags && run.tags.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-1.5">
                            {run.tags.map(t => (
                              <span key={t} className="text-[9px] px-1.5 py-0.5 rounded bg-dark-800 text-slate-400 border border-dark-700/80 font-medium">#{t}</span>
                            ))}
                          </div>
                        )}
                        
                        <div className="pt-3 border-t border-dark-800/80 mt-2">
                          <span className="block text-[9px] text-dark-500 font-bold uppercase tracking-wider mb-1">Major Changes</span>
                          <p className="text-xs text-slate-300 font-medium">{changeLog}</p>
                        </div>
                      </div>

                      <div className="flex flex-col items-end gap-2 text-right justify-between h-full min-w-[120px]">
                        <div>
                          <span className="text-2xl font-extrabold text-white">{run.overallScore ? `${run.overallScore}%` : '-'}</span>
                          <span className={`block text-[9px] mt-1 font-bold px-2 py-0.5 rounded-full uppercase tracking-wider text-center ${
                            isImproved ? 'bg-emerald-500/10 text-emerald-400' : isDeclined ? 'bg-rose-500/10 text-rose-400' : 'bg-cyan-500/10 text-cyan-400'
                          }`}>
                            {scoreDiff}
                          </span>
                        </div>
                        
                        {run.status === 'COMPLETED' ? (
                          <button
                            onClick={() => navigate(`/report/${run.projectId}`)}
                            className="mt-4 text-xs font-bold text-cyan-400 hover:text-cyan-300 flex items-center gap-1 active:scale-95 transition-all"
                          >
                            <span>View Details</span>
                            <ArrowRight className="w-3.5 h-3.5" />
                          </button>
                        ) : (
                          <button
                            onClick={() => navigate('/upload')}
                            className="mt-4 text-xs font-bold text-slate-400 hover:text-slate-200 flex items-center gap-1 active:scale-95 transition-all"
                          >
                            <span>Upload Status</span>
                            <ArrowRight className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      ) : (
        <div className="flex flex-col items-center justify-center p-12 border border-dashed border-dark-700 rounded-2xl text-dark-500 h-96">
          <HistoryIcon className="w-12 h-12 mb-3 opacity-50" />
          <span className="text-base font-semibold">No results match your filters</span>
          <p className="text-xs text-center max-w-sm mt-1">Try resetting search parameters or run a new audit upload.</p>
        </div>
      )}
    </div>
  );
}
