import { collection, getDocs, query, where, orderBy, Timestamp } from 'firebase/firestore';
import { db } from './firebase';
import { listRecipes } from './recipeService';

const pingsRef = collection(db, 'pings');

export interface PlatformStats {
  totalDownloads: number;
  uniqueDevices: number;
  avgRating: number;
  countriesReached: number;
}

export interface RegionCount {
  countryCode: string;
  count: number;
}

export interface RecipeAnalytics {
  recipeId: string;
  recipeName: string;
  downloads: number;
  rating: number;
}

export async function fetchPlatformStats(): Promise<PlatformStats> {
  const recipes = await listRecipes();

  let totalDownloads = 0;
  let totalRating = 0;
  let ratedCount = 0;

  for (const r of recipes) {
    const dl = parseInt(r.stats?.downloads || '0') || 0;
    totalDownloads += dl;
    if (r.stats?.rating > 0) {
      totalRating += r.stats.rating;
      ratedCount++;
    }
  }

  const pingsSnap = await getDocs(pingsRef);
  const devices = new Set<string>();
  const countries = new Set<string>();
  pingsSnap.forEach(doc => {
    const data = doc.data();
    if (data.device_hash) devices.add(data.device_hash);
    if (data.country_code) countries.add(data.country_code);
  });

  return {
    totalDownloads: Math.max(totalDownloads, pingsSnap.size),
    uniqueDevices: devices.size,
    avgRating: ratedCount > 0 ? totalRating / ratedCount : 0,
    countriesReached: countries.size,
  };
}

export async function fetchRegionCounts(): Promise<RegionCount[]> {
  const pingsSnap = await getDocs(pingsRef);
  const counts: Record<string, number> = {};
  pingsSnap.forEach(doc => {
    const cc = doc.data().country_code as string || 'XX';
    counts[cc] = (counts[cc] || 0) + 1;
  });
  return Object.entries(counts)
    .map(([countryCode, count]) => ({ countryCode, count }))
    .sort((a, b) => b.count - a.count);
}

export async function fetchRecipeAnalytics(): Promise<RecipeAnalytics[]> {
  const recipes = await listRecipes();
  return recipes.map(r => ({
    recipeId: r.id,
    recipeName: r.recipeName,
    downloads: parseInt(r.stats?.downloads || '0') || 0,
    rating: r.stats?.rating || 0,
  })).sort((a, b) => b.downloads - a.downloads);
}
