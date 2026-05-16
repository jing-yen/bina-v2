import { initializeApp } from 'firebase/app';
import { getFirestore, collection, addDoc, getDocs, doc, updateDoc, Timestamp } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: 'AIzaSyCKMwZ0iFo9Mxt9iIX497ZanQEUrxGxsT0',
  authDomain: 'bina-ai-a2d96.firebaseapp.com',
  projectId: 'bina-ai-a2d96',
  storageBucket: 'bina-ai-a2d96.firebasestorage.app',
  messagingSenderId: '260186437518',
  appId: '1:260186437518:web:ed891835707efbdfce16df',
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const pingsRef = collection(db, 'pings');
const recipesRef = collection(db, 'recipes');

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

async function seed() {
  // Seed pings
  console.log('Seeding pings...');
  const batch = [];
  for (let i = 0; i < 150; i++) {
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
  console.log(`Created ${batch.length} pings`);

  // Update recipe stats
  console.log('Updating recipe stats...');
  const recipesSnap = await getDocs(recipesRef);
  const updates = [];
  recipesSnap.forEach(d => {
    const dl = Math.floor(Math.random() * 800) + 120;
    const users = Math.floor(dl * (0.4 + Math.random() * 0.4));
    const rating = +(3.5 + Math.random() * 1.5).toFixed(1);
    console.log(`  ${d.id}: ${dl} downloads, ${users} users, ${rating} rating`);
    updates.push(updateDoc(doc(db, 'recipes', d.id), {
      'stats.downloads': String(dl),
      'stats.users': String(users),
      'stats.rating': rating,
    }));
  });
  await Promise.all(updates);

  console.log('Done.');
  process.exit(0);
}

seed().catch(e => { console.error(e); process.exit(1); });
