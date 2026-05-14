import { FARM_BUDDY_RECIPE } from './farmBuddy';
import { MATERNAL_HEALTH_RECIPE } from './maternalHealth';
import { FLOOD_RESPONSE_RECIPE } from './floodResponse';
import { MATH_TUTOR_RECIPE } from './mathTutor';
import { MARKET_PRICE_RECIPE } from './marketPrice';
import type { RecipeConfig } from './types';

export const RECIPES: Record<string, RecipeConfig> = {
  'Farm Buddy': FARM_BUDDY_RECIPE,
  'Maternal Health Guide': MATERNAL_HEALTH_RECIPE,
  'Flood Response': FLOOD_RESPONSE_RECIPE,
  'Math Tutor': MATH_TUTOR_RECIPE,
  'Market Price Check': MARKET_PRICE_RECIPE,
};

export type { RecipeConfig, RecipeDocument, RecipeWithId, RecipeStats, IntroPageConfig, ScreenRouting, ShowWhenCondition } from './types';
export { CURRENT_RECIPE_VERSION, defaultIntroPage, checkShowWhen } from './types';
export type { ThemeKey, ScreenConfig, WidgetConfig, KnowledgeFile } from './types';
export { SCREEN_TEMPLATES, FORMULA_TEMPLATES, getScreenTemplate, createScreen, resolveFormula, resolveTemplateWidgets, resolveScreenWidgets, generateScreenDescription, generatePrefillHints, getScreenAcceptedInputs } from './screenTemplates';
