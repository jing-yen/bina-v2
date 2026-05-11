import { FARM_BUDDY_RECIPE } from './farmBuddy';
import type { RecipeConfig } from './types';

export const RECIPES: Record<string, RecipeConfig> = {
  'Farm Buddy': FARM_BUDDY_RECIPE,
};

export type { RecipeConfig } from './types';
export type { ThemeKey, ScreenConfig, WidgetConfig, KnowledgeFile } from './types';
export { SCREEN_TEMPLATES, FORMULA_TEMPLATES, getScreenTemplate, createScreen, resolveFormula, resolveTemplateWidgets, resolveScreenWidgets } from './screenTemplates';
