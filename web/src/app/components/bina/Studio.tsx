import { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router';
import { toast } from 'sonner';
import {
  FileText, Palette, Upload, Download, Save,
  ChevronLeft, ChevronRight, ChevronDown, Eye, X, Plus, Trash2,
  Camera, Mic, Globe, Home, Link2, Shield, User, Copy, MessageCircle, LayoutGrid,
  Loader2, Sparkles, Search, Check, GitBranch, Undo2, Redo2, PartyPopper,
} from 'lucide-react';
import { Input } from '../ui/input';
import { Textarea } from '../ui/textarea';
import { Progress } from '../ui/progress';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '../ui/select';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from '../ui/dialog';
import type {
  ThemeKey, ScreenConfig, WidgetConfig, KnowledgeFile, RecipeConfig,
  IntroPageConfig, ScreenRouting, RecipeTranslation, TranslationStatus,
} from './recipes';
import {
  SCREEN_TEMPLATES, FORMULA_TEMPLATES, getScreenTemplate, createScreen,
  resolveFormula, resolveScreenWidgets, defaultIntroPage, checkShowWhen,
  generateScreenDescription, generatePrefillHints, getScreenAcceptedInputs,
} from './recipes';
import { getRecipe, createRecipe as createFirestoreRecipe, updateRecipe } from '../../lib/recipeService';
import { DEMO_DOCUMENTS } from './recipes/demoDocuments';

// ─── Constants ───

const THEMES: { key: ThemeKey; label: string; primary: string; secondary: string }[] = [
  { key: 'navy', label: 'Navy', primary: '#C45A3A', secondary: '#ADC8FF' },
  { key: 'forest', label: 'Forest', primary: '#2E7D32', secondary: '#A5D6A7' },
  { key: 'coral', label: 'Coral', primary: '#DC2626', secondary: '#FECACA' },
  { key: 'amber', label: 'Amber', primary: '#D97706', secondary: '#FDE68A' },
  { key: 'custom', label: 'Custom', primary: '#6B21A8', secondary: '#E9D5FF' },
];


const EMOJI_ICONS = [
  '\u{1F33E}', '\u{1F3E5}', '\u{1F6A8}', '\u{1F4DA}', '\u{1F9EA}', '\u{1F4B0}',
  '\u{1F30D}', '\u{2764}\u{FE0F}', '\u{1F680}', '\u{1F916}', '\u{1F331}', '\u{2B50}',
  '\u{1F4A1}', '\u{1F4F7}', '\u{1F3AF}', '\u{1F50D}',
];

const CATEGORIES = ['Agriculture', 'Health', 'Education', 'Emergency', 'Finance', 'Environment'];

const LANGUAGE_GROUPS = [
  { label: 'Southeast Asian', languages: [
    { code: 'ms', label: 'Bahasa Melayu', native: 'Bahasa Melayu' },
    { code: 'id', label: 'Bahasa Indonesia', native: 'Bahasa Indonesia' },
    { code: 'tl', label: 'Filipino / Tagalog', native: 'Filipino' },
    { code: 'th', label: 'Thai', native: 'ไทย' },
    { code: 'vi', label: 'Vietnamese', native: 'Tiếng Việt' },
    { code: 'my', label: 'Burmese', native: 'မြန်မာ' },
    { code: 'km', label: 'Khmer', native: 'ខ្មែរ' },
    { code: 'lo', label: 'Lao', native: 'ລາວ' },
    { code: 'jv', label: 'Javanese', native: 'Basa Jawa' },
    { code: 'su', label: 'Sundanese', native: 'Basa Sunda' },
    { code: 'ceb', label: 'Cebuano', native: 'Sinugboanon' },
    { code: 'ilo', label: 'Ilocano', native: 'Ilokano' },
  ]},
  { label: 'South Asian', languages: [
    { code: 'ta', label: 'Tamil', native: 'தமிழ்' },
    { code: 'hi', label: 'Hindi', native: 'हिन्दी' },
    { code: 'bn', label: 'Bengali', native: 'বাংলা' },
    { code: 'ur', label: 'Urdu', native: 'اردو' },
    { code: 'te', label: 'Telugu', native: 'తెలుగు' },
    { code: 'mr', label: 'Marathi', native: 'मराठी' },
    { code: 'ne', label: 'Nepali', native: 'नेपाली' },
    { code: 'si', label: 'Sinhala', native: 'සිංහල' },
  ]},
  { label: 'East Asian', languages: [
    { code: 'zh', label: 'Chinese (Simplified)', native: '中文' },
    { code: 'zh-TW', label: 'Chinese (Traditional)', native: '繁體中文' },
    { code: 'ja', label: 'Japanese', native: '日本語' },
    { code: 'ko', label: 'Korean', native: '한국어' },
  ]},
  { label: 'Other', languages: [
    { code: 'en', label: 'English', native: 'English' },
    { code: 'ar', label: 'Arabic', native: 'العربية' },
    { code: 'sw', label: 'Swahili', native: 'Kiswahili' },
    { code: 'pt', label: 'Portuguese', native: 'Português' },
    { code: 'fr', label: 'French', native: 'Français' },
    { code: 'es', label: 'Spanish', native: 'Español' },
  ]},
];
const ALL_LANGUAGES = LANGUAGE_GROUPS.flatMap(g => g.languages);

const STEPS = [
  { id: 1, label: 'Knowledge', icon: Upload, color: '#5B6ABF' },
  { id: 2, label: 'Identity', icon: FileText, color: '#C45A3A' },
  { id: 3, label: 'Style & Layout', icon: Palette, color: '#C98A1A' },
  { id: 4, label: 'Review', icon: Eye, color: '#1A8A6A' },
];

// ─── Gemini ───

const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemma-4-31b-it:generateContent';

async function callGemini(prompt: string, apiKey: string, retries = 3): Promise<string> {
  const res = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }], generationConfig: { temperature: 0.7, maxOutputTokens: 2048 } }),
  });
  if ([429, 500, 502, 503].includes(res.status) && retries > 0) {
    await new Promise(r => setTimeout(r, 2000 * (4 - retries)));
    return callGemini(prompt, apiKey, retries - 1);
  }
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(friendlyGeminiError(res.status));
  }
  const data = await res.json();
  return data.candidates?.[0]?.content?.parts?.[0]?.text || '';
}

function friendlyGeminiError(status: number): string {
  if (status === 429) return 'The AI is busy right now. Wait a moment and try again.';
  if (status === 401 || status === 403) return 'Invalid API key. Check your Gemini API key and try again.';
  if (status >= 500) return 'The AI service is temporarily unavailable. Try again in a few seconds.';
  return 'Something went wrong connecting to the AI. Check your connection and try again.';
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
async function callGeminiJSON<T = any>(prompt: string, apiKey: string, schema: Record<string, unknown>, retries = 2, maxOutputTokens = 2048): Promise<T> {
  const res = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: prompt }] }],
      generationConfig: {
        temperature: 0.7,
        maxOutputTokens,
        responseMimeType: 'application/json',
        responseSchema: schema,
      },
    }),
  });
  if ([429, 500, 502, 503].includes(res.status) && retries > 0) {
    await new Promise(r => setTimeout(r, 2000 * (4 - retries)));
    return callGeminiJSON<T>(prompt, apiKey, schema, retries - 1);
  }
  if (!res.ok) {
    throw new Error(friendlyGeminiError(res.status));
  }
  const data = await res.json();
  const text = data.candidates?.[0]?.content?.parts?.[0]?.text || '';
  return JSON.parse(text);
}

// ─── Emoji sanitizer ───
// Gemini returns CLDR slug names like "emoji_seedling" instead of Unicode characters.
// We use the full Unicode CLDR slug→emoji map (1900+ entries) as fallback.
import emojiSlugMap from './recipes/emojiSlugMap.json';

function sanitizeEmoji(raw: string): string {
  if (!raw) return '\u{1F916}';
  const trimmed = raw.trim();
  const emojiMatch = trimmed.match(/(\p{Emoji_Presentation}|\p{Emoji}\u{FE0F})/u);
  if (emojiMatch) return emojiMatch[0];
  const cleaned = trimmed.replace(/^emoji_?/i, '').replace(/^:/, '').replace(/:$/, '').toLowerCase().trim();
  if ((emojiSlugMap as Record<string, string>)[cleaned]) return (emojiSlugMap as Record<string, string>)[cleaned];
  return '\u{1F916}';
}

// ─── YAML serializer ───

function widgetToYaml(w: WidgetConfig, allScreens: { id: string; title: string }[]): string {
  const p = w.props;
  const esc = (s: string) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  const line = (k: string, v: string, q = false) => q ? `\n          ${k}: "${esc(v)}"` : `\n          ${k}: ${v}`;
  const opt = (k: string, v: string | undefined, q = false) => v ? line(k, v, q) : '';
  const visOpt = opt('visible_if', p.visible_if, true) + opt('hidden_if', p.hidden_if, true);

  switch (w.type) {
    case 'text_label':
      return `      - text_label:${line('text', p.text || 'Welcome', true)}${line('style', p.style || 'body')}${opt('align', p.align !== 'left' ? p.align : undefined)}${opt('color', p.color, true)}`;
    case 'text_input': {
      let y = `      - text_input:${line('bind', p.bind || 'user_text')}${line('hint', p.hint || 'Type here...', true)}`;
      if (p.label) y += line('label', p.label, true);
      if (p.input_type && p.input_type !== 'text') y += line('input_type', p.input_type);
      if (p.input_type === 'dropdown' && p.options) {
        const opts = p.options.split(',').map((o: string) => o.trim()).filter(Boolean);
        if (opts.length > 0) y += `\n          options:\n${opts.map((o: string) => `            - "${o}"`).join('\n')}`;
      }
      return y;
    }
    case 'voice_input':
      return `      - voice_input:${line('bind', p.bind || 'user_text')}${line('hint', p.hint || 'Speak...', true)}${opt('language', p.language, true)}${opt('mode', p.mode !== 'tap' ? p.mode : undefined)}`;
    case 'camera_input':
      return `      - camera_input:${line('bind', p.bind || 'photo_path')}${line('label', p.label || 'Take Photo', true)}${line('preview', 'true')}`;
    case 'action_button':
      return `      - action_button:${line('label', p.label || 'Submit', true)}${line('action', p.action || 'ask:{{user_text}}', true)}${line('style', p.style || 'primary')}${opt('icon', p.icon, true)}${opt('confirm', p.confirm, true)}`;
    case 'markdown_output':
      return `      - markdown_output:${line('source', p.source || 'ai_response')}${line('streaming', p.streaming || 'true')}${opt('empty_text', p.empty_text, true)}`;
    case 'macro_grid': {
      const screens = p._allScreens ? JSON.parse(p._allScreens) as { id: string; title: string; icon?: string }[] : allScreens.filter(s => s.id !== 'home');
      const btns = screens.length > 0
        ? screens.map(s => {
            const icon = (s as { icon?: string }).icon;
            return icon
              ? `            - { label: "${s.title}", action: "go:${s.id}", icon: "${icon}" }`
              : `            - { label: "${s.title}", action: "go:${s.id}" }`;
          }).join('\n')
        : '            - { label: "Option 1", action: "go:home" }';
      return `      - macro_grid:${line('columns', p.columns || '2')}\n          buttons:\n${btns}`;
    }
    case 'slider':
      return `      - slider:${line('bind', p.bind || 'calc_rate')}${line('min', p.min || '0')}${line('max', p.max || '100')}${line('step', p.step || '1')}${line('label', p.label || 'Value', true)}${line('show_value', p.show_value || 'true')}`;
    case 'metric_card':
      return `      - metric_card:${line('source', p.source || 'calc_result')}${line('label', p.label || 'Result', true)}${opt('prefix', p.prefix, true)}${opt('suffix', p.suffix, true)}${line('format', p.format || 'decimal_2')}`;
    case 'geo_display':
      return `      - geo_display:${line('data', p.data || 'places')}${line('limit', p.limit || '5')}${line('show_distance', p.show_distance || 'true')}${opt('empty_text', p.empty_text, true)}`;
    case 'progress_bar':
      return `      - progress_bar:${line('bind', p.bind || 'checklist_step')}${line('total', p.total || '3')}`;
    case 'checklist_items': {
      let items: { label: string; type: string }[] = [];
      try { items = JSON.parse(p.items || '[]'); } catch {}
      const itemsYaml = items.map(it => `            - { label: "${it.label}", type: "${it.type}" }`).join('\n');
      return `      - checklist_items:${line('bind', p.bind || 'checklist_step')}\n          items:\n${itemsYaml}`;
    }
    default:
      return `      - ${w.type}: {}`;
  }
}

// ─── Undo/Redo ───

interface RecipeSnapshot {
  recipeName: string; recipeDescription: string; recipeIcon: string;
  systemPrompt: string; blockedKeywords: string; introPage: IntroPageConfig;
  category: string; selectedLanguages: string[];
  selectedTheme: ThemeKey; customPrimary: string; customSecondary: string;
  screens: ScreenConfig[]; knowledgeSummary: string; maxClarifications: number; fallbackScreen: string;
}

const MAX_HISTORY = 50;

// ─── Component ───

export function Studio() {
  const { id: recipeId } = useParams<{ id?: string }>();
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [saving, setSaving] = useState(false);
  const [publishSuccess, setPublishSuccess] = useState(false);
  const [pageLoading, setPageLoading] = useState(false);
  const [dirty, setDirty] = useState(false);
  const loadedRef = useRef(false);

  useEffect(() => {
    if (!dirty) return;
    const handler = (e: BeforeUnloadEvent) => { e.preventDefault(); };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  // Step 1
  const [recipeName, setRecipeName] = useState('');
  const [recipeDescription, setRecipeDescription] = useState('');
  const [recipeIcon, setRecipeIcon] = useState('\u{1F916}');
  const [systemPrompt, setSystemPrompt] = useState('You are a helpful assistant.');
  const [blockedKeywords, setBlockedKeywords] = useState('');
  const [introPage, setIntroPage] = useState<IntroPageConfig>({ ...defaultIntroPage(), enabled: true });
  const [category, setCategory] = useState('Education');
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>(ALL_LANGUAGES.map(l => l.code));
  const [langSearch, setLangSearch] = useState('');
  const [maxClarifications, setMaxClarifications] = useState(2);
  const [fallbackScreen, setFallbackScreen] = useState('');

  // Step 2
  const [selectedTheme, setSelectedTheme] = useState<ThemeKey>('navy');
  const [customPrimary, setCustomPrimary] = useState('#6B21A8');
  const [customSecondary, setCustomSecondary] = useState('#E9D5FF');
  const [screens, setScreens] = useState<ScreenConfig[]>([
    { id: 'home', title: '', isHome: true, gridColumns: 1, templateId: 'ask_ai', fieldValues: { heading: 'How can I help?', hint: 'Ask a question...' }, disabledWidgets: [] },
    { id: 'main', title: 'Ask AI', isHome: false, gridColumns: 1, ...createScreen('ask_ai') },
  ]);
  const [activeScreenIndex, setActiveScreenIndex] = useState(0);
  const [showTemplatePicker, setShowTemplatePicker] = useState(false);
  const [newScreenTemplate, setNewScreenTemplate] = useState<string | null>(null);
  const [newScreenTitle, setNewScreenTitle] = useState('');
  const [newScreenEmoji, setNewScreenEmoji] = useState('');
  const [showLangDialog, setShowLangDialog] = useState(false);
  const [showIconPicker, setShowIconPicker] = useState(false);
  const [showThemePicker, setShowThemePicker] = useState(false);

  // Step 3
  const [knowledgeFiles, setKnowledgeFiles] = useState<KnowledgeFile[]>([]);
  const [knowledgeSummary, setKnowledgeSummary] = useState('');
  const [knowledgeSuggestions, setKnowledgeSuggestions] = useState<{ recipeName: string; recipeDescription: string; category: string; systemPrompt: string; recipeIcon: string; themeKey: string; authorName: string; authorOrg: string; links: { label: string; url: string }[]; homeHeading: string; homeHint: string; sampleConversation: { userMessage: string; aiClarification: string; userReply: string }; screens: { id: string; title: string; emoji: string; templateId: string; heading: string; hint: string; description: string; buttonLabel: string; aiInstruction: string }[] } | null>(null);
  const [suggestionSelections, setSuggestionSelections] = useState<{ name: boolean; description: boolean; category: boolean; systemPrompt: boolean; icon: boolean; theme: boolean; author: boolean; links: boolean; homePreview: boolean; screens: Record<number, boolean> }>({ name: true, description: true, category: true, systemPrompt: true, icon: true, theme: true, author: true, links: true, homePreview: true, screens: {} });
  const [identityExpanded, setIdentityExpanded] = useState(false);
  const [suggestionsApplied, setSuggestionsApplied] = useState(false);

  const updateSuggestionSelection = (updater: (prev: typeof suggestionSelections) => typeof suggestionSelections) => {
    setSuggestionSelections(updater);
    setSuggestionsApplied(false);
  };
  const fileInputRef = useRef<HTMLInputElement>(null);
  const coverInputRef = useRef<HTMLInputElement>(null);

  // LLM
  const [apiKey, setApiKey] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [showYamlPreview, setShowYamlPreview] = useState(false);
  const [previewYamlLang, setPreviewYamlLang] = useState<string>('en');
  const [showApiKeyDialog, setShowApiKeyDialog] = useState(false);
  const [previewIntro, setPreviewIntro] = useState(false);
  const previewContainerRef = useRef<HTMLDivElement>(null);
  const [phoneScale, setPhoneScale] = useState(1);

  // Translation
  const [translations, setTranslations] = useState<Record<string, RecipeTranslation>>({});
  const [translationStatus, setTranslationStatus] = useState<Record<string, TranslationStatus>>({});
  const [translatingAll, setTranslatingAll] = useState(false);

  // ─── Undo / Redo ───
  const historyRef = useRef<RecipeSnapshot[]>([]);
  const historyIndexRef = useRef(-1);
  const restoringRef = useRef(false);

  const takeSnapshot = useCallback((): RecipeSnapshot => ({
    recipeName, recipeDescription, recipeIcon, systemPrompt, blockedKeywords,
    introPage, category, selectedLanguages, selectedTheme, customPrimary,
    customSecondary, screens, knowledgeSummary, maxClarifications, fallbackScreen,
  }), [recipeName, recipeDescription, recipeIcon, systemPrompt, blockedKeywords,
    introPage, category, selectedLanguages, selectedTheme, customPrimary,
    customSecondary, screens, knowledgeSummary, maxClarifications, fallbackScreen]);

  const applySnapshot = useCallback((snap: RecipeSnapshot) => {
    restoringRef.current = true;
    setRecipeName(snap.recipeName);
    setRecipeDescription(snap.recipeDescription);
    setRecipeIcon(snap.recipeIcon);
    setSystemPrompt(snap.systemPrompt);
    setBlockedKeywords(snap.blockedKeywords);
    setIntroPage(snap.introPage);
    setCategory(snap.category);
    setSelectedLanguages(snap.selectedLanguages);
    setSelectedTheme(snap.selectedTheme);
    setCustomPrimary(snap.customPrimary);
    setCustomSecondary(snap.customSecondary);
    setScreens(snap.screens);
    setKnowledgeSummary(snap.knowledgeSummary);
    setTimeout(() => { restoringRef.current = false; }, 0);
  }, []);

  const pushHistory = useCallback(() => {
    if (restoringRef.current || !loadedRef.current) return;
    const snap = takeSnapshot();
    const h = historyRef.current;
    const idx = historyIndexRef.current;
    historyRef.current = [...h.slice(0, idx + 1), snap].slice(-MAX_HISTORY);
    historyIndexRef.current = historyRef.current.length - 1;
  }, [takeSnapshot]);

  const canUndo = historyIndexRef.current > 0;
  const canRedo = historyIndexRef.current < historyRef.current.length - 1;

  const undo = useCallback(() => {
    if (historyIndexRef.current <= 0) return;
    historyIndexRef.current -= 1;
    applySnapshot(historyRef.current[historyIndexRef.current]);
  }, [applySnapshot]);

  const redo = useCallback(() => {
    if (historyIndexRef.current >= historyRef.current.length - 1) return;
    historyIndexRef.current += 1;
    applySnapshot(historyRef.current[historyIndexRef.current]);
  }, [applySnapshot]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (!(e.metaKey || e.ctrlKey)) return;
      if (e.key === 'z' && !e.shiftKey) { e.preventDefault(); undo(); }
      if ((e.key === 'z' && e.shiftKey) || e.key === 'y') { e.preventDefault(); redo(); }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [undo, redo]);

  // Sync preview with editing context
  useEffect(() => {
    if (currentStep <= 2) {
      setPreviewIntro(true);
    } else {
      setPreviewIntro(false);
      if (currentStep === 3) {
        const homeIdx = screens.findIndex(s => s.isHome);
        if (homeIdx >= 0) setActiveScreenIndex(homeIdx);
      }
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentStep]);

  // ─── Derived ───
  const activePrimary = selectedTheme === 'custom' ? customPrimary : THEMES.find(t => t.key === selectedTheme)!.primary;
  const activeSecondary = selectedTheme === 'custom' ? customSecondary : THEMES.find(t => t.key === selectedTheme)!.secondary;
  const activeScreen = screens[activeScreenIndex] || screens[0];
  const previewWidgetsRaw = activeScreen ? resolveScreenWidgets(activeScreen, screens) : [];
  const previewWidgets = previewWidgetsRaw.reduce<WidgetConfig[]>((acc, w) => {
    if (w.type === 'voice_input' && acc.length > 0 && acc[acc.length - 1].type === 'text_input') {
      const prev = acc[acc.length - 1];
      acc[acc.length - 1] = { ...prev, props: { ...prev.props, _hasMic: 'true' } };
      return acc;
    }
    acc.push(w);
    return acc;
  }, []);
  const screenTitle = (s: ScreenConfig) => s.isHome ? (recipeName || 'Home') : (s.title || 'Untitled');

  // ─── Load recipe from navigation state ───
  const loadRecipe = (recipe: RecipeConfig) => {
    setRecipeName(recipe.recipeName);
    setRecipeDescription(recipe.recipeDescription);
    setRecipeIcon(sanitizeEmoji(recipe.recipeIcon));
    setSystemPrompt(recipe.systemPrompt);
    setBlockedKeywords(recipe.blockedKeywords);
    setIntroPage({ ...(recipe.introPage || defaultIntroPage(recipe.disclaimer)), enabled: true });
    setCategory(recipe.category);
    setSelectedLanguages(recipe.selectedLanguages);
    setSelectedTheme(recipe.selectedTheme);
    setCustomPrimary(recipe.customPrimary);
    setCustomSecondary(recipe.customSecondary);
    setScreens(recipe.screens.map(s => s.screenIcon ? { ...s, screenIcon: sanitizeEmoji(s.screenIcon) } : s));
    setKnowledgeSummary(recipe.knowledgeSummary);
    setMaxClarifications(recipe.maxClarifications ?? 2);
    setFallbackScreen(recipe.fallbackScreen ?? '');
    setCurrentStep(1);
  };

  // ─── Helpers ───
  const toggleLanguage = (code: string) => setSelectedLanguages(prev => prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code]);

  const addScreenFromTemplate = (templateId: string, customTitle?: string, customEmoji?: string) => {
    const def = getScreenTemplate(templateId);
    const id = `screen_${Date.now()}`;
    const title = customTitle || def?.name || 'New Screen';
    const newScreen: ScreenConfig = { id, title, isHome: false, gridColumns: 1, ...createScreen(templateId), ...(customEmoji ? { screenIcon: sanitizeEmoji(customEmoji) } : {}) };
    setScreens(prev => [...prev, newScreen]);
    setShowTemplatePicker(false);
    setTimeout(() => {
      setScreens(cur => {
        setActiveScreenIndex(cur.length - 1);
        return cur;
      });
    }, 0);
  };

  const removeScreen = (index: number) => {
    setScreens(prev => {
      if (prev[index]?.isHome || prev.length <= 1) return prev;
      const next = prev.filter((_, i) => i !== index);
      const contentScreens = next.filter(s => !s.isHome);
      if (contentScreens.length <= 1) {
        return contentScreens.length === 1 ? contentScreens : next;
      }
      return next;
    });
    setActiveScreenIndex(i => i >= index ? Math.max(0, i - 1) : i);
  };

  const duplicateScreen = (index: number) => {
    const src = screens[index];
    if (!src || src.isHome) return;
    const dup: ScreenConfig = {
      ...structuredClone(src),
      id: `screen_${Date.now()}`,
      title: `${src.title} (copy)`,
    };
    setScreens(prev => [...prev.slice(0, index + 1), dup, ...prev.slice(index + 1)]);
    setActiveScreenIndex(index + 1);
  };

  const updateScreenTitle = (index: number, title: string) => {
    setScreens(prev => prev.map((s, i) => i === index ? { ...s, title } : s));
  };

  const updateGridColumns = (cols: number) => {
    setScreens(prev => prev.map(s => s.isHome ? { ...s, gridColumns: cols } : s));
  };

  const updateScreenField = (screenIndex: number, fieldKey: string, value: string) => {
    setScreens(prev => prev.map((s, si) => {
      if (si !== screenIndex || !s.templateId) return s;
      return { ...s, fieldValues: { ...s.fieldValues, [fieldKey]: value } };
    }));
  };

  const getScreenBindVars = (screen: ScreenConfig): string[] => {
    if (!screen.templateId) return [];
    const widgets = resolveScreenWidgets(screen, screens);
    return widgets.filter(w => w.props.bind).map(w => w.props.bind);
  };

  const toggleScreenRouting = (si: number) => {
    setScreens(prev => prev.map((s, i) => {
      if (i !== si) return s;
      return s.routing
        ? { ...s, routing: undefined }
        : { ...s, routing: { field: '', rules: [], fallback: '' } };
    }));
  };

  const updateScreenRouting = (si: number, routing: ScreenRouting) => {
    setScreens(prev => prev.map((s, i) => i === si ? { ...s, routing } : s));
  };

  const toggleScreenWidget = (screenIndex: number, wid: string) => {
    setScreens(prev => prev.map((s, si) => {
      if (si !== screenIndex || !s.templateId) return s;
      const disabled = s.disabledWidgets.includes(wid)
        ? s.disabledWidgets.filter(w => w !== wid)
        : [...s.disabledWidgets, wid];
      return { ...s, disabledWidgets: disabled };
    }));
  };

  const regenerateScreenMeta = (si: number) => {
    setScreens(prev => prev.map((s, i) => {
      if (i !== si || !s.templateId) return s;
      return { ...s, description: generateScreenDescription(s), prefillHints: generatePrefillHints(s) };
    }));
  };

  const regenerateAllScreenMeta = () => {
    setScreens(prev => prev.map(s => {
      if (!s.templateId || s.isHome) return s;
      return { ...s, description: generateScreenDescription(s), prefillHints: generatePrefillHints(s) };
    }));
  };

  const updateScreenDescription = (si: number, description: string) => {
    setScreens(prev => prev.map((s, i) => i === si ? { ...s, description } : s));
  };

  const updateScreenPrefillHint = (si: number, oldKey: string, newKey: string, value: string) => {
    setScreens(prev => prev.map((s, i) => {
      if (i !== si) return s;
      const hints = { ...s.prefillHints };
      if (oldKey && oldKey !== newKey) delete hints[oldKey];
      if (newKey) hints[newKey] = value;
      else if (oldKey) delete hints[oldKey];
      return { ...s, prefillHints: hints };
    }));
  };

  // ─── AI ───
  const ensureApiKey = (cb: () => void) => { if (apiKey) { cb(); return; } setShowApiKeyDialog(true); };

  const generateSystemPrompt = useCallback(async () => {
    if (!apiKey) return;
    setAiLoading(true);
    try {
      const result = await callGemini(`Generate a system prompt for a Bina.ai recipe: "${recipeName}" (${category}). Target: grassroots communities, ${selectedLanguages.join(',')}. Return ONLY the prompt text.`, apiKey);
      setSystemPrompt(result.trim());
      toast.success('System prompt generated');
    } catch (e) { toast.error(`${e instanceof Error ? e.message : 'Error'}`); }
    finally { setAiLoading(false); }
  }, [apiKey, recipeName, category, selectedLanguages]);

  const generateRecipeWithAI = useCallback(async () => {
    if (!apiKey || !recipeName) return;
    setAiLoading(true);
    try {
      const knowledgeContext = knowledgeFiles.filter(f => f.status === 'ready' && f.summary).map(f => `${f.name}: ${f.summary}`).join('\n');
      const parsed = await callGeminiJSON(
        `Design a Bina.ai recipe. Name: "${recipeName}", Desc: "${recipeDescription}", Category: ${category}.${knowledgeContext ? `\nKnowledge context from uploaded documents:\n${knowledgeContext}\nUse this knowledge to inform the screens, system prompt, and content.` : ''}
Available screen templates: ask_ai, camera_analysis, calculator, nearby_places, info_display, checklist
First screen must be home with isHome:true. Each non-home screen has exactly one template.
Do not assume any specific domain — use the recipe name, category, and knowledge context to guide content.`,
        apiKey,
        {
          type: 'OBJECT',
          properties: {
            system_prompt: { type: 'STRING' },
            screens: {
              type: 'ARRAY',
              items: {
                type: 'OBJECT',
                properties: {
                  id: { type: 'STRING' },
                  title: { type: 'STRING' },
                  isHome: { type: 'BOOLEAN' },
                  gridColumns: { type: 'INTEGER' },
                  templateId: { type: 'STRING', enum: ['ask_ai', 'camera_analysis', 'calculator', 'nearby_places', 'info_display', 'checklist', 'sms_dispatch'] },
                  fields: { type: 'OBJECT', properties: { hint: { type: 'STRING' }, ai_instruction: { type: 'STRING' } } },
                },
                required: ['id', 'title', 'templateId'],
              },
            },
            blocked_keywords: { type: 'ARRAY', items: { type: 'STRING' } },
            disclaimer: { type: 'STRING' },
          },
          required: ['system_prompt', 'screens'],
        },
      );
      if (parsed.system_prompt) setSystemPrompt(parsed.system_prompt);
      if (parsed.screens?.length > 0) {
        const newScreens: ScreenConfig[] = parsed.screens.map((s: { id: string; title: string; isHome?: boolean; gridColumns?: number; templateId?: string; fields?: Record<string, string> }) => ({
          id: s.id, title: s.title || '', isHome: !!s.isHome,
          gridColumns: s.gridColumns || 1,
          ...(s.templateId ? createScreen(s.templateId, s.fields) : { templateId: null, fieldValues: {}, disabledWidgets: [] }),
        }));
        setScreens(newScreens);
        setActiveScreenIndex(0);
      }
      if (parsed.blocked_keywords) setBlockedKeywords(parsed.blocked_keywords.join(', '));
      if (parsed.disclaimer) setIntroPage(prev => ({ ...prev, disclaimer: parsed.disclaimer }));
      toast.success('AI configured your recipe');
    } catch (e) { toast.error(`${e instanceof Error ? e.message : 'Error'}`); }
    finally { setAiLoading(false); }
  }, [apiKey, recipeName, recipeDescription, category, knowledgeFiles]);

  // ─── Knowledge ───
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    const fileList = Array.from(files);
    for (let fi = 0; fi < fileList.length; fi++) {
      const file = fileList[fi];
      const sizeStr = file.size > 1024 * 1024 ? `${(file.size / (1024 * 1024)).toFixed(1)} MB` : `${(file.size / 1024).toFixed(0)} KB`;
      setKnowledgeFiles(prev => [...prev, { name: file.name, size: sizeStr, status: 'uploading' }]);
      const text = await file.text();
      setKnowledgeFiles(prev => prev.map(f => f.name === file.name ? { ...f, status: 'processing' } : f));
      const chunks: string[] = []; const words = text.split(/\s+/); let cur = '';
      for (const w of words) { if (cur.length + w.length > 500) { chunks.push(cur.trim()); cur = w; } else cur += ' ' + w; }
      if (cur.trim()) chunks.push(cur.trim());
      let summary = `Document: ${file.name} (${chunks.length} chunks)`;
      if (apiKey && chunks.length > 0) {
        if (fi > 0) await new Promise(r => setTimeout(r, 1500));
        try { summary = (await callGemini(`Summarize in 2-3 sentences:\n\n${chunks.slice(0, 3).join('\n\n')}`, apiKey)).trim(); } catch {}
      }
      setKnowledgeFiles(prev => prev.map(f => f.name === file.name ? { ...f, status: 'ready', chunks: chunks.length, summary } : f));
    }
    e.target.value = '';
  };
  const removeFile = (i: number) => setKnowledgeFiles(prev => prev.filter((_, idx) => idx !== i));
  const generateKnowledgeSuggestions = useCallback(async () => {
    if (!apiKey) return;
    const ready = knowledgeFiles.filter(f => f.status === 'ready' && f.summary);
    if (!ready.length) { toast.error('No files'); return; }
    setAiLoading(true);
    try {
      const demoMeta = ready.map(f => DEMO_DOCUMENTS.find(d => d.name === f.name)).filter(Boolean);
      const metaHint = demoMeta.length > 0 ? `\n\nDocument metadata (use these exactly):\n${demoMeta.map(d => `- Author: ${d!.author}, Organisation: ${d!.org}\n  Links: ${d!.links.map(l => `${l.label}: ${l.url}`).join(', ')}`).join('\n')}` : '';
      setSuggestionsApplied(false);
      const parsed = await callGeminiJSON(
        `Based on these documents, suggest a Bina.ai recipe configuration.\n\nDocuments:\n${ready.map(f => `${f.name}: ${f.summary}`).join('\n\n')}${metaHint}\n\nAvailable screen templates: ask_ai (chat/Q&A), camera_analysis (photo analysis), calculator (numeric calc), nearby_places (location finder), info_display (static info), checklist (step-by-step guide), sms_dispatch (pre-configured contacts for SMS or phone calls — no AI needed).\nSuggest 2-4 screens relevant to the document content. First should be ask_ai for general Q&A. Category must be one of: Agriculture, Health, Education, Emergency, Finance, Environment.\nChoose an appropriate emoji icon (return the actual Unicode emoji character like 🌾, NOT text names like "seedling" or "emoji_seedling") for the recipe and a theme color (navy for general, forest for agriculture/nature, coral for emergency/health, amber for finance/education).\n\nAlso provide:\n- authorName and authorOrg: Use the document metadata author/org if provided above. Otherwise suggest appropriate ones.\n- links: Use the document metadata links if provided above. Otherwise suggest 2-3 relevant reference links.\n- homeHeading: A contextual heading for the home chat screen, e.g. "What crop issue can I help with?"\n- homeHint: An input placeholder for the home chat, e.g. "Describe your crop symptoms..."\n- sampleConversation: A sample conversation demonstrating the recipe in action with a userMessage, aiClarification, and userReply. Make it contextual to the uploaded documents.\n- For each screen: heading (screen heading text), hint (input placeholder or "call"/"sms" for sms_dispatch), description (for sms_dispatch: contacts as "Name | Phone" lines separated by newlines, e.g. "Ambulance | 999\\nFire Dept | 994"), buttonLabel (action button text like "Diagnose", "Calculate", "Find"), and aiInstruction (the AI prompt instruction — for ask_ai use "ask:{{user_text}}" but prefix with context like "ask:Based on the crop disease knowledge base, diagnose and recommend treatment for: {{user_text}}". For camera_analysis use "vision_ask:Analyze this photo and identify [specific thing based on content]. {{user_text}}". For sms_dispatch use the SMS message template e.g. "Emergency at my location. Need help."). Make aiInstruction specific and useful, not generic.`,
        apiKey,
        {
          type: 'OBJECT',
          properties: {
            recipeName: { type: 'STRING' },
            recipeDescription: { type: 'STRING' },
            category: { type: 'STRING', enum: ['Agriculture', 'Health', 'Education', 'Emergency', 'Finance', 'Environment'] },
            systemPrompt: { type: 'STRING' },
            recipeIcon: { type: 'STRING', description: 'A single Unicode emoji character like 🌾 or 🏥. Do NOT return emoji names like "seedling" — return the actual Unicode character.' },
            themeKey: { type: 'STRING', enum: ['navy', 'forest', 'coral', 'amber'] },
            authorName: { type: 'STRING' },
            authorOrg: { type: 'STRING' },
            links: { type: 'ARRAY', items: { type: 'OBJECT', properties: { label: { type: 'STRING' }, url: { type: 'STRING' } }, required: ['label', 'url'] } },
            homeHeading: { type: 'STRING', description: 'Contextual heading for the home chat screen, e.g. "What crop issue can I help with?"' },
            homeHint: { type: 'STRING', description: 'Input placeholder for home chat, e.g. "Describe your crop symptoms..."' },
            sampleConversation: { type: 'OBJECT', properties: { userMessage: { type: 'STRING' }, aiClarification: { type: 'STRING' }, userReply: { type: 'STRING' } }, required: ['userMessage', 'aiClarification', 'userReply'] },
            screens: {
              type: 'ARRAY',
              items: {
                type: 'OBJECT',
                properties: {
                  id: { type: 'STRING' },
                  title: { type: 'STRING' },
                  emoji: { type: 'STRING', description: 'A single Unicode emoji character like 📷 or 📊. Do NOT return emoji names — return the actual character.' },
                  templateId: { type: 'STRING', enum: ['ask_ai', 'camera_analysis', 'calculator', 'nearby_places', 'info_display', 'checklist', 'sms_dispatch'] },
                  heading: { type: 'STRING', description: 'Screen heading text' },
                  hint: { type: 'STRING', description: 'Input hint/placeholder text' },
                  description: { type: 'STRING', description: 'Short description of screen purpose' },
                  buttonLabel: { type: 'STRING', description: 'Action button text, e.g. "Diagnose", "Calculate", "Find"' },
                  aiInstruction: { type: 'STRING', description: 'AI prompt instruction, e.g. "ask:Based on crop disease knowledge, diagnose: {{user_text}}"' },
                },
                required: ['id', 'title', 'emoji', 'templateId', 'heading', 'hint', 'description', 'buttonLabel', 'aiInstruction'],
              },
            },
          },
          required: ['recipeName', 'recipeDescription', 'category', 'systemPrompt', 'recipeIcon', 'themeKey', 'authorName', 'authorOrg', 'links', 'homeHeading', 'homeHint', 'sampleConversation', 'screens'],
        },
      );
      setKnowledgeSuggestions(parsed);
      const allSel = { name: true, description: true, category: true, systemPrompt: true, icon: true, theme: true, author: true, links: true, homePreview: true, screens: Object.fromEntries(parsed.screens.map((_: unknown, i: number) => [i, true])) };
      setSuggestionSelections(allSel);
      applySuggestions(parsed, allSel);
      toast.success('Recipe configured — review and adjust below');
    } catch (e) { toast.error(`${e instanceof Error ? e.message : 'Error'}`); }
    finally { setAiLoading(false); }
  }, [apiKey, knowledgeFiles]);

  const applySuggestions = (directData?: typeof knowledgeSuggestions, directSel?: typeof suggestionSelections) => {
    const s = directData || knowledgeSuggestions;
    if (!s) return;
    const sel = directSel || suggestionSelections;
    if (sel.name && s.recipeName) setRecipeName(s.recipeName);
    if (sel.description && s.recipeDescription) setRecipeDescription(s.recipeDescription);
    if (sel.category && s.category) setCategory(s.category);
    if (sel.icon && s.recipeIcon) setRecipeIcon(sanitizeEmoji(s.recipeIcon));
    if (sel.theme && s.themeKey) {
      const t = THEMES.find(t => t.key === s.themeKey);
      if (t) { setSelectedTheme(t.key as ThemeKey); setCustomPrimary(t.primary); setCustomSecondary(t.secondary); }
    }
    if (sel.systemPrompt && s.systemPrompt) setSystemPrompt(s.systemPrompt);
    if (sel.author && (s.authorName || s.authorOrg)) setIntroPage(p => ({ ...p, authorName: s.authorName || p.authorName, authorOrg: s.authorOrg || p.authorOrg, authorVerified: true }));
    if (sel.links && s.links?.length) setIntroPage(p => ({ ...p, links: s.links }));
    const selectedScreens = s.screens?.filter((_: unknown, i: number) => sel.screens[i]) || [];
    if (selectedScreens.length > 0) {
      const hasHome = selectedScreens.some((_: unknown, i: number) => {
        const origIdx = s.screens.indexOf(selectedScreens[i]);
        return origIdx === 0;
      });
      const newScreens: ScreenConfig[] = selectedScreens.map((sc: { id: string; title: string; emoji: string; templateId: string; heading: string; hint: string; description: string; buttonLabel: string; aiInstruction: string }, i: number) => {
        const tid = sc.templateId || 'ask_ai';
        const overrides: Record<string, string> = {};
        switch (tid) {
          case 'ask_ai':
            overrides.heading = sc.heading || 'How can I help?';
            overrides.hint = sc.hint || 'Ask a question...';
            overrides.button_label = sc.buttonLabel || 'Ask';
            overrides.ai_instruction = sc.aiInstruction || 'ask:{{user_text}}';
            break;
          case 'camera_analysis':
            overrides.button_label = sc.buttonLabel || 'Analyse';
            overrides.ai_instruction = sc.aiInstruction?.startsWith('vision_ask:') ? sc.aiInstruction : `vision_ask:${sc.aiInstruction || 'Analyse this image. {{user_text}}'}`;
            break;
          case 'calculator':
            overrides.field_a_label = sc.heading || 'Value A';
            overrides.field_a_hint = sc.hint || 'Enter value';
            overrides.result_label = sc.buttonLabel || 'Result';
            break;
          case 'checklist':
            overrides.steps = sc.description ? sc.description.split(/[,;]/).map(s => s.trim() + '|text').join('\n') : 'Step 1|text\nStep 2|text\nStep 3|text';
            break;
          case 'nearby_places':
            overrides.heading = sc.heading || 'Find places near you';
            break;
          case 'info_display':
            overrides.text = sc.heading || 'Welcome';
            overrides.style = 'heading';
            break;
          case 'sms_dispatch':
            overrides.heading = sc.heading || 'Emergency Contacts';
            overrides.contact_type = sc.hint?.includes('call') ? 'call' : 'sms';
            overrides.contacts = sc.description || 'Contact 1 | +60123456789\nContact 2 | +60198765432';
            overrides.sms_template = sc.aiInstruction || 'Help needed. Please respond.';
            break;
        }
        return {
          id: (i === 0 && hasHome) ? 'home' : (sc.id || sc.title.toLowerCase().replace(/[^a-z0-9]+/g, '_')),
          title: (i === 0 && hasHome) ? '' : sc.title,
          isHome: i === 0 && hasHome,
          gridColumns: 1,
          ...createScreen(tid, overrides),
          screenIcon: sanitizeEmoji(sc.emoji),
          description: sc.description,
        };
      });
      if (sel.homePreview && hasHome && s.homeHeading) {
        const home = newScreens.find(ns => ns.isHome);
        if (home) {
          home.fieldValues = { ...home.fieldValues, heading: s.homeHeading, hint: s.homeHint || home.fieldValues.hint };
        }
      }
      setScreens(newScreens);
      setActiveScreenIndex(0);
    }
    setSuggestionsApplied(true);
    toast.success('Applied — review in Identity & Layout steps');
  };

  // ─── Translation ───
  // Translatable: recipeName, recipeDescription, systemPrompt, disclaimer, acceptLabel,
  //   screen titles, fieldValues (heading, hint, button_label, ai_instruction text portion, field labels, steps)
  // NOT translated: screen IDs, variable names, action prefixes (ask:, vision_ask:, go:, formula:, geolocate, set:, increment:),
  //   formula expressions, coordinates, theme, icon, technical config, knowledge summary (already summarised)

  const KEEP_ORIGINAL_FIELDS = new Set(['mode', 'form_field_count', 'columns', 'formula', 'formula_type', 'contact_type', 'contacts', 'data']);

  const collectTranslatableStrings = useCallback((): Record<string, string> => {
    const strings: Record<string, string> = {};
    strings['recipe.name'] = recipeName;
    strings['recipe.description'] = recipeDescription;
    strings['recipe.systemPrompt'] = systemPrompt;
    strings['recipe.disclaimer'] = introPage.disclaimer;
    strings['recipe.acceptLabel'] = introPage.acceptLabel;
    if (knowledgeSummary) strings['recipe.knowledgeSummary'] = knowledgeSummary;
    for (const s of screens) {
      if (s.title) strings[`screen.${s.id}.title`] = s.title;
      for (const [k, v] of Object.entries(s.fieldValues)) {
        if (!v || KEEP_ORIGINAL_FIELDS.has(k) || k.startsWith('_')) continue;
        strings[`screen.${s.id}.field.${k}`] = v;
      }
    }
    return strings;
  }, [recipeName, recipeDescription, systemPrompt, introPage.disclaimer, introPage.acceptLabel, knowledgeSummary, screens]);

  const translateToLanguage = useCallback(async (langCode: string) => {
    if (!apiKey || langCode === 'en') return;
    const langLabel = ALL_LANGUAGES.find(l => l.code === langCode)?.label || langCode;
    setTranslationStatus(prev => ({ ...prev, [langCode]: 'translating' }));
    try {
      const strings = collectTranslatableStrings();
      const keyList = Object.keys(strings);
      const indexed = keyList.map((k, i) => ({ idx: `t${i}`, key: k, value: strings[k] }));
      const prompt = indexed.map(e => `${e.idx}: "${e.value.replace(/"/g, '\\"')}"`).join('\n');
      const schemaProperties: Record<string, { type: string }> = {};
      const required: string[] = [];
      for (const e of indexed) { schemaProperties[e.idx] = { type: 'STRING' }; required.push(e.idx); }
      const raw = await callGeminiJSON<Record<string, string>>(
        `Translate the following numbered strings from English to ${langLabel} (${langCode}). This is for a mobile app used by grassroots communities.

Rules:
- Translate naturally, not word-for-word
- Keep {{variable}} placeholders exactly as-is
- Keep action prefixes like "ask:", "vision_ask:", "formula:", "go:", "geolocate", "set:", "increment:" at the start of strings exactly as-is — only translate the human-readable text after the prefix
- Keep technical terms if no good local equivalent exists
- Return the SAME numbered keys (t0, t1, ...) with translated values

${prompt}`,
        apiKey,
        { type: 'OBJECT', properties: schemaProperties, required },
        2,
        8192,
      );
      const parsed: Record<string, string> = {};
      for (const e of indexed) parsed[e.key] = raw[e.idx] || strings[e.key];
      const translation: RecipeTranslation = {
        recipeName: parsed['recipe.name'] || recipeName,
        recipeDescription: parsed['recipe.description'] || recipeDescription,
        systemPrompt: parsed['recipe.systemPrompt'] || systemPrompt,
        disclaimer: parsed['recipe.disclaimer'] || introPage.disclaimer,
        acceptLabel: parsed['recipe.acceptLabel'] || introPage.acceptLabel,
        screens: {},
      };
      for (const s of screens) {
        const screenTrans: { title: string; fieldValues: Record<string, string> } = {
          title: parsed[`screen.${s.id}.title`] || s.title,
          fieldValues: {},
        };
        for (const [k, v] of Object.entries(s.fieldValues)) {
          const transKey = `screen.${s.id}.field.${k}`;
          screenTrans.fieldValues[k] = parsed[transKey] || v;
        }
        translation.screens[s.id] = screenTrans;
      }
      setTranslations(prev => ({ ...prev, [langCode]: translation }));
      setTranslationStatus(prev => ({ ...prev, [langCode]: 'done' }));
    } catch {
      setTranslationStatus(prev => ({ ...prev, [langCode]: 'error' }));
    }
  }, [apiKey, collectTranslatableStrings, recipeName, recipeDescription, systemPrompt, introPage.disclaimer, introPage.acceptLabel, screens]);

  const translateAll = useCallback(async () => {
    if (!apiKey) return;
    setTranslatingAll(true);
    const nonEnLangs = selectedLanguages.filter(l => l !== 'en' && translationStatus[l] !== 'done');
    for (const lang of nonEnLangs) {
      await translateToLanguage(lang);
      if (nonEnLangs.indexOf(lang) < nonEnLangs.length - 1) {
        await new Promise(r => setTimeout(r, 1500));
      }
    }
    setTranslatingAll(false);
    toast.success(`Translated to ${nonEnLangs.length} language${nonEnLangs.length !== 1 ? 's' : ''}`);
  }, [apiKey, selectedLanguages, translationStatus, translateToLanguage]);

  // ─── YAML ───
  const yamlEsc = (s: string) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  const generateYaml = (langCode?: string, translation?: RecipeTranslation): string => {
    const t = (key: 'recipeName' | 'recipeDescription' | 'systemPrompt' | 'disclaimer' | 'acceptLabel', fallback: string) =>
      langCode && translation ? (translation[key] || fallback) : fallback;
    const tScreenTitle = (screenId: string, fallback: string) =>
      langCode && translation ? (translation.screens[screenId]?.title || fallback) : fallback;
    const tField = (screenId: string, fieldKey: string, fallback: string) =>
      langCode && translation ? (translation.screens[screenId]?.fieldValues[fieldKey] || fallback) : fallback;

    const name = t('recipeName', recipeName || 'My Recipe');
    const desc = t('recipeDescription', recipeDescription || 'A custom AI recipe');
    const sysPromptText = t('systemPrompt', systemPrompt).trim();
    const disclaimerText = t('disclaimer', introPage.disclaimer);
    const acceptLabelText = t('acceptLabel', introPage.acceptLabel);

    const id = recipeName.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'my_recipe';
    const sysPrompt = sysPromptText ? `  system_prompt: |\n    ${sysPromptText.split('\n').join('\n    ')}` : '  system_prompt: "You are a helpful assistant."';
    const blocked = blockedKeywords.split(',').map(k => k.trim()).filter(Boolean);
    const blockedYaml = blocked.length > 0 ? blocked.map(k => `    - ${k}`).join('\n') : '    - harmful';

    const allResolved = screens.map(s => resolveScreenWidgets(s, screens));
    const allTypes = allResolved.flat().map(w => w.type);
    const perms: string[] = [];
    if (allTypes.includes('camera_input')) perms.push('  - camera');
    if (allTypes.includes('geo_display')) perms.push('  - location');

    const hasCalc = allTypes.includes('slider') || allTypes.includes('metric_card');
    const vars = ['  user_text:    { type: string, default: "" }', '  ai_response:  { type: string, default: "" }'];
    if (allTypes.includes('camera_input')) vars.push('  photo_path:   { type: string, default: "" }');
    if (hasCalc) {
      vars.push('  calc_a:       { type: number, default: "0" }', '  calc_b:       { type: number, default: "0" }', '  calc_rate:    { type: number, default: "10" }', '  calc_result:  { type: number, default: "0" }');
      const hasC = screens.some(s => s.templateId === 'calculator' && !s.disabledWidgets.includes('input_c'));
      const hasD = screens.some(s => s.templateId === 'calculator' && !s.disabledWidgets.includes('input_d'));
      if (hasC) vars.push('  calc_c:       { type: number, default: "0" }');
      if (hasD) vars.push('  calc_d:       { type: number, default: "0" }');
    }
    const formScreens = screens.filter(s => s.templateId === 'ask_ai' && s.fieldValues.mode === 'form');
    if (formScreens.length > 0) {
      const maxFields = Math.max(...formScreens.map(s => parseInt(s.fieldValues.form_field_count || '2') || 2));
      for (let i = 1; i <= maxFields; i++) {
        const hasField = formScreens.some(s => s.fieldValues[`f${i}_label`]);
        if (hasField) vars.push(`  form_f${i}:     { type: string, default: "" }`);
      }
    }
    if (allTypes.includes('checklist_items')) {
      vars.push('  checklist_step: { type: number, default: "0" }');
    }
    if (screens.some(s => s.templateId === 'sms_dispatch' && s.fieldValues.contact_type === 'sms')) {
      vars.push('  sms_body:     { type: string, default: "" }');
    }

    const questionsYaml = screens.map(s => {
      if (!s.templateId) return '';
      if (s.templateId !== 'ask_ai') return '';
      const qs = [s.fieldValues.q1, s.fieldValues.q2, s.fieldValues.q3, s.fieldValues.q4]
        .map((q, qi) => q && q.trim() ? tField(s.id, `q${qi + 1}`, q) : '')
        .filter(Boolean);
      if (qs.length === 0) return '';
      return `  ${s.id}:\n${qs.map(q => `    - "${yamlEsc(q)}"`).join('\n')}`;
    }).filter(Boolean).join('\n');

    const nonHomeScreens = screens.filter(s => !s.isHome).map(s => ({ id: s.id, title: tScreenTitle(s.id, s.title), icon: s.screenIcon }));
    const screensYaml = screens.map((screen, si) => {
      const widgets = allResolved[si];
      const title = tScreenTitle(screen.id, screen.title || recipeName || 'My Recipe');
      const translatedWidgets = langCode && translation
        ? widgets.map(w => {
            const screenTrans = translation.screens[screen.id];
            if (!screenTrans) return w;
            const translatedProps = { ...w.props };
            for (const [pk, pv] of Object.entries(w.props)) {
              for (const [fk, fv] of Object.entries(screenTrans.fieldValues)) {
                if (pv === screen.fieldValues[fk] && fv) {
                  translatedProps[pk] = fv;
                  break;
                }
              }
            }
            return { ...w, props: translatedProps };
          })
        : widgets;
      const body = translatedWidgets.map(w => widgetToYaml(w, nonHomeScreens)).join('\n');
      let routingYaml = '';
      if (screen.routing && screen.routing.field && screen.routing.rules.length > 0) {
        const rulesStr = screen.routing.rules.map(r => `        - { value: "${r.value}", goto: "${r.goto}" }`).join('\n');
        routingYaml = `\n    next:\n      field: ${screen.routing.field}\n      rules:\n${rulesStr}${screen.routing.fallback ? `\n      fallback: ${screen.routing.fallback}` : ''}`;
      }
      return `  - id: ${screen.id}\n    title: "${yamlEsc(title)}"\n    body:\n${body}${routingYaml}`;
    }).join('\n');

    let formulas = '';
    if (hasCalc) {
      const calcScreen = screens.find(s => s.templateId === 'calculator');
      const expr = calcScreen ? resolveFormula(calcScreen) : '{{calc_a}} * {{calc_b}}';
      formulas = `\nformulas:\n  calc:\n    expression: "${expr}"\n    output: calc_result\n`;
    }
    let data = '';
    if (allTypes.includes('geo_display')) {
      data = `\ndata:\n  places:\n    type: points\n    items:\n      - { name: "Location 1", lat: 3.139, lng: 101.687, info: "Edit in YAML" }\n      - { name: "Location 2", lat: 3.145, lng: 101.710, info: "Edit in YAML" }\n`;
    }
    const loc = selectedLanguages.length > 0 ? `\nlocalisation:\n  supported:\n${selectedLanguages.map(l => `    - ${l}`).join('\n')}\n  default: ${selectedLanguages[0] || 'en'}\n` : '';
    const know = knowledgeSummary ? `\nknowledge:\n  always_loaded: |\n    ${knowledgeSummary.split('\n').join('\n    ')}\n  chunks: ${knowledgeFiles.filter(f => f.status === 'ready').reduce((a, f) => a + (f.chunks || 0), 0)}\n` : '';
    const questionsBlock = questionsYaml ? `\nquestions:\n${questionsYaml}\n` : '';
    let setupBlock = '';
    {
      let introYaml = `\nsetup:\n  intro_page:\n    accept_label: "${yamlEsc(acceptLabelText || 'I Understand')}"`;
      if (introPage.disclaimer) introYaml += `\n    disclaimer: "${yamlEsc(disclaimerText)}"`;
      if (introPage.coverPhoto) introYaml += `\n    cover_photo: true`;
      if (introPage.authorName) introYaml += `\n    author:\n      name: "${yamlEsc(introPage.authorName)}"${introPage.authorOrg ? `\n      organisation: "${yamlEsc(introPage.authorOrg)}"` : ''}${introPage.authorVerified ? '\n      verified: true' : ''}`;
      if (introPage.links.length > 0) {
        introYaml += `\n    links:`;
        introPage.links.forEach(l => { if (l.label && l.url) introYaml += `\n      - { label: "${yamlEsc(l.label)}", url: "${l.url}" }`; });
      }
      setupBlock = introYaml + '\n';
    }

    const catalogScreens = screens.filter(s => !s.isHome && s.templateId);
    const screenCatalog = catalogScreens.length > 0 ? `\nscreen_catalog:\n${catalogScreens.map(s => {
      const desc = s.description || generateScreenDescription(s);
      const accepted = getScreenAcceptedInputs(s);
      const hints = s.prefillHints || generatePrefillHints(s);
      const hintEntries = Object.entries(hints);
      let entry = `  - id: ${s.id}\n    title: "${yamlEsc(tScreenTitle(s.id, s.title))}"\n    template: ${s.templateId}`;
      if (s.screenIcon) entry += `\n    icon: "${s.screenIcon}"`;
      if (desc) entry += `\n    description: "${yamlEsc(desc)}"`;
      if (accepted.length) entry += `\n    accepted_inputs: [${accepted.join(', ')}]`;
      if (hintEntries.length) {
        entry += `\n    prefill_hints:`;
        hintEntries.forEach(([k, v]) => { entry += `\n      ${k}: ${v}`; });
      }
      return entry;
    }).join('\n')}\n` : '';

    const homeScreen = screens.find(s => s.isHome);
    const homeMode = homeScreen?.templateId ? 'chat' : 'grid';
    const triageBlock = `\ntriage:\n  home_mode: "${homeMode}"\n  max_clarifications: ${maxClarifications}\n  fallback: "${fallbackScreen || 'show_all'}"\n`;

    const langSuffix = langCode ? `\nlanguage: ${langCode}\n` : '';
    return `id: ${id}\nname: "${yamlEsc(name)}"\ndescription: "${yamlEsc(desc)}"\nicon: "${recipeIcon}"\nversion: "1.0.0"\ncategory: ${category}${langSuffix}\n\nauthor:\n  name: User\n  organisation: ""\n  verified: false\n\nmodel:\n  model_id: gemma-4-e2b-it\n  backend: cpu\n${sysPrompt}\n\ntheme:\n  primary: "${activePrimary}"\n  secondary: "${activeSecondary}"\n\nvariables:\n${vars.join('\n')}\n\nscreens:\n${screensYaml}\n${formulas}${data}\nsafety:\n  blocked_keywords:\n${blockedYaml}\n  escalation_message: "This request has been blocked for safety."\n  disclaimer: "${yamlEsc(disclaimerText || 'AI-generated content.')}"\n\npermissions:\n${perms.length > 0 ? perms.join('\n') : '  []'}${loc}${know}${questionsBlock}${setupBlock}${screenCatalog}${triageBlock}`;
  };

  // ─── Download YAML ───
  const downloadYaml = () => {
    const id = recipeName.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'my_recipe';
    const downloadFile = (content: string, filename: string) => {
      const blob = new Blob([content], { type: 'text/yaml' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    };
    downloadFile(generateYaml(), `${id}.yaml`);
    for (const [langCode, trans] of Object.entries(translations)) {
      if (translationStatus[langCode] === 'done') {
        downloadFile(generateYaml(langCode, trans), `${id}_${langCode}.yaml`);
      }
    }
  };

  // ─── Firestore persistence ───
  const API_KEY_STORAGE = 'bina_studio_api_key';

  useEffect(() => {
    const savedKey = localStorage.getItem(API_KEY_STORAGE) || import.meta.env.VITE_GEMINI_API_KEY || '';
    if (savedKey) setApiKey(savedKey);

    if (recipeId) {
      setPageLoading(true);
      getRecipe(recipeId)
        .then(recipe => {
          if (recipe) {
            loadRecipe(recipe);
          } else {
            toast.error('Recipe not found');
            navigate('/');
          }
        })
        .catch(() => { toast.error('Failed to load recipe'); navigate('/'); })
        .finally(() => { setPageLoading(false); loadedRef.current = true; pushHistory(); });
    } else {
      loadedRef.current = true;
      pushHistory();
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recipeId]);

  useEffect(() => {
    if (loadedRef.current) { setDirty(true); pushHistory(); }
  }, [recipeName, recipeDescription, recipeIcon, systemPrompt, blockedKeywords, introPage, category, selectedLanguages, selectedTheme, customPrimary, customSecondary, screens, knowledgeSummary, maxClarifications, fallbackScreen, pushHistory]);

  useEffect(() => {
    if (apiKey) localStorage.setItem(API_KEY_STORAGE, apiKey);
  }, [apiKey]);

  useEffect(() => {
    const el = previewContainerRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => {
      const { width, height } = entry.contentRect;
      setPhoneScale(Math.min(width / 310, height / 620, 1.2));
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const handleSave = async () => {
    if (!recipeName.trim()) { toast.error('Recipe name is required'); return; }
    const nonEnLangs = selectedLanguages.filter(l => l !== 'en');
    const untranslated = nonEnLangs.filter(l => translationStatus[l] !== 'done');
    if (nonEnLangs.length > 0 && untranslated.length > 0) {
      toast.error(`Translate all languages before publishing (${untranslated.length} remaining)`);
      return;
    }
    setSaving(true);
    try {
      const translatedYamls: Record<string, string> = {};
      for (const [langCode, trans] of Object.entries(translations)) {
        if (translationStatus[langCode] === 'done') {
          translatedYamls[langCode] = generateYaml(langCode, trans);
        }
      }
      const config: RecipeConfig = {
        recipeName, recipeDescription, recipeIcon, systemPrompt,
        blockedKeywords, disclaimer: introPage.disclaimer, category, selectedLanguages,
        selectedTheme, customPrimary, customSecondary, screens, knowledgeSummary,
        introPage, maxClarifications, ...(fallbackScreen ? { fallbackScreen } : {}),
        generatedYaml: generateYaml(),
        translations,
        translationStatus,
      };
      const saveData = { ...config, translatedYamls };
      if (recipeId) {
        await updateRecipe(recipeId, saveData);
      } else {
        const newId = await createFirestoreRecipe(saveData as RecipeConfig);
        navigate(`/studio/${newId}`, { replace: true });
      }
      setDirty(false);
      setPublishSuccess(true);
    } catch (e) {
      toast.error('Could not save your recipe. Check your connection and try again.');
    } finally {
      setSaving(false);
    }
  };

  // ─── Language filter ───
  const filteredGroups = LANGUAGE_GROUPS.map(g => ({ ...g, languages: g.languages.filter(l => !langSearch || l.label.toLowerCase().includes(langSearch.toLowerCase()) || l.native.toLowerCase().includes(langSearch.toLowerCase())) })).filter(g => g.languages.length > 0);

  // ─── Preview renderer ───
  const renderWidget = (w: WidgetConfig, wi: number) => {
    const p = w.props;
    switch (w.type) {
      case 'text_label': return <p key={wi} className="text-[10px] font-semibold px-1" style={{ color: activePrimary }}>{p.text || 'Welcome'}</p>;
      case 'text_input': {
        const inputType = p.input_type || 'text';
        if (inputType === 'dropdown') {
          const opts = (p.options || '').split(',').map((o: string) => o.trim()).filter(Boolean);
          return (
            <div key={wi} className="px-1 py-0.5">
              {p.label && <p className="text-[8px] text-stone-500 mb-0.5 px-1">{p.label}</p>}
              <div className="w-full h-7 rounded-lg border border-stone-200 bg-white flex items-center px-2 justify-between">
                <span className="text-[9px] text-stone-500">{opts[0] || 'Select...'}</span>
                <ChevronDown size={8} className="text-stone-500" />
              </div>
            </div>
          );
        }
        if (inputType === 'toggle') {
          return (
            <div key={wi} className="px-1 py-1 rounded-lg bg-white flex items-center justify-between">
              <span className="text-[9px] text-stone-600 px-1">{p.label || 'Toggle'}</span>
              <div className="w-7 h-4 rounded-full bg-stone-300 relative mr-1">
                <div className="absolute top-0.5 left-0.5 w-3 h-3 rounded-full bg-white" />
              </div>
            </div>
          );
        }
        return (
          <div key={wi} className="px-1 py-0.5">
            {p.label && <p className="text-[8px] text-stone-500 mb-0.5 px-1">{p.label}</p>}
            <div className="w-full h-7 rounded-lg border border-stone-200 bg-white flex items-center px-2">
              {p._hasMic === 'true' && <Mic size={10} className="shrink-0 mr-1.5" style={{ color: activePrimary }} />}
              <span className="text-[9px] text-stone-500 flex-1">{p.hint || p.label || 'Type here...'}</span>
              {inputType === 'number' && <span className="text-[8px] text-stone-400 ml-auto">#</span>}
            </div>
          </div>
        );
      }
      case 'voice_input': return null;
      case 'camera_input': return <div key={wi} className="rounded-xl bg-stone-800 flex items-center justify-center" style={{ height: 70 }}><Camera size={18} className="text-white/50" /></div>;
      case 'action_button': return <button key={wi} className={`w-full py-1.5 rounded-lg text-[10px] font-semibold ${p.style === 'secondary' ? 'border border-stone-300 text-stone-700 bg-white' : p.style === 'danger' ? 'text-white bg-red-600' : 'text-white'}`} style={p.style !== 'secondary' && p.style !== 'danger' ? { background: activePrimary } : undefined}>{p.label || 'Submit'}</button>;
      case 'markdown_output': return <div key={wi} className="rounded-xl p-2 bg-white/50"><div className="space-y-1"><div className="h-1.5 rounded-full bg-stone-300/50 w-full" /><div className="h-1.5 rounded-full bg-stone-300/50 w-4/5" /><div className="h-1.5 rounded-full bg-stone-300/50 w-3/5" /></div></div>;
      case 'macro_grid': {
        const cols = parseInt(p.columns) || 2;
        const others = screens.filter(s => !s.isHome);
        const templateEmoji: Record<string, string> = {
          ask_ai: '\u{1F4AC}', camera_analysis: '\u{1F4F7}', calculator: '\u{1F9EE}',
          nearby_places: '\u{1F4CD}', info_display: '\u{1F4DD}', checklist: '\u{2705}',
          sms_dispatch: '\u{1F4F1}',
        };
        return (
          <div key={wi} className="gap-1.5" style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
            {others.map((s, i) => {
              const tmplEmoji = s.templateId ? templateEmoji[s.templateId] || '' : '';
              const screenEmoji = s.screenIcon || tmplEmoji || '\u{1F4CB}';
              return (
                <button key={s.id} onClick={() => setActiveScreenIndex(screens.indexOf(s))}
                  className="rounded-xl p-2.5 flex flex-col items-center gap-1 cursor-pointer hover:opacity-80"
                  style={{ background: activePrimary, boxShadow: '0 1px 3px rgba(28,25,23,0.08)' }}>
                  <span className="text-base">{screenEmoji}</span>
                  <span className="text-[7px] font-semibold leading-tight text-center text-white">{s.title}</span>
                </button>
              );
            })}
          </div>
        );
      }
      case 'slider': return <div key={wi} className="px-1 py-1"><p className="text-[8px] text-stone-500 mb-0.5">{p.label || 'Value'}</p><div className="h-1.5 rounded-full bg-stone-300 relative"><div className="absolute left-1/3 -top-1 w-3 h-3 rounded-full" style={{ background: activePrimary }} /></div></div>;
      case 'metric_card': return (
        <div key={wi} className="rounded-xl p-3 bg-white/70 text-center">
          <span className="text-lg font-bold" style={{ color: activePrimary }}>
            {p.prefix && <span className="text-xs font-medium mr-0.5">{p.prefix}</span>}
            0
            {p.suffix && <span className="text-xs font-medium ml-0.5">{p.suffix}</span>}
          </span>
          <p className="text-[8px] text-stone-500">{p.label || 'Result'}</p>
        </div>
      );
      case 'geo_display': return <div key={wi} className="rounded-xl p-2 bg-white/70 flex items-center gap-2"><span className="text-xs">{'\u{1F4CD}'}</span><span className="text-[9px] text-stone-500">Nearby places</span></div>;
      case 'progress_bar': {
        const total = parseInt(p.total) || 3;
        const current = 1;
        return (
          <div key={wi} className="px-1 py-1">
            <p className="text-[8px] text-stone-500 mb-0.5">Step {current} of {total}</p>
            <div className="h-1.5 rounded-full bg-stone-200 overflow-hidden">
              <div className="h-full rounded-full transition-all" style={{ width: `${(current / total) * 100}%`, background: activePrimary }} />
            </div>
          </div>
        );
      }
      case 'checklist_items': {
        let steps: { label: string; type: string }[] = [];
        try { steps = JSON.parse(p.items || '[]'); } catch {}
        return (
          <div key={wi} className="space-y-1 px-1">
            {steps.map((step, si) => (
              <div key={si} className="flex items-center gap-2 rounded-lg px-2 py-1.5" style={{ background: si === 0 ? activePrimary + '15' : 'white' }}>
                <div className="w-4 h-4 rounded-full flex items-center justify-center text-[8px] font-bold"
                  style={{ background: si === 0 ? activePrimary : '#E7E0D8', color: si === 0 ? 'white' : '#A8A29E' }}>
                  {si + 1}
                </div>
                <span className="text-[9px] font-medium" style={{ color: si === 0 ? activePrimary : '#6B7280' }}>{step.label}</span>
                <span className="text-[7px] text-stone-500 ml-auto">{step.type}</span>
              </div>
            ))}
          </div>
        );
      }
      default: return null;
    }
  };

  const renderScreenEditor = (screen: ScreenConfig, si: number) => {
    if (!screen.templateId) return null;
    const def = getScreenTemplate(screen.templateId);
    if (!def) return null;

    return (
      <div className="space-y-2">
          {def.fields.map(f => {
            if (!checkShowWhen(f.showWhen, screen.fieldValues)) return null;
            return (
              <div key={f.key} className="flex items-center gap-2">
                <label className="text-[10px] text-stone-500 w-20 shrink-0">{f.label}</label>
                {f.type === 'select' ? (
                  <select value={screen.fieldValues[f.key] || f.defaultValue}
                    onChange={e => updateScreenField(si, f.key, e.target.value)}
                    className="flex-1 h-6 text-[11px] text-stone-700 rounded border border-stone-200 bg-white px-1.5 outline-none">
                    {f.options?.map(o => <option key={o} value={o}>{o}</option>)}
                  </select>
                ) : f.type === 'textarea' ? (
                  <textarea value={screen.fieldValues[f.key] || ''} onChange={e => updateScreenField(si, f.key, e.target.value)}
                    placeholder={f.placeholder}
                    className="flex-1 text-[11px] text-stone-700 rounded border border-stone-200 bg-white px-1.5 py-1 outline-none resize-none" rows={3} />
                ) : (
                  <input value={screen.fieldValues[f.key] || ''} onChange={e => updateScreenField(si, f.key, e.target.value)}
                    placeholder={f.placeholder}
                    className="flex-1 h-6 text-[11px] text-stone-700 rounded border border-stone-200 bg-white px-1.5 outline-none focus:border-stone-400" />
                )}
              </div>
            );
          })}
          {screen.templateId === 'calculator' && (
            <div className="rounded bg-stone-50 px-2 py-1.5 mt-1">
              <p className="text-[9px] text-stone-500 mb-0.5">Formula preview</p>
              <code className="text-[10px] font-mono text-stone-700">{resolveFormula(screen)}</code>
            </div>
          )}
          {/* Dynamic form field editor for ask_ai form mode */}
          {screen.templateId === 'ask_ai' && screen.fieldValues.mode === 'form' && (() => {
            const fieldCount = parseInt(screen.fieldValues.form_field_count || '2') || 2;
            return (
              <div className="border-t border-stone-100 pt-2 space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-[10px] font-semibold text-stone-500 uppercase tracking-wider">Form Fields</label>
                  <button onClick={() => {
                    const nc = fieldCount + 1;
                    setScreens(prev => prev.map((s, i) => i !== si ? s : {
                      ...s, fieldValues: { ...s.fieldValues, form_field_count: String(nc), [`f${nc}_label`]: '', [`f${nc}_type`]: 'text' },
                    }));
                  }}
                    className="text-[9px] font-medium px-1.5 py-0.5 rounded" style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                    + Add Field
                  </button>
                </div>
                {Array.from({ length: fieldCount }, (_, idx) => idx + 1).map(n => (
                  <div key={n} className="flex items-center gap-1.5">
                    <span className="text-[9px] text-stone-500 w-3 shrink-0">{n}</span>
                    <input value={screen.fieldValues[`f${n}_label`] || ''}
                      onChange={e => updateScreenField(si, `f${n}_label`, e.target.value)}
                      placeholder={`Field ${n} label`}
                      className="flex-1 h-6 text-[11px] text-stone-700 rounded border border-stone-200 bg-white px-1.5 outline-none focus:border-stone-400" />
                    <select value={screen.fieldValues[`f${n}_type`] || 'text'}
                      onChange={e => updateScreenField(si, `f${n}_type`, e.target.value)}
                      className="w-[72px] h-6 text-[10px] text-stone-600 rounded border border-stone-200 bg-white px-1 outline-none">
                      <option value="text">Text</option>
                      <option value="number">Number</option>
                      <option value="dropdown">Dropdown</option>
                      <option value="toggle">Toggle</option>
                    </select>
                    {screen.fieldValues[`f${n}_type`] === 'dropdown' && (
                      <input value={screen.fieldValues[`f${n}_options`] || ''}
                        onChange={e => updateScreenField(si, `f${n}_options`, e.target.value)}
                        placeholder="opt1, opt2"
                        className="w-24 h-6 text-[10px] text-stone-600 rounded border border-stone-200 bg-white px-1 outline-none" />
                    )}
                    {fieldCount > 1 && (
                      <button onClick={() => {
                        const newFV = { ...screen.fieldValues };
                        for (let j = n; j < fieldCount; j++) {
                          newFV[`f${j}_label`] = newFV[`f${j + 1}_label`] || '';
                          newFV[`f${j}_type`] = newFV[`f${j + 1}_type`] || 'text';
                          newFV[`f${j}_options`] = newFV[`f${j + 1}_options`] || '';
                        }
                        delete newFV[`f${fieldCount}_label`];
                        delete newFV[`f${fieldCount}_type`];
                        delete newFV[`f${fieldCount}_options`];
                        newFV.form_field_count = String(fieldCount - 1);
                        setScreens(prev => prev.map((s, i) => i === si ? { ...s, fieldValues: newFV } : s));
                      }}
                        className="text-stone-400 hover:text-red-500 shrink-0" aria-label="Remove field"><X size={10} /></button>
                    )}
                  </div>
                ))}
              </div>
            );
          })()}
          {/* AI Routing: Description + Prefill Hints */}
          {!screen.isHome && (
            <div className="border-t border-stone-100 pt-2 space-y-2">
              <p className="text-[10px] font-semibold text-stone-500 uppercase tracking-wider">AI Routing</p>
              <p className="text-[10px] text-stone-500 -mt-1">Helps the AI understand this screen so it can route users here in chat mode.</p>
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-[10px] text-stone-500">Screen Summary</label>
                  <div className="flex items-center gap-1">
                    <button onClick={() => regenerateScreenMeta(si)}
                      className="text-[9px] font-medium px-1.5 py-0.5 rounded" style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                      Regenerate
                    </button>
                    <button onClick={() => {
                      ensureApiKey(async () => {
                        setAiLoading(true);
                        try {
                          const base = screen.description || generateScreenDescription(screen);
                          const result = await callGemini(
                            `Rewrite this screen summary for an on-device LLM routing prompt. Make it richer with trigger phrases and natural language synonyms a user might say. Keep it under 80 words. Input:\n${base}`,
                            apiKey,
                          );
                          updateScreenDescription(si, result.trim());
                          toast.success('Description polished');
                        } catch (e) { toast.error(`${e instanceof Error ? e.message : 'Error'}`); }
                        finally { setAiLoading(false); }
                      });
                    }}
                      disabled={aiLoading}
                      className="text-[9px] font-medium px-1.5 py-0.5 rounded flex items-center gap-0.5"
                      style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                      {aiLoading ? <Loader2 size={8} className="animate-spin" /> : <Sparkles size={8} />} Polish
                    </button>
                  </div>
                </div>
                <textarea value={screen.description || generateScreenDescription(screen)}
                  onChange={e => updateScreenDescription(si, e.target.value)}
                  className="w-full text-[10px] text-stone-600 rounded border border-stone-200 bg-white px-2 py-1 outline-none resize-none" rows={3} />
              </div>
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="text-[10px] text-stone-500">Prefill Hints</label>
                  <button onClick={() => {
                    const hints = generatePrefillHints(screen);
                    setScreens(prev => prev.map((s, i) => i === si ? { ...s, prefillHints: hints } : s));
                  }}
                    className="text-[9px] font-medium px-1.5 py-0.5 rounded" style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                    Auto-derive
                  </button>
                </div>
                {Object.entries(screen.prefillHints || {}).map(([key, val]) => (
                  <div key={key} className="flex items-center gap-1.5 mb-1">
                    <input value={key}
                      onChange={e => updateScreenPrefillHint(si, key, e.target.value, val)}
                      placeholder="entity"
                      className="w-24 h-5 text-[10px] rounded border border-stone-200 bg-white px-1.5 outline-none font-mono" />
                    <span className="text-[9px] text-stone-500">{'→'}</span>
                    <input value={val}
                      onChange={e => updateScreenPrefillHint(si, key, key, e.target.value)}
                      placeholder="bind_var"
                      className="w-20 h-5 text-[10px] rounded border border-stone-200 bg-white px-1.5 outline-none font-mono" />
                    <button onClick={() => updateScreenPrefillHint(si, key, '', '')} className="text-stone-400 hover:text-red-500" aria-label="Remove hint"><X size={10} /></button>
                  </div>
                ))}
                <button onClick={() => updateScreenPrefillHint(si, '', `hint_${Date.now()}`, '')}
                  className="text-[9px] font-medium px-1.5 py-0.5 rounded" style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                  + Add Hint
                </button>
              </div>
            </div>
          )}
      </div>
    );
  };

  // ─── Render ───
  if (pageLoading) {
    return (
      <div className="flex h-full min-h-[100dvh] items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <Loader2 size={32} className="animate-spin text-stone-500" />
          <span className="text-sm text-stone-500">Loading recipe...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-full min-h-[100dvh]">
      {/* Left panel */}
      <div className="flex-[3] flex flex-col overflow-y-auto border-r border-stone-200">
        <div className="px-8 pt-8 pb-4 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-stone-900">Recipe Studio</h1>
            <p className="text-sm text-stone-500 mt-1">{recipeId ? 'Editing recipe' : 'Turn your expertise into an AI-powered tool'}</p>
          </div>
          <div className="flex items-center gap-1">
            {dirty && <span className="text-[10px] font-medium text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full mr-1">Unsaved</span>}
            <button onClick={undo} disabled={!canUndo} className="min-w-[44px] min-h-[44px] flex items-center justify-center rounded-md hover:bg-stone-100 disabled:opacity-30" title="Undo (Cmd+Z)" aria-label="Undo"><Undo2 size={16} className="text-stone-500" /></button>
            <button onClick={redo} disabled={!canRedo} className="min-w-[44px] min-h-[44px] flex items-center justify-center rounded-md hover:bg-stone-100 disabled:opacity-30" title="Redo (Cmd+Shift+Z)" aria-label="Redo"><Redo2 size={16} className="text-stone-500" /></button>
          </div>
        </div>

        {/* Steps */}
        <div className="px-8 pb-6">
          <div className="flex items-center">
            {STEPS.map((step, i) => {
              const Icon = step.icon; const isActive = currentStep === step.id; const done = currentStep > step.id;
              const stepColor = step.color;
              return (
                <div key={step.id} className="flex items-center flex-1">
                  <button onClick={() => setCurrentStep(step.id)} className="flex items-center gap-2">
                    <div className="w-9 h-9 rounded-full flex items-center justify-center border-2 transition-all duration-200" style={{ background: isActive || done ? stepColor : '#F5F0EB', borderColor: isActive || done ? stepColor : '#E7E0D8' }}>
                      <Icon size={16} style={{ color: isActive || done ? 'white' : '#A8A29E' }} />
                    </div>
                    <span className="text-xs font-semibold hidden lg:inline" style={{ color: isActive ? stepColor : done ? '#57534E' : '#A8A29E' }}>{step.label}</span>
                  </button>
                  {i < STEPS.length - 1 && <div className="flex-1 h-0.5 mx-3 rounded-full" style={{ background: done ? STEPS[i + 1].color + '40' : '#E7E0D8' }} />}
                </div>
              );
            })}
          </div>
        </div>

        <div className="flex-1 px-8 pb-4">
          {/* ─── Step 2: Identity ─── */}
          {currentStep === 2 && (
            <div className="space-y-5 max-w-2xl mx-auto">
              <h2 className="text-lg font-semibold text-stone-900">Define Identity</h2>
              <p className="text-sm text-stone-500 -mt-3">Give your recipe a personality that resonates with your community.</p>
              <div><label className="text-sm font-medium text-stone-700 mb-1.5 block">Recipe Name</label><Input value={recipeName} onChange={e => setRecipeName(e.target.value)} placeholder="e.g. Health Assistant" /></div>
              <div><label className="text-sm font-medium text-stone-700 mb-1.5 block">Description</label><Input value={recipeDescription} onChange={e => setRecipeDescription(e.target.value)} placeholder="Short description" /></div>

              {/* Compact row: Icon + Cover Photo + Theme */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-xs text-stone-500 mb-1 block">Icon</label>
                  <button onClick={() => setShowIconPicker(true)}
                    className="w-full h-16 rounded-xl border-2 border-stone-200 flex items-center justify-center text-3xl hover:border-stone-300 cursor-pointer bg-white">{recipeIcon}</button>
                </div>
                <div>
                  <label className="text-xs text-stone-500 mb-1 block">Cover Photo</label>
                  {introPage.coverPhoto ? (
                    <div className="relative rounded-xl overflow-hidden border border-stone-200 h-16 cursor-pointer" onClick={() => coverInputRef.current?.click()}>
                      <img src={introPage.coverPhoto} alt="Cover" className="w-full h-full object-cover" />
                      <button onClick={e => { e.stopPropagation(); setIntroPage(p => ({ ...p, coverPhoto: undefined })); }}
                        className="absolute top-1 right-1 w-5 h-5 rounded-full bg-stone-900/50 flex items-center justify-center hover:bg-stone-900/70" aria-label="Remove cover photo"><X size={10} className="text-white" /></button>
                    </div>
                  ) : (
                    <button onClick={() => coverInputRef.current?.click()}
                      className="w-full h-16 rounded-xl border-2 border-dashed border-stone-300 flex flex-col items-center justify-center gap-0.5 hover:border-stone-400 cursor-pointer">
                      <Camera size={16} className="text-stone-500" />
                      <span className="text-[9px] text-stone-500">Upload</span>
                    </button>
                  )}
                  <input ref={coverInputRef} type="file" accept="image/*" className="hidden" onChange={e => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    const reader = new FileReader();
                    reader.onload = () => setIntroPage(p => ({ ...p, coverPhoto: reader.result as string }));
                    reader.readAsDataURL(file);
                    e.target.value = '';
                  }} />
                </div>
                <div>
                  <label className="text-xs text-stone-500 mb-1 block">Theme</label>
                  <button onClick={() => setShowThemePicker(true)}
                    className="w-full h-16 rounded-xl border-2 border-stone-200 flex items-center justify-center gap-2 hover:border-stone-300 cursor-pointer bg-white">
                    <div className="w-5 h-5 rounded-full" style={{ background: activePrimary }} />
                    <div className="w-5 h-5 rounded-full" style={{ background: activeSecondary }} />
                    <span className="text-[10px] text-stone-500">{THEMES.find(t => t.key === selectedTheme)?.label}</span>
                  </button>
                </div>
              </div>

              {/* Languages — compact summary */}
              <div>
                <button onClick={() => setShowLangDialog(true)}
                  className="w-full flex items-center gap-2 px-3 py-2.5 rounded-xl border border-stone-200 bg-white hover:border-stone-300 text-left cursor-pointer">
                  <Globe size={14} className="text-stone-500 shrink-0" />
                  <span className="text-xs text-stone-600 flex-1 truncate">
                    {selectedLanguages.length > 0
                      ? selectedLanguages.map(c => ALL_LANGUAGES.find(l => l.code === c)?.label || c).join(', ')
                      : 'Select languages...'}
                  </span>
                  {selectedLanguages.length > 0 && <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-full shrink-0" style={{ background: '#C45A3A', color: 'white' }}>{selectedLanguages.length}</span>}
                  <ChevronRight size={14} className="text-stone-500 shrink-0" />
                </button>
              </div>

              <div><label className="text-sm font-medium text-stone-700 mb-1.5 block">Category</label><Select value={category} onValueChange={setCategory}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent>{CATEGORIES.map(c => <SelectItem key={c} value={c}>{c}</SelectItem>)}</SelectContent></Select></div>

              {/* Author */}
              <div className="grid grid-cols-2 gap-3">
                <div><label className="text-sm font-medium text-stone-700 mb-1.5 block">Author Name</label><Input value={introPage.authorName} onChange={e => setIntroPage(p => ({ ...p, authorName: e.target.value }))} placeholder="Your name" /></div>
                <div><label className="text-sm font-medium text-stone-700 mb-1.5 block">Organisation</label><Input value={introPage.authorOrg} onChange={e => setIntroPage(p => ({ ...p, authorOrg: e.target.value }))} placeholder="Your org" /></div>
              </div>
              {/* Links */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-sm font-medium text-stone-700">Links</label>
                  <button onClick={() => setIntroPage(p => ({ ...p, links: [...p.links, { label: '', url: '' }] }))}
                    className="text-xs font-medium px-2 py-0.5 rounded" style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                    + Add Link
                  </button>
                </div>
                {introPage.links.map((link, li) => (
                  <div key={li} className="flex items-center gap-2 mb-1.5">
                    <Input value={link.label} onChange={e => setIntroPage(p => ({ ...p, links: p.links.map((l, i) => i === li ? { ...l, label: e.target.value } : l) }))} placeholder="Label" className="h-8 text-xs flex-1" />
                    <Input value={link.url} onChange={e => setIntroPage(p => ({ ...p, links: p.links.map((l, i) => i === li ? { ...l, url: e.target.value } : l) }))} placeholder="https://..." className="h-8 text-xs flex-1" />
                    <button onClick={() => setIntroPage(p => ({ ...p, links: p.links.filter((_, i) => i !== li) }))} className="text-stone-400 hover:text-red-500" aria-label="Remove link"><X size={14} /></button>
                  </div>
                ))}
              </div>

              {/* System prompt & safety */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-sm font-medium text-stone-700">System Prompt</label>
                  <button onClick={() => ensureApiKey(generateSystemPrompt)} disabled={aiLoading || !recipeName} className="text-xs font-medium px-3 py-1 rounded-md flex items-center gap-1" style={{ background: '#C45A3A10', color: '#C45A3A', opacity: !recipeName ? 0.4 : 1 }}>{aiLoading ? <Loader2 size={12} className="animate-spin" /> : <Sparkles size={12} />} AI Generate</button>
                </div>
                <Textarea value={systemPrompt} onChange={e => setSystemPrompt(e.target.value)} placeholder="You are a helpful assistant..." rows={5} />
              </div>
              <div><label className="text-sm font-medium text-stone-700 mb-1.5 block">Blocked Keywords</label><Input value={blockedKeywords} onChange={e => setBlockedKeywords(e.target.value)} placeholder="Comma-separated" /></div>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="text-xs text-stone-500 mb-1 block">Disclaimer</label><Input value={introPage.disclaimer} onChange={e => setIntroPage(p => ({ ...p, disclaimer: e.target.value }))} placeholder="AI-generated content..." className="h-8 text-xs" /></div>
                <div><label className="text-xs text-stone-500 mb-1 block">Accept Button Label</label><Input value={introPage.acceptLabel} onChange={e => setIntroPage(p => ({ ...p, acceptLabel: e.target.value }))} placeholder="I Understand" className="h-8 text-xs" /></div>
              </div>
            </div>
          )}

          {/* ─── Step 3: Style & Layout ─── */}
          {currentStep === 3 && (
            <div className="space-y-6 max-w-2xl mx-auto">
              <h2 className="text-lg font-semibold text-stone-900">Style & Layout</h2>
              <p className="text-sm text-stone-500 -mt-4">Design the experience your Builders will see on their devices.</p>

              {/* Home Screen Config */}
              {(() => {
                const homeScreen = screens.find(s => s.isHome);
                const homeIdx = screens.findIndex(s => s.isHome);
                const isChatMode = !!homeScreen?.templateId;
                const nonHomeScreens = screens.filter(s => !s.isHome);
                return (
                  <div className="rounded-xl border-2 border-stone-100 bg-stone-50/50 p-4 space-y-4">
                    <div>
                      <label className="text-sm font-semibold text-stone-800 mb-1 block">Home Screen</label>
                      <p className="text-xs text-stone-500 mb-3">How builders navigate your recipe on first launch</p>
                      {nonHomeScreens.length > 0 && (
                        <div className="grid grid-cols-2 gap-3">
                          {[
                            { mode: 'chat' as const, icon: MessageCircle, label: 'Chat-first', desc: 'Builder speaks or types; AI routes to the right screen.', hasTemplate: true },
                            { mode: 'grid' as const, icon: LayoutGrid, label: 'Grid-first', desc: 'Builder picks a screen directly from buttons.', hasTemplate: false },
                          ].map(opt => {
                            const isSelected = opt.hasTemplate ? isChatMode : !isChatMode;
                            return (
                              <button key={opt.mode} onClick={() => {
                                if (isSelected) return;
                                setScreens(prev => prev.map(s => {
                                  if (!s.isHome) return s;
                                  return opt.hasTemplate
                                    ? { ...s, templateId: 'ask_ai', fieldValues: { heading: s.fieldValues.heading || 'How can I help?', hint: s.fieldValues.hint || 'Ask a question...' }, disabledWidgets: [] }
                                    : { ...s, templateId: null, fieldValues: {}, disabledWidgets: [] };
                                }));
                              }}
                                className="flex flex-col gap-2 p-3 rounded-xl border-2 text-left transition-all"
                                style={{ borderColor: isSelected ? '#C45A3A' : '#E7E0D8', background: isSelected ? '#C45A3A08' : 'white' }}>
                                <div className="flex items-center gap-2">
                                  <opt.icon size={16} style={{ color: isSelected ? '#C45A3A' : '#A8A29E' }} />
                                  <span className="text-xs font-semibold" style={{ color: isSelected ? '#C45A3A' : '#4B5563' }}>{opt.label}</span>
                                </div>
                                <p className="text-[10px] text-stone-500 leading-relaxed">{opt.desc}</p>
                              </button>
                            );
                          })}
                        </div>
                      )}
                    </div>

                    {/* Grid-first: column layout */}
                    {!isChatMode && nonHomeScreens.length > 0 && (
                      <div>
                        <label className="text-[10px] font-medium text-stone-500 uppercase tracking-wider mb-2 block">Grid Layout</label>
                        <div className="flex gap-2">
                          {[1, 2, 3].map(cols => {
                            const selected = (homeScreen?.gridColumns || 1) === cols;
                            const previewItems = Math.min(nonHomeScreens.length, cols * 2);
                            return (
                              <button key={cols} onClick={() => updateGridColumns(cols)}
                                className="flex-1 rounded-lg border-2 p-2.5 flex flex-col items-center gap-1.5 transition-all"
                                style={{ borderColor: selected ? '#C45A3A' : '#E7E0D8', background: selected ? '#C45A3A08' : 'white' }}>
                                <div className="gap-1" style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)`, width: '100%' }}>
                                  {Array.from({ length: previewItems }).map((_, j) => (
                                    <div key={j} className="h-3 rounded" style={{ background: selected ? '#C45A3A' : '#E7E0D8' }} />
                                  ))}
                                </div>
                                <span className="text-[9px] font-medium" style={{ color: selected ? '#C45A3A' : '#A8A29E' }}>{cols} col{cols > 1 ? 's' : ''}</span>
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* Chat-first: simplified config */}
                    {isChatMode && homeScreen && homeIdx >= 0 && (
                      <div className="space-y-3">
                        <div>
                          <label className="text-[10px] font-medium text-stone-500 uppercase tracking-wider mb-1 block">Heading</label>
                          <Input value={homeScreen.fieldValues.heading || ''} onChange={e => updateScreenField(homeIdx, 'heading', e.target.value)}
                            placeholder="How can I help?" className="h-8 text-sm" />
                        </div>
                        <div>
                          <label className="text-[10px] font-medium text-stone-500 uppercase tracking-wider mb-1 block">Input Hint</label>
                          <Input value={homeScreen.fieldValues.hint || ''} onChange={e => updateScreenField(homeIdx, 'hint', e.target.value)}
                            placeholder="Ask a question..." className="h-8 text-sm" />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <label className="text-[10px] font-medium text-stone-500 uppercase tracking-wider mb-1 block">Max Clarifications</label>
                            <div className="flex items-center gap-1.5">
                              {[1, 2, 3, 4, 5].map(n => (
                                <button key={n} onClick={() => setMaxClarifications(n)}
                                  className="w-7 h-7 rounded-lg border-2 text-[11px] font-bold transition-all"
                                  style={{
                                    borderColor: maxClarifications === n ? '#C45A3A' : '#E7E0D8',
                                    background: maxClarifications === n ? '#C45A3A' : 'white',
                                    color: maxClarifications === n ? 'white' : '#A8A29E',
                                  }}>
                                  {n}
                                </button>
                              ))}
                            </div>
                          </div>
                          <div>
                            <label className="text-[10px] font-medium text-stone-500 uppercase tracking-wider mb-1 block">Fallback Screen</label>
                            <select value={fallbackScreen} onChange={e => setFallbackScreen(e.target.value)}
                              className="w-full h-7 text-[11px] rounded border border-stone-200 bg-white px-1.5 outline-none">
                              <option value="">Show all screens</option>
                              {nonHomeScreens.map(s => <option key={s.id} value={s.id}>{s.title || s.id}</option>)}
                            </select>
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })()}

              {/* Screens (non-home only) */}
              <div className="rounded-xl border-2 border-stone-100 bg-stone-50/50 p-4">
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <label className="text-sm font-semibold text-stone-800">Screens ({screens.filter(s => !s.isHome).length})</label>
                    <p className="text-xs text-stone-500 mt-0.5">Configure each screen and its template</p>
                  </div>
                  <button onClick={() => setShowTemplatePicker(true)} className="flex items-center gap-1 text-xs font-medium px-2.5 py-1 rounded-md" style={{ background: '#C45A3A10', color: '#C45A3A' }}><Plus size={14} /> Add Screen</button>
                </div>
                <div className="space-y-2">
                  {screens.map((screen, si) => {
                    if (screen.isHome) return null;
                    const isActive = si === activeScreenIndex;
                    const tmpl = screen.templateId ? getScreenTemplate(screen.templateId) : null;
                    return (
                      <div key={screen.id} className="rounded-xl border overflow-hidden bg-white" style={{ borderColor: isActive ? '#C45A3A' : '#E7E0D8', boxShadow: isActive ? '0 0 0 1px #C45A3A' : 'none' }}>
                        <button onClick={() => { setActiveScreenIndex(si); setPreviewIntro(false); }} className="w-full flex items-center gap-2 px-4 py-2.5 text-left" style={{ background: isActive ? '#C45A3A08' : '#FAF8F5' }}>
                          <span className="text-sm shrink-0">{screen.screenIcon || tmpl?.emoji || '\u{1F4CB}'}</span>
                          <span className="text-sm font-medium text-stone-900 flex-1 truncate">{screen.title || tmpl?.name || 'Untitled'}</span>
                          <ChevronDown size={14} className="text-stone-500" style={{ transform: isActive ? 'rotate(180deg)' : 'rotate(0)' }} />
                          <button onClick={e => { e.stopPropagation(); duplicateScreen(si); }} className="text-stone-400 hover:text-stone-600" title="Duplicate"><Copy size={13} /></button>
                          {screens.filter(s => !s.isHome).length > 1 && <button onClick={e => { e.stopPropagation(); removeScreen(si); }} className="text-stone-400 hover:text-red-500" title="Delete"><Trash2 size={13} /></button>}
                        </button>

                        {isActive && (
                          <div className="px-4 py-3 border-t border-stone-100 bg-white space-y-3">
                            <div>
                              <label className="text-[10px] font-medium text-stone-500 uppercase tracking-wider mb-1.5 block">Screen Title & Icon</label>
                              <div className="flex items-center gap-2">
                                <select
                                  value={screen.screenIcon || (tmpl?.emoji || '\u{1F4CB}')}
                                  onChange={e => setScreens(prev => prev.map((s, i) => i === si ? { ...s, screenIcon: e.target.value } : s))}
                                  className="w-10 h-8 text-center text-lg appearance-none rounded-lg border border-stone-200 bg-white cursor-pointer outline-none focus:border-stone-400">
                                  {['\u{1F4CB}', '\u{1F4AC}', '\u{1F4F7}', '\u{1F9EE}', '\u{1F4CD}', '\u{1F4DD}', '\u{2705}', '\u{1F4F1}',
                                    '\u{1F33E}', '\u{1F3E5}', '\u{1F6A8}', '\u{1F4DA}', '\u{1F4B0}', '\u{1F331}', '\u{2764}\u{FE0F}',
                                    '\u{1F50D}', '\u{2B50}', '\u{1F4A1}', '\u{1F3AF}', '\u{1F916}', '\u{1F30D}',
                                    '\u{1F6E1}\u{FE0F}', '\u{1F4CA}', '\u{1F4E6}', '\u{2615}', '\u{1F37D}\u{FE0F}'].map(e => (
                                    <option key={e} value={e}>{e}</option>
                                  ))}
                                </select>
                                <Input value={screen.title} onChange={e => updateScreenTitle(si, e.target.value)}
                                  placeholder="Screen title" className="h-8 text-sm flex-1" />
                              </div>
                            </div>

                            {/* Screen template editor */}
                            {renderScreenEditor(screen, si)}

                            {/* Routing editor (non-home only) */}
                            {!screen.isHome && screen.templateId && (
                              <div className="rounded-lg border border-stone-200 overflow-hidden mt-2">
                                <button onClick={() => toggleScreenRouting(si)}
                                  className="w-full flex items-center gap-2 px-3 py-2 bg-stone-50 border-b border-stone-100 text-left">
                                  <GitBranch size={12} className="text-stone-500" />
                                  <span className="text-xs font-semibold text-stone-700 flex-1">Route by Answer</span>
                                  <div className="relative w-8 h-4 rounded-full transition-colors"
                                    style={{ background: screen.routing ? '#C45A3A' : '#D1D5DB' }}>
                                    <div className="absolute top-0.5 w-3 h-3 rounded-full bg-white shadow transition-transform"
                                      style={{ left: screen.routing ? 14 : 2 }} />
                                  </div>
                                </button>
                                {screen.routing && (() => {
                                  const bindVars = getScreenBindVars(screen);
                                  const otherScreens = screens.filter(s => s.id !== screen.id);
                                  return (
                                    <div className="px-3 py-2 space-y-2">
                                      <div className="flex items-center gap-2">
                                        <label className="text-[10px] text-stone-500 w-16 shrink-0">Field</label>
                                        <select value={screen.routing.field}
                                          onChange={e => updateScreenRouting(si, { ...screen.routing!, field: e.target.value })}
                                          className="flex-1 h-6 text-[11px] rounded border border-stone-200 bg-white px-1.5 outline-none">
                                          <option value="">Select variable</option>
                                          {bindVars.map(v => <option key={v} value={v}>{v}</option>)}
                                        </select>
                                      </div>
                                      {screen.routing.rules.map((rule, ri) => (
                                        <div key={ri} className="flex items-center gap-1.5">
                                          <span className="text-[10px] text-stone-500 w-16 shrink-0">If =</span>
                                          <input value={rule.value}
                                            onChange={e => {
                                              const rules = [...screen.routing!.rules];
                                              rules[ri] = { ...rules[ri], value: e.target.value };
                                              updateScreenRouting(si, { ...screen.routing!, rules });
                                            }}
                                            placeholder="value"
                                            className="w-20 h-6 text-[11px] rounded border border-stone-200 bg-white px-1.5 outline-none" />
                                          <span className="text-[10px] text-stone-500">{'→'}</span>
                                          <select value={rule.goto}
                                            onChange={e => {
                                              const rules = [...screen.routing!.rules];
                                              rules[ri] = { ...rules[ri], goto: e.target.value };
                                              updateScreenRouting(si, { ...screen.routing!, rules });
                                            }}
                                            className="flex-1 h-6 text-[11px] rounded border border-stone-200 bg-white px-1.5 outline-none">
                                            <option value="">Go to...</option>
                                            {otherScreens.map(s => <option key={s.id} value={s.id}>{s.title || s.id}</option>)}
                                          </select>
                                          <button onClick={() => {
                                            const rules = screen.routing!.rules.filter((_, i) => i !== ri);
                                            updateScreenRouting(si, { ...screen.routing!, rules });
                                          }} className="text-stone-400 hover:text-red-500" aria-label="Remove rule"><X size={12} /></button>
                                        </div>
                                      ))}
                                      <button onClick={() => updateScreenRouting(si, { ...screen.routing!, rules: [...screen.routing!.rules, { value: '', goto: '' }] })}
                                        className="text-[10px] font-medium px-2 py-0.5 rounded" style={{ background: '#C45A3A10', color: '#C45A3A' }}>
                                        + Add Rule
                                      </button>
                                      <div className="flex items-center gap-2">
                                        <label className="text-[10px] text-stone-500 w-16 shrink-0">Fallback</label>
                                        <select value={screen.routing.fallback}
                                          onChange={e => updateScreenRouting(si, { ...screen.routing!, fallback: e.target.value })}
                                          className="flex-1 h-6 text-[11px] rounded border border-stone-200 bg-white px-1.5 outline-none">
                                          <option value="">Next screen</option>
                                          {otherScreens.map(s => <option key={s.id} value={s.id}>{s.title || s.id}</option>)}
                                        </select>
                                      </div>
                                    </div>
                                  );
                                })()}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* ─── Step 1: Knowledge ─── */}
          {currentStep === 1 && (
            <div className="space-y-6 max-w-2xl mx-auto">
              {(knowledgeFiles.length > 0 || knowledgeSuggestions) && (
                <>
                  <h2 className="text-lg font-semibold text-stone-900">Knowledge Base</h2>
                  <p className="text-sm text-stone-500 -mt-4">Your domain knowledge powers every Function we suggest.</p>
                </>
              )}

              {/* Upload area — doubles as hero when no files */}
              {knowledgeFiles.length === 0 && (
                <div onClick={() => fileInputRef.current?.click()} className="rounded-2xl p-8 flex flex-col items-center gap-3 cursor-pointer transition-all hover:shadow-interactive border-2 border-dashed" style={{ background: 'linear-gradient(135deg, #C45A3A08 0%, #C98A1A06 50%, #1A8A6A08 100%)', borderColor: '#C45A3A30' }}>
                  <div className="flex items-center gap-3 mb-1">
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg" style={{ background: '#5B6ABF15' }}>{'\u{1F4C4}'}</div>
                    <ChevronRight size={14} style={{ color: '#C98A1A' }} />
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg" style={{ background: '#C98A1A15' }}>{'\u{2728}'}</div>
                    <ChevronRight size={14} style={{ color: '#1A8A6A' }} />
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center text-lg" style={{ background: '#1A8A6A15' }}>{'\u{1F4F1}'}</div>
                  </div>
                  <h2 className="text-xl font-bold text-stone-900">Start with what you know</h2>
                  <p className="text-sm text-stone-500 max-w-sm mx-auto text-center">Upload your expertise — guides, manuals, protocols — and we&apos;ll turn it into an AI-powered recipe.</p>
                  <div className="flex items-center gap-2 mt-2">
                    <div className="w-11 h-11 rounded-xl flex items-center justify-center shadow-card" style={{ background: '#C45A3A' }}><Upload size={20} className="text-white" /></div>
                  </div>
                  <p className="text-xs text-stone-400">PDF, TXT, CSV, MD — up to 10 MB</p>
                </div>
              )}
              <input ref={fileInputRef} type="file" accept=".pdf,.txt,.csv,.md" multiple className="hidden" onChange={handleFileUpload} />

              {/* Demo docs */}
              {knowledgeFiles.length === 0 && (
                <div>
                  <p className="text-xs font-medium text-stone-500 mb-2">Or start with a demo document</p>
                  <div className="grid grid-cols-2 gap-3">
                    {DEMO_DOCUMENTS.map(doc => (
                      <button key={doc.name} onClick={() => {
                        const words = doc.content.split(/\s+/);
                        const chunks: string[] = []; let cur = '';
                        for (const w of words) { if (cur.length + w.length > 500) { chunks.push(cur.trim()); cur = w; } else cur += ' ' + w; }
                        if (cur.trim()) chunks.push(cur.trim());
                        setKnowledgeFiles(prev => [...prev, { name: doc.name, size: `${(doc.content.length / 1024).toFixed(1)} KB`, status: 'ready', chunks: chunks.length, summary: doc.content.slice(0, 200) + '...' }]);
                      }}
                        className="flex items-center gap-3 p-3.5 rounded-xl border border-stone-200 bg-white hover:border-stone-300 hover:shadow-card transition-all cursor-pointer text-left">
                        <span className="text-2xl shrink-0">{doc.emoji}</span>
                        <div className="min-w-0">
                          <span className="text-xs font-medium text-stone-800 block truncate">{doc.name}</span>
                          <span className="text-[10px] text-stone-500">{doc.category}</span>
                          <span className="text-[10px] text-stone-400 block truncate">{doc.author} · {doc.org}</span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Uploaded files + compact add button */}
              {knowledgeFiles.length > 0 && (
                <div className="space-y-2">
                  {knowledgeFiles.map((f, i) => (
                    <div key={i} className="flex items-center gap-3 p-3 rounded-lg border border-stone-100 bg-white">
                      <FileText size={20} className={f.status === 'ready' ? 'text-green-600' : 'text-stone-500'} />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-stone-800 truncate">{f.name}</p>
                        <div className="flex items-center gap-2 mt-0.5">
                          <p className="text-xs text-stone-500">{f.size}</p>
                          {f.status === 'ready' && f.chunks && <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-green-50 text-green-700">{f.chunks} chunks</span>}
                          {f.status !== 'ready' && <span className="text-[10px] text-amber-600">{f.status}...</span>}
                        </div>
                        {f.status !== 'ready' && <Progress value={f.status === 'uploading' ? 40 : 75} className="mt-1.5 h-1.5" />}
                      </div>
                      <button onClick={() => removeFile(i)} className="text-stone-500 hover:text-stone-600 shrink-0" aria-label="Remove file"><X size={16} /></button>
                    </div>
                  ))}
                  <button onClick={() => fileInputRef.current?.click()}
                    className="flex items-center gap-2 p-3 rounded-lg border border-dashed border-stone-300 hover:border-stone-400 hover:bg-stone-50 transition-colors w-full">
                    <Plus size={16} className="text-stone-500" />
                    <span className="text-sm text-stone-500">Add another document</span>
                  </button>
                </div>
              )}

              {/* AI Suggestions */}
              {knowledgeFiles.some(f => f.status === 'ready') && (
                <div className="rounded-xl border overflow-hidden" style={{ borderColor: '#C98A1A25', background: '#C98A1A06' }}>
                  <div className="flex items-center justify-between p-4 border-b" style={{ borderColor: '#C98A1A15' }}>
                    <div>
                      <p className="text-sm font-semibold text-stone-800">Recipe Blueprint</p>
                      <p className="text-xs text-stone-500">Analyze your documents and design a complete recipe — name, screens, prompts, and more.</p>
                    </div>
                    <button onClick={() => ensureApiKey(generateKnowledgeSuggestions)} disabled={aiLoading}
                      className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-white hover:opacity-90 shrink-0"
                      style={{ background: '#C45A3A' }}>
                      {aiLoading ? <Loader2 size={12} className="animate-spin" /> : <Sparkles size={12} />}
                      {knowledgeSuggestions ? 'Regenerate' : 'Generate Blueprint'}
                    </button>
                  </div>
                  {aiLoading && !knowledgeSuggestions && (
                    <div className="p-4 space-y-3 relative overflow-hidden">
                      <div className="absolute inset-0 pointer-events-none" style={{
                        background: 'linear-gradient(90deg, transparent 0%, rgba(196,90,58,0.04) 50%, transparent 100%)',
                        backgroundSize: '200% 100%',
                        animation: 'shimmerSweep 30s linear infinite',
                      }} />
                      <style>{`@keyframes shimmerSweep { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }`}</style>
                      <div className="flex items-center gap-2 mb-2">
                        <Loader2 size={14} className="animate-spin text-stone-500" />
                        <span className="text-xs text-stone-500 animate-pulse">Analyzing your documents and designing screens...</span>
                      </div>
                      {[1, 2, 3].map(i => (
                        <div key={i} className="rounded-lg border border-stone-100 p-3 space-y-2 animate-pulse">
                          <div className="flex items-center gap-2">
                            <div className="w-6 h-6 rounded-full bg-stone-200" />
                            <div className="h-3 bg-stone-200 rounded w-32" />
                          </div>
                          <div className="h-2 bg-stone-100 rounded w-full" />
                          <div className="h-2 bg-stone-100 rounded w-3/4" />
                        </div>
                      ))}
                    </div>
                  )}
                  {knowledgeSuggestions && (
                    <div className="p-4 space-y-3">
                      {/* Identity — collapsible */}
                      <button onClick={() => setIdentityExpanded(!identityExpanded)} className="flex items-center gap-2 w-full text-left">
                        <ChevronRight size={12} className={`text-stone-500 transition-transform ${identityExpanded ? 'rotate-90' : ''}`} />
                        <p className="text-[10px] font-semibold text-stone-500 uppercase tracking-wider">Identity</p>
                        <span className="text-[10px] text-stone-500 ml-auto">{knowledgeSuggestions.recipeName} · {knowledgeSuggestions.recipeIcon} · {(() => { const t = THEMES.find(t => t.key === knowledgeSuggestions.themeKey); return t?.label || knowledgeSuggestions.themeKey; })()}</span>
                      </button>
                      {identityExpanded && (
                        <div className="space-y-2 pl-1">
                          <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer">
                            <input type="checkbox" checked={suggestionSelections.name} onChange={e => updateSuggestionSelection(prev => ({ ...prev, name: e.target.checked }))}
                              className="mt-0.5 rounded border-stone-300" />
                            <div className="flex-1 min-w-0">
                              <p className="text-xs font-medium text-stone-500">Recipe Name</p>
                              <p className="text-sm font-semibold text-stone-900">{knowledgeSuggestions.recipeName}</p>
                            </div>
                          </label>
                          <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer">
                            <input type="checkbox" checked={suggestionSelections.description} onChange={e => updateSuggestionSelection(prev => ({ ...prev, description: e.target.checked }))}
                              className="mt-0.5 rounded border-stone-300" />
                            <div className="flex-1 min-w-0">
                              <p className="text-xs font-medium text-stone-500">Description</p>
                              <p className="text-sm text-stone-700">{knowledgeSuggestions.recipeDescription}</p>
                            </div>
                          </label>
                          <div className="flex gap-4">
                            <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer flex-1">
                              <input type="checkbox" checked={suggestionSelections.category} onChange={e => updateSuggestionSelection(prev => ({ ...prev, category: e.target.checked }))}
                                className="mt-0.5 rounded border-stone-300" />
                              <div>
                                <p className="text-xs font-medium text-stone-500">Category</p>
                                <p className="text-sm text-stone-700">{knowledgeSuggestions.category}</p>
                              </div>
                            </label>
                            <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer flex-1">
                              <input type="checkbox" checked={suggestionSelections.systemPrompt} onChange={e => updateSuggestionSelection(prev => ({ ...prev, systemPrompt: e.target.checked }))}
                                className="mt-0.5 rounded border-stone-300" />
                              <div>
                                <p className="text-xs font-medium text-stone-500">System Prompt</p>
                                <p className="text-sm text-stone-700 line-clamp-1">{knowledgeSuggestions.systemPrompt}</p>
                              </div>
                            </label>
                          </div>
                          <div className="flex gap-4">
                            <label className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer flex-1">
                              <input type="checkbox" checked={suggestionSelections.icon} onChange={e => updateSuggestionSelection(prev => ({ ...prev, icon: e.target.checked }))}
                                className="rounded border-stone-300" />
                              <div className="flex items-center gap-2">
                                <p className="text-xs font-medium text-stone-500">Icon</p>
                                <span className="text-xl">{knowledgeSuggestions.recipeIcon}</span>
                              </div>
                            </label>
                            <label className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer flex-1">
                              <input type="checkbox" checked={suggestionSelections.theme} onChange={e => updateSuggestionSelection(prev => ({ ...prev, theme: e.target.checked }))}
                                className="rounded border-stone-300" />
                              <div className="flex items-center gap-2">
                                <p className="text-xs font-medium text-stone-500">Theme</p>
                                {(() => { const t = THEMES.find(t => t.key === knowledgeSuggestions.themeKey); return t ? <div className="flex items-center gap-1"><div className="w-3 h-3 rounded-full" style={{ background: t.primary }} /><span className="text-sm text-stone-700 capitalize">{t.label}</span></div> : <span className="text-sm text-stone-700">{knowledgeSuggestions.themeKey}</span>; })()}
                              </div>
                            </label>
                          </div>
                          <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer">
                            <input type="checkbox" checked={suggestionSelections.author} onChange={e => updateSuggestionSelection(prev => ({ ...prev, author: e.target.checked }))}
                              className="mt-0.5 rounded border-stone-300" />
                            <div className="flex-1 min-w-0">
                              <p className="text-xs font-medium text-stone-500">Author</p>
                              <p className="text-sm text-stone-700">{knowledgeSuggestions.authorName} — {knowledgeSuggestions.authorOrg}</p>
                            </div>
                          </label>
                          <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer">
                            <input type="checkbox" checked={suggestionSelections.links} onChange={e => updateSuggestionSelection(prev => ({ ...prev, links: e.target.checked }))}
                              className="mt-0.5 rounded border-stone-300" />
                            <div className="flex-1 min-w-0">
                              <p className="text-xs font-medium text-stone-500">Links</p>
                              <p className="text-sm text-stone-700">{knowledgeSuggestions.links?.length || 0} reference link{(knowledgeSuggestions.links?.length || 0) !== 1 ? 's' : ''}</p>
                            </div>
                          </label>
                          <label className="flex items-start gap-3 p-2.5 rounded-lg hover:bg-stone-50 cursor-pointer">
                            <input type="checkbox" checked={suggestionSelections.homePreview} onChange={e => updateSuggestionSelection(prev => ({ ...prev, homePreview: e.target.checked }))}
                              className="mt-0.5 rounded border-stone-300" />
                            <div className="flex-1 min-w-0">
                              <p className="text-xs font-medium text-stone-500">Home Preview</p>
                              <p className="text-sm text-stone-700">{knowledgeSuggestions.homeHeading} &middot; {knowledgeSuggestions.homeHint}</p>
                            </div>
                          </label>
                        </div>
                      )}

                      {/* Screens with checkboxes — full width cards */}
                      <p className="text-[10px] font-semibold text-stone-500 uppercase tracking-wider mt-4">Functions</p>
                      <div className="space-y-2">
                        {knowledgeSuggestions.screens?.map((sc, i) => {
                          const tpl = getScreenTemplate(sc.templateId);
                          return (
                            <label key={i} className="flex items-center gap-3 p-3 rounded-lg border border-stone-100 hover:border-stone-200 hover:bg-stone-50 cursor-pointer transition-colors">
                              <input type="checkbox" checked={!!suggestionSelections.screens[i]}
                                onChange={e => updateSuggestionSelection(prev => ({ ...prev, screens: { ...prev.screens, [i]: e.target.checked } }))}
                                className="rounded border-stone-300 shrink-0" />
                              <span className="text-lg shrink-0">{sc.emoji}</span>
                              <div className="flex-1 min-w-0">
                                <p className="text-sm font-semibold text-stone-900">{sc.title}</p>
                                {sc.description && <p className="text-[11px] text-stone-600 mt-0.5">{sc.description}</p>}
                                <p className="text-[11px] text-stone-500">{tpl?.name || sc.templateId} template</p>
                              </div>
                              <span className="text-[10px] px-2 py-0.5 rounded-full bg-stone-100 text-stone-500 shrink-0">{tpl?.emoji} {tpl?.name || sc.templateId}</span>
                            </label>
                          );
                        })}
                      </div>

                      {/* Apply button */}
                      <button onClick={applySuggestions} disabled={suggestionsApplied}
                        className="w-full py-2.5 rounded-lg text-sm font-medium mt-2 flex items-center justify-center gap-1.5 transition-all"
                        style={{ background: suggestionsApplied ? '#E7E0D8' : '#10B981', color: suggestionsApplied ? '#A8A29E' : 'white' }}>
                        {suggestionsApplied ? <><Check size={14} /> Applied</> : 'Apply Selected'}
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ─── Step 4: Review ─── */}
          {currentStep === 4 && (
            <div className="space-y-5 max-w-2xl mx-auto">
              <h2 className="text-lg font-semibold text-stone-900">Review & Publish</h2>
              <p className="text-sm text-stone-500 -mt-3">Check everything looks right before sharing with the world.</p>
              <div className="rounded-xl border border-stone-200 bg-white overflow-hidden shadow-card">
                <div className="px-5 py-4 flex items-center gap-3" style={{ background: '#C45A3A08' }}>
                  <span className="text-3xl">{recipeIcon}</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-base font-bold text-stone-900">{recipeName || 'Untitled'}</p>
                    <p className="text-xs text-stone-500 truncate">{recipeDescription || 'No description'}</p>
                  </div>
                  <span className="text-xs font-medium px-2.5 py-1 rounded-full text-white shrink-0" style={{ background: '#C45A3A' }}>{category}</span>
                </div>
                <div className="px-5 py-4 grid grid-cols-3 gap-4 text-center border-y border-stone-100">
                  <div><p className="text-xl font-bold text-stone-900">{screens.length}</p><p className="text-[11px] text-stone-500">Screens</p></div>
                  <div><p className="text-xl font-bold text-stone-900">{screens.filter(s => s.templateId).length}</p><p className="text-[11px] text-stone-500">Templates</p></div>
                  <div><p className="text-xl font-bold text-stone-900">{selectedLanguages.length}</p><p className="text-[11px] text-stone-500">Languages</p></div>
                </div>
                <div className="px-5 py-3 divide-y divide-stone-50">
                  {screens.map(s => {
                    const pd = s.templateId ? getScreenTemplate(s.templateId) : null;
                    return (
                      <div key={s.id} className="flex items-center gap-2.5 py-2">
                        <span className="text-sm">{s.screenIcon || (pd?.emoji) || '\u{1F4CB}'}</span>
                        <span className="text-sm font-medium text-stone-800">{screenTitle(s)}</span>
                        {pd && <span className="text-[10px] px-1.5 py-0.5 rounded bg-stone-100 text-stone-500">{pd.name}</span>}
                        {s.isHome && screens.length > 1 && <span className="text-[10px] text-stone-400 ml-auto">{s.gridColumns}-col grid</span>}
                      </div>
                    );
                  })}
                </div>
              </div>
              {/* Formula summary for calculator */}
              {screens.some(s => s.templateId === 'calculator') && (
                <div className="rounded-xl border border-stone-200 bg-white p-4">
                  <p className="text-xs font-medium text-stone-500 mb-2">Calculator Formula</p>
                  {screens.filter(s => s.templateId === 'calculator').map(s => (
                    <div key={s.id} className="flex items-center gap-2">
                      <span className="text-xs font-medium text-stone-700">{s.title}:</span>
                      <code className="text-xs font-mono text-stone-600 bg-stone-50 px-2 py-0.5 rounded">{resolveFormula(s)}</code>
                    </div>
                  ))}
                </div>
              )}
              {selectedLanguages.length > 0 && (
                <div className="rounded-xl border border-stone-200 bg-white overflow-hidden">
                  <div className="px-5 py-3 flex items-center justify-between border-b border-stone-100">
                    <div>
                      <p className="text-sm font-semibold text-stone-900 flex items-center gap-2"><Globe size={14} /> Translation</p>
                      <p className="text-[11px] text-stone-500 mt-0.5">One YAML per language — translated by Gemini before publishing</p>
                    </div>
                    {selectedLanguages.filter(l => l !== 'en').length > 0 && (
                      <button
                        onClick={() => { if (!apiKey) { setShowApiKeyDialog(true); return; } translateAll(); }}
                        disabled={translatingAll || selectedLanguages.filter(l => l !== 'en').every(l => translationStatus[l] === 'done')}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-white transition-opacity disabled:opacity-40"
                        style={{ background: '#C45A3A' }}
                      >
                        {translatingAll ? <><Loader2 size={12} className="animate-spin" /> Translating...</> : selectedLanguages.filter(l => l !== 'en').every(l => translationStatus[l] === 'done') ? <><Check size={12} /> All Translated</> : <><Sparkles size={12} /> Translate All</>}
                      </button>
                    )}
                  </div>
                  <div className="divide-y divide-stone-50">
                    {selectedLanguages.map(langCode => {
                      const lang = ALL_LANGUAGES.find(l => l.code === langCode);
                      const status = langCode === 'en' ? 'done' as TranslationStatus : (translationStatus[langCode] || 'pending');
                      const isEn = langCode === 'en';
                      return (
                        <div key={langCode} className="px-5 py-2.5 flex items-center gap-3">
                          <div className="flex-1 min-w-0">
                            <span className="text-sm font-medium text-stone-800">{lang?.label || langCode}</span>
                            <span className="text-xs text-stone-400 ml-2">{lang?.native}</span>
                          </div>
                          {isEn ? (
                            <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-stone-100 text-stone-500">Source</span>
                          ) : status === 'done' ? (
                            <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 flex items-center gap-1"><Check size={10} /> Done</span>
                          ) : status === 'translating' ? (
                            <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 flex items-center gap-1"><Loader2 size={10} className="animate-spin" /> Translating</span>
                          ) : status === 'error' ? (
                            <button onClick={() => translateToLanguage(langCode)} className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-red-50 text-red-700 hover:bg-red-100 cursor-pointer">Retry</button>
                          ) : (
                            <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-stone-50 text-stone-400">Pending</span>
                          )}
                        </div>
                      );
                    })}
                  </div>
                  {selectedLanguages.filter(l => l !== 'en').length > 0 && (
                    <div className="px-5 py-2.5 border-t border-stone-100 bg-stone-50/50">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-1.5 rounded-full bg-stone-200 overflow-hidden">
                          <div className="h-full rounded-full transition-all duration-500" style={{
                            width: `${(selectedLanguages.filter(l => l !== 'en' && translationStatus[l] === 'done').length / Math.max(selectedLanguages.filter(l => l !== 'en').length, 1)) * 100}%`,
                            background: '#C45A3A',
                          }} />
                        </div>
                        <span className="text-[10px] text-stone-500 shrink-0">
                          {selectedLanguages.filter(l => l !== 'en' && translationStatus[l] === 'done').length}/{selectedLanguages.filter(l => l !== 'en').length}
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Publish success overlay */}
        {publishSuccess && (
          <div className="absolute inset-0 z-50 bg-white flex flex-col items-center justify-center text-center px-8 overflow-hidden">
            <style>{`
              @keyframes confettiFall { 0% { transform: translateY(-10vh) rotate(0deg); opacity: 1; } 100% { transform: translateY(110vh) rotate(720deg); opacity: 0; } }
              .confetti-piece { position: absolute; top: -10px; width: 8px; height: 8px; border-radius: 2px; animation: confettiFall linear forwards; }
            `}</style>
            {Array.from({ length: 40 }).map((_, i) => (
              <div key={i} className="confetti-piece" style={{
                left: `${Math.random() * 100}%`,
                background: ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#C45A3A'][i % 7],
                width: `${6 + Math.random() * 6}px`,
                height: `${6 + Math.random() * 6}px`,
                borderRadius: Math.random() > 0.5 ? '50%' : '2px',
                animationDuration: `${2 + Math.random() * 3}s`,
                animationDelay: `${Math.random() * 2}s`,
              }} />
            ))}
            <div className="relative z-10">
              <div className="w-16 h-16 rounded-full bg-green-50 flex items-center justify-center mx-auto mb-5">
                <PartyPopper size={32} className="text-green-600" />
              </div>
              <h2 className="text-2xl font-bold text-stone-900 mb-2">Recipe Published!</h2>
              <p className="text-sm text-stone-500 max-w-md mb-1">
                <span className="font-semibold text-stone-700">{recipeName}</span> is now live.
              </p>
              <p className="text-sm text-stone-500 max-w-md mb-8">
                Thank you for sharing your knowledge. Every recipe helps communities access the tools and expertise they need, right on their phones — no internet required.
              </p>
              <div className="flex items-center gap-3 justify-center">
                <button onClick={() => { setPublishSuccess(false); navigate('/studio', { replace: true }); window.location.reload(); }}
                  className="px-5 py-2.5 rounded-lg border border-stone-200 text-sm font-medium text-stone-700 hover:bg-stone-50">
                  Create Another
                </button>
                <button onClick={() => navigate('/')}
                  className="px-5 py-2.5 rounded-lg text-white text-sm font-medium hover:opacity-90"
                  style={{ background: '#C45A3A' }}>
                  Back to Dashboard
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Bottom bar */}
        {!publishSuccess && (
        <div className="sticky bottom-0 border-t border-stone-100 px-8 py-4 flex items-center justify-between" style={{ background: 'linear-gradient(to top, #FFFFFF 60%, rgba(255,255,255,0.95))' }}>
          <div>{currentStep > 1 && <button onClick={() => setCurrentStep(s => s - 1)} className="flex items-center gap-1 text-sm font-medium text-stone-600 hover:text-stone-900"><ChevronLeft size={16} /> Previous</button>}</div>
          <div className="flex items-center gap-3">
            {currentStep === 4 && <>
              <button onClick={() => setShowYamlPreview(true)} className="flex items-center gap-1.5 px-4 py-2 rounded-lg border border-stone-200 text-sm font-medium text-stone-700 hover:bg-stone-50"><Eye size={16} /> Preview YAML</button>
              <button onClick={downloadYaml} className="flex items-center gap-1.5 px-4 py-2 rounded-lg border border-stone-200 text-sm font-medium text-stone-700 hover:bg-stone-50"><Download size={16} /> Download YAML</button>
              <button onClick={handleSave} disabled={saving}
                className="flex items-center gap-1.5 px-5 py-2 rounded-lg text-white text-sm font-medium hover:opacity-90 transition-all"
                style={{ background: saving ? '#A8A29E' : '#10B981' }}>
                {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
                {saving ? 'Saving...' : 'Save & Publish'}
              </button>
            </>}
            {currentStep < 4 && <button onClick={() => setCurrentStep(s => s + 1)} className="flex items-center gap-1 px-5 py-2 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: '#C45A3A' }}>Next Step <ChevronRight size={16} /></button>}
          </div>
        </div>
        )}
      </div>

      {/* ─── Right: Live Preview ─── */}
      <div className="flex-[2] flex flex-col items-center bg-stone-50 p-4 overflow-hidden">
        <div className="flex items-center gap-2 mb-2 self-start shrink-0">
          <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
          <span className="text-xs font-semibold text-stone-500 uppercase tracking-wider">Live Preview</span>
          {screens.length > 1 && <span className="text-[10px] text-stone-500 ml-2">{screenTitle(activeScreen)}</span>}
        </div>
        <div ref={previewContainerRef} className="flex-1 w-full flex items-center justify-center min-h-0">
        <div className="rounded-[2.5rem] p-3 shadow-elevated" style={{ background: '#292524', width: 280, transform: `scale(${phoneScale})`, transformOrigin: 'center center' }}>
          <div className="rounded-[2rem] overflow-hidden flex flex-col" style={{ background: activeSecondary, height: 560 }}>
            <div className="flex items-center justify-between px-5 pt-3 pb-1">
              <span className="text-[10px] font-medium" style={{ color: activePrimary }}>9:41</span>
              <div className="flex gap-1"><div className="w-3 h-2 rounded-sm" style={{ background: activePrimary }} /><div className="w-3 h-2 rounded-sm" style={{ background: activePrimary, opacity: 0.5 }} /></div>
            </div>
            {previewIntro ? (
              <>
                <div className="flex-1 overflow-y-auto flex flex-col">
                  {introPage.coverPhoto ? (
                    <div className="w-full h-28 shrink-0 relative">
                      <img src={introPage.coverPhoto} alt="" className="w-full h-full object-cover" />
                      <div className="absolute inset-0" style={{ background: `linear-gradient(transparent 40%, ${activeSecondary})` }} />
                    </div>
                  ) : (
                    <div className="h-16 shrink-0" />
                  )}
                  <div className="flex-1 px-5 pb-4 flex flex-col items-center text-center relative">
                    <span className="text-4xl mb-1">{recipeIcon}</span>
                    <p className="text-sm font-bold mb-0.5" style={{ color: activePrimary }}>{recipeName || 'My Recipe'}</p>
                    <p className="text-[9px] text-stone-500 mb-2">{recipeDescription || 'A custom AI recipe'}</p>
                    {introPage.authorName && (
                      <div className="flex flex-col items-center gap-0.5 mb-2">
                        <div className="flex items-center gap-1">
                          <User size={10} style={{ color: activePrimary }} />
                          <span className="text-[9px] font-medium" style={{ color: activePrimary }}>{introPage.authorName}</span>
                          {introPage.authorVerified && <Check size={8} className="text-green-600" />}
                        </div>
                        {introPage.authorOrg && <span className="text-[8px] text-stone-500">{introPage.authorOrg}</span>}
                      </div>
                    )}
                    {introPage.links.filter(l => l.label && l.url).map((l, li) => (
                      <div key={li} className="flex items-center gap-1 mb-0.5">
                        <Link2 size={8} style={{ color: activePrimary }} />
                        <span className="text-[8px] underline" style={{ color: activePrimary }}>{l.label}</span>
                      </div>
                    ))}
                    {screens.filter(s => !s.isHome).length > 0 && (
                      <div className="w-full mt-3 space-y-1">
                        {screens.filter(s => !s.isHome).map(s => (
                          <div key={s.id} className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg bg-white/60">
                            <span className="text-sm">{s.screenIcon || (s.templateId ? getScreenTemplate(s.templateId)?.emoji : '') || '📋'}</span>
                            <span className="text-[9px] font-medium truncate" style={{ color: activePrimary }}>{s.title || (s.templateId ? getScreenTemplate(s.templateId)?.name : 'Screen')}</span>
                          </div>
                        ))}
                      </div>
                    )}
                    <div className="mt-auto w-full pt-3">
                      {introPage.disclaimer && (
                        <p className="text-[7px] text-stone-500 mb-2">{introPage.disclaimer}</p>
                      )}
                      <button className="w-full py-1.5 rounded-lg text-white text-[10px] font-semibold" style={{ background: activePrimary }}>{introPage.acceptLabel || 'I Understand'}</button>
                    </div>
                  </div>
                </div>
              </>
            ) : activeScreen.isHome && activeScreen.templateId ? (
              /* Chat-first home: multi-turn demo */
              (() => {
                const homeFields = activeScreen.fieldValues;
                const heading = homeFields.heading || recipeName || 'How can I help?';
                const hint = homeFields.hint || 'Ask a question...';
                const demoScreen = screens.find(s => !s.isHome) || null;
                const demoScreenTitle = demoScreen?.title || 'Screen';
                const demoScreenIcon = demoScreen?.screenIcon || (demoScreen?.templateId ? getScreenTemplate(demoScreen.templateId)?.emoji : '') || '\u{1F4CB}';
                return (
                  <>
                    <div className="flex items-center gap-2 px-5 py-3">
                      <span className="text-lg">{recipeIcon}</span>
                      <span className="text-sm font-semibold truncate" style={{ color: activePrimary }}>{recipeName || 'Home'}</span>
                    </div>
                    <div className="flex-1 px-4 pb-2 overflow-y-auto flex flex-col">
                      {/* Demo conversation */}
                      <div className="flex-1 flex flex-col justify-end space-y-2">
                        {/* AI greeting */}
                        <div className="flex justify-start">
                          <div className="rounded-2xl rounded-bl-sm px-3 py-1.5 max-w-[80%] bg-white/80">
                            <p className="text-[9px] text-stone-700 font-semibold">{heading}</p>
                          </div>
                        </div>
                        {/* User message */}
                        <div className="flex justify-end">
                          <div className="rounded-2xl rounded-br-sm px-3 py-1.5 max-w-[75%]" style={{ background: activePrimary }}>
                            <p className="text-[9px] text-white">{knowledgeSuggestions?.sampleConversation?.userMessage || 'My crops have yellow spots'}</p>
                          </div>
                        </div>
                        {/* AI clarification */}
                        <div className="flex justify-start">
                          <div className="rounded-2xl rounded-bl-sm px-3 py-1.5 max-w-[80%] bg-white/80">
                            <p className="text-[9px] text-stone-700">{knowledgeSuggestions?.sampleConversation?.aiClarification || 'Can you tell me what crop this is? And would you like to take a photo of the affected leaves?'}</p>
                          </div>
                        </div>
                        {/* User reply */}
                        <div className="flex justify-end">
                          <div className="rounded-2xl rounded-br-sm px-3 py-1.5 max-w-[75%]" style={{ background: activePrimary }}>
                            <p className="text-[9px] text-white">{knowledgeSuggestions?.sampleConversation?.userReply || 'It\'s rice paddy, let me take a photo'}</p>
                          </div>
                        </div>
                        {/* AI recommendation */}
                        <div className="flex justify-start">
                          <div className="rounded-2xl rounded-bl-sm px-3 py-2 max-w-[85%] bg-white/80">
                            <p className="text-[9px] text-stone-700 mb-1.5">I think {demoScreenTitle} can help. Let me take you there.</p>
                            <button onClick={() => { if (demoScreen) setActiveScreenIndex(screens.indexOf(demoScreen)); }}
                              className="w-full py-1.5 rounded-lg text-white text-[9px] font-semibold flex items-center justify-center gap-1"
                              style={{ background: activePrimary }}>
                              <span>{demoScreenIcon}</span> Go to {demoScreenTitle}
                            </button>
                          </div>
                        </div>
                      </div>
                      {/* Input bar */}
                      <div className="mt-2 rounded-2xl bg-white/80 p-1.5 flex items-center gap-1.5">
                        <button className="w-6 h-6 rounded-full flex items-center justify-center shrink-0" style={{ background: activePrimary + '15' }}>
                          <Camera size={10} style={{ color: activePrimary }} />
                        </button>
                        <div className="flex-1 h-6 rounded-full bg-stone-100 flex items-center px-2.5">
                          <span className="text-[8px] text-stone-500">{hint}</span>
                        </div>
                        <button className="w-6 h-6 rounded-full flex items-center justify-center shrink-0" style={{ background: activePrimary + '15' }}>
                          <Mic size={10} style={{ color: activePrimary }} />
                        </button>
                      </div>
                    </div>
                  </>
                );
              })()
            ) : (
              /* Grid-first home or content screen */
              <>
                <div className="flex items-center gap-2 px-5 py-3">
                  {!activeScreen.isHome && <button onClick={() => setActiveScreenIndex(screens.findIndex(s => s.isHome))} className="opacity-50 hover:opacity-100"><ChevronLeft size={14} style={{ color: activePrimary }} /></button>}
                  <span className="text-lg">{activeScreen.isHome ? recipeIcon : (activeScreen.screenIcon || (activeScreen.templateId ? getScreenTemplate(activeScreen.templateId)?.emoji : '') || recipeIcon)}</span>
                  <span className="text-sm font-semibold truncate" style={{ color: activePrimary }}>{screenTitle(activeScreen)}</span>
                </div>
                <div className="flex-1 px-4 pb-4 overflow-y-auto flex flex-col justify-end">
                  <div className="space-y-2">{previewWidgets.map((w, i) => renderWidget(w, i))}</div>
                  {activeScreen.routing && activeScreen.routing.field && (
                    <div className="flex items-center gap-1 mt-2 px-2 py-1 rounded bg-white/50">
                      <GitBranch size={8} style={{ color: activePrimary }} />
                      <span className="text-[7px] font-medium" style={{ color: activePrimary }}>Routes by: {activeScreen.routing.field}</span>
                    </div>
                  )}
                </div>
              </>
            )}
            <div className="flex gap-1 justify-center pb-2 px-3">
              {screens.filter(s => !s.isHome).map((s) => {
                const i = screens.indexOf(s);
                return <button key={s.id} onClick={() => { setPreviewIntro(false); setActiveScreenIndex(i); }} className="px-2 py-0.5 rounded-full text-[7px] font-medium cursor-pointer" style={{ background: !previewIntro && i === activeScreenIndex ? activePrimary : activePrimary + '20', color: !previewIntro && i === activeScreenIndex ? 'white' : activePrimary }}>{s.title}</button>;
              })}
              <button onClick={() => { setPreviewIntro(false); setActiveScreenIndex(screens.findIndex(s => s.isHome)); }} className="px-2 py-0.5 rounded-full text-[7px] font-medium cursor-pointer" style={{ background: !previewIntro && activeScreen.isHome ? activePrimary : activePrimary + '20', color: !previewIntro && activeScreen.isHome ? 'white' : activePrimary }}>{'\u{1F3E0}'}</button>
            </div>
            <div className="flex justify-center pb-2"><div className="w-24 h-1 rounded-full" style={{ background: activePrimary + '40' }} /></div>
          </div>
        </div>
        </div>
      </div>

      {/* ─── Dialogs ─── */}
      <Dialog open={showYamlPreview} onOpenChange={v => { setShowYamlPreview(v); if (!v) setPreviewYamlLang('en'); }}>
        <DialogContent className="max-w-5xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Recipe YAML</DialogTitle>
            <DialogDescription>Generated DSL configuration{Object.keys(translations).length > 0 ? ` — ${Object.keys(translations).length + 1} language files` : ''}</DialogDescription>
          </DialogHeader>
          {Object.keys(translations).length > 0 && (
            <div className="flex flex-wrap gap-1.5 pb-2">
              <button onClick={() => setPreviewYamlLang('en')} className="px-2.5 py-1 rounded-md text-xs font-medium transition-colors" style={{ background: previewYamlLang === 'en' ? '#C45A3A' : '#F5F5F4', color: previewYamlLang === 'en' ? 'white' : '#78716C' }}>English (source)</button>
              {Object.entries(translations).map(([code]) => (
                <button key={code} onClick={() => setPreviewYamlLang(code)} className="px-2.5 py-1 rounded-md text-xs font-medium transition-colors" style={{ background: previewYamlLang === code ? '#C45A3A' : '#F5F5F4', color: previewYamlLang === code ? 'white' : '#78716C' }}>{ALL_LANGUAGES.find(l => l.code === code)?.label || code}</button>
              ))}
            </div>
          )}
          <pre className="bg-stone-900 text-green-400 p-4 rounded-lg text-xs leading-relaxed overflow-x-auto whitespace-pre-wrap">
            {previewYamlLang === 'en' ? generateYaml() : generateYaml(previewYamlLang, translations[previewYamlLang])}
          </pre>
          <button onClick={downloadYaml} className="flex items-center justify-center gap-2 w-full py-2.5 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: '#C45A3A' }}>
            <Download size={16} /> Download All YAML Files{Object.keys(translations).length > 0 ? ` (${Object.keys(translations).length + 1})` : ''}
          </button>
        </DialogContent>
      </Dialog>

      <Dialog open={showApiKeyDialog} onOpenChange={setShowApiKeyDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>Google AI Studio API Key</DialogTitle><DialogDescription>Enter your Gemini API key to enable AI features.</DialogDescription></DialogHeader>
          <Input value={apiKey} onChange={e => setApiKey(e.target.value)} placeholder="AIza..." type="password" />
          <button onClick={() => { setShowApiKeyDialog(false); localStorage.setItem(API_KEY_STORAGE, apiKey); toast.success('API key saved'); }} disabled={!apiKey} className="w-full py-2.5 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: apiKey ? '#C45A3A' : '#A8A29E' }}>Save & Continue</button>
        </DialogContent>
      </Dialog>

      <Dialog open={showTemplatePicker} onOpenChange={v => { setShowTemplatePicker(v); if (!v) { setNewScreenTemplate(null); setNewScreenTitle(''); setNewScreenEmoji(''); } }}>
        <DialogContent className="max-w-lg">
          {!newScreenTemplate ? (
            <>
              <DialogHeader><DialogTitle>Add Screen</DialogTitle><DialogDescription>Choose a screen type</DialogDescription></DialogHeader>
              <div className="grid grid-cols-2 gap-3">
                {SCREEN_TEMPLATES.map(pkg => (
                  <button key={pkg.id} onClick={() => { setNewScreenTemplate(pkg.id); setNewScreenTitle(pkg.name); setNewScreenEmoji(pkg.emoji); }}
                    className="flex flex-col gap-2 p-4 rounded-xl border-2 border-stone-200 text-left hover:border-stone-300 hover:bg-stone-50 transition-all">
                    <div className="flex items-center gap-2">
                      <span className="text-xl">{pkg.emoji}</span>
                      <span className="text-sm font-semibold text-stone-900">{pkg.name}</span>
                    </div>
                    <p className="text-xs text-stone-500">{pkg.description}</p>
                  </button>
                ))}
              </div>
            </>
          ) : (() => {
            const tmpl = getScreenTemplate(newScreenTemplate);
            return (
              <>
                <DialogHeader>
                  <DialogTitle className="flex items-center gap-2">
                    <button onClick={() => setNewScreenTemplate(null)} className="text-stone-500 hover:text-stone-600"><ChevronLeft size={18} /></button>
                    <span>{tmpl?.emoji} {tmpl?.name}</span>
                  </DialogTitle>
                  <DialogDescription>Set a name and icon for this screen</DialogDescription>
                </DialogHeader>
                <div className="space-y-4">
                  <div>
                    <label className="text-xs text-stone-500 mb-1 block">Screen Title</label>
                    <Input value={newScreenTitle} onChange={e => setNewScreenTitle(e.target.value)} placeholder="Screen title" autoFocus />
                  </div>
                  <div>
                    <label className="text-xs text-stone-500 mb-1.5 block">Icon</label>
                    <div className="flex flex-wrap gap-1.5">
                      {[tmpl?.emoji || '\u{1F4CB}', '\u{1F4AC}', '\u{1F4F7}', '\u{1F9EE}', '\u{1F4CD}', '\u{1F4DD}', '\u{2705}',
                        '\u{1F33E}', '\u{1F3E5}', '\u{1F6A8}', '\u{1F4DA}', '\u{1F4B0}', '\u{1F331}', '\u{2764}\u{FE0F}',
                        '\u{1F50D}', '\u{2B50}', '\u{1F4A1}', '\u{1F3AF}', '\u{1F916}', '\u{1F30D}'].map(e => (
                        <button key={e} onClick={() => setNewScreenEmoji(e)}
                          className="w-9 h-9 rounded-lg flex items-center justify-center text-lg border-2 hover:scale-105"
                          style={{ borderColor: newScreenEmoji === e ? '#C45A3A' : '#E7E0D8', background: newScreenEmoji === e ? '#C45A3A10' : 'white' }}>{e}</button>
                      ))}
                    </div>
                  </div>
                  <button onClick={() => { addScreenFromTemplate(newScreenTemplate, newScreenTitle, newScreenEmoji); setNewScreenTemplate(null); setNewScreenTitle(''); setNewScreenEmoji(''); }}
                    disabled={!newScreenTitle.trim()}
                    className="w-full py-2.5 rounded-lg text-white text-sm font-medium hover:opacity-90"
                    style={{ background: newScreenTitle.trim() ? '#C45A3A' : '#A8A29E' }}>
                    Create Screen
                  </button>
                </div>
              </>
            );
          })()}
        </DialogContent>
      </Dialog>

      {/* Icon Picker Dialog */}
      <Dialog open={showIconPicker} onOpenChange={setShowIconPicker}>
        <DialogContent className="max-w-sm">
          <DialogHeader><DialogTitle>Choose Icon</DialogTitle><DialogDescription>Select a recipe icon</DialogDescription></DialogHeader>
          <div className="flex flex-wrap gap-2 justify-center">
            {EMOJI_ICONS.map(e => <button key={e} onClick={() => { setRecipeIcon(e); setShowIconPicker(false); }}
              className="w-12 h-12 rounded-xl flex items-center justify-center text-2xl border-2 hover:scale-110 transition-transform"
              style={{ borderColor: recipeIcon === e ? '#C45A3A' : '#E7E0D8', background: recipeIcon === e ? '#C45A3A10' : 'white' }}>{e}</button>)}
          </div>
        </DialogContent>
      </Dialog>

      {/* Theme Picker Dialog */}
      <Dialog open={showThemePicker} onOpenChange={setShowThemePicker}>
        <DialogContent className="max-w-sm">
          <DialogHeader><DialogTitle>Choose Theme</DialogTitle><DialogDescription>Select a color theme</DialogDescription></DialogHeader>
          <div className="grid grid-cols-2 gap-3">
            {THEMES.map(t => <button key={t.key} onClick={() => { setSelectedTheme(t.key); if (t.key !== 'custom') setShowThemePicker(false); }}
              className="flex items-center gap-3 p-3 rounded-xl border-2 text-left"
              style={{ borderColor: selectedTheme === t.key ? '#C45A3A' : '#E7E0D8', background: selectedTheme === t.key ? '#FAF8F5' : 'white' }}>
              <div className="flex gap-1"><div className="w-6 h-6 rounded-full" style={{ background: t.primary }} /><div className="w-6 h-6 rounded-full" style={{ background: t.secondary }} /></div>
              <span className="text-xs font-medium text-stone-700">{t.label}</span>
            </button>)}
          </div>
          {selectedTheme === 'custom' && <div className="flex gap-4 mt-2"><div><label className="text-xs text-stone-500 mb-1 block">Primary</label><Input value={customPrimary} onChange={e => setCustomPrimary(e.target.value)} className="w-32" /></div><div><label className="text-xs text-stone-500 mb-1 block">Secondary</label><Input value={customSecondary} onChange={e => setCustomSecondary(e.target.value)} className="w-32" /></div></div>}
        </DialogContent>
      </Dialog>

      {/* Language Picker Dialog */}
      <Dialog open={showLangDialog} onOpenChange={setShowLangDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>Target Languages</DialogTitle><DialogDescription>Select languages your recipe should support</DialogDescription></DialogHeader>
          <div className="px-1 py-2 border-b border-stone-100"><div className="flex items-center gap-2 px-2 py-1.5 rounded-lg bg-stone-50"><Search size={14} className="text-stone-500" /><input value={langSearch} onChange={e => setLangSearch(e.target.value)} placeholder="Search languages..." className="bg-transparent text-xs outline-none flex-1 text-stone-700" /></div></div>
          <div className="max-h-72 overflow-y-auto">
            {filteredGroups.map(g => <div key={g.label} className="mb-2"><p className="text-[10px] font-semibold text-stone-500 uppercase tracking-wider px-2 py-1">{g.label}</p><div className="grid grid-cols-2 gap-1">{g.languages.map(l => { const sel = selectedLanguages.includes(l.code); return <button key={l.code} onClick={() => toggleLanguage(l.code)} className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-left text-xs" style={{ background: sel ? '#C45A3A' : 'transparent', color: sel ? 'white' : '#4B5563' }}>{sel && <Check size={10} />}<span className="font-medium">{l.label}</span><span className="opacity-50 text-[10px] ml-auto">{l.native}</span></button>; })}</div></div>)}
          </div>
          {selectedLanguages.length > 0 && <div className="pt-2 border-t border-stone-100 flex flex-wrap gap-1">{selectedLanguages.map(code => { const l = ALL_LANGUAGES.find(x => x.code === code); return <span key={code} className="flex items-center gap-1 px-2 py-0.5 rounded-lg text-[10px] font-medium text-white" style={{ background: '#C45A3A' }}>{l?.label || code}<button onClick={() => toggleLanguage(code)}><X size={8} className="opacity-60" /></button></span>; })}</div>}
        </DialogContent>
      </Dialog>
    </div>
  );
}
