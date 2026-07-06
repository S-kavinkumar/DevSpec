import React from 'react';

export default function ScoreGauge({ score, label, size = 120 }) {
  const percentage = Math.min(100, Math.max(0, score || 0));
  const radius = size * 0.4;
  const strokeWidth = size * 0.08;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

  // Determine color based on score thresholds
  let strokeColor = '#f43f5e'; // Rose (Red) < 60
  let glowColor = 'rgba(244, 63, 94, 0.4)';

  if (percentage >= 80) {
    strokeColor = '#10b981'; // Emerald (Green)
    glowColor = 'rgba(16, 185, 129, 0.4)';
  } else if (percentage >= 60) {
    strokeColor = '#f59e0b'; // Amber (Yellow)
    glowColor = 'rgba(245, 158, 11, 0.4)';
  }

  return (
    <div className="flex flex-col items-center justify-center p-4">
      <div className="relative" style={{ width: size, height: size }}>
        <svg className="transform -rotate-90 w-full h-full">
          {/* Background circle */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            className="stroke-dark-700"
            strokeWidth={strokeWidth}
            fill="transparent"
          />
          {/* Progress circle */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={strokeColor}
            strokeWidth={strokeWidth}
            fill="transparent"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            style={{
              filter: `drop-shadow(0 0 6px ${strokeColor})`,
              transition: 'stroke-dashoffset 0.8s ease-in-out',
            }}
          />
        </svg>
        {/* Center label */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
          <span className="text-2xl font-bold tracking-tight text-white">{Math.round(percentage)}</span>
          <span className="text-xs text-dark-500 font-medium">/ 100</span>
        </div>
      </div>
      {label && <span className="mt-3 text-sm font-semibold tracking-wide text-slate-300 uppercase">{label}</span>}
    </div>
  );
}
