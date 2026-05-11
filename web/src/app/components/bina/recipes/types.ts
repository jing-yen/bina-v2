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

export interface TemplateField {
  key: string;
  label: string;
  placeholder: string;
  type: 'text' | 'textarea' | 'select';
  options?: string[];
  defaultValue: string;
  showWhen?: { field: string; value: string };
}

export interface ScreenTemplate {
  id: string;
  name: string;
  emoji: string;
  description: string;
  widgets: TemplateWidget[];
  fields: TemplateField[];
}

export interface ScreenConfig {
  id: string;
  title: string;
  isHome: boolean;
  gridColumns: number;
  templateId: string | null;
  fieldValues: Record<string, string>;
  disabledWidgets: string[];
}

export interface KnowledgeFile {
  name: string;
  size: string;
  chunks?: number;
  status: 'uploading' | 'processing' | 'ready';
  summary?: string;
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
}
