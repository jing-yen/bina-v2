import { collection, getDocs, addDoc, doc, updateDoc, query, orderBy, limit, Timestamp } from 'firebase/firestore';
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

export interface FeedItem {
  flag: string;
  text: string;
  time: string;
}

const COUNTRY_CODE_TO_NAME: Record<string, string> = {
  MY: 'Malaysia', ID: 'Indonesia', TH: 'Thailand', PH: 'Philippines',
  VN: 'Vietnam', SG: 'Singapore', KH: 'Cambodia', MM: 'Myanmar',
  LA: 'Laos', BN: 'Brunei', IN: 'India', BD: 'Bangladesh',
  PK: 'Pakistan', NP: 'Nepal', LK: 'Sri Lanka', NG: 'Nigeria',
  KE: 'Kenya', ET: 'Ethiopia', GH: 'Ghana', TZ: 'Tanzania',
};

const COUNTRY_CODE_TO_FLAG: Record<string, string> = {
  MY: '\u{1F1F2}\u{1F1FE}', ID: '\u{1F1EE}\u{1F1E9}', TH: '\u{1F1F9}\u{1F1ED}',
  PH: '\u{1F1F5}\u{1F1ED}', VN: '\u{1F1FB}\u{1F1F3}', SG: '\u{1F1F8}\u{1F1EC}',
  KH: '\u{1F1F0}\u{1F1ED}', MM: '\u{1F1F2}\u{1F1F2}', LA: '\u{1F1F1}\u{1F1E6}',
  BN: '\u{1F1E7}\u{1F1F3}', IN: '\u{1F1EE}\u{1F1F3}', BD: '\u{1F1E7}\u{1F1E9}',
  PK: '\u{1F1F5}\u{1F1F0}', NP: '\u{1F1F3}\u{1F1F5}', LK: '\u{1F1F1}\u{1F1F0}',
  NG: '\u{1F1F3}\u{1F1EC}', KE: '\u{1F1F0}\u{1F1EA}', ET: '\u{1F1EA}\u{1F1F9}',
  GH: '\u{1F1EC}\u{1F1ED}', TZ: '\u{1F1F9}\u{1F1FF}',
};

function relativeTimeFeed(ts: Timestamp): string {
  const diff = Date.now() - ts.toMillis();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

/**
 * Fetch the most recent `maxItems` pings from Firestore and format them as
 * live-feed items. Pass a map of recipeId → recipeName so we can label each
 * ping without an extra Firestore read.
 */
export async function fetchRecentPings(
  recipeNames: Record<string, string>,
  maxItems = 8,
): Promise<FeedItem[]> {
  const q = query(pingsRef, orderBy('timestamp', 'desc'), limit(maxItems));
  const snap = await getDocs(q);

  const items: FeedItem[] = [];
  snap.forEach(d => {
    const data = d.data();
    const cc = (data.country_code as string) || 'XX';
    const flag = COUNTRY_CODE_TO_FLAG[cc] || '🌐';
    const country = COUNTRY_CODE_TO_NAME[cc] || cc;
    const recipeName = recipeNames[data.recipe_id as string] || 'a recipe';
    const ts = data.timestamp as Timestamp | undefined;
    const time = ts ? relativeTimeFeed(ts) : 'recently';
    items.push({ flag, text: `User in ${country} opened ${recipeName}`, time });
  });

  return items;
}

export interface GrowthPoint {
  day: string;
  label: string;
  downloads: number;
}

/** Aggregate pings by day over the last 30 days. Falls back to zeros if no data. */
export async function fetchGrowthData(): Promise<GrowthPoint[]> {
  const pingsSnap = await getDocs(pingsRef);

  // Build a map of ISO date string -> count
  const countByDay: Record<string, number> = {};
  pingsSnap.forEach(d => {
    const ts = d.data().timestamp as Timestamp | undefined;
    if (ts) {
      const date = ts.toDate();
      const key = date.toISOString().slice(0, 10); // YYYY-MM-DD
      countByDay[key] = (countByDay[key] || 0) + 1;
    }
  });

  // Generate last-30-days window
  const points: GrowthPoint[] = [];
  const now = new Date();
  for (let i = 29; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    const dayNum = 30 - i;
    const monthDay = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    points.push({ day: String(dayNum), label: monthDay, downloads: countByDay[key] || 0 });
  }

  return points;
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
