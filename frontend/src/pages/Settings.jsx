import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Shield, Save, RefreshCw, Cpu, Database, Settings as SettingsIcon, Sliders, FileText } from 'lucide-react';

export default function Settings() {
    const [settings, setSettings] = useState({
        ai_provider: 'gemini',
        max_upload_size: '52428800',
        workspace_location: 'c:/Users/kavin/OneDrive/Desktop/DevSpec/temp-uploads',
        analysis_timeout: '30',
        logging_level: 'INFO',
        thread_pool_size: '4',
        pdf_header: 'DEVSPEC QUALITY REVIEW',
        pdf_footer: 'Page X | Confidential'
    });
    
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [isAdmin, setIsAdmin] = useState(true); // Default check

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const res = await axios.get('/api/settings', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSettings(res.data);
            setError('');
        } catch (err) {
            console.error('Failed to load settings', err);
            setError('Could not retrieve settings. Make sure you are authenticated.');
        } finally {
            setLoading(false);
        }
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setSettings(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = async (e) => {
        e.preventDefault();
        try {
            setSaving(true);
            setMessage('');
            setError('');
            const token = localStorage.getItem('token');
            await axios.post('/api/settings', settings, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setMessage('System configurations saved successfully.');
            // Clear message after 3 seconds
            setTimeout(() => setMessage(''), 4000);
        } catch (err) {
            console.error('Failed to save settings', err);
            if (err.response && err.response.status === 403) {
                setError('Access Denied: Only users with ADMIN role can alter system settings.');
            } else {
                setError('Failed to update configurations. Check connection or inputs.');
            }
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[80vh]">
                <RefreshCw className="w-8 h-8 text-cyan-400 animate-spin" />
                <span className="ml-3 text-cyan-400 font-medium">Loading system configurations...</span>
            </div>
        );
    }

    return (
        <div className="p-6 max-w-6xl mx-auto text-slate-100">
            {/* Header */}
            <div className="flex items-center justify-between mb-8 pb-4 border-b border-slate-800">
                <div className="flex items-center gap-3">
                    <div className="p-3 bg-cyan-950/50 border border-cyan-800/30 rounded-xl text-cyan-400">
                        <SettingsIcon className="w-6 h-6" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold tracking-tight text-white">System Settings</h1>
                        <p className="text-slate-400 text-sm">Manage AI providers, workspace directory pools, thread schedulers, and PDF reports.</p>
                    </div>
                </div>
                <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-800/40 border border-slate-700/50 rounded-lg text-xs font-semibold text-slate-300">
                    <Shield className="w-3.5 h-3.5 text-cyan-500" />
                    <span>Administrator Mode</span>
                </div>
            </div>

            {/* Alert Messages */}
            {message && (
                <div className="mb-6 p-4 bg-emerald-950/40 border border-emerald-500/30 text-emerald-400 rounded-lg text-sm font-medium animate-pulse">
                    {message}
                </div>
            )}
            {error && (
                <div className="mb-6 p-4 bg-rose-950/40 border border-rose-500/30 text-rose-400 rounded-lg text-sm font-medium">
                    {error}
                </div>
            )}

            <form onSubmit={handleSave} className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    
                    {/* Panel 1: AI Provider Config */}
                    <div className="bg-slate-900/60 backdrop-blur-md border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
                        <div className="flex items-center gap-2.5 pb-3 border-b border-slate-800/80">
                            <Cpu className="w-5 h-5 text-cyan-400" />
                            <h2 className="text-base font-bold text-white">AI Engine Configuration</h2>
                        </div>
                        
                        <div className="space-y-3">
                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Active AI Provider</label>
                                <select 
                                    name="ai_provider"
                                    value={settings.ai_provider}
                                    onChange={handleChange}
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                >
                                    <option value="gemini">Google Gemini Engine</option>
                                    <option value="groq">Groq LLaMA Engine</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    {/* Panel 2: Engine Performance */}
                    <div className="bg-slate-900/60 backdrop-blur-md border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
                        <div className="flex items-center gap-2.5 pb-3 border-b border-slate-800/80">
                            <Sliders className="w-5 h-5 text-cyan-400" />
                            <h2 className="text-base font-bold text-white">Execution & Schedulers</h2>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Thread Pool Size</label>
                                <input 
                                    type="number"
                                    name="thread_pool_size"
                                    value={settings.thread_pool_size}
                                    onChange={handleChange}
                                    min="1"
                                    max="32"
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Analysis Timeout (mins)</label>
                                <input 
                                    type="number"
                                    name="analysis_timeout"
                                    value={settings.analysis_timeout}
                                    onChange={handleChange}
                                    min="5"
                                    max="180"
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                />
                            </div>
                        </div>
                    </div>

                    {/* Panel 3: Workspaces & Upload limits */}
                    <div className="bg-slate-900/60 backdrop-blur-md border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4 md:col-span-2">
                        <div className="flex items-center gap-2.5 pb-3 border-b border-slate-800/80">
                            <Database className="w-5 h-5 text-cyan-400" />
                            <h2 className="text-base font-bold text-white">Workspace & Memory Configuration</h2>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Maximum Zip Upload Size (Bytes)</label>
                                <input 
                                    type="number"
                                    name="max_upload_size"
                                    value={settings.max_upload_size}
                                    onChange={handleChange}
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                />
                                <span className="text-[10px] text-slate-500 mt-1 block">E.g., 52428800 Bytes = 50 MegaBytes (MB)</span>
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Active System Logging Level</label>
                                <select 
                                    name="logging_level"
                                    value={settings.logging_level}
                                    onChange={handleChange}
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                >
                                    <option value="INFO">INFO (Production Standard)</option>
                                    <option value="DEBUG">DEBUG (Detailed Scans)</option>
                                    <option value="WARN">WARN (Warnings Only)</option>
                                    <option value="ERROR">ERROR (System Failures)</option>
                                </select>
                            </div>

                            <div className="md:col-span-2">
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Temporary Workspace Directory</label>
                                <input 
                                    type="text"
                                    name="workspace_location"
                                    value={settings.workspace_location}
                                    onChange={handleChange}
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                />
                            </div>
                        </div>
                    </div>

                    {/* Panel 4: PDF Settings */}
                    <div className="bg-slate-900/60 backdrop-blur-md border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4 md:col-span-2">
                        <div className="flex items-center gap-2.5 pb-3 border-b border-slate-800/80">
                            <FileText className="w-5 h-5 text-cyan-400" />
                            <h2 className="text-base font-bold text-white">PDF Report Document Styling</h2>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Document Running Header</label>
                                <input 
                                    type="text"
                                    name="pdf_header"
                                    value={settings.pdf_header}
                                    onChange={handleChange}
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                />
                            </div>

                            <div>
                                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Document Running Footer</label>
                                <input 
                                    type="text"
                                    name="pdf_footer"
                                    value={settings.pdf_footer}
                                    onChange={handleChange}
                                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none focus:border-cyan-500 transition-colors"
                                />
                                <span className="text-[10px] text-slate-500 mt-1 block">Place 'Page X' to substitute dynamic page numbers.</span>
                            </div>
                        </div>
                    </div>

                </div>

                {/* Save Button */}
                <div className="flex justify-end pt-4">
                    <button
                        type="submit"
                        disabled={saving}
                        className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white rounded-xl font-semibold shadow-lg shadow-cyan-950/20 hover:shadow-cyan-400/10 transition-all active:scale-[0.98] disabled:opacity-50"
                    >
                        {saving ? (
                            <RefreshCw className="w-5 h-5 animate-spin" />
                        ) : (
                            <Save className="w-5 h-5" />
                        )}
                        <span>Save Settings Configuration</span>
                    </button>
                </div>
            </form>
        </div>
    );
}
