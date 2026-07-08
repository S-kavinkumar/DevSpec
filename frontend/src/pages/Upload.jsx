import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { 
  Upload as UploadIcon, GitBranch, Terminal, AlertCircle, CheckCircle2, 
  Loader2, Sparkles, XCircle, Clock, FileCode, CheckSquare
} from 'lucide-react';
import API_BASE_URL from "../config/api";

const PIPELINE_STAGES = [
  { key: 'QUEUED', label: 'Queued in Pool' },
  { key: 'VALIDATING', label: 'Validating Integrity' },
  { key: 'EXTRACTING', label: 'Extracting Workspace' },
  { key: 'ANALYZING', label: 'Analyzing Code Structures' },
  { key: 'STATIC_ANALYSIS', label: 'Running Static Analysis' },
  { key: 'RUNNING_TESTS', label: 'Executing Unit Tests' },
  { key: 'GENERATING_AI_REVIEW', label: 'Generating AI Reviews' },
  { key: 'GENERATING_REPORT', label: 'Compiling Final Report' },
  { key: 'COMPLETED', label: 'Completed' },
];

export default function Upload() {
  const [activeTab, setActiveTab] = useState('zip'); // 'zip' or 'git'
  const [file, setFile] = useState(null);
  const [repoUrl, setRepoUrl] = useState('');
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Git reference selector state
  const [branches, setBranches] = useState([]);
  const [tags, setTags] = useState([]);
  const [commits, setCommits] = useState([]);
  const [selectedBranch, setSelectedBranch] = useState('');
  const [selectedTag, setSelectedTag] = useState('');
  const [selectedCommit, setSelectedCommit] = useState('');
  const [fetchingRefs, setFetchingRefs] = useState(false);
  const [refsFetched, setRefsFetched] = useState(false);

  const handleFetchRefs = async () => {
    if (!repoUrl) return;
    setFetchingRefs(true);
    setError('');
    const tokenLocal = localStorage.getItem('token');
    try {
      const branchesRes = await axios.get(`${API_BASE_URL}/api/projects/git/branches`, {
        params: { repoUrl, token },
        headers: { Authorization: `Bearer ${tokenLocal}` }
      });
      setBranches(branchesRes.data);
      
      const tagsRes = await axios.get(`${API_BASE_URL}/api/projects/git/tags`, {
        params: { repoUrl, token },
        headers: { Authorization: `Bearer ${tokenLocal}` }
      });
      setTags(tagsRes.data);

      setRefsFetched(true);

      const mainBranch = branchesRes.data.find(b => b.name === 'main' || b.name === 'master');
      const defaultBranch = mainBranch ? mainBranch.name : (branchesRes.data[0]?.name || '');
      setSelectedBranch(defaultBranch);
      if (defaultBranch) {
        fetchCommits(defaultBranch);
      }
    } catch (err) {
      console.error(err);
      setError('Failed to query remote references. Confirm Repository URL is public or token is correct.');
    } finally {
      setFetchingRefs(false);
    }
  };

  const fetchCommits = async (branchName) => {
    if (!repoUrl || !branchName) return;
    const tokenLocal = localStorage.getItem('token');
    try {
      const res = await axios.get(`${API_BASE_URL}/api/projects/git/commits`, {
        params: { repoUrl, token, branchOrTag: branchName },
        headers: { Authorization: `Bearer ${tokenLocal}` }
      });
      setCommits(res.data);
      setSelectedCommit(res.data[0]?.commitHash || '');
    } catch (err) {
      console.error("Failed to query remote commit log details", err);
    }
  };

  const handleBranchChange = (e) => {
    const val = e.target.value;
    setSelectedBranch(val);
    setSelectedTag(''); 
    fetchCommits(val);
  };

  const handleTagChange = (e) => {
    const val = e.target.value;
    setSelectedTag(val);
    setSelectedBranch(''); 
    setCommits([]);
    setSelectedCommit('');
  };

  // SSE Pipeline state
  const [analysisId, setAnalysisId] = useState(null);
  const [currentStatus, setCurrentStatus] = useState(null);
  const [currentStageLabel, setCurrentStageLabel] = useState('');
  const [percentage, setPercentage] = useState(0);
  const [operation, setOperation] = useState('');
  const [currentFile, setCurrentFile] = useState('');
  const [remainingSeconds, setRemainingSeconds] = useState(null);
  const [pipelineError, setPipelineError] = useState('');
  const [projectId, setProjectId] = useState(null);

  const navigate = useNavigate();

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleZipSubmit = async (e) => {
    e.preventDefault();
    if (!file) return;

    setError('');
    setLoading(true);
    setPipelineError('');
    setCurrentStatus('QUEUED');
    setCurrentStageLabel('Queued in Pool');
    setPercentage(5);

    const formData = new FormData();
    formData.append('file', file);

    const tokenLocal = localStorage.getItem('token');
    try {
      const res = await axios.post(`${API_BASE_URL}/api/projects/upload/zip`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${tokenLocal}`,
        },
      });

      setAnalysisId(res.data.analysisId);
      setProjectId(res.data.projectId);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to initiate zip upload analysis');
      setLoading(false);
      setCurrentStatus(null);
    }
  };

  const handleGitSubmit = async (e) => {
    e.preventDefault();
    if (!repoUrl) return;

    setError('');
    setLoading(true);
    setPipelineError('');
    setCurrentStatus('QUEUED');
    setCurrentStageLabel('Queued in Pool');
    setPercentage(5);

    const tokenLocal = localStorage.getItem('token');
    try {
      const res = await axios.post(
        `${API_BASE_URL}/api/projects/upload/git`,
        { 
          repoUrl, 
          token,
          branch: selectedBranch || undefined,
          tag: selectedTag || undefined,
          commit: selectedCommit || undefined
        },
        {
          headers: {
            Authorization: `Bearer ${tokenLocal}`,
          },
        }
      );

      setAnalysisId(res.data.analysisId);
      setProjectId(res.data.projectId);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.response?.data || 'Failed to clone repository');
      setLoading(false);
      setCurrentStatus(null);
    }
  };

  const handleCancel = async () => {
    if (!analysisId) return;

    const tokenLocal = localStorage.getItem('token');
    try {
      await axios.post(`${API_BASE_URL}/api/projects/analysis/cancel/${analysisId}`, {}, {
        headers: { Authorization: `Bearer ${tokenLocal}` }
      });
      setCurrentStatus('CANCELLED');
      setCurrentStageLabel('Cancelled');
      setPipelineError('Analysis run was terminated by user request.');
    } catch (err) {
      console.error("Failed to cancel run:", err);
    }
  };

  // Real-time EventSource Progress Listener
  useEffect(() => {
    if (!analysisId) return;

    const eventSource = new EventSource(`${API_BASE_URL}/api/projects/progress/${analysisId}`);

    eventSource.addEventListener('progress', (e) => {
      try {
        const data = JSON.parse(e.data);
        const { status, percentage, operation, file, remainingSeconds } = data;
        
        setCurrentStatus(status);
        setPercentage(percentage);
        setOperation(operation);
        setCurrentFile(file || '');
        setRemainingSeconds(remainingSeconds);

        // Find stage label
        const stage = PIPELINE_STAGES.find(s => s.key === status);
        if (stage) {
          setCurrentStageLabel(stage.label);
        }

        if (status === 'COMPLETED') {
          eventSource.close();
          setTimeout(() => {
            navigate(`/report/${projectId}`);
          }, 1500);
        }
      } catch (err) {
        console.error('Failed to parse progress SSE event:', err);
      }
    });

    eventSource.addEventListener('error', (e) => {
      // In SSE, timeout or connection drop triggers reconnect or error. We fetch backup status if it failed
      checkBackupStatus(eventSource);
    });

    return () => {
      eventSource.close();
    };
  }, [analysisId, navigate, projectId]);

  const checkBackupStatus = async (eventSource) => {
    if (!analysisId) return;
    const tokenLocal = localStorage.getItem('token');
    try {
      const res = await axios.get(`${API_BASE_URL}/api/projects/status/analysis/${analysisId}`, {
        headers: { Authorization: `Bearer ${tokenLocal}` }
      });
      const { status, stage, errorMessage } = res.data;
      if (status === 'COMPLETED') {
        eventSource.close();
        navigate(`/report/${projectId}`);
      } else if (status === 'FAILED') {
        eventSource.close();
        setPipelineError(errorMessage || 'Analysis execution failed.');
        setLoading(false);
      } else if (status === 'CANCELLED') {
        eventSource.close();
        setCurrentStatus('CANCELLED');
        setPipelineError('Analysis run was cancelled.');
        setLoading(false);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const getStageIndex = (statusKey) => {
    if (!statusKey) return -1;
    if (statusKey === 'FAILED' || statusKey === 'CANCELLED') return -1;
    return PIPELINE_STAGES.findIndex((s) => s.key === statusKey);
  };

  const activeIndex = getStageIndex(currentStatus);

  return (
    <div className="flex-1 bg-dark-900 p-8 overflow-y-auto flex items-center justify-center">
      <div className="max-w-2xl w-full">
        {!loading && !analysisId ? (
          <div className="glass-panel p-8 rounded-2xl border border-dark-700 relative overflow-hidden">
            <div className="absolute -top-40 -right-40 w-80 h-80 bg-cyan-500/5 rounded-full blur-3xl" />
            <h1 className="text-xl font-bold text-white mb-2 tracking-wide flex items-center space-x-2">
              <Sparkles className="w-5 h-5 text-cyan-400" />
              <span>Review New Project</span>
            </h1>
            <p className="text-sm text-dark-500 mb-6">Select a project type to start quality assessments</p>

            {error && (
              <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center space-x-3 text-rose-500 text-sm">
                <AlertCircle className="w-5 h-5 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Tabs */}
            <div className="flex border-b border-dark-700 mb-6">
              <button
                onClick={() => setActiveTab('zip')}
                className={`py-3 px-6 text-sm font-semibold tracking-wider transition-all border-b-2 flex items-center space-x-2 ${
                  activeTab === 'zip'
                    ? 'border-cyan-500 text-cyan-400'
                    : 'border-transparent text-slate-400 hover:text-white'
                }`}
              >
                <UploadIcon className="w-4 h-4" />
                <span>ZIP Upload</span>
              </button>
              <button
                onClick={() => setActiveTab('git')}
                className={`py-3 px-6 text-sm font-semibold tracking-wider transition-all border-b-2 flex items-center space-x-2 ${
                  activeTab === 'git'
                    ? 'border-cyan-500 text-cyan-400'
                    : 'border-transparent text-slate-400 hover:text-white'
                }`}
              >
                <GitBranch className="w-4 h-4" />
                <span>GitHub URL</span>
              </button>
            </div>

            {/* ZIP form */}
            {activeTab === 'zip' && (
              <form onSubmit={handleZipSubmit} className="space-y-6">
                <div className="border-2 border-dashed border-dark-700 hover:border-cyan-500/50 rounded-xl p-8 flex flex-col items-center justify-center cursor-pointer transition-colors bg-dark-900/40 relative">
                  <input
                    type="file"
                    accept=".zip"
                    onChange={handleFileChange}
                    className="absolute inset-0 opacity-0 cursor-pointer"
                  />
                  <UploadIcon className="w-10 h-10 text-cyan-400 mb-3" />
                  <span className="text-sm text-slate-300 font-semibold mb-1">
                    {file ? file.name : 'Select project ZIP archive'}
                  </span>
                  <span className="text-xs text-dark-500">Maximum size limit: 50MB</span>
                </div>

                <button
                  type="submit"
                  disabled={!file}
                  className="w-full py-3 px-4 rounded-lg bg-cyan-500 hover:bg-cyan-400 disabled:opacity-40 text-dark-900 font-bold text-sm tracking-wide shadow-lg shadow-cyan-500/10 active:scale-95 transition-all"
                >
                  Start Project Analysis
                </button>
              </form>
            )}

            {/* Git form */}
            {activeTab === 'git' && (
              <div className="space-y-5">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300 tracking-wider uppercase block">
                    Repository URL
                  </label>
                  <div className="relative">
                    <GitBranch className="absolute left-3 top-3.5 w-5 h-5 text-dark-500" />
                    <input
                      type="url"
                      required
                      value={repoUrl}
                      onChange={(e) => {
                        setRepoUrl(e.target.value);
                        setRefsFetched(false);
                      }}
                      className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-3 pl-10 pr-4 text-sm text-white placeholder-dark-500 transition-colors"
                      placeholder="https://github.com/username/project"
                    />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300 tracking-wider uppercase block">
                    Personal Access Token <span className="text-[10px] text-dark-500 font-normal">(Optional)</span>
                  </label>
                  <div className="relative">
                    <Terminal className="absolute left-3 top-3.5 w-5 h-5 text-dark-500" />
                    <input
                      type="password"
                      value={token}
                      onChange={(e) => {
                        setToken(e.target.value);
                        setRefsFetched(false);
                      }}
                      className="w-full bg-dark-900 border border-dark-700 hover:border-dark-600 focus:border-cyan-500 focus:outline-none rounded-lg py-3 pl-10 pr-4 text-sm text-white placeholder-dark-500 transition-colors"
                      placeholder="ghp_xxxxxxxxxxxxxxxx"
                    />
                  </div>
                </div>

                {!refsFetched ? (
                  <button
                    type="button"
                    onClick={handleFetchRefs}
                    disabled={!repoUrl || fetchingRefs}
                    className="w-full py-3 px-4 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-cyan-400 border border-cyan-800/30 font-bold text-sm tracking-wide active:scale-95 transition-all flex items-center justify-center gap-2"
                  >
                    {fetchingRefs && <Loader2 className="w-4 h-4 animate-spin" />}
                    <span>{fetchingRefs ? 'Querying Repository References...' : 'Fetch Repository Details'}</span>
                  </button>
                ) : (
                  <form onSubmit={handleGitSubmit} className="space-y-4 pt-2 border-t border-dark-800 animate-fadeIn">
                    <div className="grid grid-cols-2 gap-4">
                      {/* Branch Selection */}
                      <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-400 tracking-wider uppercase block">
                          Checkout Branch
                        </label>
                        <select
                          value={selectedBranch}
                          onChange={handleBranchChange}
                          className="w-full bg-dark-900 border border-dark-700 focus:border-cyan-500 focus:outline-none rounded-lg py-2.5 px-3 text-sm text-slate-200"
                        >
                          <option value="">-- Choose Branch --</option>
                          {branches.map(b => (
                            <option key={b.name} value={b.name}>{b.name}</option>
                          ))}
                        </select>
                      </div>

                      {/* Tag Selection */}
                      <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-400 tracking-wider uppercase block">
                          Or Release Tag
                        </label>
                        <select
                          value={selectedTag}
                          onChange={handleTagChange}
                          className="w-full bg-dark-900 border border-dark-700 focus:border-cyan-500 focus:outline-none rounded-lg py-2.5 px-3 text-sm text-slate-200"
                        >
                          <option value="">-- Choose Tag --</option>
                          {tags.map(t => (
                            <option key={t.name} value={t.name}>{t.name}</option>
                          ))}
                        </select>
                      </div>
                    </div>

                    {/* Commit Selection */}
                    {selectedBranch && commits.length > 0 && (
                      <div className="space-y-1">
                        <label className="text-xs font-semibold text-slate-400 tracking-wider uppercase block">
                          Select Commit Hash
                        </label>
                        <select
                          value={selectedCommit}
                          onChange={(e) => setSelectedCommit(e.target.value)}
                          className="w-full bg-dark-900 border border-dark-700 focus:border-cyan-500 focus:outline-none rounded-lg py-2.5 px-3 text-sm text-slate-200"
                        >
                          {commits.map(c => (
                            <option key={c.commitHash} value={c.commitHash}>
                              [{c.commitHash.substring(0, 8)}] {c.message} ({c.author})
                            </option>
                          ))}
                        </select>
                      </div>
                    )}

                    <button
                      type="submit"
                      className="w-full py-3 px-4 rounded-lg bg-cyan-500 hover:bg-cyan-400 text-dark-900 font-bold text-sm tracking-wide shadow-lg shadow-cyan-500/10 active:scale-95 transition-all"
                    >
                      Clone & Analyze Project
                    </button>
                  </form>
                )}
              </div>
            )}
          </div>
        ) : (
          /* Execution Pipeline Progress Card */
          <div className="glass-panel p-8 rounded-2xl border border-dark-700">
            <div className="flex items-center justify-between mb-4 border-b border-dark-700 pb-4">
              <div>
                <h1 className="text-lg font-bold text-white tracking-wide">Analysis Engine Active</h1>
                <p className="text-xs text-dark-500 mt-1">Status: {currentStatus} — {currentStageLabel}</p>
              </div>
              {currentStatus !== 'COMPLETED' && currentStatus !== 'FAILED' && currentStatus !== 'CANCELLED' && (
                <button
                  onClick={handleCancel}
                  className="flex items-center space-x-1.5 px-3 py-1.5 rounded bg-rose-500/10 hover:bg-rose-500/20 text-rose-500 font-bold text-xs border border-rose-500/20 active:scale-95 transition-all"
                >
                  <XCircle className="w-4 h-4" />
                  <span>Cancel Audit</span>
                </button>
              )}
            </div>

            {/* Estimated time and details */}
            {!pipelineError && currentStatus !== 'COMPLETED' && (
              <div className="grid grid-cols-2 gap-4 mb-6 text-xs text-slate-300">
                <div className="bg-dark-900 p-3 rounded-lg border border-dark-700/50 flex items-center space-x-2.5">
                  <Clock className="w-4.5 h-4.5 text-cyan-400" />
                  <div>
                    <span className="block text-[10px] text-dark-500 uppercase tracking-widest font-bold">Est. Remaining Time</span>
                    <span className="font-semibold text-white">{remainingSeconds ? `${remainingSeconds}s` : 'Calculating...'}</span>
                  </div>
                </div>
                <div className="bg-dark-900 p-3 rounded-lg border border-dark-700/50 flex items-center space-x-2.5">
                  <Loader2 className="w-4.5 h-4.5 text-cyan-400 animate-spin" />
                  <div>
                    <span className="block text-[10px] text-dark-500 uppercase tracking-widest font-bold">Active Operation</span>
                    <span className="font-semibold text-white truncate max-w-[160px] block">{operation || 'Starting metrics...'}</span>
                  </div>
                </div>
              </div>
            )}

            {/* ProgressBar */}
            {!pipelineError && currentStatus !== 'COMPLETED' && (
              <div className="mb-6 space-y-1">
                <div className="flex justify-between text-[10px] font-bold text-cyan-400 uppercase tracking-wider">
                  <span>Progress Scale</span>
                  <span>{percentage}%</span>
                </div>
                <div className="w-full bg-dark-900 rounded-full h-2.5 overflow-hidden border border-dark-700">
                  <div 
                    className="bg-cyan-500 h-2.5 rounded-full transition-all duration-500 shadow-md shadow-cyan-500/30"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
                {currentFile && (
                  <div className="flex items-center space-x-1.5 text-[10px] text-dark-500 mt-2 font-mono truncate">
                    <FileCode className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Analyzing: {currentFile}</span>
                  </div>
                )}
              </div>
            )}

            {pipelineError && (
              <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex flex-col text-rose-500 text-sm">
                <div className="flex items-center space-x-2 font-bold mb-1">
                  <AlertCircle className="w-5 h-5" />
                  <span>Pipeline Execution Stopped</span>
                </div>
                <p className="text-xs ml-7 leading-relaxed">{pipelineError}</p>
                <button
                  onClick={() => {
                    setLoading(false);
                    setAnalysisId(null);
                    setCurrentStatus(null);
                    setPercentage(0);
                    setOperation('');
                    setCurrentFile('');
                    setRemainingSeconds(null);
                  }}
                  className="mt-4 self-start px-4 py-2 rounded bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition-all shadow-md active:scale-95"
                >
                  Go Back
                </button>
              </div>
            )}

            {/* Stages Timeline */}
            <div className="space-y-4">
              {PIPELINE_STAGES.map((stage, idx) => {
                const isPassed = activeIndex > idx;
                const isActive = activeIndex === idx;
                const isFailed = (pipelineError || currentStatus === 'CANCELLED') && activeIndex === idx;

                return (
                  <div key={stage.key} className="flex items-center space-x-4">
                    <div className="flex flex-col items-center">
                      <div
                        className={`w-6 h-6 rounded-full flex items-center justify-center border transition-all ${
                          isPassed
                            ? 'bg-emerald-500/10 border-emerald-500 text-emerald-400'
                            : isActive
                            ? 'bg-cyan-500/10 border-cyan-500 text-cyan-400 ring-2 ring-cyan-500/20 animate-pulse'
                            : isFailed
                            ? 'bg-rose-500/10 border-rose-500 text-rose-400'
                            : 'bg-dark-900 border-dark-700 text-dark-500'
                        }`}
                      >
                        {isPassed ? (
                          <CheckCircle2 className="w-4 h-4" />
                        ) : isActive ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        ) : isFailed ? (
                          <AlertCircle className="w-4 h-4" />
                        ) : (
                          <span className="text-[10px] font-bold">{idx + 1}</span>
                        )}
                      </div>
                      {idx < PIPELINE_STAGES.length - 1 && (
                        <div
                          className={`w-[2px] h-6 transition-all ${
                            isPassed ? 'bg-emerald-500/30' : 'bg-dark-700'
                          }`}
                        />
                      )}
                    </div>
                    <span
                      className={`text-xs font-medium ${
                        isPassed
                          ? 'text-slate-400 font-normal line-through'
                          : isActive
                          ? 'text-cyan-400 font-bold text-sm glow-text'
                          : isFailed
                          ? 'text-rose-500 font-bold'
                          : 'text-dark-500'
                      }`}
                    >
                      {stage.label}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
