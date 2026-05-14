import { useState, useEffect } from 'react';
import { TrendingUp, Download, Globe, Users, BarChart3, Award, Star, MessageSquare } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Cell } from 'recharts';
import { fetchPlatformStats, fetchRegionCounts, type PlatformStats, type RegionCount } from '../../lib/analyticsService';

const COUNTRY_NAMES: Record<string, string> = {
  ID: 'Indonesia', MY: 'Malaysia', PH: 'Philippines', VN: 'Vietnam',
  TH: 'Thailand', MM: 'Myanmar', KH: 'Cambodia', LA: 'Laos',
  SG: 'Singapore', BN: 'Brunei', TL: 'Timor-Leste',
};

const FEEDBACK = [
  { user: 'Siti R.', location: 'East Java', text: 'Farm Buddy helped me identify crop disease early. Saved my harvest!', rating: 5 },
  { user: 'Maria L.', location: 'Cebu', text: 'The offline mode is amazing. Works even in our remote barangay.', rating: 5 },
  { user: 'Nguyen T.', location: 'Da Nang', text: 'Very useful for daily farming decisions. Voice input is great.', rating: 4 },
];

export function Analytics() {
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [regions, setRegions] = useState<RegionCount[]>([]);

  useEffect(() => {
    fetchPlatformStats().then(setStats).catch(() => {});
    fetchRegionCounts().then(setRegions).catch(() => {});
  }, []);

  const formatCount = (n: number) => n >= 1000 ? `${(n / 1000).toFixed(1)}K` : String(n);
  const totalRegionCount = regions.reduce((a, r) => a + r.count, 0) || 1;

  return (
    <div className="p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Analytics Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">Track your published recipes' performance</p>
      </div>

      {/* Stats grid - 4 columns */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {/* Total Downloads */}
        <div className="bg-white rounded-xl border border-gray-100 p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ background: '#10B98115' }}>
              <Download size={20} className="text-green-600" />
            </div>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats ? formatCount(stats.totalDownloads) : '—'}</p>
          <p className="text-sm text-gray-500 mt-1">Total Downloads</p>
          <div className="flex items-center gap-1 mt-2">
            <TrendingUp size={14} className="text-green-600" />
            <span className="text-xs font-semibold text-green-600">Live from Firestore</span>
          </div>
        </div>

        {/* Average Rating */}
        <div className="bg-white rounded-xl border border-gray-100 p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ background: '#F59E0B15' }}>
              <Award size={20} className="text-amber-500" />
            </div>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats && stats.avgRating > 0 ? stats.avgRating.toFixed(1) : '—'}</p>
          <p className="text-sm text-gray-500 mt-1">Average Rating</p>
          <div className="flex gap-0.5 mt-2">
            {[1, 2, 3, 4, 5].map(s => (
              <Star key={s} size={14} className="text-amber-400 fill-amber-400" />
            ))}
          </div>
        </div>

        {/* Active Users */}
        <div className="bg-white rounded-xl border border-gray-100 p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ background: '#091A7A15' }}>
              <Users size={20} style={{ color: '#091A7A' }} />
            </div>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats ? formatCount(stats.uniqueDevices) : '—'}</p>
          <p className="text-sm text-gray-500 mt-1">Active Users</p>
          <div className="flex items-center gap-1 mt-2">
            <TrendingUp size={14} className="text-green-600" />
            <span className="text-xs font-semibold text-green-600">Unique devices</span>
          </div>
        </div>

        {/* Regions */}
        <div className="bg-white rounded-xl border border-gray-100 p-5 shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-lg flex items-center justify-center" style={{ background: '#3B82F615' }}>
              <Globe size={20} className="text-blue-500" />
            </div>
          </div>
          <p className="text-2xl font-bold text-gray-900">{stats ? stats.countriesReached : '—'}</p>
          <p className="text-sm text-gray-500 mt-1">Countries Reached</p>
          <p className="text-xs text-blue-500 mt-2">Southeast Asia focused</p>
        </div>
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-2 gap-6 mb-8">
        {/* Download Trends */}
        <div className="bg-white rounded-xl border border-gray-100 p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-semibold text-gray-900">Download Trends</h3>
            <BarChart3 size={18} className="text-gray-400" />
          </div>
          <div className="h-52">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={regions.slice(0, 6).map(r => ({ name: COUNTRY_NAMES[r.countryCode] || r.countryCode, downloads: r.count }))}>
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 11 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontSize: 12 }} />
                <Bar dataKey="downloads" radius={[6, 6, 0, 0]}>
                  {regions.slice(0, 6).map((_entry, index) => (
                    <Cell key={`cell-${index}`} fill="#091A7A" />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Regional Distribution */}
        <div className="bg-white rounded-xl border border-gray-100 p-6 shadow-sm">
          <h3 className="text-base font-semibold text-gray-900 mb-4">Regional Distribution</h3>
          <div className="space-y-4">
            {(regions.length > 0 ? regions.slice(0, 6) : [{ countryCode: '—', count: 0 }]).map((rc) => {
              const pct = Math.round((rc.count / totalRegionCount) * 100);
              const name = COUNTRY_NAMES[rc.countryCode] || rc.countryCode;
              return (
                <div key={rc.countryCode}>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-sm font-medium text-gray-800">{name}</span>
                    <span className="text-xs text-gray-500">{rc.count.toLocaleString()} downloads</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex-1 h-2 rounded-full bg-gray-100 overflow-hidden">
                      <div className="h-full rounded-full" style={{ background: '#091A7A', width: `${pct}%` }} />
                    </div>
                    <span className="text-xs font-semibold text-gray-700 min-w-[32px] text-right">{pct}%</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* User feedback cards */}
      <div className="mb-8">
        <h3 className="text-base font-semibold text-gray-900 mb-4">User Feedback</h3>
        <div className="grid grid-cols-3 gap-4">
          {FEEDBACK.map((fb, i) => (
            <div key={i} className="bg-white rounded-xl border border-gray-100 p-5 shadow-sm">
              <div className="flex items-center gap-2 mb-3">
                <div className="w-8 h-8 rounded-full flex items-center justify-center" style={{ background: '#091A7A' }}>
                  <MessageSquare size={14} className="text-white" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-gray-900">{fb.user}</p>
                  <p className="text-xs text-gray-400">{fb.location}</p>
                </div>
              </div>
              <p className="text-sm text-gray-600 mb-3 leading-relaxed">{fb.text}</p>
              <div className="flex gap-0.5">
                {[1, 2, 3, 4, 5].map(s => (
                  <Star key={s} size={12} className={s <= fb.rating ? 'text-amber-400 fill-amber-400' : 'text-gray-200'} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Achievement badge */}
      <div className="rounded-xl border border-amber-100 p-6 shadow-sm" style={{ background: '#FFFBEB' }}>
        <div className="flex gap-4 items-center">
          <div className="w-14 h-14 rounded-xl flex items-center justify-center bg-amber-400 shrink-0">
            <Award size={28} className="text-white" />
          </div>
          <div>
            <h4 className="text-base font-semibold text-gray-900 mb-1">Top Contributor</h4>
            <p className="text-sm text-gray-600">
              Your recipes have reached over 20K users across Southeast Asia. Thank you for empowering grassroots communities!
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
