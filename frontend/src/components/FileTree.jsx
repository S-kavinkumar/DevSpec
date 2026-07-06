import React, { useState } from 'react';
import { Folder, FolderOpen, FileCode, ChevronRight, ChevronDown } from 'lucide-react';

function TreeNode({ node, depth = 0 }) {
  const [isOpen, setIsOpen] = useState(depth === 0); // Root node is open by default
  const isDir = node.type === 'dir';

  const handleToggle = (e) => {
    e.stopPropagation();
    if (isDir) {
      setIsOpen(!isOpen);
    }
  };

  return (
    <div className="select-none text-xs">
      <div 
        onClick={handleToggle}
        className={`flex items-center space-x-2 py-1 px-2 rounded hover:bg-dark-800/40 cursor-pointer transition-colors ${
          isDir ? 'text-slate-200' : 'text-slate-400 font-mono'
        }`}
        style={{ paddingLeft: `${depth * 14 + 8}px` }}
      >
        {isDir ? (
          <>
            <div className="text-dark-500">
              {isOpen ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
            </div>
            {isOpen ? <FolderOpen className="w-4 h-4 text-cyan-400" /> : <Folder className="w-4 h-4 text-cyan-400" />}
            <span className="font-semibold">{node.name}</span>
          </>
        ) : (
          <>
            <FileCode className="w-4 h-4 text-slate-500" />
            <span>{node.name}</span>
          </>
        )}
      </div>

      {isDir && isOpen && node.children && node.children.length > 0 && (
        <div className="mt-0.5 border-l border-dark-700/50 ml-[15px]">
          {node.children
            .sort((a, b) => {
              // Sort directories first, then files alphabetically
              if (a.type !== b.type) return a.type === 'dir' ? -1 : 1;
              return a.name.compareToIgnoreCase ? a.name.compareToIgnoreCase(b.name) : a.name.localeCompare(b.name);
            })
            .map((child, index) => (
              <TreeNode key={index} node={child} depth={depth + 1} />
            ))}
        </div>
      )}
    </div>
  );
}

export default function FileTree({ treeData }) {
  if (!treeData) {
    return <div className="text-dark-500 text-xs italic">Loading directory layout...</div>;
  }

  // If root is a dummy wrapper containing children, render children directly
  if (treeData.name === 'root' && treeData.children) {
    return (
      <div className="space-y-1 p-2 bg-dark-900/50 border border-dark-700/50 rounded-xl max-h-[400px] overflow-y-auto">
        {treeData.children.map((child, idx) => (
          <TreeNode key={idx} node={child} depth={0} />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-1 p-2 bg-dark-900/50 border border-dark-700/50 rounded-xl max-h-[400px] overflow-y-auto">
      <TreeNode node={treeData} depth={0} />
    </div>
  );
}
