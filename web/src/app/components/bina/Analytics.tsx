import { useState, useEffect } from 'react';
import { TrendingUp, Download, Globe, Users, BarChart3, Award, Star, MessageSquare, AlertCircle, RefreshCw } from 'lucide-react';
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadData = () => {
    setLoading(true);
    setError(false);
    Promise.all([
      fetchPlatformStats().then(setStats),
      fetchRegionCounts().then(setRegions),
    ])
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const formatCount = (n: number) => n >= 1000 ? `${(n / 1000).toFixed(1)}K` : String(n);
  const totalRegionCount = regions.reduce((a, r) => a + r.count, 0) || 1;

  if (error) {
    return (
      <div className="p-8 max-w-6xl mx-auto">
        <div className="flex flex-col items-center justify-center py-24 gap-4">
          <div className="w-14 h-14 rounded-2xl flex items-center justify-center bg-red-50">
            <AlertCircle size={28} className="text-red-500" />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-stone-700">Could not load analytics</p>
            <p className="text-xs text-stone-500 mt-1">Check your connection and try again.</p>
          </div>
          <button onClick={loadData} className="flex items-center gap-2 px-4 py-2 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: '#C45A3A' }}>
            <RefreshCw size={14} /> Retry
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="p-8 max-w-6xl mx-auto">
        <div className="mb-8">
          <div className="h-7 w-48 bg-stone-200 rounded animate-pulse" />
          <div className="h-4 w-64 bg-stone-100 rounded animate-pulse mt-2" />
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="bg-white rounded-xl border border-stone-200 p-5 shadow-card animate-pulse">
              <div className="w-10 h-10 rounded-lg bg-stone-100 mb-3" />
              <div className="h-7 w-16 bg-stone-200 rounded" />
              <div className="h-4 w-24 bg-stone-100 rounded mt-2" />
            </div>
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {[1, 2].map(i => (
            <div key={i} className="bg-white rounded-xl border border-stone-200 p-6 shadow-card animate-pulse">
              <div className="h-5 w-36 bg-stone-200 rounded mb-4" />
              <div className="h-52 bg-stone-100 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-stone-900">Analytics Dashboard</h1>
        <p className="text-sm text-stone-500 mt-1">Track your published recipes' performance</p>
      </div>

      {/* Stats grid - 4 columns */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {/* Total Downloads */}
        <div className="rounded-xl border p-5 shadow-card hover:shadow-interactive transition-all duration-200 hover:-translate-y-0.5" style={{ background: '#1A8A6A0C', borderColor: '#1A8A6A20' }}>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: '#1A8A6A18' }}>
              <Download size={20} style={{ color: '#1A8A6A' }} />
            </div>
          </div>
          <p className="text-3xl font-bold text-stone-900">{stats ? formatCount(stats.totalDownloads) : '—'}</p>
          <p className="text-sm text-stone-500 mt-1">Total Downloads</p>
          <div className="flex items-center gap-1 mt-2">
            <TrendingUp size={14} style={{ color: '#1A8A6A' }} />
            <span className="text-xs font-medium text-stone-500">All published recipes</span>
          </div>
        </div>

        {/* Average Rating */}
        <div className="rounded-xl border p-5 shadow-card hover:shadow-interactive transition-all duration-200 hover:-translate-y-0.5" style={{ background: '#C98A1A0C', borderColor: '#C98A1A20' }}>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: '#C98A1A18' }}>
              <Award size={20} style={{ color: '#C98A1A' }} />
            </div>
          </div>
          <p className="text-3xl font-bold text-stone-900">{stats && stats.avgRating > 0 ? stats.avgRating.toFixed(1) : '—'}</p>
          <p className="text-sm text-stone-500 mt-1">Average Rating</p>
          <div className="flex gap-0.5 mt-2">
            {[1, 2, 3, 4, 5].map(s => (
              <Star key={s} size={14} className={s <= Math.round(stats?.avgRating || 0) ? 'text-amber-400 fill-amber-400' : 'text-stone-200'} />
            ))}
          </div>
        </div>

        {/* Active Users */}
        <div className="rounded-xl border p-5 shadow-card hover:shadow-interactive transition-all duration-200 hover:-translate-y-0.5" style={{ background: '#C45A3A0C', borderColor: '#C45A3A20' }}>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: '#C45A3A18' }}>
              <Users size={20} style={{ color: '#C45A3A' }} />
            </div>
          </div>
          <p className="text-3xl font-bold text-stone-900">{stats ? formatCount(stats.uniqueDevices) : '—'}</p>
          <p className="text-sm text-stone-500 mt-1">Active Users</p>
          <div className="flex items-center gap-1 mt-2">
            <span className="text-xs font-medium text-stone-500">Unique devices</span>
          </div>
        </div>

        {/* Regions */}
        <div className="rounded-xl border p-5 shadow-card hover:shadow-interactive transition-all duration-200 hover:-translate-y-0.5" style={{ background: '#5B6ABF0C', borderColor: '#5B6ABF20' }}>
          <div className="flex items-center gap-2 mb-3">
            <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: '#5B6ABF18' }}>
              <Globe size={20} style={{ color: '#5B6ABF' }} />
            </div>
          </div>
          <p className="text-3xl font-bold text-stone-900">{stats ? stats.countriesReached : '—'}</p>
          <p className="text-sm text-stone-500 mt-1">Countries Reached</p>
          <p className="text-xs font-medium mt-2" style={{ color: '#5B6ABF' }}>Southeast Asia focused</p>
        </div>
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Download Trends */}
        <div className="bg-white rounded-xl border border-stone-200 p-6 shadow-card">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-base font-semibold text-stone-900">Download Trends</h3>
            <BarChart3 size={18} className="text-stone-400" />
          </div>
          <div className="h-52">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={regions.slice(0, 6).map(r => ({ name: COUNTRY_NAMES[r.countryCode] || r.countryCode, downloads: r.count }))}>
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#57534E', fontSize: 11 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#57534E', fontSize: 12 }} />
                <Bar dataKey="downloads" radius={[6, 6, 0, 0]}>
                  {regions.slice(0, 6).map((_entry, index) => (
                    <Cell key={`cell-${index}`} fill={['#C45A3A', '#1A8A6A', '#5B6ABF', '#C98A1A', '#BE3554', '#78350F'][index % 6]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Regional Distribution */}
        <div className="bg-white rounded-xl border border-stone-200 p-6 shadow-card">
          <h3 className="text-base font-semibold text-stone-900 mb-4">Regional Distribution</h3>
          <div className="space-y-4">
            {(regions.length > 0 ? regions.slice(0, 6) : [{ countryCode: '—', count: 0 }]).map((rc, idx) => {
              const pct = Math.round((rc.count / totalRegionCount) * 100);
              const name = COUNTRY_NAMES[rc.countryCode] || rc.countryCode;
              const barColor = ['#C45A3A', '#1A8A6A', '#5B6ABF', '#C98A1A', '#BE3554', '#78350F'][idx % 6];
              return (
                <div key={rc.countryCode}>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-sm font-medium text-stone-800">{name}</span>
                    <span className="text-xs text-stone-500">{rc.count.toLocaleString()} downloads</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="flex-1 h-2.5 rounded-full bg-stone-100 overflow-hidden">
                      <div className="h-full rounded-full" style={{ background: barColor, width: `${pct}%` }} />
                    </div>
                    <span className="text-xs font-semibold min-w-[32px] text-right" style={{ color: barColor }}>{pct}%</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* User feedback cards */}
      <div className="mb-8">
        <h3 className="text-base font-semibold text-stone-900 mb-4">User Feedback</h3>
        <div className="bg-white rounded-xl border border-stone-200 shadow-card divide-y divide-stone-100 overflow-hidden">
          {FEEDBACK.map((fb, i) => (
            <div key={i} className="flex items-start gap-4 p-5 hover:bg-stone-50/50 transition-colors">
              <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style={{ background: '#C45A3A15' }}>
                <MessageSquare size={15} style={{ color: '#C45A3A' }} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-1">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-stone-900">{fb.user}</p>
                    <p className="text-[11px] text-stone-400">{fb.location}</p>
                  </div>
                  <div className="flex gap-0.5">
                    {[1, 2, 3, 4, 5].map(s => (
                      <Star key={s} size={12} className={s <= fb.rating ? 'text-amber-400 fill-amber-400' : 'text-stone-200'} />
                    ))}
                  </div>
                </div>
                <p className="text-sm text-stone-600 leading-relaxed">{fb.text}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Achievement badge */}
      <div className="rounded-xl border p-6 shadow-card" style={{ background: '#C45A3A08', borderColor: '#C45A3A20' }}>
        <div className="flex gap-4 items-center">
          <div className="w-14 h-14 rounded-xl flex items-center justify-center shrink-0" style={{ background: '#C45A3A' }}>
            <Award size={28} className="text-white" />
          </div>
          <div>
            <h4 className="text-base font-semibold text-stone-900 mb-1">Top Contributor</h4>
            <p className="text-sm text-stone-600">
              Your recipes have reached over 20K users across Southeast Asia. Thank you for empowering grassroots communities!
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
