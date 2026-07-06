import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import axios from 'axios';
import { 
  ChevronLeft, ArrowUpDown, TrendingUp, TrendingDown, Minus, 
  Sparkles, ShieldCheck, Activity, Award, AlertCircle 
} from 'lucide-react';

export default function CompareReports() {
  const { projectId } = useParams();
  const [reports, setReports] = useState([]);
  const [report1Id, setReport1Id] = useState('');
  const [report2Id, setReport2Id] = useState('');
  const [comparison, setComparison] = useState(null);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingComp, setLoadingComp] = useState(false);
  const [error, setError] = useState('');
  const [projectName, setProjectName] = useState('');

  useEffect(() => {
    const fetchHistory = async () => {
      const token = localStorage.getItem('token');
      if (!token) return;

      try {
        const historyRes = await axios.get(`http://localhost:8080/api/reports/${projectId}/history`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setReports(historyRes.data);
        
        if (historyRes.data.length > 0) {
          // Default selection: set report1 to oldest, report2 to newest
          setReport1Id(historyRes.data[historyRes.data.length - 1].reportId.toString());
          setReport2Id(historyRes.data[0].reportId.toString());
        }

        // Fetch project name
        const projRes = await axios.get(`http://localhost:8080/api/projects/report/${projectId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setProjectName(projRes.data.projectName);

      } catch (err) {
        console.error(err);
        setError('Failed to fetch project history runs.');
      } finally {
        setLoadingList(false);
      }
    };

    fetchHistory();
  }, [projectId]);

  useEffect(() => {
    if (reports.length >= 2 && report1Id && report2Id) {
      // Auto-compare on initial load of report listings
      const triggerCompare = async () => {
        setError('');
        setLoadingComp(true);
        setComparison(null);
        const token = localStorage.getItem('token');
        try {
          const res = await axios.get(`http://localhost:8080/api/reports/compare?reportId1=${report1Id}&reportId2=${report2Id}`, {
            headers: { Authorization: `Bearer ${token}` }
          });
          setComparison(res.data);
        } catch (err) {
          console.error(err);
          setError('Comparison failed. AI provider or database error.');
        } finally {
          setLoadingComp(false);
        }
      };
      triggerCompare();
    }
  }, [reports, report1Id, report2Id]);

  const handleCompare = async () => {
    if (!report1Id || !report2Id) return;
    if (report1Id === report2Id) {
      setError('Please select two different reports to compare.');
      return;
    }

    setError('');
    setLoadingComp(true);
    setComparison(null);

    const token = localStorage.getItem('token');
    try {
      const res = await axios.get(`http://localhost:8080/api/reports/compare?reportId1=${report1Id}&reportId2=${report2Id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setComparison(res.data);
    } catch (err) {
      console.error(err);
      setError('Comparison failed. AI provider or database error.');
    } finally {
      setLoadingComp(false);
    }
  };

  const getStatusIcon = (status) => {
    if (status === 'Improved') return <TrendingUp className="w-4 h-4 text-emerald-400" />;
    if (status === 'Declined') return <TrendingDown className="w-4 h-4 text-rose-500" />;
    return <Minus className="w-4 h-4 text-slate-500" />;
  };

  const getStatusColor = (status) => {
    if (status === 'Improved') return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
    if (status === 'Declined') return 'text-rose-500 bg-rose-500/10 border-rose-500/20';
    return 'text-slate-400 bg-slate-800/50 border-slate-700';
  };

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto">
      {/* Header */}
      <div className="mb-8 flex items-center space-x-4">
        <Link to={`/report/${projectId}`} className="p-2 bg-dark-800 border border-dark-700 text-slate-300 hover:text-white rounded-lg transition-colors">
          <ChevronLeft className="w-5 h-5" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-white tracking-wide">Compare Audits</h1>
          <p className="text-xs text-dark-500 mt-1">Project Workspace: {projectName || 'Loading...'}</p>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center space-x-3 text-rose-500 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Selectors */}
      <div className="glass-panel p-6 rounded-2xl border border-dark-700 mb-8">
        <h2 className="text-xs font-bold text-white uppercase tracking-wider mb-4">Select Versions to Compare</h2>
        <div className="flex flex-col md:flex-row items-end gap-6">
          <div className="flex-1 space-y-1.5 w-full">
            <label className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block">Baseline Report (Report 1)</label>
            <select
              value={report1Id}
              onChange={(e) => setReport1Id(e.target.value)}
              className="w-full bg-dark-900 border border-dark-700 focus:border-cyan-500 focus:outline-none rounded-lg p-2.5 text-xs text-white cursor-pointer"
            >
              {reports.map((r) => (
                <option key={r.reportId} value={r.reportId}>
                  Version {r.reportVersion} ({r.projectVersion}) — Score: {r.overallScore}% ({new Date(r.reviewDate).toLocaleDateString()})
                </option>
              ))}
            </select>
          </div>

          <div className="text-dark-500 flex items-center justify-center h-10 w-full md:w-auto">
            <ArrowUpDown className="w-5 h-5 rotate-90 hidden md:block" />
            <ArrowUpDown className="w-5 h-5 md:hidden" />
          </div>

          <div className="flex-1 space-y-1.5 w-full">
            <label className="text-[10px] font-bold text-dark-500 uppercase tracking-widest block">Target Report (Report 2)</label>
            <select
              value={report2Id}
              onChange={(e) => setReport2Id(e.target.value)}
              className="w-full bg-dark-900 border border-dark-700 focus:border-cyan-500 focus:outline-none rounded-lg p-2.5 text-xs text-white cursor-pointer"
            >
              {reports.map((r) => (
                <option key={r.reportId} value={r.reportId}>
                  Version {r.reportVersion} ({r.projectVersion}) — Score: {r.overallScore}% ({new Date(r.reviewDate).toLocaleDateString()})
                </option>
              ))}
            </select>
          </div>

          <button
            onClick={handleCompare}
            disabled={loadingComp || reports.length < 2}
            className="px-6 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 disabled:opacity-40 text-dark-900 font-bold text-xs tracking-wide shadow-lg shadow-cyan-500/10 transition-all active:scale-95 whitespace-nowrap w-full md:w-auto"
          >
            {loadingComp ? 'Comparing...' : 'Compare Runs'}
          </button>
        </div>
      </div>

      {/* Loading comp */}
      {loadingComp && (
        <div className="flex flex-col items-center justify-center py-20">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400 mb-4" />
          <span className="text-xs text-dark-500">Generating AI comparative summaries...</span>
        </div>
      )}

      {/* Comparison Results */}
      {comparison && (
        <div className="space-y-8 animate-fade-in">
          {/* Comparison table */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-6">Metrics Comparison Matrix</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {Object.entries(comparison.scores).map(([key, diff]) => {
                // Format camelCase key to readable text
                const label = key.replace(/([A-Z])/g, ' $1').toUpperCase();
                return (
                  <div key={key} className="p-4 bg-dark-800/40 border border-dark-700/50 rounded-xl flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-bold text-dark-500 block uppercase tracking-widest">{label}</span>
                      <div className="flex items-baseline space-x-2 mt-1">
                        <span className="text-sm text-dark-500 font-semibold">{diff.val1}%</span>
                        <span className="text-xs text-dark-500">→</span>
                        <span className="text-lg font-extrabold text-white">{diff.val2}%</span>
                      </div>
                    </div>
                    <div className={`flex items-center space-x-1.5 px-2.5 py-1 rounded-full text-xs font-bold border uppercase tracking-wider ${getStatusColor(diff.status)}`}>
                      {getStatusIcon(diff.status)}
                      <span>{diff.difference > 0 ? `+${diff.difference}` : diff.difference}%</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* AI Comparison Summary */}
          <div className="glass-panel p-6 rounded-2xl border border-dark-700 relative overflow-hidden">
            <div className="absolute -top-40 -right-40 w-80 h-80 bg-cyan-500/5 rounded-full blur-3xl" />
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center space-x-2">
              <Sparkles className="w-4.5 h-4.5 text-cyan-400" />
              <span>AI Comparative Review Verdict</span>
            </h3>
            <p className="text-xs text-slate-300 leading-relaxed whitespace-pre-line font-medium bg-dark-900/50 p-5 border border-dark-700/50 rounded-xl">
              {comparison.aiSummary}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
