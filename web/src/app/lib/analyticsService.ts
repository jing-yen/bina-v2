import { collection, getDocs, addDoc, doc, updateDoc, query, where, orderBy, Timestamp } from 'firebase/firestore';
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

const MOCK_COUNTRIES = [
  { code: 'MY', weight: 35 },
  { code: 'ID', weight: 28 },
  { code: 'TH', weight: 15 },
  { code: 'PH', weight: 10 },
  { code: 'VN', weight: 6 },
  { code: 'SG', weight: 3 },
  { code: 'KH', weight: 2 },
  { code: 'MM', weight: 1 },
];

export async function seedMockPings(count = 120): Promise<void> {
  const existing = await getDocs(pingsRef);
  if (existing.size > 10) return;

  const batch: Promise<unknown>[] = [];
  for (let i = 0; i < count; i++) {
    const rand = Math.random() * 100;
    let cumulative = 0;
    let cc = 'MY';
    for (const c of MOCK_COUNTRIES) {
      cumulative += c.weight;
      if (rand < cumulative) { cc = c.code; break; }
    }
    const daysAgo = Math.floor(Math.random() * 30);
    const ts = Timestamp.fromDate(new Date(Date.now() - daysAgo * 86400000));
    batch.push(addDoc(pingsRef, {
      device_hash: `mock_${Math.random().toString(36).slice(2, 10)}`,
      country_code: cc,
      timestamp: ts,
      recipe_id: `demo_${Math.floor(Math.random() * 5)}`,
    }));
  }
  await Promise.all(batch);

  const recipes = await listRecipes();
  let madeViral = false;
  const statUpdates = recipes.map(r => {
    const isBidan = r.recipeName.toLowerCase().includes('bidan');
    let dl: number, users: number, rating: number;
    if (isBidan || (!madeViral && r.recipeName.toLowerCase().includes('health'))) {
      dl = 12400; users = 4180; rating = 4.9;
      madeViral = true;
    } else {
      dl = Math.floor(Math.random() * 800) + 50;
      users = Math.floor(dl * (0.4 + Math.random() * 0.4));
      rating = +(3.5 + Math.random() * 1.5).toFixed(1);
    }
    return updateDoc(doc(db, 'recipes', r.id), {
      'stats.downloads': String(dl),
      'stats.users': String(users),
      'stats.rating': rating,
    });
  });
  await Promise.all(statUpdates);
}
