import { initializeApp } from 'firebase/app';
import { getFirestore, collection, addDoc, getDocs, query, where, Timestamp } from 'firebase/firestore';
import { readFileSync } from 'fs';
import { execSync } from 'child_process';

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
const recipesRef = collection(db, 'recipes');

const demos = [
  { file: 'bidan_pintar.yaml', name: 'Bidan Pintar', icon: '👩‍⚕️', desc: 'Rural midwife assistant. Prenatal care guidance and emergency protocols, fully offline.', category: 'Health' },
  { file: 'farm_buddy.yaml', name: 'Farm Buddy', icon: '🌾', desc: 'Diagnose crops, calculate profit, find nearby agro shops.', category: 'Agriculture' },
  { file: 'buku_kira_kira.yaml', name: 'Buku Kira-Kira', icon: '📒', desc: 'Smart bookkeeping for sari-sari stores. Track sales, costs, and daily profit.', category: 'Business' },
];

async function seed() {
  for (const demo of demos) {
    // Read YAML from git history since we deleted the files
    let yaml;
    try {
      yaml = execSync(`git show HEAD:app/src/main/assets/miniapps/${demo.file}`, { encoding: 'utf-8' });
    } catch {
      console.error(`Could not read ${demo.file} from git`);
      continue;
    }

    // Check if already exists
    const existing = await getDocs(query(recipesRef, where('recipeName', '==', demo.name)));
    if (!existing.empty) {
      console.log(`Skipping "${demo.name}" — already exists (${existing.docs[0].id})`);
      continue;
    }

    const doc = {
      recipeName: demo.name,
      recipeIcon: demo.icon,
      recipeDescription: demo.desc,
      category: demo.category,
      generatedYaml: yaml,
      selectedLanguages: ['en', 'ms', 'id', 'tl', 'zh', 'ta', 'ar'],
      selectedTheme: 'custom',
      screens: [],
      _version: 2,
      stats: { downloads: '0', users: '0', rating: 0 },
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
    };

    const ref = await addDoc(recipesRef, doc);
    console.log(`Created "${demo.name}" → ${ref.id}`);
  }

  console.log('Done.');
  process.exit(0);
}

seed().catch(e => { console.error(e); process.exit(1); });
