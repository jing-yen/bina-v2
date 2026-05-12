export type ThemeKey = 'navy' | 'forest' | 'coral' | 'amber' | 'custom';

export interface WidgetProps { [key: string]: string }
export interface WidgetConfig { type: string; props: WidgetProps }

export interface TemplateWidget {
  wid: string;
  type: string;
  optional: boolean;
  defaultOn: boolean;
  staticProps: WidgetProps;
  fieldMap: Record<string, string>;
}

export type ShowWhenCondition = { field: string; value: string };

export interface TemplateField {
  key: string;
  label: string;
  placeholder: string;
  type: 'text' | 'textarea' | 'select';
  options?: string[];
  defaultValue: string;
  showWhen?: ShowWhenCondition | ShowWhenCondition[];
}

export interface ScreenTemplate {
  id: string;
  name: string;
  emoji: string;
  description: string;
  widgets: TemplateWidget[];
  fields: TemplateField[];
}

export interface RoutingRule {
  value: string;
  goto: string;
}

export interface ScreenRouting {
  field: string;
  rules: RoutingRule[];
  fallback: string;
}

export interface ScreenConfig {
  id: string;
  title: string;
  isHome: boolean;
  gridColumns: number;
  templateId: string | null;
  fieldValues: Record<string, string>;
  disabledWidgets: string[];
  routing?: ScreenRouting;
}

export interface KnowledgeFile {
  name: string;
  size: string;
  chunks?: number;
  status: 'uploading' | 'processing' | 'ready';
  summary?: string;
}

export interface IntroPageConfig {
  enabled: boolean;
  disclaimer: string;
  authorName: string;
  authorOrg: string;
  authorVerified: boolean;
  links: { label: string; url: string }[];
  acceptLabel: string;
}

export function defaultIntroPage(disclaimer?: string): IntroPageConfig {
  return {
    enabled: !!disclaimer,
    disclaimer: disclaimer || 'AI-generated content. Not a professional consultation.',
    authorName: '',
    authorOrg: '',
    authorVerified: false,
    links: [],
    acceptLabel: 'I Understand',
  };
}

export interface RecipeConfig {
  recipeName: string;
  recipeDescription: string;
  recipeIcon: string;
  systemPrompt: string;
  blockedKeywords: string;
  disclaimer: string;
  category: string;
  selectedLanguages: string[];
  selectedTheme: ThemeKey;
  customPrimary: string;
  customSecondary: string;
  screens: ScreenConfig[];
  knowledgeSummary: string;
  introPage?: IntroPageConfig;
}

export const CURRENT_RECIPE_VERSION = 2;

export interface RecipeStats {
  downloads: string;
  users: string;
  rating: number;
}

export interface RecipeDocument extends RecipeConfig {
  _version: number;
  createdAt: Date;
  updatedAt: Date;
  stats: RecipeStats;
}

export interface RecipeWithId extends RecipeDocument {
  id: string;
}

export function checkShowWhen(
  condition: ShowWhenCondition | ShowWhenCondition[] | undefined,
  fieldValues: Record<string, string>,
): boolean {
  if (!condition) return true;
  const conditions = Array.isArray(condition) ? condition : [condition];
  return conditions.every(c => fieldValues[c.field] === c.value);
}
