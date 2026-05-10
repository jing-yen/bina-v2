import { useState } from 'react';
import { useNavigate } from 'react-router';
import { FileText, Users, Download, Star, Plus, ChevronRight, BadgeCheck, X } from 'lucide-react';

const STATS = [
  { label: 'Total Recipes', value: '4', icon: FileText, color: '#091A7A' },
  { label: 'Active Users', value: '21.3K', icon: Users, color: '#3B82F6' },
  { label: 'Downloads', value: '8.7K', icon: Download, color: '#10B981' },
  { label: 'Avg Rating', value: '4.8', icon: Star, color: '#F59E0B' },
];

const MOCK_RECIPES = [
  { name: 'Farm Buddy', icon: '\u{1F33E}', category: 'Agriculture', downloads: '8.7K', users: '12.4K', rating: 4.8, updated: '2 days ago', description: 'AI assistant for smallholder farmers with crop disease detection and fertiliser guidance.', screens: 5, languages: ['en', 'ms', 'id'] },
  { name: 'Health Assistant', icon: '\u{1F3E5}', category: 'Health', downloads: '3.2K', users: '5.1K', rating: 4.6, updated: '5 days ago', description: 'Prenatal care guidance and health screening for rural communities.', screens: 3, languages: ['en', 'id', 'tl'] },
  { name: 'Flood Triage', icon: '\u{1F6A8}', category: 'Emergency', downloads: '1.1K', users: '2.3K', rating: 4.9, updated: '1 week ago', description: 'Emergency flood response and resource allocation for disaster teams.', screens: 4, languages: ['en', 'ms', 'th'] },
  { name: 'Math Tutor', icon: '\u{1F4DA}', category: 'Education', downloads: '950', users: '1.8K', rating: 4.5, updated: '3 days ago', description: 'Interactive math lessons with voice input for primary school students.', screens: 2, languages: ['en', 'ta'] },
];

const HEATMAP_REGIONS = [
  { name: 'Indonesia', x: 108, y: 20, size: 18, downloads: 3200 },
  { name: 'Malaysia', x: 101, y: 15, size: 14, downloads: 2100 },
  { name: 'Philippines', x: 120, y: 8, size: 12, downloads: 1400 },
  { name: 'Vietnam', x: 107, y: 6, size: 10, downloads: 800 },
  { name: 'Thailand', x: 102, y: 7, size: 9, downloads: 650 },
  { name: 'Myanmar', x: 97, y: 4, size: 7, downloads: 350 },
  { name: 'Cambodia', x: 104, y: 9, size: 6, downloads: 200 },
];

interface RecipeDetail {
  name: string;
  icon: string;
  category: string;
  downloads: string;
  users: string;
  rating: number;
  updated: string;
  description: string;
  screens: number;
  languages: string[];
}

export function Dashboard() {
  const navigate = useNavigate();
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeDetail | null>(null);

  return (
    <div className="p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Welcome back, Jing Yen</h1>
          <div className="flex items-center gap-2 mt-1.5">
            <span className="text-lg">{'\u{1F33E}'}</span>
            <span className="text-sm text-gray-600">Ministry of Agriculture, Malaysia</span>
            <BadgeCheck size={16} className="text-blue-500" />
            <span className="text-[10px] font-semibold text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">Verified</span>
          </div>
        </div>
        <button
          onClick={() => navigate('/studio')}
          className="flex items-center gap-2 px-5 py-2.5 rounded-lg text-white text-sm font-medium transition-colors hover:opacity-90"
          style={{ background: '#091A7A' }}
        >
          <Plus size={18} />
          Create New Recipe
        </button>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {STATS.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="bg-white rounded-xl border border-gray-100 p-5 shadow-sm">
              <div className="flex items-center justify-between mb-3">
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center"
                  style={{ background: stat.color + '15' }}
                >
                  <Icon size={20} style={{ color: stat.color }} />
                </div>
              </div>
              <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
              <p className="text-sm text-gray-500 mt-1">{stat.label}</p>
            </div>
          );
        })}
      </div>

      {/* Download heatmap */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6 mb-8">
        <h2 className="text-lg font-semibold text-gray-900 mb-1">Download Heatmap</h2>
        <p className="text-xs text-gray-500 mb-4">Where your recipes are being used across Southeast Asia</p>
        <div className="relative bg-gray-50 rounded-xl overflow-hidden" style={{ height: 260 }}>
          {/* SE Asia map with heatmap overlay */}
          <svg viewBox="90 -15 55 45" className="w-full h-full" preserveAspectRatio="xMidYMid meet">
            {/* Simplified SE Asia country outlines */}
            {/* Myanmar */}
            <path d="M96,0 L98,-2 L100,-1 L101,2 L100,5 L99,8 L97,10 L96,8 L95,5 L96,2 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Thailand */}
            <path d="M100,2 L103,1 L104,4 L103,7 L102,10 L101,13 L100,11 L99,8 L100,5 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Laos */}
            <path d="M103,1 L105,0 L106,2 L105,5 L104,7 L103,5 L103,3 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Vietnam */}
            <path d="M106,2 L108,1 L109,4 L108,7 L107,10 L106,13 L105,10 L105,7 L105,5 L106,3 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Cambodia */}
            <path d="M103,7 L106,7 L106,10 L104,11 L102,10 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Malaysia (Peninsular) */}
            <path d="M100,13 L102,12 L102,16 L101,18 L100,16 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Malaysia (Borneo) + Brunei */}
            <path d="M108,14 L114,13 L116,15 L114,17 L110,17 L108,16 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Indonesia (Sumatra) */}
            <path d="M96,14 L100,12 L101,16 L99,20 L96,22 L94,19 L95,16 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Indonesia (Java) */}
            <path d="M103,20 L110,19 L112,20 L110,22 L105,22 L103,21 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Indonesia (Sulawesi) */}
            <path d="M117,17 L119,15 L121,17 L120,20 L118,21 L116,19 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Indonesia (Kalimantan) */}
            <path d="M110,17 L114,17 L116,19 L115,22 L112,23 L110,21 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            {/* Philippines */}
            <path d="M118,2 L120,1 L121,4 L120,7 L118,6 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            <path d="M117,7 L120,7 L121,10 L119,11 L117,9 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            <path d="M119,11 L121,10 L122,13 L120,14 L118,12 Z" fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />

            {/* Heatmap dots */}
            {HEATMAP_REGIONS.map(region => (
              <g key={region.name}>
                <circle cx={region.x} cy={region.y} r={region.size / 3} fill="#091A7A" opacity={0.12} />
                <circle cx={region.x} cy={region.y} r={region.size / 4.5} fill="#091A7A" opacity={0.3} />
                <circle cx={region.x} cy={region.y} r={region.size / 8} fill="#091A7A" opacity={0.65} />
                <text x={region.x} y={region.y + region.size / 3 + 1.5} textAnchor="middle" fontSize="1.5" fill="#4B5563" fontWeight="500">
                  {region.name}
                </text>
              </g>
            ))}
          </svg>
          {/* Legend */}
          <div className="absolute bottom-3 right-3 flex items-center gap-3 bg-white/90 rounded-lg px-3 py-1.5">
            <div className="flex items-center gap-1">
              <div className="w-2 h-2 rounded-full bg-[#091A7A] opacity-30" />
              <span className="text-[10px] text-gray-500">Low</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full bg-[#091A7A] opacity-50" />
              <span className="text-[10px] text-gray-500">Medium</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded-full bg-[#091A7A] opacity-70" />
              <span className="text-[10px] text-gray-500">High</span>
            </div>
          </div>
        </div>
      </div>

      {/* Recipes table */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm">
        <div className="px-6 py-4 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900">Your Recipes</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-50">
                <th className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider px-6 py-3">Recipe</th>
                <th className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider px-6 py-3">Category</th>
                <th className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider px-6 py-3">Downloads</th>
                <th className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider px-6 py-3">Users</th>
                <th className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider px-6 py-3">Rating</th>
                <th className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider px-6 py-3">Updated</th>
                <th className="w-10"></th>
              </tr>
            </thead>
            <tbody>
              {MOCK_RECIPES.map((recipe) => (
                <tr
                  key={recipe.name}
                  onClick={() => setSelectedRecipe(recipe)}
                  className="border-b border-gray-50 last:border-0 hover:bg-gray-50/50 transition-colors cursor-pointer"
                >
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <span className="text-xl">{recipe.icon}</span>
                      <span className="text-sm font-medium text-gray-900">{recipe.name}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="text-xs font-medium px-2.5 py-1 rounded-full bg-gray-100 text-gray-600">
                      {recipe.category}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600">{recipe.downloads}</td>
                  <td className="px-6 py-4 text-sm text-gray-600">{recipe.users}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1">
                      <Star size={14} className="text-amber-400 fill-amber-400" />
                      <span className="text-sm font-medium text-gray-900">{recipe.rating}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">{recipe.updated}</td>
                  <td className="px-6 py-3">
                    <ChevronRight size={16} className="text-gray-300" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recipe detail slide-over */}
      {selectedRecipe && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-black/20" onClick={() => setSelectedRecipe(null)} />
          <div className="relative w-[480px] bg-white shadow-2xl overflow-y-auto animate-in slide-in-from-right">
            <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{selectedRecipe.icon}</span>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">{selectedRecipe.name}</h3>
                  <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-gray-100 text-gray-600">{selectedRecipe.category}</span>
                </div>
              </div>
              <button onClick={() => setSelectedRecipe(null)} className="text-gray-400 hover:text-gray-600">
                <X size={20} />
              </button>
            </div>
            <div className="p-6 space-y-6">
              <p className="text-sm text-gray-600 leading-relaxed">{selectedRecipe.description}</p>

              <div className="grid grid-cols-3 gap-4">
                <div className="text-center p-3 rounded-xl bg-gray-50">
                  <p className="text-lg font-bold text-gray-900">{selectedRecipe.downloads}</p>
                  <p className="text-[11px] text-gray-500">Downloads</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-gray-50">
                  <p className="text-lg font-bold text-gray-900">{selectedRecipe.users}</p>
                  <p className="text-[11px] text-gray-500">Active Users</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-gray-50">
                  <div className="flex items-center justify-center gap-1">
                    <Star size={14} className="text-amber-400 fill-amber-400" />
                    <p className="text-lg font-bold text-gray-900">{selectedRecipe.rating}</p>
                  </div>
                  <p className="text-[11px] text-gray-500">Rating</p>
                </div>
              </div>

              <div>
                <h4 className="text-sm font-medium text-gray-700 mb-2">Details</h4>
                <div className="space-y-2">
                  <div className="flex justify-between py-2 border-b border-gray-50">
                    <span className="text-sm text-gray-500">Screens</span>
                    <span className="text-sm font-medium text-gray-900">{selectedRecipe.screens}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-gray-50">
                    <span className="text-sm text-gray-500">Languages</span>
                    <span className="text-sm font-medium text-gray-900">{selectedRecipe.languages.join(', ').toUpperCase()}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-gray-50">
                    <span className="text-sm text-gray-500">Last Updated</span>
                    <span className="text-sm font-medium text-gray-900">{selectedRecipe.updated}</span>
                  </div>
                </div>
              </div>

              {/* Mini heatmap for this recipe */}
              <div>
                <h4 className="text-sm font-medium text-gray-700 mb-2">Download Distribution</h4>
                <div className="space-y-2">
                  {[
                    { region: 'Indonesia', pct: 42 },
                    { region: 'Malaysia', pct: 28 },
                    { region: 'Philippines', pct: 18 },
                    { region: 'Others', pct: 12 },
                  ].map(r => (
                    <div key={r.region} className="flex items-center gap-3">
                      <span className="text-xs text-gray-600 w-24">{r.region}</span>
                      <div className="flex-1 h-2 rounded-full bg-gray-100 overflow-hidden">
                        <div className="h-full rounded-full" style={{ background: '#091A7A', width: `${r.pct}%` }} />
                      </div>
                      <span className="text-xs font-medium text-gray-700 w-8 text-right">{r.pct}%</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={() => { setSelectedRecipe(null); navigate('/studio'); }}
                  className="flex-1 py-2.5 rounded-lg text-white text-sm font-medium"
                  style={{ background: '#091A7A' }}
                >
                  Edit Recipe
                </button>
                <button
                  onClick={() => { setSelectedRecipe(null); navigate('/analytics'); }}
                  className="flex-1 py-2.5 rounded-lg border border-gray-200 text-sm font-medium text-gray-700 hover:bg-gray-50"
                >
                  View Analytics
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
