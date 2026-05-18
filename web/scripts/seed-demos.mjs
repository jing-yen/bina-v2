import { initializeApp } from 'firebase/app';
import { getFirestore, collection, addDoc, getDocs, deleteDoc, Timestamp } from 'firebase/firestore';
import { readFileSync, readdirSync } from 'fs';
import { join } from 'path';

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

const ASSETS_DIR = join(import.meta.dirname, '../../app/src/main/assets/miniapps');

const META = {
  mock_bidan:        { icon: '👩‍⚕️', category: 'Health' },
  kira_mikro:        { icon: '💰', category: 'Finance' },
  mock_dengue:       { icon: '🦟', category: 'Health' },
  mock_khmer:        { icon: '🌾', category: 'Agriculture' },
  mock_myanmar:      { icon: '🏥', category: 'Health' },
  mock_nutrition:    { icon: '🍎', category: 'Health' },
  mock_plant_doctor: { icon: '🌿', category: 'Agriculture' },
  mock_sawit:        { icon: '🌴', category: 'Agriculture' },
  mock_thai:         { icon: '🏪', category: 'Business' },
  mock_viet:         { icon: '🚜', category: 'Agriculture' },
  pakar_sawit:       { icon: '🌴', category: 'Agriculture' },
  triage_ibu_hamil:  { icon: '🚨', category: 'Health' },
};

async function seed() {
  // 1. Delete all existing recipes
  console.log('Deleting all existing recipes...');
  const existing = await getDocs(recipesRef);
  let deleted = 0;
  for (const doc of existing.docs) {
    await deleteDoc(doc.ref);
    deleted++;
  }
  console.log(`Deleted ${deleted} documents.`);

  // 2. Read and upload all asset YAMLs (with l10n)
  const files = readdirSync(ASSETS_DIR).filter(f => f.endsWith('.yaml') || f.endsWith('.yml'));
  console.log(`Found ${files.length} asset recipes to seed.`);

  for (const file of files) {
    const yaml = readFileSync(join(ASSETS_DIR, file), 'utf-8');
    const idMatch = yaml.match(/^id:\s*(.+)/m);
    const nameMatch = yaml.match(/^name:\s*"?(.+?)"?\s*$/m);
    const descMatch = yaml.match(/^description:\s*"?(.+?)"?\s*$/m);
    const recipeId = idMatch?.[1]?.trim() ?? file.replace(/\.ya?ml$/, '');
    const name = nameMatch?.[1]?.trim() ?? recipeId;
    const desc = descMatch?.[1]?.trim() ?? '';
    const meta = META[recipeId] ?? { icon: '📦', category: '' };

    const doc = {
      recipeName: name,
      recipeIcon: meta.icon,
      recipeDescription: desc,
      category: meta.category,
      generatedYaml: yaml,
      selectedLanguages: ['en', 'ms', 'in', 'vi', 'th', 'km', 'my', 'ta', 'zh'],
      selectedTheme: 'custom',
      screens: [],
      _version: 2,
      stats: { downloads: '0', users: '0', rating: 0 },
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
    };

    const ref = await addDoc(recipesRef, doc);
    console.log(`Created "${name}" (${recipeId}) → ${ref.id}`);
  }

  console.log('Done.');
  process.exit(0);
}

seed().catch(e => { console.error(e); process.exit(1); });
