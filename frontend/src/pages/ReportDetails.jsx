import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { 
  BarChart3, ShieldAlert, CheckCircle2, ChevronRight, ChevronDown, 
  Download, FileCode2, Terminal, AlertCircle, FileText, ChevronLeft, 
  ArrowLeft, ArrowUpDown, History, Layers, Folder, Award, Sparkles,
  Database, ShieldCheck, Clock
} from 'lucide-react';
import ScoreGauge from '../components/ScoreGauge';
import FileTree from '../components/FileTree';
import API_BASE_URL from "../config/api";

export default function ReportDetails() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('overview'); // 'overview', 'static', 'tests', 'ai'
  const [expandedTest, setExpandedTest] = useState(null);
  
  // History & Tree Explorer States
  const [historyList, setHistoryList] = useState([]);
  const [treeData, setTreeData] = useState(null);

  // Project Tagging States
  const [tags, setTags] = useState([]);
  const [newTag, setNewTag] = useState('');

  const fetchBaseReport = async (reportId = null) => {
    setLoading(true);
    const token = localStorage.getItem('token');
    if (!token) return;

    const url = reportId 
      ? `${API_BASE_URL}/api/reports/detail/${reportId}`
      : `${API_BASE_URL}/api/projects/report/${projectId}`;

    try {
      const res = await axios.get(url, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setReport(res.data);
      setTags(res.data.tags || []);
    } catch (err) {
      console.error(err);
      setError('Failed to fetch detailed report results.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddTag = async () => {
    if (!newTag.trim()) return;
    const token = localStorage.getItem('token');
    try {
      const res = await axios.post(`${API_BASE_URL}/api/projects/${projectId}/tags`, { tag: newTag.trim() }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setTags(res.data.tags || []);
      setNewTag('');
    } catch (e) {
      console.error(e);
      alert('Failed to add tag');
    }
  };

  const handleDeleteTag = async (tagToDelete) => {
    const token = localStorage.getItem('token');
    try {
      const res = await axios.delete(`${API_BASE_URL}/api/projects/${projectId}/tags/${tagToDelete}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setTags(res.data.tags || []);
    } catch (e) {
      console.error(e);
      alert('Failed to delete tag');
    }
  };

  const fetchProjectMetadata = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      // Fetch History Runs
      const histRes = await axios.get(`${API_BASE_URL}/api/reports/${projectId}/history`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setHistoryList(histRes.data);

      // Fetch Tree Structure
      const treeRes = await axios.get(`${API_BASE_URL}/api/reports/${projectId}/structure`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setTreeData(treeRes.data);
    } catch (e) {
      console.error("Failed to load metadata extensions:", e);
    }
  };

  useEffect(() => {
    fetchBaseReport();
    fetchProjectMetadata();
  }, [projectId]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-dark-900">
        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-cyan-400" />
      </div>
    );
  }

  if (error || !report) {
    return (
      <div className="flex-1 bg-dark-900 p-8 flex items-center justify-center">
        <div className="max-w-md w-full glass-panel p-8 text-center rounded-2xl border border-dark-700">
          <AlertCircle className="w-12 h-12 text-rose-500 mx-auto mb-4" />
          <h2 className="text-lg font-bold text-white mb-2">Failed to load report</h2>
          <p className="text-sm text-dark-500 mb-6">{error || 'Project data is currently unavailable'}</p>
          <Link to="/" className="inline-flex items-center space-x-2 text-cyan-400 hover:underline">
            <ArrowLeft className="w-4 h-4" />
            <span>Return to Dashboard</span>
          </Link>
        </div>
      </div>
    );
  }

  // Parse findings and data arrays
  const strengths = typeof report.strengths === 'string' ? JSON.parse(report.strengths || '[]') : (report.strengths || []);
  const weaknesses = typeof report.weaknesses === 'string' ? JSON.parse(report.weaknesses || '[]') : (report.weaknesses || []);
  const staticFindings = report.analysisMetrics || [];
  const unitTestResult = report.unitTestResult || { totalTests: 0, passed: 0, failed: 0, skipped: 0, executionTime: 0, failures: [] };
  const aiFindings = typeof report.aiSuggestions === 'string' ? JSON.parse(report.aiSuggestions || '[]') : (report.aiSuggestions || []);

  // Severity counts
  const staticIssuesCount = staticFindings.length;
  const criticalCount = staticFindings.filter(f => f.severity === 'Critical').length;
  const warningCount = staticFindings.filter(f => f.severity === 'Warning').length;
  const suggestionCount = staticFindings.filter(f => f.severity === 'Suggestion' || f.severity === 'Good Practice').length;

  const handleDownloadPdf = () => {
    window.open(`${API_BASE_URL}/api/projects/download/${report.id}`, '_blank');
  };

  const handleDownloadJson = () => {
    window.open(`${API_BASE_URL}/api/reports/export/json/${report.id}`, '_blank');
  };

  const handleDownloadHtml = () => {
    window.open(`${API_BASE_URL}/api/reports/export/html/${report.id}`, '_blank');
  };

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto flex flex-col">
      {/* Page Header */}
      <div className="mb-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center space-x-4">
          <Link to="/history" className="p-2 bg-dark-800 border border-dark-700 text-slate-300 hover:text-white rounded-lg transition-colors">
            <ChevronLeft className="w-5 h-5" />
          </Link>
          <div>
            <h1 className="text-xl font-bold text-white tracking-wide">{report.projectName}</h1>
            <p className="text-xs text-dark-500 mt-1">
              Active Audit: <span className="text-cyan-400 font-semibold">{report.reportVersion} ({report.projectVersion})</span> | Reviewer: {report.reviewer}
            </p>
            <div className="flex flex-wrap items-center gap-2 mt-2.5">
              {tags.map(t => (
                <span key={t} className="text-[10px] px-2 py-0.5 rounded bg-dark-850 text-slate-300 border border-dark-700/80 flex items-center space-x-1 font-medium">
                  <span>#{t}</span>
                  <button onClick={() => handleDeleteTag(t)} className="text-slate-500 hover:text-rose-500 font-bold ml-1 text-[10px] leading-none">&times;</button>
                </span>
              ))}
              <div className="flex items-center space-x-1.5 ml-2">
                <input
                  type="text"
                  placeholder="New tag..."
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleAddTag(); }}
                  className="bg-dark-950 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded py-0.5 px-2 text-[10px] text-white w-20 placeholder-dark-500"
                />
                <button
                  onClick={handleAddTag}
                  className="px-2 py-0.5 bg-cyan-500 hover:bg-cyan-400 text-dark-900 font-bold rounded text-[9px] active:scale-95 transition-all"
                >
                  + Add
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={handleDownloadJson}
            className="flex items-center space-x-2 px-3 py-2 rounded-lg bg-dark-800 hover:bg-dark-700 text-slate-300 hover:text-white font-semibold text-xs border border-dark-700 transition-all shadow-lg active:scale-95"
          >
            <span>Export JSON</span>
          </button>
          <button
            onClick={handleDownloadHtml}
            className="flex items-center space-x-2 px-3 py-2 rounded-lg bg-dark-800 hover:bg-dark-700 text-slate-300 hover:text-white font-semibold text-xs border border-dark-700 transition-all shadow-lg active:scale-95"
          >
            <span>Export HTML</span>
          </button>
          <button
            onClick={handleDownloadPdf}
            className="flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-dark-900 font-bold text-xs shadow-lg shadow-cyan-500/10 transition-all active:scale-95"
          >
            <Download className="w-4 h-4" />
            <span>Download PDF</span>
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap border-b border-dark-700 mb-6 gap-y-1">
        {[
          { key: 'overview', label: 'Overview', icon: FileText },
          { key: 'static', label: `Static (${staticIssuesCount})`, icon: ShieldAlert },
          { key: 'dependency', label: `Dependencies`, icon: Layers },
          { key: 'config', label: `Configurations`, icon: Terminal },
          { key: 'api', label: `REST APIs`, icon: BarChart3 },
          { key: 'db', label: `Database JPA`, icon: Database },
          { key: 'tests', label: `Unit Tests (${unitTestResult.totalTests})`, icon: CheckCircle2 },
          { key: 'ai', label: 'AI Review Summary', icon: FileCode2 }
        ].map(tab => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`py-3 px-6 text-sm font-semibold tracking-wider transition-all border-b-2 flex items-center space-x-2 ${
                activeTab === tab.key
                  ? 'border-cyan-500 text-cyan-400'
                  : 'border-transparent text-slate-400 hover:text-white'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Tab Panels */}
      <div className="flex-1 min-h-0">
        
        {/* OVERVIEW PANEL */}
        {activeTab === 'overview' && (
          <div className="space-y-8 animate-fade-in">
            {/* Score Gauges Grid */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6 glass-panel p-6 rounded-2xl border border-dark-700">
              <ScoreGauge score={report.overallScore} label="Overall Score" size={130} />
              <ScoreGauge score={report.codeQualityScore} label="Code Quality" size={120} />
              <ScoreGauge score={report.maintainabilityScore} label="Maintainability" size={120} />
              <ScoreGauge score={report.documentationScore} label="Documentation" size={120} />
            </div>

            {/* Technical Debt & Review Insights Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              
              {/* Technical Debt Status */}
              <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2 flex items-center space-x-2">
                  <Clock className="w-4.5 h-4.5 text-cyan-400" />
                  <span>Technical Debt Summary</span>
                </h2>
                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-dark-900/60 p-3 rounded-xl border border-dark-800">
                    <span className="block text-[10px] text-slate-400 uppercase font-semibold">Remediation Effort</span>
                    <span className="block text-xl font-bold text-cyan-400 mt-1">{report.techDebtHours || 0} Hours</span>
                  </div>
                  <div className="bg-dark-900/60 p-3 rounded-xl border border-dark-800">
                    <span className="block text-[10px] text-slate-400 uppercase font-semibold">Debt Complexity</span>
                    <span className="block text-xl font-bold text-amber-500 mt-1">{report.techDebtComplexity || 'Low'}</span>
                  </div>
                  <div className="bg-dark-900/60 p-3 rounded-xl border border-dark-800">
                    <span className="block text-[10px] text-slate-400 uppercase font-semibold">Risk Level</span>
                    <span className="block text-xl font-bold text-rose-500 mt-1">{report.techDebtRisk || 'Low'}</span>
                  </div>
                  <div className="bg-dark-900/60 p-3 rounded-xl border border-dark-800">
                    <span className="block text-[10px] text-slate-400 uppercase font-semibold">Fix Priority</span>
                    <span className="block text-xl font-bold text-purple-500 mt-1">{report.techDebtPriority || 'Low'}</span>
                  </div>
                </div>
                <div className="pt-2 border-t border-dark-800">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold block mb-1">AI Maintainability Index</span>
                  <span className="text-xs font-semibold text-slate-300">Maintainability Verdict: <strong className="text-emerald-400">{report.estimatedMaintainability || 'Good'}</strong></span>
                </div>
              </div>

              {/* Review Insights */}
              <div className="glass-panel p-6 rounded-2xl border border-dark-700 md:col-span-2 space-y-4">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2 flex items-center space-x-2">
                  <Sparkles className="w-4.5 h-4.5 text-cyan-400" />
                  <span>Intelligent Review Insights</span>
                </h2>
                
                {report.reviewInsights ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div className="flex items-center justify-between p-2.5 bg-dark-900/40 rounded-lg border border-dark-800">
                      <span className="text-slate-400 font-semibold">Largest Class:</span>
                      <span className="text-white font-bold text-right truncate max-w-[180px]">{report.reviewInsights.largestClass || 'N/A'}</span>
                    </div>
                    <div className="flex items-center justify-between p-2.5 bg-dark-900/40 rounded-lg border border-dark-800">
                      <span className="text-slate-400 font-semibold">Longest Method:</span>
                      <span className="text-white font-bold text-right truncate max-w-[180px]">{report.reviewInsights.longestMethod || 'N/A'}</span>
                    </div>
                    <div className="flex items-center justify-between p-2.5 bg-dark-900/40 rounded-lg border border-dark-800">
                      <span className="text-slate-400 font-semibold">Most Common Issue:</span>
                      <span className="text-white font-bold text-right truncate max-w-[180px]">{report.reviewInsights.mostCommonIssue || 'N/A'}</span>
                    </div>
                    <div className="flex items-center justify-between p-2.5 bg-dark-900/40 rounded-lg border border-dark-800">
                      <span className="text-slate-400 font-semibold">Highest Risk Module:</span>
                      <span className="text-white font-bold text-right truncate max-w-[180px]">{report.reviewInsights.highestRiskComponent || 'N/A'}</span>
                    </div>
                  </div>
                ) : (
                  <div className="text-xs text-dark-500 italic py-6 text-center">
                    No intelligent review insights compiled yet.
                  </div>
                )}
              </div>

            </div>

            {/* Structure metrics, Tree & history */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              
              {/* Build Stack & Tree */}
              <div className="lg:col-span-2 space-y-8">
                {/* Tech specifications */}
                <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                  <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-4 border-b border-dark-700 pb-2 flex items-center space-x-2">
                    <Layers className="w-4.5 h-4.5 text-cyan-400" />
                    <span>Technology Stack Spec</span>
                  </h2>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div>
                      <span className="text-[10px] text-dark-500 font-semibold block uppercase">Language</span>
                      <span className="text-xs font-semibold text-white mt-1 block">{report.structuralMetrics?.language || 'Java'}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-dark-500 font-semibold block uppercase">Build tool</span>
                      <span className="text-xs font-semibold text-white mt-1 block">{report.structuralMetrics?.buildTool || 'Maven'}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-dark-500 font-semibold block uppercase">Framework</span>
                      <span className="text-xs font-semibold text-white mt-1 block">{report.structuralMetrics?.framework || 'Spring Boot'}</span>
                    </div>
                    <div>
                      <span className="text-[10px] text-dark-500 font-semibold block uppercase">Packages count</span>
                      <span className="text-xs font-semibold text-white mt-1 block">{report.structuralMetrics?.numPackages || 0}</span>
                    </div>
                  </div>
                  <p className="text-xs text-dark-500 mt-4 leading-relaxed bg-dark-900/40 p-3 rounded-lg border border-dark-700/50">
                    {report.techStack}
                  </p>
                </div>

                {/* Folder Structure tree explorer */}
                <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                  <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-4 border-b border-dark-700 pb-2 flex items-center space-x-2">
                    <Folder className="w-4.5 h-4.5 text-cyan-400" />
                    <span>Folder Structure Explorer</span>
                  </h2>
                  <FileTree treeData={treeData} />
                </div>
              </div>

              {/* History & Comparisons Panel */}
              <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-6">
                <div className="flex items-center justify-between border-b border-dark-700 pb-2">
                  <h2 className="text-sm font-bold text-white uppercase tracking-wider flex items-center space-x-2">
                    <History className="w-4.5 h-4.5 text-cyan-400" />
                    <span>Audit Runs History</span>
                  </h2>
                  {historyList.length >= 2 && (
                    <button
                      onClick={() => navigate(`/compare/${projectId}`)}
                      className="text-[10px] bg-cyan-500 hover:bg-cyan-400 text-dark-900 px-2.5 py-1 rounded font-bold transition-colors active:scale-95 shadow-md shadow-cyan-500/10"
                    >
                      Compare Audits
                    </button>
                  )}
                </div>

                <div className="space-y-3 max-h-[300px] overflow-y-auto pr-1">
                  {historyList.map((hist) => {
                    const isSelected = hist.reportId === report.id;
                    return (
                      <div
                        key={hist.reportId}
                        onClick={() => {
                          if (!isSelected) fetchBaseReport(hist.reportId);
                        }}
                        className={`p-3 rounded-lg border cursor-pointer transition-all flex items-center justify-between ${
                          isSelected
                            ? 'bg-gradient-to-r from-cyan-900/20 to-indigo-900/10 border-cyan-500 text-cyan-400 font-semibold shadow-inner'
                            : 'bg-dark-800/30 border-dark-700 hover:border-dark-600 hover:bg-dark-800/60 text-slate-300'
                        }`}
                      >
                        <div>
                          <span className="block text-xs">Run Version {hist.reportVersion}</span>
                          <span className="block text-[9px] text-dark-500 mt-0.5">{new Date(hist.reviewDate).toLocaleDateString()}</span>
                        </div>
                        <span className={`text-xs font-bold px-2 py-0.5 rounded ${
                          isSelected ? 'bg-cyan-500/10 text-cyan-400' : 'bg-dark-700 text-slate-300'
                        }`}>{hist.overallScore}%</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Strengths & Weaknesses */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="p-6 rounded-2xl bg-emerald-500/5 border border-emerald-500/10">
                <h2 className="text-sm font-bold text-emerald-400 uppercase tracking-wider mb-4 flex items-center space-x-2">
                  <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                  <span>Key Strengths</span>
                </h2>
                <ul className="space-y-3">
                  {strengths.map((str, i) => (
                    <li key={i} className="text-xs text-slate-300 flex items-start space-x-2 leading-relaxed">
                      <ChevronRight className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                      <span>{str}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="p-6 rounded-2xl bg-rose-500/5 border border-rose-500/10">
                <h2 className="text-sm font-bold text-rose-400 uppercase tracking-wider mb-4 flex items-center space-x-2">
                  <ShieldAlert className="w-5 h-5 text-rose-400" />
                  <span>Areas for Improvement</span>
                </h2>
                <ul className="space-y-3">
                  {weaknesses.map((weak, i) => (
                    <li key={i} className="text-xs text-slate-300 flex items-start space-x-2 leading-relaxed">
                      <ChevronRight className="w-4 h-4 text-rose-400 flex-shrink-0 mt-0.5" />
                      <span>{weak}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Executive Summary & Final Verdict */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-3">Executive Summary</h2>
                <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/40 p-4 rounded-xl border border-dark-700/50">
                  {report.executiveSummary || 'Comprehensive audit summary generated.'}
                </p>
              </div>

              <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-3">Final Verdict</h2>
                <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/40 p-4 rounded-xl border border-dark-700/50">
                  {report.finalVerdict}
                </p>
              </div>
            </div>

          </div>
        )}

        {/* STATIC ANALYSIS PANEL */}
        {activeTab === 'static' && (
          <div className="space-y-6 animate-fade-in">
            {/* Severity Breakdown Widgets */}
            <div className="grid grid-cols-3 gap-6">
              <div className="p-4 bg-rose-500/5 border border-rose-500/10 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-rose-400 block mb-1">Critical Issues</span>
                <span className="text-2xl font-extrabold text-rose-500">{criticalCount}</span>
              </div>
              <div className="p-4 bg-amber-500/5 border border-amber-500/10 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400 block mb-1">Warnings</span>
                <span className="text-2xl font-extrabold text-amber-500">{warningCount}</span>
              </div>
              <div className="p-4 bg-cyan-500/5 border border-cyan-500/10 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400 block mb-1">Suggestions</span>
                <span className="text-2xl font-extrabold text-cyan-500">{suggestionCount}</span>
              </div>
            </div>

            {/* Findings List */}
            {staticFindings.length > 0 ? (
              <div className="space-y-4">
                {staticFindings.map((finding, idx) => {
                  const isCrit = finding.severity === 'Critical';
                  const isWarn = finding.severity === 'Warning';
                  
                  return (
                    <div key={idx} className="glass-panel p-5 rounded-2xl border border-dark-700">
                      <div className="flex items-center justify-between mb-2">
                        <span className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                          isCrit ? 'bg-rose-500/10 text-rose-400' : isWarn ? 'bg-amber-500/10 text-amber-400' : 'bg-cyan-500/10 text-cyan-400'
                        }`}>
                          {finding.severity}
                        </span>
                        <span className="text-xs text-dark-500 font-semibold">{finding.file} : Line {finding.line}</span>
                      </div>
                      <h3 className="text-sm font-bold text-white mb-2">{finding.title}</h3>
                      <p className="text-xs text-slate-400 mb-4 leading-relaxed">{finding.description}</p>
                      
                      {finding.snippet && (
                        <div className="bg-dark-900 border border-dark-700/80 rounded-lg p-3 font-mono text-[10px] text-slate-300 overflow-x-auto">
                          <code>{finding.snippet}</code>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="border border-dashed border-dark-700 p-12 rounded-2xl text-center text-dark-500">
                <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
                <span className="text-sm font-semibold">Zero static findings detected</span>
                <p className="text-xs mt-1">Excellent! Codebase aligns fully with structural guidelines.</p>
              </div>
            )}
          </div>
        )}

        {/* UNIT TESTS PANEL */}
        {activeTab === 'tests' && (
          <div className="space-y-6 animate-fade-in">
            {/* Test Stats Widgets */}
            <div className="grid grid-cols-4 gap-6">
              <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-dark-500 block mb-1">Total Tests</span>
                <span className="text-2xl font-extrabold text-white">{unitTestResult.totalTests}</span>
              </div>
              <div className="p-4 bg-emerald-500/5 border border-emerald-500/10 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400 block mb-1">Passed</span>
                <span className="text-2xl font-extrabold text-emerald-500">{unitTestResult.passed}</span>
              </div>
              <div className="p-4 bg-rose-500/5 border border-rose-500/10 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-rose-400 block mb-1">Failed</span>
                <span className="text-2xl font-extrabold text-rose-500">{unitTestResult.failed}</span>
              </div>
              <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                <span className="text-[10px] font-bold uppercase tracking-wider text-dark-500 block mb-1">Execution Time</span>
                <span className="text-xl font-extrabold text-white mt-0.5 block">{(unitTestResult.executionTime / 1000).toFixed(2)}s</span>
              </div>
            </div>

            {/* Failures List */}
            {unitTestResult.failures && unitTestResult.failures.length > 0 ? (
              <div className="space-y-4">
                <h2 className="text-sm font-bold text-rose-500 uppercase tracking-wider">Failed Test Diagnoses</h2>
                {unitTestResult.failures.map((fail, idx) => {
                  const isExpanded = expandedTest === idx;
                  return (
                    <div key={idx} className="glass-panel rounded-2xl border border-rose-500/10 overflow-hidden">
                      <div 
                        onClick={() => setExpandedTest(isExpanded ? null : idx)}
                        className="p-4 bg-rose-500/5 flex items-center justify-between cursor-pointer hover:bg-rose-500/10 transition-colors"
                      >
                        <div>
                          <span className="block text-xs font-bold text-white">{fail.methodName}</span>
                          <span className="block text-[10px] text-dark-500 mt-1">{fail.className}</span>
                        </div>
                        {isExpanded ? <ChevronDown className="w-4 h-4 text-slate-400" /> : <ChevronRight className="w-4 h-4 text-slate-400" />}
                      </div>
                      
                      {isExpanded && (
                        <div className="p-4 border-t border-dark-700 bg-dark-900/50 space-y-3 font-mono text-[10px]">
                          <div>
                            <span className="text-rose-400 font-bold block mb-1">Error Message:</span>
                            <span className="text-slate-300">{fail.message || 'Assertion Error'}</span>
                          </div>
                          {fail.stackTrace && (
                            <div>
                              <span className="text-rose-400 font-bold block mb-1">Stack Trace:</span>
                              <pre className="text-slate-500 overflow-x-auto p-3 bg-dark-900 border border-dark-700/50 rounded-lg max-h-60">
                                {fail.stackTrace}
                              </pre>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="border border-dashed border-dark-700 p-12 rounded-2xl text-center text-dark-500">
                <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
                <span className="text-sm font-semibold">All unit tests passed successfully</span>
                <p className="text-xs mt-1">Surefire execution reported 100% test run compliance.</p>
              </div>
            )}
          </div>
        )}

        {/* AI REVIEW PANEL */}
        {activeTab === 'ai' && (
          <div className="space-y-6 animate-fade-in">
            {/* Architecture Review */}
            <div className="glass-panel p-6 rounded-2xl border border-dark-700">
              <h2 className="text-xs font-bold text-white uppercase tracking-widest mb-3 flex items-center space-x-2">
                <Award className="w-4.5 h-4.5 text-cyan-400" />
                <span>Architecture Review</span>
              </h2>
              <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/50 p-4 border border-dark-700/30 rounded-xl">
                {report.architectureSummary}
              </p>
            </div>

            {/* Security Assessment */}
            <div className="glass-panel p-6 rounded-2xl border border-dark-700">
              <h2 className="text-xs font-bold text-white uppercase tracking-widest mb-3 flex items-center space-x-2">
                <ShieldCheck className="w-4.5 h-4.5 text-cyan-400" />
                <span>Security Review</span>
              </h2>
              <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/50 p-4 border border-dark-700/30 rounded-xl">
                {report.securityAnalysis}
              </p>
            </div>

            {/* Code Quality Review */}
            {report.codeQualityReview && (
              <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                <h2 className="text-xs font-bold text-white uppercase tracking-widest mb-3">Code Quality Review</h2>
                <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/50 p-4 border border-dark-700/30 rounded-xl">
                  {report.codeQualityReview}
                </p>
              </div>
            )}

            {/* Testing Review */}
            {report.testingReview && (
              <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                <h2 className="text-xs font-bold text-white uppercase tracking-widest mb-3">Testing Review</h2>
                <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/50 p-4 border border-dark-700/30 rounded-xl">
                  {report.testingReview}
                </p>
              </div>
            )}

            {/* Documentation Review */}
            {report.documentationReview && (
              <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                <h2 className="text-xs font-bold text-white uppercase tracking-widest mb-3">Documentation Review</h2>
                <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/50 p-4 border border-dark-700/30 rounded-xl">
                  {report.documentationReview}
                </p>
              </div>
            )}

            {/* Maintainability Review */}
            {report.maintainabilityReview && (
              <div className="glass-panel p-6 rounded-2xl border border-dark-700">
                <h2 className="text-xs font-bold text-white uppercase tracking-widest mb-3">Maintainability Review</h2>
                <p className="text-xs text-slate-300 leading-relaxed font-medium bg-dark-900/50 p-4 border border-dark-700/30 rounded-xl">
                  {report.maintainabilityReview}
                </p>
              </div>
            )}

            {/* Specific AI Findings */}
            <div className="glass-panel p-6 rounded-2xl border border-dark-700">
              <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-4 border-b border-dark-700 pb-2">AI Specific Recommendations</h2>
              {aiFindings.length > 0 ? (
                <div className="space-y-6">
                  {aiFindings.map((rec, i) => {
                    const isCrit = rec.severity === 'Critical';
                    const isWarn = rec.severity === 'Warning';
                    
                    return (
                      <div key={i} className="flex flex-col space-y-2 pb-4 border-b border-dark-700/50 last:border-b-0 last:pb-0 text-xs">
                        <div className="flex items-center space-x-2">
                          <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                            isCrit ? 'bg-rose-500/10 text-rose-400' : isWarn ? 'bg-amber-500/10 text-amber-400' : 'bg-cyan-500/10 text-cyan-400'
                          }`}>
                            {rec.severity}
                          </span>
                          <h3 className="text-sm font-bold text-white">{rec.title}</h3>
                        </div>
                        <p className="text-slate-300 leading-relaxed">{rec.description}</p>
                        <div className="text-[10px] text-dark-500 flex items-center space-x-2">
                          <span className="font-bold">Category:</span>
                          <span className="text-slate-400 font-semibold">{rec.category || 'Quality'}</span>
                          <span>•</span>
                          <span className="font-bold">Reason:</span>
                          <span>{rec.reason}</span>
                          <span>•</span>
                          <span className="font-bold">Confidence:</span>
                          <span>{rec.confidencePercentage}%</span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="text-xs text-slate-300 italic leading-relaxed">
                  No improvements are recommended because the implementation already follows good software engineering practices.
                </p>
              )}
            </div>
          </div>
        )}

        {/* DEPENDENCY ANALYSIS PANEL */}
        {activeTab === 'dependency' && (
          <div className="space-y-6 animate-fade-in text-xs">
            {report.dependencyAnalysis ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                    <span className="block text-[10px] text-dark-500 font-bold uppercase mb-1">Total Checked</span>
                    <span className="text-2xl font-extrabold text-white">{report.dependencyAnalysis.totalDependencies || 0}</span>
                  </div>
                  <div className="p-4 bg-rose-500/5 border border-rose-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-rose-400 font-bold uppercase mb-1">Duplicates</span>
                    <span className="text-2xl font-extrabold text-rose-500">{report.dependencyAnalysis.duplicateCount || 0}</span>
                  </div>
                  <div className="p-4 bg-amber-500/5 border border-amber-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-amber-400 font-bold uppercase mb-1">Outdated / Vulnerable</span>
                    <span className="text-2xl font-extrabold text-amber-500">{report.dependencyAnalysis.outdatedCount || 0}</span>
                  </div>
                  <div className="p-4 bg-rose-500/5 border border-rose-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-rose-400 font-bold uppercase mb-1">Unused Declared</span>
                    <span className="text-2xl font-extrabold text-rose-500">{report.dependencyAnalysis.unusedCount || 0}</span>
                  </div>
                </div>

                {/* Details List */}
                <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4">
                  <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">Vulnerable and Outdated Items</h2>
                  {report.dependencyAnalysis.findings && report.dependencyAnalysis.findings.length > 0 ? (
                    <div className="space-y-3">
                      {report.dependencyAnalysis.findings.map((dep, i) => (
                        <div key={i} className="flex justify-between items-center p-3 bg-dark-900/40 border border-dark-800 rounded-lg">
                          <div>
                            <span className="block font-bold text-white">{dep.groupId}:{dep.artifactId}</span>
                            <span className="block text-[10px] text-dark-500 mt-1">Details: {dep.description}</span>
                          </div>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                            dep.severity === 'Critical' ? 'bg-rose-500/10 text-rose-400' : 'bg-amber-500/10 text-amber-400'
                          }`}>{dep.severity}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-dark-500 italic">No vulnerable dependencies detected.</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="border border-dashed border-dark-700 p-12 rounded-2xl text-center text-dark-500">
                <Layers className="w-10 h-10 text-cyan-500 mx-auto mb-2" />
                <span className="text-sm font-semibold">No dependency data available</span>
              </div>
            )}
          </div>
        )}

        {/* CONFIGURATION ANALYSIS PANEL */}
        {activeTab === 'config' && (
          <div className="space-y-6 animate-fade-in text-xs">
            {report.configurationAnalysis ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                    <span className="block text-[10px] text-dark-500 font-bold uppercase mb-1">Properties Files</span>
                    <span className="text-2xl font-extrabold text-white">{report.configurationAnalysis.filesScanned || 0}</span>
                  </div>
                  <div className="p-4 bg-rose-500/5 border border-rose-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-rose-400 font-bold uppercase mb-1">Plain-text Secrets</span>
                    <span className="text-2xl font-extrabold text-rose-500">{report.configurationAnalysis.plainTextSecretsCount || 0}</span>
                  </div>
                  <div className="p-4 bg-amber-500/5 border border-amber-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-amber-400 font-bold uppercase mb-1">Duplicate Keys</span>
                    <span className="text-2xl font-extrabold text-amber-500">{report.configurationAnalysis.duplicateKeysCount || 0}</span>
                  </div>
                </div>

                {/* Configuration Findings */}
                <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4">
                  <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">Configuration Diagnostics</h2>
                  {report.configurationAnalysis.findings && report.configurationAnalysis.findings.length > 0 ? (
                    <div className="space-y-3">
                      {report.configurationAnalysis.findings.map((f, i) => (
                        <div key={i} className="p-3 bg-dark-900/40 border border-dark-800 rounded-lg space-y-1">
                          <div className="flex justify-between items-center">
                            <span className="font-bold text-white">{f.fileName}</span>
                            <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                              f.severity === 'Critical' ? 'bg-rose-500/10 text-rose-400' : 'bg-amber-500/10 text-amber-400'
                            }`}>{f.severity}</span>
                          </div>
                          <p className="text-slate-400 mt-1">{f.description}</p>
                          {f.key && <span className="block text-[10px] text-dark-500 font-mono">Target Key: {f.key}</span>}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-dark-500 italic">No configuration issues detected.</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="border border-dashed border-dark-700 p-12 rounded-2xl text-center text-dark-500">
                <Terminal className="w-10 h-10 text-cyan-500 mx-auto mb-2" />
                <span className="text-sm font-semibold">No configuration analysis details available</span>
              </div>
            )}
          </div>
        )}

        {/* API DOCUMENTATION PANEL */}
        {activeTab === 'api' && (
          <div className="space-y-6 animate-fade-in text-xs">
            {report.apiAnalysis ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                    <span className="block text-[10px] text-dark-500 font-bold uppercase mb-1">Controllers Scanned</span>
                    <span className="text-2xl font-extrabold text-white">{report.apiAnalysis.totalControllers || 0}</span>
                  </div>
                  <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                    <span className="block text-[10px] text-dark-500 font-bold uppercase mb-1">REST Endpoints</span>
                    <span className="text-2xl font-extrabold text-white">{report.apiAnalysis.totalEndpoints || 0}</span>
                  </div>
                  <div className="p-4 bg-cyan-500/5 border border-cyan-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-cyan-400 font-bold uppercase mb-1">Validation Coverage</span>
                    <span className="text-2xl font-extrabold text-cyan-500">{report.apiAnalysis.validationPercentage || 0}%</span>
                  </div>
                  <div className="p-4 bg-emerald-500/5 border border-emerald-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-emerald-400 font-bold uppercase mb-1">API Health Score</span>
                    <span className="text-2xl font-extrabold text-emerald-500">{report.apiAnalysis.apiQualityScore || 100}</span>
                  </div>
                </div>

                {/* API Specs */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-3">
                    <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">Endpoint Method Distribution</h2>
                    {report.apiAnalysis.methodDistribution ? (
                      <div className="space-y-2">
                        {Object.entries(report.apiAnalysis.methodDistribution).map(([method, count]) => (
                          <div key={method} className="flex justify-between items-center p-2 bg-dark-900/40 rounded border border-dark-800">
                            <span className="font-bold text-cyan-400 font-mono uppercase">{method}</span>
                            <span className="text-white font-semibold">{count}</span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-dark-500 italic">No distribution metrics calculated.</p>
                    )}
                  </div>

                  <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4">
                    <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">API Specifications</h2>
                    <div className="space-y-3">
                      <div className="flex justify-between items-center p-2.5 bg-dark-900/40 rounded border border-dark-800">
                        <span className="text-slate-400">Global Exception Handler Advisor</span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          report.apiAnalysis.hasGlobalExceptionHandler ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                        }`}>{report.apiAnalysis.hasGlobalExceptionHandler ? 'Configured' : 'Missing'}</span>
                      </div>
                      <div className="flex justify-between items-center p-2.5 bg-dark-900/40 rounded border border-dark-800">
                        <span className="text-slate-400">Swagger OpenAPI Integration</span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                          report.apiAnalysis.hasSwaggerOpenApi ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                        }`}>{report.apiAnalysis.hasSwaggerOpenApi ? 'Configured' : 'Missing'}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="border border-dashed border-dark-700 p-12 rounded-2xl text-center text-dark-500">
                <BarChart3 className="w-10 h-10 text-cyan-500 mx-auto mb-2" />
                <span className="text-sm font-semibold">No API endpoints captured in codebase</span>
              </div>
            )}
          </div>
        )}

        {/* DATABASE PERSISTENCE PANEL */}
        {activeTab === 'db' && (
          <div className="space-y-6 animate-fade-in text-xs">
            {report.databaseAnalysis ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                    <span className="block text-[10px] text-dark-500 font-bold uppercase mb-1">Entities Parsed</span>
                    <span className="text-2xl font-extrabold text-white">{report.databaseAnalysis.totalEntities || 0}</span>
                  </div>
                  <div className="p-4 bg-dark-800/40 border border-dark-700 rounded-xl text-center">
                    <span className="block text-[10px] text-dark-500 font-bold uppercase mb-1">Spring Data Repositories</span>
                    <span className="text-2xl font-extrabold text-white">{report.databaseAnalysis.totalRepositories || 0}</span>
                  </div>
                  <div className="p-4 bg-rose-500/5 border border-rose-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-rose-400 font-bold uppercase mb-1">Performance Warnings</span>
                    <span className="text-2xl font-extrabold text-rose-500">{report.databaseAnalysis.eagerFetchWarningsCount || 0}</span>
                  </div>
                  <div className="p-4 bg-emerald-500/5 border border-emerald-500/10 rounded-xl text-center">
                    <span className="block text-[10px] text-emerald-400 font-bold uppercase mb-1">Database Score</span>
                    <span className="text-2xl font-extrabold text-emerald-500">{report.databaseAnalysis.databaseQualityScore || 100}</span>
                  </div>
                </div>

                {/* JPA Entities & Performance Checks */}
                <div className="glass-panel p-6 rounded-2xl border border-dark-700 space-y-4">
                  <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">Performance & Cardinality Audit</h2>
                  {report.databaseAnalysis.findings && report.databaseAnalysis.findings.length > 0 ? (
                    <div className="space-y-3">
                      {report.databaseAnalysis.findings.map((f, i) => (
                        <div key={i} className="p-3 bg-dark-900/40 border border-dark-800 rounded-lg space-y-1">
                          <div className="flex justify-between items-center">
                            <span className="font-bold text-white font-mono">{f.entityName || 'Persistence Context'}</span>
                            <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                              f.severity === 'Warning' ? 'bg-amber-500/10 text-amber-400' : 'bg-cyan-500/10 text-cyan-400'
                            }`}>{f.severity}</span>
                          </div>
                          <p className="text-slate-400 mt-1">{f.description}</p>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-dark-500 italic">No JPA performance warnings detected. Excellent persistence config!</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="border border-dashed border-dark-700 p-12 rounded-2xl text-center text-dark-500">
                <Database className="w-10 h-10 text-cyan-500 mx-auto mb-2" />
                <span className="text-sm font-semibold">No JPA persistence configurations analyzed</span>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
