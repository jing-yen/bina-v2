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
  { name: 'Indonesia', x: 118.0, y: 2.5, size: 18, downloads: 3200 },
  { name: 'Malaysia', x: 101.7, y: -3.1, size: 14, downloads: 2100 },
  { name: 'Philippines', x: 121.8, y: -12.9, size: 12, downloads: 1400 },
  { name: 'Vietnam', x: 108.0, y: -16.0, size: 10, downloads: 800 },
  { name: 'Thailand', x: 100.5, y: -15.9, size: 9, downloads: 650 },
  { name: 'Myanmar', x: 96.2, y: -19.8, size: 7, downloads: 350 },
  { name: 'Cambodia', x: 104.9, y: -12.6, size: 6, downloads: 200 },
];

const SE_ASIA_PATHS = [
  { name: 'Indonesia', d: 'M141.0,2.6 L141.0,5.9 L141.0,9.1 L140.1,8.3 L139.1,8.1 L138.9,8.4 L137.6,8.4 L138.0,7.6 L138.7,7.3 L138.4,6.2 L137.9,5.4 L136.0,4.5 L135.2,4.5 L133.7,3.5 L133.4,4.0 L133.0,4.1 L132.8,3.7 L132.8,3.3 L132.0,2.8 L133.1,2.5 L133.8,2.5 L133.7,2.2 L132.2,2.2 L131.8,1.6 L130.9,1.4 L130.5,0.9 L131.9,0.7 L132.4,0.4 L134.0,0.8 L134.1,1.2 L134.4,2.8 L135.5,3.4 L136.3,2.3 L137.4,1.7 L138.3,1.7 L139.2,2.1 L139.9,2.4 L141.0,2.6 Z M125.0,8.9 L125.1,9.1 L125.1,9.4 L124.4,10.1 L123.6,10.4 L123.5,10.2 L123.6,9.9 L124.0,9.3 L125.0,8.9 Z M134.2,6.9 L134.1,6.1 L134.3,5.8 L134.5,5.4 L134.7,5.7 L134.7,6.2 L134.2,6.9 Z M117.9,-4.1 L117.3,-3.2 L118.0,-2.3 L117.9,-1.8 L119.0,-0.9 L117.8,-0.8 L117.5,-0.1 L117.5,0.8 L116.6,1.5 L116.5,2.5 L116.1,4.0 L116.0,3.7 L114.9,4.1 L114.5,3.5 L113.8,3.4 L113.3,3.1 L112.1,3.5 L111.7,3.0 L111.0,3.0 L110.2,2.9 L110.1,1.6 L109.6,1.3 L109.1,0.5 L109.0,-0.4 L109.1,-1.3 L109.7,-2.0 L109.8,-1.3 L110.5,-0.8 L111.2,-1.0 L111.8,-0.9 L112.4,-1.4 L112.9,-1.5 L113.8,-1.2 L114.6,-1.4 L115.1,-2.8 L115.5,-3.2 L115.9,-4.3 L117.0,-4.3 L117.9,-4.1 Z M129.4,2.8 L130.5,3.1 L130.8,3.9 L130.0,3.4 L129.2,3.4 L128.6,3.4 L127.9,3.4 L128.1,2.8 L129.4,2.8 Z M126.9,3.8 L126.2,3.6 L126.0,3.2 L127.0,3.1 L127.2,3.5 L126.9,3.8 Z M127.9,-2.2 L128.0,-1.6 L128.6,-1.5 L128.7,-1.1 L128.6,-0.3 L128.1,-0.4 L128.0,0.3 L128.4,0.8 L128.1,0.9 L127.7,0.3 L127.4,-1.0 L127.6,-1.8 L127.9,-2.2 Z M122.9,-0.9 L124.1,-0.9 L125.1,-1.6 L125.2,-1.4 L124.4,-0.4 L123.7,-0.2 L122.7,-0.4 L121.1,-0.4 L120.2,-0.2 L120.0,0.5 L120.9,1.4 L121.5,1.0 L123.3,0.6 L123.3,1.1 L122.8,0.9 L122.4,1.5 L121.5,1.9 L122.5,3.2 L122.3,3.5 L123.2,4.7 L123.2,5.3 L122.6,5.6 L122.2,5.3 L122.7,4.5 L121.7,4.9 L121.5,4.6 L121.6,4.2 L120.9,3.6 L121.0,2.6 L120.3,2.9 L120.4,4.1 L120.4,5.5 L119.8,5.7 L119.4,5.4 L119.7,4.5 L119.5,3.5 L119.1,3.5 L118.8,2.8 L119.2,2.1 L119.3,1.4 L119.8,-0.2 L120.0,-0.6 L120.9,-1.3 L121.7,-1.0 L122.9,-0.9 Z M120.3,10.3 L119.0,9.6 L119.9,9.4 L120.4,9.7 L120.8,10.0 L120.7,10.2 L120.3,10.3 Z M121.3,8.5 L122.0,8.5 L122.9,8.1 L122.8,8.6 L121.3,8.9 L119.9,8.8 L119.9,8.4 L120.7,8.2 L121.3,8.5 Z M118.3,8.4 L118.9,8.3 L119.1,8.7 L118.0,8.9 L117.3,9.0 L116.7,9.0 L117.1,8.5 L117.6,8.4 L117.9,8.1 L118.3,8.4 Z M108.5,6.4 L108.6,6.8 L110.5,6.9 L110.8,6.5 L112.6,6.9 L113.0,7.6 L114.5,7.8 L115.7,8.4 L114.6,8.8 L113.5,8.3 L112.6,8.4 L111.5,8.3 L110.6,8.1 L109.4,7.7 L108.7,7.6 L108.3,7.8 L106.5,7.4 L106.3,6.9 L105.4,6.9 L106.1,5.9 L107.3,6.0 L108.1,6.3 L108.5,6.4 Z M104.4,1.1 L104.5,1.8 L104.9,2.3 L105.6,2.4 L106.1,3.1 L105.9,4.3 L105.8,5.9 L104.7,5.9 L103.9,5.0 L102.6,4.2 L102.2,3.6 L101.4,2.8 L100.9,2.1 L100.1,0.7 L99.3,-0.2 L99.0,-1.0 L98.6,-1.8 L97.7,-2.5 L97.2,-3.3 L96.4,-3.9 L95.4,-5.0 L95.3,-5.5 L95.9,-5.4 L97.5,-5.2 L98.4,-4.3 L99.1,-3.6 L99.7,-3.2 L100.6,-2.1 L101.7,-2.1 L102.5,-1.4 L103.1,-0.6 L103.8,-0.1 L103.4,0.7 L104.0,1.1 L104.4,1.1 Z' },
  { name: 'Timor-Leste', d: 'M125.0,8.9 L125.1,8.7 L125.9,8.4 L126.6,8.4 L127.0,8.3 L127.3,8.4 L127.0,8.7 L125.9,9.1 L125.1,9.4 L125.1,9.1 L125.0,8.9 Z' },
  { name: 'Cambodia', d: 'M102.6,-12.2 L102.3,-13.4 L103.0,-14.2 L104.3,-14.4 L105.2,-14.3 L106.0,-13.9 L106.5,-14.6 L107.4,-14.2 L107.6,-13.5 L107.5,-12.3 L105.8,-11.6 L106.2,-11.0 L105.2,-10.9 L104.3,-10.5 L103.5,-10.6 L103.1,-11.2 L102.6,-12.2 Z' },
  { name: 'Thailand', d: 'M105.2,-14.3 L104.3,-14.4 L103.0,-14.2 L102.3,-13.4 L102.6,-12.2 L101.7,-12.6 L100.8,-12.6 L101.0,-13.4 L100.1,-13.4 L100.0,-12.3 L99.5,-10.8 L99.2,-10.0 L99.2,-9.2 L99.9,-9.2 L100.3,-8.3 L100.5,-7.4 L101.0,-6.9 L101.6,-6.7 L102.1,-6.2 L101.8,-5.8 L101.2,-5.7 L101.1,-6.2 L100.3,-6.6 L100.1,-6.5 L99.7,-6.8 L99.5,-7.3 L99.0,-7.9 L98.5,-8.4 L98.3,-7.8 L98.2,-8.4 L98.3,-9.0 L98.6,-9.9 L99.0,-11.0 L99.6,-11.9 L99.2,-12.8 L99.2,-13.3 L99.1,-13.8 L98.4,-14.6 L98.2,-15.1 L98.5,-15.3 L98.9,-16.2 L98.5,-16.8 L97.9,-17.6 L97.4,-18.4 L97.8,-18.6 L98.3,-19.7 L99.0,-19.8 L99.5,-20.2 L100.1,-20.4 L100.5,-20.1 L100.6,-19.5 L101.3,-19.5 L101.0,-18.4 L101.1,-17.5 L102.1,-18.1 L102.4,-17.9 L103.0,-18.0 L103.2,-18.3 L104.0,-18.2 L104.7,-17.4 L104.8,-16.4 L105.6,-15.6 L105.5,-14.7 L105.2,-14.3 Z' },
  { name: 'Laos', d: 'M107.4,-14.2 L106.5,-14.6 L106.0,-13.9 L105.2,-14.3 L105.5,-14.7 L105.6,-15.6 L104.8,-16.4 L104.7,-17.4 L104.0,-18.2 L103.2,-18.3 L103.0,-18.0 L102.4,-17.9 L102.1,-18.1 L101.1,-17.5 L101.0,-18.4 L101.3,-19.5 L100.6,-19.5 L100.5,-20.1 L100.1,-20.4 L100.3,-20.8 L101.2,-21.4 L101.3,-21.2 L101.8,-21.2 L101.7,-22.3 L102.2,-22.5 L102.8,-21.7 L103.2,-20.8 L104.4,-20.8 L104.8,-19.9 L104.2,-19.6 L103.9,-19.3 L105.1,-18.7 L105.9,-17.5 L106.6,-16.6 L107.3,-15.9 L107.6,-15.2 L107.4,-14.2 Z' },
  { name: 'Myanmar', d: 'M100.1,-20.4 L99.5,-20.2 L99.0,-19.8 L98.3,-19.7 L97.8,-18.6 L97.4,-18.4 L97.9,-17.6 L98.5,-16.8 L98.9,-16.2 L98.5,-15.3 L98.2,-15.1 L98.4,-14.6 L99.1,-13.8 L99.2,-13.3 L99.2,-12.8 L99.6,-11.9 L99.0,-11.0 L98.6,-9.9 L98.5,-10.7 L98.8,-11.4 L98.4,-12.0 L98.5,-13.1 L98.1,-13.6 L97.8,-14.8 L97.6,-16.1 L97.2,-16.9 L96.5,-16.4 L95.4,-15.7 L94.8,-15.8 L94.2,-16.0 L94.5,-17.3 L94.3,-18.2 L93.5,-19.4 L93.7,-19.7 L93.1,-19.9 L92.4,-20.7 L92.3,-21.5 L92.7,-21.3 L92.7,-22.0 L93.2,-22.3 L93.1,-22.7 L93.3,-23.0 L93.3,-24.1 L94.1,-23.9 L94.6,-24.7 L94.6,-25.2 L95.2,-26.0 L95.1,-26.6 L96.4,-27.3 L97.1,-27.1 L97.1,-27.7 L97.4,-27.9 L97.3,-28.3 L97.9,-28.3 L98.2,-27.7 L98.7,-27.5 L98.7,-26.7 L98.7,-25.9 L97.7,-25.1 L97.6,-23.9 L98.7,-24.1 L98.9,-23.1 L99.5,-22.9 L99.2,-22.1 L100.0,-21.7 L100.4,-21.6 L101.2,-21.8 L101.2,-21.4 L100.3,-20.8 L100.1,-20.4 Z' },
  { name: 'Vietnam', d: 'M104.3,-10.5 L105.2,-10.9 L106.2,-11.0 L105.8,-11.6 L107.5,-12.3 L107.6,-13.5 L107.4,-14.2 L107.6,-15.2 L107.3,-15.9 L106.6,-16.6 L105.9,-17.5 L105.1,-18.7 L103.9,-19.3 L104.2,-19.6 L104.8,-19.9 L104.4,-20.8 L103.2,-20.8 L102.8,-21.7 L102.2,-22.5 L102.7,-22.7 L103.5,-22.7 L104.5,-22.8 L105.3,-23.4 L105.8,-23.0 L106.7,-22.8 L106.6,-22.2 L107.0,-21.8 L108.1,-21.6 L106.7,-20.7 L105.9,-19.8 L105.7,-19.1 L106.4,-18.0 L107.4,-16.7 L108.3,-16.1 L108.9,-15.3 L109.3,-13.4 L109.2,-11.7 L108.4,-11.0 L107.2,-10.4 L106.4,-9.5 L105.2,-8.6 L104.8,-9.2 L105.1,-9.9 L104.3,-10.5 Z' },
  { name: 'Philippines', d: 'M120.8,-12.7 L120.3,-13.5 L121.2,-13.4 L121.5,-13.1 L121.3,-12.2 L120.8,-12.7 Z M122.6,-10.0 L122.8,-10.3 L122.9,-10.9 L123.5,-10.9 L123.3,-10.3 L124.1,-11.2 L124.0,-10.3 L123.6,-10.0 L123.3,-9.3 L123.0,-9.0 L122.4,-9.7 L122.6,-10.0 Z M126.4,-8.4 L126.5,-7.8 L126.5,-7.2 L126.2,-6.3 L125.8,-7.3 L125.4,-6.8 L125.7,-6.0 L125.4,-5.6 L124.2,-6.2 L123.9,-6.9 L124.2,-7.4 L123.6,-7.8 L123.3,-7.4 L122.8,-7.5 L122.1,-6.9 L121.9,-7.2 L122.3,-8.0 L122.9,-8.3 L123.5,-8.7 L123.8,-8.2 L124.6,-8.5 L124.8,-9.0 L125.5,-9.0 L125.4,-9.8 L126.2,-9.3 L126.3,-8.8 L126.4,-8.4 Z M118.5,-9.3 L117.2,-8.4 L117.7,-9.1 L118.4,-9.7 L119.0,-10.4 L119.5,-11.4 L119.7,-10.6 L119.0,-10.0 L118.5,-9.3 Z M122.3,-18.2 L122.2,-17.8 L122.5,-17.1 L122.3,-16.3 L121.7,-15.9 L121.5,-15.1 L121.7,-14.3 L122.3,-14.2 L122.7,-14.3 L124.0,-13.8 L123.9,-13.2 L124.2,-13.0 L124.1,-12.5 L123.3,-13.0 L122.9,-13.6 L122.7,-13.2 L122.0,-13.8 L121.1,-13.6 L120.6,-13.9 L120.7,-14.3 L121.0,-14.5 L120.7,-14.8 L120.6,-14.4 L120.1,-15.0 L119.9,-15.4 L119.9,-16.4 L120.3,-16.0 L120.4,-17.6 L120.7,-18.5 L121.3,-18.5 L121.9,-18.2 L122.2,-18.5 L122.3,-18.2 Z M122.0,-11.4 L121.9,-11.9 L122.5,-11.6 L123.1,-11.6 L123.1,-11.2 L122.6,-10.7 L122.0,-10.4 L122.0,-10.9 L122.0,-11.4 Z M125.5,-12.2 L125.8,-11.0 L125.0,-11.3 L125.0,-11.0 L125.3,-10.4 L124.8,-10.1 L124.8,-10.8 L124.5,-10.9 L124.3,-11.5 L124.9,-11.4 L124.9,-11.8 L124.3,-12.6 L125.2,-12.5 L125.5,-12.2 Z' },
  { name: 'Malaysia', d: 'M100.1,-6.5 L100.3,-6.6 L101.1,-6.2 L101.2,-5.7 L101.8,-5.8 L102.1,-6.2 L102.4,-6.1 L103.0,-5.5 L103.4,-4.9 L103.4,-4.2 L103.3,-3.7 L103.4,-3.4 L103.5,-2.8 L103.9,-2.5 L104.2,-1.6 L104.2,-1.3 L103.5,-1.2 L102.6,-2.0 L101.4,-2.8 L101.3,-3.3 L100.7,-3.9 L100.6,-4.8 L100.2,-5.3 L100.3,-6.0 L100.1,-6.5 Z M117.9,-4.1 L117.0,-4.3 L115.9,-4.3 L115.5,-3.2 L115.1,-2.8 L114.6,-1.4 L113.8,-1.2 L112.9,-1.5 L112.4,-1.4 L111.8,-0.9 L111.2,-1.0 L110.5,-0.8 L109.8,-1.3 L109.7,-2.0 L110.4,-1.7 L111.2,-1.9 L111.4,-2.7 L111.8,-2.9 L113.0,-3.1 L113.7,-3.9 L114.2,-4.5 L114.7,-4.0 L114.9,-4.3 L115.3,-4.3 L115.4,-5.0 L115.5,-5.4 L116.2,-6.1 L116.7,-6.9 L117.1,-6.9 L117.6,-6.4 L117.7,-6.0 L118.3,-5.7 L119.2,-5.4 L119.1,-5.0 L118.4,-5.0 L118.6,-4.5 L117.9,-4.1 Z' },
  { name: 'Brunei', d: 'M115.5,-5.4 L115.4,-5.0 L115.3,-4.3 L114.9,-4.3 L114.7,-4.0 L114.2,-4.5 L114.6,-4.9 L115.5,-5.4 Z' },
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
        <div className="relative bg-white rounded-xl overflow-hidden" style={{ height: 260 }}>
          <svg viewBox="90 -30 53 43" className="w-full h-full" preserveAspectRatio="xMidYMid meet">
            {SE_ASIA_PATHS.map(country => (
              <path key={country.name} d={country.d} fill="#E2E8F0" stroke="#CBD5E1" strokeWidth="0.15" />
            ))}
            {HEATMAP_REGIONS.map(region => (
              <g key={region.name}>
                <circle cx={region.x} cy={region.y} r={region.size / 3} fill="#091A7A" opacity={0.15} />
                <circle cx={region.x} cy={region.y} r={region.size / 4.5} fill="#091A7A" opacity={0.35} />
                <circle cx={region.x} cy={region.y} r={region.size / 8} fill="#091A7A" opacity={0.7} />
                <text x={region.x} y={region.y + region.size / 3 + 1.8} textAnchor="middle" fontSize="1.8" fill="#4B5563" fontWeight="500">
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
                  onClick={() => { const name = selectedRecipe.name; setSelectedRecipe(null); navigate('/studio', { state: { recipe: name } }); }}
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
