import { useState, useEffect } from 'react';
import { TrendingUp, Download, Globe, Users, Award, Star, AlertCircle, RefreshCw, BarChart3 } from 'lucide-react';
import { AreaChart, Area, XAxis, YAxis, ResponsiveContainer, Tooltip } from 'recharts';
import {
  fetchPlatformStats, fetchRegionCounts, fetchRecipeAnalytics, fetchGrowthData,
  type PlatformStats, type RegionCount, type RecipeAnalytics, type GrowthPoint,
} from '../../lib/analyticsService';

const COUNTRY_NAMES: Record<string, string> = {
  ID: 'Indonesia', MY: 'Malaysia', PH: 'Philippines', VN: 'Vietnam',
  TH: 'Thailand', MM: 'Myanmar', KH: 'Cambodia', LA: 'Laos',
  SG: 'Singapore', BN: 'Brunei', TL: 'Timor-Leste',
};

const COUNTRY_FLAGS: Record<string, string> = {
  MY: '\u{1F1F2}\u{1F1FE}', ID: '\u{1F1EE}\u{1F1E9}', TH: '\u{1F1F9}\u{1F1ED}',
  PH: '\u{1F1F5}\u{1F1ED}', VN: '\u{1F1FB}\u{1F1F3}', SG: '\u{1F1F8}\u{1F1EC}',
  KH: '\u{1F1F0}\u{1F1ED}', MM: '\u{1F1F2}\u{1F1F2}', LA: '\u{1F1F1}\u{1F1E6}',
};

const FEATURE_FEEDBACK = [
  {
    screen: 'Symptom Checker', recipe: 'Bidan Pintar', uses: 8420, rating: 4.9,
    feedback: [
      { text: 'Accurate risk detection — saved us a hospital referral trip', country: 'MY', user: 'Siti R.' },
      { text: 'My patients trust the AI recommendations now', country: 'ID', user: 'Dewi A.' },
    ],
  },
  {
    screen: 'Prevention Tips', recipe: 'Cegah Denggi', uses: 2180, rating: 4.7,
    feedback: [
      { text: 'Great visuals for community education sessions', country: 'MY', user: 'Aminah K.' },
      { text: 'Helped reduce dengue cases in our kampung', country: 'MY', user: 'Razak M.' },
    ],
  },
  {
    screen: 'Risk Assessment', recipe: 'Triage Ibu Hamil', uses: 1540, rating: 4.8,
    feedback: [
      { text: 'Simple enough for volunteer health workers', country: 'ID', user: 'Maria L.' },
      { text: 'Caught a high-risk pregnancy early — mother is safe', country: 'TH', user: 'Nguyen T.' },
    ],
  },
];

export function Analytics() {
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [regions, setRegions] = useState<RegionCount[]>([]);
  const [topRecipes, setTopRecipes] = useState<RecipeAnalytics[]>([]);
  const [growthData, setGrowthData] = useState<GrowthPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadData = () => {
    setLoading(true);
    setError(false);
    Promise.all([
      fetchPlatformStats().then(setStats),
      fetchRegionCounts().then(setRegions),
      fetchRecipeAnalytics().then(data => setTopRecipes(data.slice(0, 5))),
      fetchGrowthData().then(setGrowthData),
    ])
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const formatCount = (n: number) => n >= 1000 ? `${(n / 1000).toFixed(1)}K` : String(n);
  const maxRecipeDl = topRecipes[0]?.downloads || 1;

  if (error) {
    return (
      <div className="p-6 max-w-6xl mx-auto h-[100dvh] overflow-y-auto">
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
      <div className="p-6 max-w-6xl mx-auto h-[100dvh] overflow-y-auto">
        <div className="mb-6"><div className="h-6 w-48 bg-stone-200 rounded animate-pulse" /></div>
        <div className="grid grid-cols-4 gap-3 mb-6">
          {[1, 2, 3, 4].map(i => <div key={i} className="bg-white rounded-xl border border-stone-200 px-4 py-3 shadow-card animate-pulse"><div className="h-5 w-16 bg-stone-200 rounded" /></div>)}
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-6xl mx-auto h-[100dvh] overflow-y-auto">
      {/* Top Contributor banner */}
      <div className="rounded-xl border px-5 py-4 mb-5 flex items-center gap-4" style={{ background: '#C45A3A08', borderColor: '#C45A3A20' }}>
        <div className="w-11 h-11 rounded-xl flex items-center justify-center shrink-0" style={{ background: '#C45A3A' }}>
          <Award size={22} className="text-white" />
        </div>
        <div className="flex-1">
          <h4 className="text-sm font-semibold text-stone-900">Top Contributor — Southeast Asia Health</h4>
          <p className="text-xs text-stone-500">Your recipes have reached over 21K healthcare workers across 10 countries this month</p>
        </div>
        <div className="flex gap-1 shrink-0">
          {[1, 2, 3, 4, 5].map(s => <Star key={s} size={14} className="text-amber-400 fill-amber-400" />)}
        </div>
      </div>

      {/* Stat cards — compact single row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-5">
        {[
          { icon: Download, label: 'Total Downloads', value: stats ? formatCount(stats.totalDownloads) : '—', color: '#1A8A6A', trend: '+24%' },
          { icon: Award, label: 'Avg Rating', value: stats && stats.avgRating > 0 ? stats.avgRating.toFixed(1) : '—', color: '#C98A1A', trend: '+0.2' },
          { icon: Users, label: 'Active Users', value: stats ? formatCount(stats.uniqueDevices) : '—', color: '#C45A3A', trend: '+18%' },
          { icon: Globe, label: 'Countries', value: stats ? String(stats.countriesReached) : '—', color: '#5B6ABF', trend: null },
        ].map((stat, i) => {
          const Icon = stat.icon;
          return (
            <div key={i} className="bg-white rounded-xl border border-stone-200 shadow-card px-3 py-2.5 flex items-center gap-3">
              <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0" style={{ background: stat.color + '15' }}>
                <Icon size={16} style={{ color: stat.color }} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-xl font-bold text-stone-900 leading-none">{stat.value}</span>
                  {stat.trend && (
                    <span className="inline-flex items-center gap-0.5 text-[11px] font-medium" style={{ color: stat.color }}>
                      <TrendingUp size={10} /> {stat.trend}
                    </span>
                  )}
                </div>
                <span className="text-[13px] text-stone-500 truncate">{stat.label}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Row 1: Growth chart + Top Recipes */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        {/* Growth trajectory */}
        <div className="bg-white rounded-xl border border-stone-200 p-4 shadow-card">
          {(() => {
            const firstHalf = growthData.slice(0, 15).reduce((s, p) => s + p.downloads, 0);
            const secondHalf = growthData.slice(15).reduce((s, p) => s + p.downloads, 0);
            const growthPct = firstHalf > 0
              ? Math.round(((secondHalf - firstHalf) / firstHalf) * 100)
              : null;
            return (
              <div className="flex items-center justify-between mb-3">
                <div>
                  <h3 className="text-sm font-semibold text-stone-900">Download Activity</h3>
                  <p className="text-[11px] text-stone-400">Last 30 days</p>
                </div>
                {growthPct !== null && (
                  <div className="flex items-center gap-1 px-2 py-1 rounded-md" style={{ background: '#1A8A6A12' }}>
                    <TrendingUp size={12} style={{ color: '#1A8A6A' }} />
                    <span className="text-[11px] font-semibold" style={{ color: '#1A8A6A' }}>{growthPct >= 0 ? '+' : ''}{growthPct}%</span>
                  </div>
                )}
              </div>
            );
          })()}
          <div className="h-44">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={growthData}>
                <defs>
                  <linearGradient id="growthGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#C45A3A" stopOpacity={0.3} />
                    <stop offset="100%" stopColor="#C45A3A" stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fill: '#A8A29E', fontSize: 10 }} interval={6} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#A8A29E', fontSize: 10 }} width={30} />
                <Tooltip
                  contentStyle={{ background: '#1C1917', border: 'none', borderRadius: 8, fontSize: 11, color: '#FAF8F5' }}
                  labelFormatter={(_, payload) => payload?.[0]?.payload?.label ?? ''}
                  formatter={(v: number) => [`${v} downloads`, '']}
                />
                <Area type="monotone" dataKey="downloads" stroke="#C45A3A" strokeWidth={2} fill="url(#growthGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Top Recipes leaderboard */}
        <div className="bg-white rounded-xl border border-stone-200 p-4 shadow-card">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-semibold text-stone-900">Top Recipes</h3>
            <BarChart3 size={14} className="text-stone-400" />
          </div>
          <div className="space-y-3">
            {topRecipes.length === 0 ? (
              <p className="text-xs text-stone-400 py-4 text-center">No recipes yet</p>
            ) : topRecipes.map((r, i) => (
              <div key={r.recipeId} className="flex items-center gap-2.5 rounded-lg px-2 py-1" style={{ background: '#C45A3A08' }}>
                <span className="text-xs font-bold w-5 text-center" style={{ color: '#C45A3A' }}>#{i + 1}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-0.5">
                    <span className="text-sm font-medium text-stone-800 truncate">{r.recipeName}</span>
                    <span className="text-xs text-stone-500 shrink-0 ml-2 font-medium">{formatCount(r.downloads)}</span>
                  </div>
                  <div className="h-2 rounded-full bg-stone-100 overflow-hidden">
                    <div className="h-full rounded-full transition-all" style={{
                      width: `${(r.downloads / maxRecipeDl) * 100}%`,
                      background: '#C45A3A',
                    }} />
                  </div>
                </div>
                <span className="inline-flex items-center gap-0.5 text-xs font-semibold text-stone-600 shrink-0">
                  <Star size={11} className="text-amber-400 fill-amber-400" />
                  {r.rating > 0 ? r.rating.toFixed(1) : '—'}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Row 2: Regional reach + Most useful features */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
        {/* Regional reach — flag + country bars */}
        <div className="bg-white rounded-xl border border-stone-200 p-4 shadow-card">
          <h3 className="text-sm font-semibold text-stone-900 mb-3">Regional Reach</h3>
          <div className="space-y-2.5">
            {(() => { const seaRegions = regions.filter(rc => rc.countryCode in COUNTRY_NAMES).slice(0, 5); return seaRegions.map((rc, idx) => {
              const pct = Math.round((rc.count / (seaRegions[0]?.count || 1)) * 100);
              const flag = COUNTRY_FLAGS[rc.countryCode] || '';
              const name = COUNTRY_NAMES[rc.countryCode] || rc.countryCode;
              const barColor = ['#C45A3A', '#1A8A6A', '#5B6ABF', '#C98A1A', '#BE3554'][idx % 5];
              return (
                <div key={rc.countryCode} className="flex items-center gap-2.5">
                  <span className="text-base shrink-0">{flag}</span>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-0.5">
                      <span className="text-xs font-medium text-stone-700">{name}</span>
                      <span className="text-xs text-stone-500">{rc.count}</span>
                    </div>
                    <div className="h-2 rounded-full bg-stone-100 overflow-hidden">
                      <div className="h-full rounded-full" style={{ background: barColor, width: `${pct}%` }} />
                    </div>
                  </div>
                </div>
              );
            }); })()}
          </div>
        </div>

        {/* Most useful features — 3 features, 2 feedback each */}
        <div className="bg-white rounded-xl border border-stone-200 p-4 shadow-card">
          <h3 className="text-sm font-semibold text-stone-900 mb-3">Most Useful Features</h3>
          <div className="space-y-3">
            {FEATURE_FEEDBACK.map((sf, i) => (
              <div key={i} className="py-1">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs font-bold" style={{ color: '#C45A3A' }}>#{i + 1}</span>
                  <span className="text-sm font-semibold text-stone-800">{sf.screen}</span>
                  <span className="text-xs text-stone-400">{sf.recipe}</span>
                  <span className="ml-auto inline-flex items-center gap-0.5 text-xs font-medium text-stone-600 shrink-0">
                    <Star size={11} className="text-amber-400 fill-amber-400" /> {sf.rating}
                  </span>
                  <span className="text-xs text-stone-400 shrink-0">{formatCount(sf.uses)} uses</span>
                </div>
                <div className="pl-5 space-y-1">
                  {sf.feedback.map((fb, j) => (
                    <p key={j} className="text-xs text-stone-500 leading-snug">
                      {COUNTRY_FLAGS[fb.country] || ''} <span className="font-medium text-stone-600">{fb.user}</span>: &ldquo;{fb.text}&rdquo;
                    </p>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
