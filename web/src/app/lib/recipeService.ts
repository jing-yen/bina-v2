import {
  collection, doc, getDoc, getDocs, addDoc, updateDoc, deleteDoc,
  Timestamp, query, orderBy,
} from 'firebase/firestore';
import { db } from './firebase';
import type { RecipeConfig, RecipeWithId } from '../components/bina/recipes';
import { CURRENT_RECIPE_VERSION, defaultIntroPage } from '../components/bina/recipes';

const COLLECTION = 'recipes';
const recipesRef = collection(db, COLLECTION);

function migrateRecipe(data: Record<string, unknown>): Record<string, unknown> {
  const version = (data._version as number) || 1;
  const migrated = { ...data };

  if (version < 2) {
    const disclaimer = (migrated.disclaimer as string) || '';
    migrated.introPage = defaultIntroPage(disclaimer);
    migrated._version = 2;
  }

  return migrated;
}

function docToRecipe(id: string, raw: Record<string, unknown>): RecipeWithId | null {
  const version = (raw._version as number) || 1;
  if (version > CURRENT_RECIPE_VERSION) return null;
  const data = version < CURRENT_RECIPE_VERSION ? migrateRecipe(raw) : raw;
  return {
    ...data,
    id,
    createdAt: (data.createdAt as Timestamp)?.toDate?.() ?? new Date(),
    updatedAt: (data.updatedAt as Timestamp)?.toDate?.() ?? new Date(),
  } as RecipeWithId;
}

export async function listRecipes(): Promise<RecipeWithId[]> {
  const q = query(recipesRef, orderBy('updatedAt', 'desc'));
  const snapshot = await getDocs(q);
  const recipes: RecipeWithId[] = [];
  snapshot.forEach(d => {
    const r = docToRecipe(d.id, d.data());
    if (r) recipes.push(r);
  });
  return recipes;
}

export async function getRecipe(id: string): Promise<RecipeWithId | null> {
  const snap = await getDoc(doc(db, COLLECTION, id));
  if (!snap.exists()) return null;
  return docToRecipe(snap.id, snap.data());
}

export async function createRecipe(config: RecipeConfig): Promise<string> {
  const docData = {
    ...config,
    _version: CURRENT_RECIPE_VERSION,
    stats: { downloads: '0', users: '0', rating: 0 },
    createdAt: Timestamp.now(),
    updatedAt: Timestamp.now(),
  };
  const ref = await addDoc(recipesRef, docData);
  return ref.id;
}

export async function updateRecipe(id: string, config: Partial<RecipeConfig>): Promise<void> {
  await updateDoc(doc(db, COLLECTION, id), {
    ...config,
    updatedAt: Timestamp.now(),
  });
}

export async function deleteRecipe(id: string): Promise<void> {
  await deleteDoc(doc(db, COLLECTION, id));
}

export async function duplicateRecipe(id: string): Promise<string> {
  const original = await getRecipe(id);
  if (!original) throw new Error('Recipe not found');
  const { id: _id, createdAt: _c, updatedAt: _u, stats: _s, _version: _v, ...config } = original;
  return createRecipe({ ...config, recipeName: `${config.recipeName} (copy)` });
}
