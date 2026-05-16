import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { FileText, Users, Download, Star, Plus, ChevronRight, BadgeCheck, X, Copy, Trash2, Loader2, Sparkles, TrendingUp, TrendingDown } from 'lucide-react';
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel,
  AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
  AlertDialogHeader, AlertDialogTitle,
} from '../ui/alert-dialog';
import type { RecipeWithId } from './recipes';
import { listRecipes, deleteRecipe, duplicateRecipe, createRecipe } from '../../lib/recipeService';
import { fetchPlatformStats, fetchRegionCounts, seedMockPings, type PlatformStats, type RegionCount } from '../../lib/analyticsService';
import { RECIPES } from './recipes';

const HEATMAP_REGION_COORDS: Record<string, { x: number; y: number; name: string }> = {
  ID: { name: 'Indonesia', x: 118.0, y: 2.5 },
  MY: { name: 'Malaysia', x: 101.7, y: -3.1 },
  PH: { name: 'Philippines', x: 121.8, y: -12.9 },
  VN: { name: 'Vietnam', x: 108.0, y: -16.0 },
  TH: { name: 'Thailand', x: 100.5, y: -15.9 },
  MM: { name: 'Myanmar', x: 96.2, y: -19.8 },
  KH: { name: 'Cambodia', x: 104.9, y: -12.6 },
  LA: { name: 'Laos', x: 102.5, y: -18.0 },
  SG: { name: 'Singapore', x: 103.8, y: -1.4 },
  BN: { name: 'Brunei', x: 114.9, y: -4.9 },
};

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

const CATEGORY_COLORS: Record<string, { bg: string; text: string }> = {
  Agriculture: { bg: '#1A8A6A18', text: '#1A8A6A' },
  Health: { bg: '#C45A3A18', text: '#C45A3A' },
  Education: { bg: '#5B6ABF18', text: '#5B6ABF' },
  Emergency: { bg: '#BE355418', text: '#BE3554' },
  Finance: { bg: '#C98A1A18', text: '#C98A1A' },
  Environment: { bg: '#1A8A6A18', text: '#1A8A6A' },
};

function relativeTime(date: Date): string {
  const now = Date.now();
  const diff = now - date.getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  const weeks = Math.floor(days / 7);
  if (weeks < 5) return `${weeks}w ago`;
  return date.toLocaleDateString();
}

export function Dashboard() {
  const navigate = useNavigate();
  const [recipes, setRecipes] = useState<RecipeWithId[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRecipe, setSelectedRecipe] = useState<RecipeWithId | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [seeding, setSeeding] = useState(false);
  const [platformStats, setPlatformStats] = useState<PlatformStats | null>(null);
  const [regionCounts, setRegionCounts] = useState<RegionCount[]>([]);

  const fetchRecipes = () => {
    setLoading(true);
    listRecipes()
      .then(setRecipes)
      .catch(() => toast.error('Failed to load recipes'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchRecipes(); }, []);
  useEffect(() => {
    seedMockPings().then(() => {
      fetchPlatformStats().then(setPlatformStats).catch(() => {});
      fetchRegionCounts().then(setRegionCounts).catch(() => {});
    }).catch(() => {
      fetchPlatformStats().then(setPlatformStats).catch(() => {});
      fetchRegionCounts().then(setRegionCounts).catch(() => {});
    });
  }, []);

  const handleDelete = async () => {
    if (!selectedRecipe) return;
    try {
      await deleteRecipe(selectedRecipe.id);
      toast.success('Recipe deleted');
      setSelectedRecipe(null);
      setShowDeleteConfirm(false);
      fetchRecipes();
    } catch { toast.error('Failed to delete'); }
  };

  const handleDuplicate = async () => {
    if (!selectedRecipe) return;
    try {
      await duplicateRecipe(selectedRecipe.id);
      toast.success('Recipe duplicated');
      fetchRecipes();
    } catch { toast.error('Failed to duplicate'); }
  };

  const handleSeedDemo = async () => {
    setSeeding(true);
    try {
      for (const recipe of Object.values(RECIPES)) {
        await createRecipe(recipe);
      }
      toast.success('Demo recipe imported');
      fetchRecipes();
    } catch { toast.error('Failed to import'); }
    finally { setSeeding(false); }
  };

  const formatCount = (n: number) => n >= 1000 ? `${(n / 1000).toFixed(1)}K` : String(n);
  const stats = [
    { label: 'Total Recipes', value: String(recipes.length), icon: FileText, color: '#C45A3A', tint: '#C45A3A0C', trend: '+2', up: true },
    { label: 'Active Users', value: platformStats ? formatCount(platformStats.uniqueDevices) : '—', icon: Users, color: '#5B6ABF', tint: '#5B6ABF0C', trend: '+18%', up: true },
    { label: 'Downloads', value: platformStats ? formatCount(platformStats.totalDownloads) : '—', icon: Download, color: '#1A8A6A', tint: '#1A8A6A0C', trend: '+24%', up: true },
    { label: 'Avg Rating', value: platformStats ? (platformStats.avgRating > 0 ? platformStats.avgRating.toFixed(1) : '—') : '—', icon: Star, color: '#C98A1A', tint: '#C98A1A0C', trend: '+0.2', up: true },
  ];

  return (
    <div className="p-6 max-w-[1400px] mx-auto h-[calc(100dvh)] overflow-hidden flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-3xl font-bold text-stone-900">Welcome back, Jing Yen</h1>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-lg">{'\u{1F33E}'}</span>
            <span className="text-sm text-stone-600">Ministry of Agriculture, Malaysia</span>
            <BadgeCheck size={16} style={{ color: '#C45A3A' }} />
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded" style={{ color: '#C45A3A', background: '#C45A3A10' }}>Verified</span>
          </div>
        </div>
        <button
          onClick={() => navigate('/studio')}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-white text-sm font-medium shadow-card hover:shadow-interactive transition-all duration-200"
          style={{ background: '#C45A3A' }}
        >
          <Plus size={16} />
          Create New Recipe
        </button>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-4 gap-3 mb-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="rounded-xl border px-4 py-2.5 shadow-card flex items-center gap-3" style={{ background: stat.tint, borderColor: stat.color + '20' }}>
              <div
                className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
                style={{ background: stat.color + '18' }}
              >
                <Icon size={16} style={{ color: stat.color }} />
              </div>
              <div className="flex items-baseline gap-1.5 min-w-0">
                <span className="text-xl font-bold text-stone-900 leading-none">{stat.value}</span>
                <span className="text-[13px] text-stone-500 truncate">{stat.label}</span>
              </div>
              <div className={`ml-auto flex items-center gap-0.5 shrink-0 text-[11px] font-semibold ${stat.up ? 'text-emerald-600' : 'text-red-500'}`}>
                {stat.up ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                {stat.trend}
              </div>
            </div>
          );
        })}
      </div>

      {/* Heatmap + Recipes side by side */}
      <div className="grid grid-cols-2 gap-4 flex-1 min-h-0">
        {/* Download heatmap */}
        <div className="rounded-xl border border-stone-200 shadow-card overflow-hidden flex flex-col bg-white">
          <div className="px-5 pt-4 pb-2" style={{ background: 'linear-gradient(135deg, #5B6ABF08 0%, #1A8A6A06 100%)' }}>
            <h2 className="text-sm font-semibold text-stone-900">Download Heatmap</h2>
            <p className="text-[11px] text-stone-500">Recipe usage across Southeast Asia</p>
          </div>
          <div className="px-4 flex-1 min-h-0">
            <div className="relative bg-white rounded-xl overflow-hidden h-full">
              <svg viewBox="90 -30 53 43" className="w-full h-full" preserveAspectRatio="xMidYMid meet">
                {SE_ASIA_PATHS.map(country => (
                  <path key={country.name} d={country.d} fill="#E7E0D8" stroke="#D6D3D1" strokeWidth="0.15" />
                ))}
                {(() => {
                  const maxCount = Math.max(1, ...regionCounts.map(r => r.count));
                  return regionCounts.map(rc => {
                    const coords = HEATMAP_REGION_COORDS[rc.countryCode];
                    if (!coords) return null;
                    const size = 6 + (rc.count / maxCount) * 14;
                    return (
                      <g key={rc.countryCode}>
                        <circle cx={coords.x} cy={coords.y} r={size / 3} fill="#C45A3A" opacity={0.15} />
                        <circle cx={coords.x} cy={coords.y} r={size / 4.5} fill="#C45A3A" opacity={0.35} />
                        <circle cx={coords.x} cy={coords.y} r={size / 8} fill="#C45A3A" opacity={0.7} />
                        <text x={coords.x} y={coords.y + size / 3 + 1.2} textAnchor="middle" fontSize="1.1" fill="#44403C" fontWeight="500">
                          {coords.name} ({rc.count})
                        </text>
                      </g>
                    );
                  });
                })()}
              </svg>
            </div>
          </div>
          {/* Legend below the map */}
          <div className="flex items-center justify-center gap-4 px-4 py-2 border-t border-stone-100">
            <div className="flex items-center gap-1">
              <div className="w-2 h-2 rounded-full bg-[#C45A3A] opacity-30" />
              <span className="text-[10px] text-stone-500">Low</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full bg-[#C45A3A] opacity-50" />
              <span className="text-[10px] text-stone-500">Medium</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded-full bg-[#C45A3A] opacity-70" />
              <span className="text-[10px] text-stone-500">High</span>
            </div>
          </div>
        </div>

        {/* Recipes table */}
        <div className="bg-white rounded-xl border border-stone-200 shadow-card flex flex-col overflow-hidden">
          <div className="px-5 py-3 border-b border-stone-200 shrink-0">
            <h2 className="text-sm font-semibold text-stone-900">Your Recipes</h2>
          </div>

          <div className="flex-1 overflow-y-auto min-h-0">
        {loading ? (
          <div className="divide-y divide-stone-100">
            {[1, 2, 3].map(i => (
              <div key={i} className="px-5 py-3 flex items-center gap-3 animate-pulse">
                <div className="w-7 h-7 rounded-lg bg-stone-100" />
                <div className="flex-1">
                  <div className="h-3.5 w-28 bg-stone-200 rounded" />
                  <div className="h-2.5 w-16 bg-stone-100 rounded mt-1" />
                </div>
              </div>
            ))}
          </div>
        ) : recipes.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 gap-3">
            <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ background: '#C45A3A10' }}>
              <FileText size={22} style={{ color: '#C45A3A' }} />
            </div>
            <div className="text-center">
              <p className="text-sm font-medium text-stone-700">No recipes yet</p>
              <p className="text-xs text-stone-500 mt-0.5">Create or import demo recipes</p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => navigate('/studio')}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-white text-xs font-medium hover:opacity-90"
                style={{ background: '#C45A3A' }}
              >
                <Plus size={14} /> Create
              </button>
              <button
                onClick={handleSeedDemo}
                disabled={seeding}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-stone-200 text-xs font-medium text-stone-700 hover:bg-stone-50"
              >
                {seeding ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />}
                Import Demo
              </button>
            </div>
          </div>
        ) : (
          <table className="w-full">
            <thead className="sticky top-0 bg-white">
              <tr className="border-b border-stone-100">
                <th className="text-left text-[11px] font-medium text-stone-500 uppercase tracking-wider px-5 py-2">Recipe</th>
                <th className="text-left text-[11px] font-medium text-stone-500 uppercase tracking-wider px-3 py-2">Category</th>
                <th className="text-left text-[11px] font-medium text-stone-500 uppercase tracking-wider px-3 py-2">Languages</th>
                <th className="text-right text-[11px] font-medium text-stone-500 uppercase tracking-wider px-3 py-2">Users</th>
                <th className="text-right text-[11px] font-medium text-stone-500 uppercase tracking-wider px-3 py-2">Downloads</th>
                <th className="text-right text-[11px] font-medium text-stone-500 uppercase tracking-wider px-3 py-2">Rating</th>
                <th className="w-6"></th>
              </tr>
            </thead>
            <tbody>
              {recipes.map((recipe) => {
                const dl = parseInt(recipe.stats?.downloads || '0') || 0;
                const users = parseInt(recipe.stats?.users || '0') || 0;
                const rating = recipe.stats?.rating || 0;
                return (
                <tr
                  key={recipe.id}
                  onClick={() => setSelectedRecipe(recipe)}
                  className="border-b border-stone-100 last:border-0 hover:bg-stone-50 transition-colors cursor-pointer group"
                >
                  <td className="px-5 py-2">
                    <div className="flex items-center gap-2">
                      <span className="text-base">{recipe.recipeIcon}</span>
                      <div>
                        <span className="text-sm font-medium text-stone-900 block leading-tight">{recipe.recipeName}</span>
                        <span className="text-[10px] text-stone-400">{recipe.screens.length} screens · {relativeTime(recipe.updatedAt)}</span>
                      </div>
                    </div>
                  </td>
                  <td className="px-3 py-2">
                    <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full" style={{ background: CATEGORY_COLORS[recipe.category]?.bg || '#E7E0D818', color: CATEGORY_COLORS[recipe.category]?.text || '#57534E' }}>
                      {recipe.category}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-[11px] text-stone-600">{recipe.selectedLanguages.map(l => l.toUpperCase()).join(', ')}</td>
                  <td className="px-3 py-2 text-xs text-stone-600 text-right font-medium">{users > 0 ? users.toLocaleString() : '—'}</td>
                  <td className="px-3 py-2 text-xs text-stone-600 text-right font-medium">{dl > 0 ? dl.toLocaleString() : '—'}</td>
                  <td className="px-3 py-2 text-right">
                    {rating > 0 ? (
                      <span className="inline-flex items-center gap-0.5 text-xs font-medium text-stone-700">
                        <Star size={10} className="text-amber-400 fill-amber-400" />
                        {rating.toFixed(1)}
                      </span>
                    ) : <span className="text-xs text-stone-400">—</span>}
                  </td>
                  <td className="px-2 py-2">
                    <ChevronRight size={14} className="text-stone-300 group-hover:text-stone-500 transition-colors" />
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        )}
          </div>
        </div>
      </div>

      {/* Recipe detail slide-over */}
      {selectedRecipe && (
        <div className="fixed inset-0 z-50 flex justify-end">
          <div className="absolute inset-0 bg-stone-900/20" onClick={() => setSelectedRecipe(null)} />
          <div className="relative w-[480px] bg-white shadow-elevated overflow-y-auto animate-in slide-in-from-right duration-300">
            <div className="sticky top-0 bg-white border-b border-stone-200 px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-2xl">{selectedRecipe.recipeIcon}</span>
                <div>
                  <h3 className="text-lg font-semibold text-stone-900">{selectedRecipe.recipeName}</h3>
                  <span className="text-xs font-medium px-2 py-0.5 rounded-full" style={{ background: CATEGORY_COLORS[selectedRecipe.category]?.bg || '#E7E0D818', color: CATEGORY_COLORS[selectedRecipe.category]?.text || '#57534E' }}>{selectedRecipe.category}</span>
                </div>
              </div>
              <button onClick={() => setSelectedRecipe(null)} className="min-w-[44px] min-h-[44px] flex items-center justify-center text-stone-400 hover:text-stone-600" aria-label="Close">
                <X size={20} />
              </button>
            </div>
            <div className="p-6 space-y-6">
              <p className="text-sm text-stone-600 leading-relaxed">{selectedRecipe.recipeDescription || 'No description'}</p>

              <div className="grid grid-cols-3 gap-4">
                <div className="text-center p-3 rounded-xl bg-stone-50">
                  <p className="text-lg font-bold text-stone-900">{selectedRecipe.screens.length}</p>
                  <p className="text-[11px] text-stone-500">Screens</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-stone-50">
                  <p className="text-lg font-bold text-stone-900">{selectedRecipe.selectedLanguages.length}</p>
                  <p className="text-[11px] text-stone-500">Languages</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-stone-50">
                  <div className="flex items-center justify-center gap-1">
                    <Star size={14} className="text-amber-400 fill-amber-400" />
                    <p className="text-lg font-bold text-stone-900">{selectedRecipe.stats.rating || '—'}</p>
                  </div>
                  <p className="text-[11px] text-stone-500">Rating</p>
                </div>
              </div>

              <div>
                <h4 className="text-sm font-medium text-stone-700 mb-2">Details</h4>
                <div className="space-y-2">
                  <div className="flex justify-between py-2 border-b border-stone-100">
                    <span className="text-sm text-stone-500">Theme</span>
                    <span className="text-sm font-medium text-stone-900 capitalize">{selectedRecipe.selectedTheme}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-stone-100">
                    <span className="text-sm text-stone-500">Languages</span>
                    <span className="text-sm font-medium text-stone-900">{selectedRecipe.selectedLanguages.join(', ').toUpperCase()}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-stone-100">
                    <span className="text-sm text-stone-500">Last Updated</span>
                    <span className="text-sm font-medium text-stone-900">{relativeTime(selectedRecipe.updatedAt)}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-stone-100">
                    <span className="text-sm text-stone-500">Created</span>
                    <span className="text-sm font-medium text-stone-900">{relativeTime(selectedRecipe.createdAt)}</span>
                  </div>
                </div>
              </div>

              {/* Mini heatmap for this recipe */}
              <div>
                <h4 className="text-sm font-medium text-stone-700 mb-2">Download Distribution</h4>
                <div className="space-y-2">
                  {[
                    { region: 'Indonesia', pct: 42 },
                    { region: 'Malaysia', pct: 28 },
                    { region: 'Philippines', pct: 18 },
                    { region: 'Others', pct: 12 },
                  ].map(r => (
                    <div key={r.region} className="flex items-center gap-3">
                      <span className="text-xs text-stone-600 w-24">{r.region}</span>
                      <div className="flex-1 h-2 rounded-full bg-stone-100 overflow-hidden">
                        <div className="h-full rounded-full" style={{ background: '#C45A3A', width: `${r.pct}%` }} />
                      </div>
                      <span className="text-xs font-medium text-stone-700 w-8 text-right">{r.pct}%</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex gap-3">
                <button
                  onClick={() => { const id = selectedRecipe.id; setSelectedRecipe(null); navigate(`/studio/${id}`); }}
                  className="flex-1 py-2.5 rounded-lg text-white text-sm font-medium"
                  style={{ background: '#C45A3A' }}
                >
                  Edit Recipe
                </button>
                <button
                  onClick={handleDuplicate}
                  className="min-w-[44px] min-h-[44px] flex items-center justify-center rounded-lg border border-stone-200 text-sm font-medium text-stone-700 hover:bg-stone-50"
                  title="Duplicate"
                  aria-label="Duplicate recipe"
                >
                  <Copy size={16} />
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(true)}
                  className="min-w-[44px] min-h-[44px] flex items-center justify-center rounded-lg border border-red-200 text-sm font-medium text-red-600 hover:bg-red-50"
                  title="Delete"
                  aria-label="Delete recipe"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete confirmation */}
      <AlertDialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete recipe?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete &ldquo;{selectedRecipe?.recipeName}&rdquo;. This cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-600 hover:bg-red-700">Delete</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
