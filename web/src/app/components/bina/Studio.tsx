import { useState, useRef, useCallback, useEffect } from 'react';
import { useLocation } from 'react-router';
import { toast } from 'sonner';
import {
  FileText, Palette, Upload, Download,
  ChevronLeft, ChevronRight, ChevronDown, Eye, X, Plus, Trash2,
  Camera, Mic, Globe, Home,
  Loader2, Sparkles, Search, Check,
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
} from './recipes';
import {
  SCREEN_TEMPLATES, FORMULA_TEMPLATES, getScreenTemplate, createScreen,
  resolveFormula, resolveScreenWidgets,
} from './recipes';
import { RECIPES } from './recipes';

// ─── Constants ───

const THEMES: { key: ThemeKey; label: string; primary: string; secondary: string }[] = [
  { key: 'navy', label: 'Navy', primary: '#091A7A', secondary: '#ADC8FF' },
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
  { id: 1, label: 'Identity', icon: FileText },
  { id: 2, label: 'Style & Layout', icon: Palette },
  { id: 3, label: 'Knowledge', icon: Upload },
  { id: 4, label: 'Review', icon: Eye },
];

// ─── Gemini ───

const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';

async function callGemini(prompt: string, apiKey: string): Promise<string> {
  const res = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }], generationConfig: { temperature: 0.7, maxOutputTokens: 2048 } }),
  });
  if (!res.ok) throw new Error(`Gemini API error: ${res.status}`);
  const data = await res.json();
  return data.candidates?.[0]?.content?.parts?.[0]?.text || '';
}

// ─── YAML serializer ───

function widgetToYaml(w: WidgetConfig, allScreens: { id: string; title: string }[]): string {
  const p = w.props;
  const line = (k: string, v: string, q = false) => q ? `\n          ${k}: "${v}"` : `\n          ${k}: ${v}`;
  const opt = (k: string, v: string | undefined, q = false) => v ? line(k, v, q) : '';

  switch (w.type) {
    case 'text_label':
      return `      - text_label:${line('text', p.text || 'Welcome', true)}${line('style', p.style || 'body')}${opt('align', p.align !== 'left' ? p.align : undefined)}${opt('color', p.color, true)}`;
    case 'text_input': {
      let y = `      - text_input:${line('bind', p.bind || 'user_text')}${line('hint', p.hint || 'Type here...', true)}`;
      if (p.label) y += line('label', p.label, true);
      if (p.input_type && p.input_type !== 'text') y += line('input_type', p.input_type);
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
      const screens = p._allScreens ? JSON.parse(p._allScreens) as { id: string; title: string }[] : allScreens.filter(s => s.id !== 'home');
      const btns = screens.length > 0
        ? screens.map(s => `            - { label: "${s.title}", action: "go:${s.id}" }`).join('\n')
        : '            - { label: "Option 1", action: "go:home" }';
      return `      - macro_grid:${line('columns', p.columns || '2')}\n          buttons:\n${btns}`;
    }
    case 'slider':
      return `      - slider:${line('bind', p.bind || 'calc_rate')}${line('min', p.min || '0')}${line('max', p.max || '100')}${line('step', p.step || '1')}${line('label', p.label || 'Value', true)}${line('show_value', p.show_value || 'true')}`;
    case 'metric_card':
      return `      - metric_card:${line('source', p.source || 'calc_result')}${line('label', p.label || 'Result', true)}${opt('prefix', p.prefix, true)}${opt('suffix', p.suffix, true)}${line('format', p.format || 'decimal_2')}`;
    case 'geo_display':
      return `      - geo_display:${line('data', p.data || 'places')}${line('limit', p.limit || '5')}${line('show_distance', p.show_distance || 'true')}${opt('empty_text', p.empty_text, true)}`;
    default:
      return `      - ${w.type}: {}`;
  }
}

// ─── Component ───

export function Studio() {
  const location = useLocation();
  const [currentStep, setCurrentStep] = useState(1);

  // Step 1
  const [recipeName, setRecipeName] = useState('');
  const [recipeDescription, setRecipeDescription] = useState('');
  const [recipeIcon, setRecipeIcon] = useState('\u{1F916}');
  const [systemPrompt, setSystemPrompt] = useState('You are a helpful assistant.');
  const [blockedKeywords, setBlockedKeywords] = useState('');
  const [disclaimer, setDisclaimer] = useState('AI-generated content. Not a professional consultation.');
  const [showDisclaimerOnStart, setShowDisclaimerOnStart] = useState(true);
  const [category, setCategory] = useState('Education');
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>(['en']);
  const [langSearch, setLangSearch] = useState('');
  const [showAllLangs, setShowAllLangs] = useState(false);

  // Step 2
  const [selectedTheme, setSelectedTheme] = useState<ThemeKey>('navy');
  const [customPrimary, setCustomPrimary] = useState('#6B21A8');
  const [customSecondary, setCustomSecondary] = useState('#E9D5FF');
  const [screens, setScreens] = useState<ScreenConfig[]>([
    { id: 'main', title: '', isHome: false, gridColumns: 2, ...createScreen('ask_ai') },
  ]);
  const [activeScreenIndex, setActiveScreenIndex] = useState(0);
  const [showTemplatePicker, setShowTemplatePicker] = useState(false);

  // Step 3
  const [knowledgeFiles, setKnowledgeFiles] = useState<KnowledgeFile[]>([]);
  const [knowledgeSummary, setKnowledgeSummary] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  // LLM
  const [apiKey, setApiKey] = useState('');
  const [aiLoading, setAiLoading] = useState(false);
  const [showYamlPreview, setShowYamlPreview] = useState(false);
  const [showApiKeyDialog, setShowApiKeyDialog] = useState(false);

  // ─── Derived ───
  const activePrimary = selectedTheme === 'custom' ? customPrimary : THEMES.find(t => t.key === selectedTheme)!.primary;
  const activeSecondary = selectedTheme === 'custom' ? customSecondary : THEMES.find(t => t.key === selectedTheme)!.secondary;
  const activeScreen = screens[activeScreenIndex] || screens[0];
  const previewWidgets = activeScreen ? resolveScreenWidgets(activeScreen, screens) : [];
  const screenTitle = (s: ScreenConfig) => s.title || recipeName || 'Untitled';

  // ─── Load recipe from navigation state ───
  const loadRecipe = (recipe: RecipeConfig) => {
    setRecipeName(recipe.recipeName);
    setRecipeDescription(recipe.recipeDescription);
    setRecipeIcon(recipe.recipeIcon);
    setSystemPrompt(recipe.systemPrompt);
    setBlockedKeywords(recipe.blockedKeywords);
    setDisclaimer(recipe.disclaimer);
    setCategory(recipe.category);
    setSelectedLanguages(recipe.selectedLanguages);
    setSelectedTheme(recipe.selectedTheme);
    setCustomPrimary(recipe.customPrimary);
    setCustomSecondary(recipe.customSecondary);
    setScreens(recipe.screens);
    setKnowledgeSummary(recipe.knowledgeSummary);
    setCurrentStep(1);
  };

  // ─── Helpers ───
  const toggleLanguage = (code: string) => setSelectedLanguages(prev => prev.includes(code) ? prev.filter(c => c !== code) : [...prev, code]);

  const addScreenFromTemplate = (templateId: string) => {
    const def = getScreenTemplate(templateId);
    const id = `screen_${Date.now()}`;
    const title = def?.name || 'New Screen';
    const newScreen: ScreenConfig = { id, title, isHome: false, gridColumns: 2, ...createScreen(templateId) };

    setScreens(prev => {
      const contentScreens = prev.filter(s => !s.isHome);
      if (contentScreens.length >= 1 && !prev.some(s => s.isHome)) {
        const home: ScreenConfig = { id: 'home', title: '', isHome: true, gridColumns: 2, ...createScreen('ask_ai') };
        const backfilled = prev.map(s => {
          if (!s.title && s.templateId) {
            const def = getScreenTemplate(s.templateId);
            return { ...s, title: def?.name || 'Screen' };
          }
          return s;
        });
        return [home, ...backfilled, newScreen];
      }
      return [...prev, newScreen];
    });
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

  const updateScreenTitle = (index: number, title: string) => {
    setScreens(prev => prev.map((s, i) => i === index ? { ...s, title } : s));
  };

  const updateGridColumns = (cols: number) => {
    setScreens(prev => prev.map((s, i) => i === activeScreenIndex && s.isHome ? { ...s, gridColumns: cols } : s));
  };

  const toggleHomeAsk = () => {
    setScreens(prev => prev.map((s, i) => {
      if (i !== activeScreenIndex || !s.isHome) return s;
      return s.templateId
        ? { ...s, templateId: null, fieldValues: {}, disabledWidgets: [] }
        : { ...s, ...createScreen('ask_ai') };
    }));
  };

  const updateScreenField = (screenIndex: number, fieldKey: string, value: string) => {
    setScreens(prev => prev.map((s, si) => {
      if (si !== screenIndex || !s.templateId) return s;
      return { ...s, fieldValues: { ...s.fieldValues, [fieldKey]: value } };
    }));
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
      const result = await callGemini(
        `Design a Bina.ai recipe. Name: "${recipeName}", Desc: "${recipeDescription}", Category: ${category}.\nReturn JSON (no markdown):\n{"system_prompt":"...","screens":[{"id":"home","title":"","isHome":true,"gridColumns":2,"templateId":"ask_ai","fields":{"hint":"...","ai_instruction":"ask:{{user_text}}"}},{"id":"camera","title":"Camera","isHome":false,"templateId":"camera_analysis","fields":{}}],"blocked_keywords":["kw"],"disclaimer":"..."}\nAvailable screen templates: ask_ai, voice_ask, camera_analysis, calculator, nearby_places, info_display\nFirst screen must be home with isHome:true. Each non-home screen has exactly one template.\nDo not assume any specific domain — use the recipe name and category to guide content.`, apiKey);
      const jsonMatch = result.match(/\{[\s\S]*\}/);
      if (!jsonMatch) throw new Error('Invalid response');
      const parsed = JSON.parse(jsonMatch[0]);
      if (parsed.system_prompt) setSystemPrompt(parsed.system_prompt);
      if (parsed.screens?.length > 0) {
        const newScreens: ScreenConfig[] = parsed.screens.map((s: { id: string; title: string; isHome?: boolean; gridColumns?: number; templateId?: string; fields?: Record<string, string> }) => ({
          id: s.id, title: s.title || '', isHome: !!s.isHome,
          gridColumns: s.gridColumns || 2,
          ...(s.templateId ? createScreen(s.templateId, s.fields) : { templateId: null, fieldValues: {}, disabledWidgets: [] }),
        }));
        setScreens(newScreens);
        setActiveScreenIndex(0);
      }
      if (parsed.blocked_keywords) setBlockedKeywords(parsed.blocked_keywords.join(', '));
      if (parsed.disclaimer) setDisclaimer(parsed.disclaimer);
      toast.success('AI configured your recipe');
    } catch (e) { toast.error(`${e instanceof Error ? e.message : 'Error'}`); }
    finally { setAiLoading(false); }
  }, [apiKey, recipeName, recipeDescription, category]);

  // ─── Knowledge ───
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;
    for (const file of Array.from(files)) {
      const sizeStr = file.size > 1024 * 1024 ? `${(file.size / (1024 * 1024)).toFixed(1)} MB` : `${(file.size / 1024).toFixed(0)} KB`;
      setKnowledgeFiles(prev => [...prev, { name: file.name, size: sizeStr, status: 'uploading' }]);
      const text = await file.text();
      setKnowledgeFiles(prev => prev.map(f => f.name === file.name ? { ...f, status: 'processing' } : f));
      const chunks: string[] = []; const words = text.split(/\s+/); let cur = '';
      for (const w of words) { if (cur.length + w.length > 500) { chunks.push(cur.trim()); cur = w; } else cur += ' ' + w; }
      if (cur.trim()) chunks.push(cur.trim());
      let summary = `Document: ${file.name} (${chunks.length} chunks)`;
      if (apiKey && chunks.length > 0) {
        try { summary = (await callGemini(`Summarize in 2-3 sentences:\n\n${chunks.slice(0, 3).join('\n\n')}`, apiKey)).trim(); } catch {}
      }
      setKnowledgeFiles(prev => prev.map(f => f.name === file.name ? { ...f, status: 'ready', chunks: chunks.length, summary } : f));
    }
    e.target.value = '';
  };
  const removeFile = (i: number) => setKnowledgeFiles(prev => prev.filter((_, idx) => idx !== i));
  const generateKnowledgeSummary = useCallback(async () => {
    if (!apiKey) return;
    const ready = knowledgeFiles.filter(f => f.status === 'ready' && f.summary);
    if (!ready.length) { toast.error('No files'); return; }
    setAiLoading(true);
    try {
      const result = await callGemini(`Create "always loaded" brief (max 300 words) for "${recipeName}" from:\n${ready.map(f => `${f.name}: ${f.summary}`).join('\n\n')}`, apiKey);
      setKnowledgeSummary(result.trim());
      toast.success('Brief generated');
    } catch (e) { toast.error(`${e instanceof Error ? e.message : 'Error'}`); }
    finally { setAiLoading(false); }
  }, [apiKey, knowledgeFiles, recipeName]);

  // ─── YAML ───
  const generateYaml = (): string => {
    const id = recipeName.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'my_recipe';
    const sysPrompt = systemPrompt.trim() ? `  system_prompt: |\n    ${systemPrompt.trim().split('\n').join('\n    ')}` : '  system_prompt: "You are a helpful assistant."';
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

    const questionsYaml = screens.map(s => {
      if (!s.templateId) return '';
      if (s.templateId !== 'ask_ai' && s.templateId !== 'voice_ask') return '';
      const qs = [s.fieldValues.q1, s.fieldValues.q2, s.fieldValues.q3, s.fieldValues.q4].filter(q => q && q.trim());
      if (qs.length === 0) return '';
      return `  ${s.id}:\n${qs.map(q => `    - "${q}"`).join('\n')}`;
    }).filter(Boolean).join('\n');

    const nonHomeScreens = screens.filter(s => !s.isHome).map(s => ({ id: s.id, title: s.title }));
    const screensYaml = screens.map((screen, si) => {
      const widgets = allResolved[si];
      const title = screen.title || recipeName || 'My Recipe';
      const body = widgets.map(w => widgetToYaml(w, nonHomeScreens)).join('\n');
      return `  - id: ${screen.id}\n    title: "${title}"\n    body:\n${body}`;
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
    const setupBlock = showDisclaimerOnStart && disclaimer ? `\nsetup:\n  disclaimer:\n    text: "${disclaimer}"\n    accept_label: "I Understand"\n` : '';

    return `id: ${id}\nname: "${recipeName || 'My Recipe'}"\ndescription: "${recipeDescription || 'A custom AI recipe'}"\nicon: "${recipeIcon}"\nversion: "1.0.0"\ncategory: ${category}\n\nauthor:\n  name: User\n  organisation: ""\n  verified: false\n\nmodel:\n  model_id: gemma-4-e2b-it\n  backend: cpu\n${sysPrompt}\n\ntheme:\n  primary: "${activePrimary}"\n  secondary: "${activeSecondary}"\n\nvariables:\n${vars.join('\n')}\n\nscreens:\n${screensYaml}\n${formulas}${data}\nsafety:\n  blocked_keywords:\n${blockedYaml}\n  escalation_message: "This request has been blocked for safety."\n  disclaimer: "${disclaimer || 'AI-generated content.'}"\n\npermissions:\n${perms.length > 0 ? perms.join('\n') : '  []'}${loc}${know}${questionsBlock}${setupBlock}`;
  };

  // ─── Download YAML ───
  const downloadYaml = () => {
    const yaml = generateYaml();
    const id = recipeName.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'my_recipe';
    const blob = new Blob([yaml], { type: 'text/yaml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${id}.yaml`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // ─── localStorage persistence ───
  const STORAGE_KEY = 'bina_studio_state';
  const API_KEY_STORAGE = 'bina_studio_api_key';

  useEffect(() => {
    const state = (location.state as { recipe?: string } | null);
    if (state?.recipe) {
      const recipe = RECIPES[state.recipe];
      if (recipe) {
        loadRecipe(recipe);
        return;
      }
    }
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const s = JSON.parse(saved);
        if (s.recipeName !== undefined) setRecipeName(s.recipeName);
        if (s.recipeDescription) setRecipeDescription(s.recipeDescription);
        if (s.recipeIcon) setRecipeIcon(s.recipeIcon);
        if (s.systemPrompt) setSystemPrompt(s.systemPrompt);
        if (s.blockedKeywords !== undefined) setBlockedKeywords(s.blockedKeywords);
        if (s.disclaimer) setDisclaimer(s.disclaimer);
        if (s.showDisclaimerOnStart !== undefined) setShowDisclaimerOnStart(s.showDisclaimerOnStart);
        if (s.category) setCategory(s.category);
        if (s.selectedLanguages) setSelectedLanguages(s.selectedLanguages);
        if (s.selectedTheme) setSelectedTheme(s.selectedTheme);
        if (s.customPrimary) setCustomPrimary(s.customPrimary);
        if (s.customSecondary) setCustomSecondary(s.customSecondary);
        if (s.screens) setScreens(s.screens);
        if (s.currentStep) setCurrentStep(s.currentStep);
      } catch {}
    }
    const savedKey = localStorage.getItem(API_KEY_STORAGE);
    if (savedKey) setApiKey(savedKey);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const state = { recipeName, recipeDescription, recipeIcon, systemPrompt, blockedKeywords, disclaimer, showDisclaimerOnStart, category, selectedLanguages, selectedTheme, customPrimary, customSecondary, screens, currentStep };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }, [recipeName, recipeDescription, recipeIcon, systemPrompt, blockedKeywords, disclaimer, showDisclaimerOnStart, category, selectedLanguages, selectedTheme, customPrimary, customSecondary, screens, currentStep]);

  useEffect(() => {
    if (apiKey) localStorage.setItem(API_KEY_STORAGE, apiKey);
  }, [apiKey]);

  // ─── Language filter ───
  const filteredGroups = LANGUAGE_GROUPS.map(g => ({ ...g, languages: g.languages.filter(l => !langSearch || l.label.toLowerCase().includes(langSearch.toLowerCase()) || l.native.toLowerCase().includes(langSearch.toLowerCase())) })).filter(g => g.languages.length > 0);

  // ─── Preview renderer ───
  const renderWidget = (w: WidgetConfig, wi: number) => {
    const p = w.props;
    switch (w.type) {
      case 'text_label': return <p key={wi} className="text-[10px] font-semibold px-1" style={{ color: activePrimary }}>{p.text || 'Welcome'}</p>;
      case 'text_input': return <div key={wi} className="rounded-xl p-2 bg-white/70"><div className="w-full h-7 rounded-lg border border-gray-200 bg-white flex items-center px-2"><span className="text-[9px] text-gray-400">{p.hint || p.label || 'Type here...'}</span></div></div>;
      case 'voice_input': return <div key={wi} className="rounded-xl p-2 bg-white/70 flex items-center gap-2"><Mic size={12} style={{ color: activePrimary }} /><span className="text-[9px] text-gray-400">{p.hint || 'Tap to speak...'}</span></div>;
      case 'camera_input': return <div key={wi} className="rounded-xl bg-gray-800 flex items-center justify-center" style={{ height: 70 }}><Camera size={18} className="text-white/50" /></div>;
      case 'action_button': return <button key={wi} className="w-full py-1.5 rounded-lg text-white text-[10px] font-semibold" style={{ background: activePrimary }}>{p.label || 'Submit'}</button>;
      case 'markdown_output': return <div key={wi} className="rounded-xl p-2 bg-white/50"><div className="space-y-1"><div className="h-1.5 rounded-full bg-gray-300/50 w-full" /><div className="h-1.5 rounded-full bg-gray-300/50 w-4/5" /><div className="h-1.5 rounded-full bg-gray-300/50 w-3/5" /></div></div>;
      case 'macro_grid': {
        const cols = parseInt(p.columns) || 2;
        const others = screens.filter(s => !s.isHome);
        return (
          <div key={wi} className="gap-1.5" style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
            {others.map((s, i) => (
              <button key={s.id} onClick={() => setActiveScreenIndex(screens.indexOf(s))}
                className="rounded-lg p-2 flex items-center justify-center cursor-pointer hover:opacity-80"
                style={{ background: i === 0 ? activePrimary : 'white' }}>
                <span className="text-[8px] font-medium" style={{ color: i === 0 ? 'white' : activePrimary }}>{s.title}</span>
              </button>
            ))}
          </div>
        );
      }
      case 'slider': return <div key={wi} className="px-1 py-1"><p className="text-[8px] text-gray-500 mb-0.5">{p.label || 'Value'}</p><div className="h-1.5 rounded-full bg-gray-300 relative"><div className="absolute left-1/3 -top-1 w-3 h-3 rounded-full" style={{ background: activePrimary }} /></div></div>;
      case 'metric_card': return (
        <div key={wi} className="rounded-xl p-3 bg-white/70 text-center">
          <span className="text-lg font-bold" style={{ color: activePrimary }}>
            {p.prefix && <span className="text-xs font-medium mr-0.5">{p.prefix}</span>}
            0
            {p.suffix && <span className="text-xs font-medium ml-0.5">{p.suffix}</span>}
          </span>
          <p className="text-[8px] text-gray-500">{p.label || 'Result'}</p>
        </div>
      );
      case 'geo_display': return <div key={wi} className="rounded-xl p-2 bg-white/70 flex items-center gap-2"><span className="text-xs">{'\u{1F4CD}'}</span><span className="text-[9px] text-gray-400">Nearby places</span></div>;
      default: return null;
    }
  };

  const renderScreenEditor = (screen: ScreenConfig, si: number) => {
    if (!screen.templateId) return null;
    const def = getScreenTemplate(screen.templateId);
    if (!def) return null;

    return (
      <div className="rounded-lg border border-gray-200 overflow-hidden">
        <div className="flex items-center gap-2 px-3 py-2 bg-gray-50 border-b border-gray-100">
          <span className="text-sm">{def.emoji}</span>
          <span className="text-xs font-semibold text-gray-800 flex-1">{def.name}</span>
        </div>
        <div className="px-3 py-2 space-y-2">
          {def.fields.map(f => {
            if (f.showWhen && screen.fieldValues[f.showWhen.field] !== f.showWhen.value) return null;
            return (
              <div key={f.key} className="flex items-center gap-2">
                <label className="text-[10px] text-gray-400 w-20 shrink-0">{f.label}</label>
                {f.type === 'select' ? (
                  <select value={screen.fieldValues[f.key] || f.defaultValue}
                    onChange={e => updateScreenField(si, f.key, e.target.value)}
                    className="flex-1 h-6 text-[11px] text-gray-700 rounded border border-gray-200 bg-white px-1.5 outline-none">
                    {f.options?.map(o => <option key={o} value={o}>{o}</option>)}
                  </select>
                ) : f.type === 'textarea' ? (
                  <textarea value={screen.fieldValues[f.key] || ''} onChange={e => updateScreenField(si, f.key, e.target.value)}
                    placeholder={f.placeholder}
                    className="flex-1 text-[11px] text-gray-700 rounded border border-gray-200 bg-white px-1.5 py-1 outline-none resize-none" rows={2} />
                ) : (
                  <input value={screen.fieldValues[f.key] || ''} onChange={e => updateScreenField(si, f.key, e.target.value)}
                    placeholder={f.placeholder}
                    className="flex-1 h-6 text-[11px] text-gray-700 rounded border border-gray-200 bg-white px-1.5 outline-none focus:border-blue-400" />
                )}
              </div>
            );
          })}
          {screen.templateId === 'calculator' && (
            <div className="rounded bg-gray-50 px-2 py-1.5 mt-1">
              <p className="text-[9px] text-gray-400 mb-0.5">Formula preview</p>
              <code className="text-[10px] font-mono text-gray-700">{resolveFormula(screen)}</code>
            </div>
          )}
          {def.widgets.some(w => w.optional) && (
            <div className="flex flex-wrap gap-1 pt-1 border-t border-gray-50">
              {def.widgets.map(pw => {
                const disabled = screen.disabledWidgets.includes(pw.wid);
                const label = pw.wid === 'input_a' ? 'Field A'
                  : pw.wid === 'input_b' ? 'Field B'
                  : pw.wid === 'input_c' ? 'Field C'
                  : pw.wid === 'input_d' ? 'Field D'
                  : pw.type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
                if (!pw.optional) return <span key={pw.wid} className="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">{label}</span>;
                return (
                  <button key={pw.wid} onClick={() => toggleScreenWidget(si, pw.wid)}
                    className="text-[10px] px-1.5 py-0.5 rounded border transition-all"
                    style={{
                      borderColor: disabled ? '#D1D5DB' : '#091A7A',
                      background: disabled ? 'transparent' : '#091A7A10',
                      color: disabled ? '#9CA3AF' : '#091A7A',
                      borderStyle: disabled ? 'dashed' : 'solid',
                    }}>
                    {disabled ? `+ ${label}` : label}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    );
  };

  // ─── Render ───
  return (
    <div className="flex h-full min-h-screen">
      {/* Left panel */}
      <div className="flex-[3] flex flex-col overflow-y-auto border-r border-gray-200">
        <div className="px-8 pt-8 pb-4 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Recipe Studio</h1>
            <p className="text-sm text-gray-500 mt-1">Create AI recipes for grassroots users</p>
          </div>
          {recipeName && (
            <button onClick={() => ensureApiKey(generateRecipeWithAI)} disabled={aiLoading}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium hover:opacity-90" style={{ background: '#091A7A', color: 'white' }}>
              {aiLoading ? <Loader2 size={14} className="animate-spin" /> : <Sparkles size={14} />} AI Auto-Configure
            </button>
          )}
        </div>

        {/* Steps */}
        <div className="px-8 pb-6">
          <div className="flex items-center">
            {STEPS.map((step, i) => {
              const Icon = step.icon; const isActive = currentStep === step.id; const done = currentStep > step.id;
              return (
                <div key={step.id} className="flex items-center flex-1">
                  <button onClick={() => setCurrentStep(step.id)} className="flex items-center gap-2">
                    <div className="w-9 h-9 rounded-full flex items-center justify-center border-2" style={{ background: isActive || done ? '#091A7A' : '#F3F4F6', borderColor: isActive || done ? '#091A7A' : '#E5E7EB' }}>
                      <Icon size={16} style={{ color: isActive || done ? 'white' : '#9CA3AF' }} />
                    </div>
                    <span className="text-xs font-medium hidden lg:inline" style={{ color: isActive ? '#091A7A' : '#9CA3AF' }}>{step.label}</span>
                  </button>
                  {i < STEPS.length - 1 && <div className="flex-1 h-0.5 mx-3" style={{ background: done ? '#091A7A' : '#E5E7EB' }} />}
                </div>
              );
            })}
          </div>
        </div>

        <div className="flex-1 px-8 pb-4">
          {/* ─── Step 1: Identity ─── */}
          {currentStep === 1 && (
            <div className="space-y-5 max-w-2xl">
              <h2 className="text-lg font-semibold text-gray-900">Define Identity</h2>
              {/* Languages */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <Globe size={16} className="text-gray-500" />
                    <label className="text-sm font-medium text-gray-700">Target Languages</label>
                    {selectedLanguages.length > 0 && <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded-full" style={{ background: '#091A7A', color: 'white' }}>{selectedLanguages.length}</span>}
                  </div>
                  <button onClick={() => setShowAllLangs(!showAllLangs)} className="text-xs font-medium flex items-center gap-1" style={{ color: '#091A7A' }}>
                    {showAllLangs ? 'Collapse' : `All (${ALL_LANGUAGES.length})`} <ChevronDown size={12} className={showAllLangs ? 'rotate-180' : ''} />
                  </button>
                </div>
                {selectedLanguages.length > 0 && !showAllLangs && (
                  <div className="flex flex-wrap gap-1.5 mb-3">
                    {selectedLanguages.map(code => { const l = ALL_LANGUAGES.find(x => x.code === code); return <button key={code} onClick={() => toggleLanguage(code)} className="flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-medium text-white" style={{ background: '#091A7A' }}>{l?.native || code}<X size={10} className="opacity-60" /></button>; })}
                  </div>
                )}
                {showAllLangs && (
                  <div className="rounded-xl border border-gray-200 bg-white overflow-hidden mb-3">
                    <div className="px-3 py-2 border-b border-gray-100"><div className="flex items-center gap-2 px-2 py-1.5 rounded-lg bg-gray-50"><Search size={14} className="text-gray-400" /><input value={langSearch} onChange={e => setLangSearch(e.target.value)} placeholder="Search..." className="bg-transparent text-xs outline-none flex-1 text-gray-700" /></div></div>
                    <div className="max-h-64 overflow-y-auto p-2">
                      {filteredGroups.map(g => <div key={g.label} className="mb-2"><p className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider px-2 py-1">{g.label}</p><div className="grid grid-cols-2 gap-1">{g.languages.map(l => { const sel = selectedLanguages.includes(l.code); return <button key={l.code} onClick={() => toggleLanguage(l.code)} className="flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-left text-xs" style={{ background: sel ? '#091A7A' : 'transparent', color: sel ? 'white' : '#4B5563' }}>{sel && <Check size={10} />}<span className="font-medium">{l.label}</span><span className="opacity-50 text-[10px] ml-auto">{l.native}</span></button>; })}</div></div>)}
                    </div>
                  </div>
                )}
                {!showAllLangs && <div className="flex flex-wrap gap-1.5">{[{ code: 'en', label: 'English' }, { code: 'ms', label: 'Malay' }, { code: 'id', label: 'Indonesian' }, { code: 'ta', label: 'Tamil' }, { code: 'zh', label: 'Chinese' }, { code: 'tl', label: 'Filipino' }, { code: 'th', label: 'Thai' }, { code: 'vi', label: 'Vietnamese' }, { code: 'hi', label: 'Hindi' }, { code: 'bn', label: 'Bengali' }].map(l => { const sel = selectedLanguages.includes(l.code); return <button key={l.code} onClick={() => toggleLanguage(l.code)} className="px-2.5 py-1 rounded-lg border text-xs font-medium" style={{ borderColor: sel ? '#091A7A' : '#E5E7EB', background: sel ? '#091A7A' : 'white', color: sel ? 'white' : '#6B7280' }}>{l.label}</button>; })}</div>}
              </div>
              <div><label className="text-sm font-medium text-gray-700 mb-1.5 block">Recipe Name</label><Input value={recipeName} onChange={e => setRecipeName(e.target.value)} placeholder="e.g. Health Assistant" /></div>
              <div><label className="text-sm font-medium text-gray-700 mb-1.5 block">Icon</label><div className="flex flex-wrap gap-2">{EMOJI_ICONS.map(e => <button key={e} onClick={() => setRecipeIcon(e)} className="w-10 h-10 rounded-lg flex items-center justify-center text-xl border-2 hover:scale-105" style={{ borderColor: recipeIcon === e ? '#091A7A' : '#E5E7EB', background: recipeIcon === e ? '#091A7A10' : 'white' }}>{e}</button>)}</div></div>
              <div><label className="text-sm font-medium text-gray-700 mb-1.5 block">Description</label><Input value={recipeDescription} onChange={e => setRecipeDescription(e.target.value)} placeholder="Short description" /></div>
              <div><label className="text-sm font-medium text-gray-700 mb-1.5 block">Category</label><Select value={category} onValueChange={setCategory}><SelectTrigger className="w-full"><SelectValue /></SelectTrigger><SelectContent>{CATEGORIES.map(c => <SelectItem key={c} value={c}>{c}</SelectItem>)}</SelectContent></Select></div>
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-sm font-medium text-gray-700">System Prompt</label>
                  <button onClick={() => ensureApiKey(generateSystemPrompt)} disabled={aiLoading || !recipeName} className="text-xs font-medium px-3 py-1 rounded-md flex items-center gap-1" style={{ background: '#091A7A10', color: '#091A7A', opacity: !recipeName ? 0.4 : 1 }}>{aiLoading ? <Loader2 size={12} className="animate-spin" /> : <Sparkles size={12} />} AI Generate</button>
                </div>
                <Textarea value={systemPrompt} onChange={e => setSystemPrompt(e.target.value)} placeholder="You are a helpful assistant..." rows={5} />
              </div>
              <div><label className="text-sm font-medium text-gray-700 mb-1.5 block">Blocked Keywords</label><Input value={blockedKeywords} onChange={e => setBlockedKeywords(e.target.value)} placeholder="Comma-separated" /></div>
              <div>
                <label className="text-sm font-medium text-gray-700 mb-1.5 block">Disclaimer</label>
                <Input value={disclaimer} onChange={e => setDisclaimer(e.target.value)} placeholder="Optional disclaimer" />
                <div className="flex items-center gap-3 mt-2 rounded-lg bg-gray-50 px-3 py-2">
                  <span className="text-xs text-gray-600 flex-1">Show disclaimer on first launch</span>
                  <button onClick={() => setShowDisclaimerOnStart(v => !v)}
                    className="relative w-9 h-5 rounded-full transition-colors"
                    style={{ background: showDisclaimerOnStart ? '#091A7A' : '#D1D5DB' }}>
                    <div className="absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform"
                      style={{ left: showDisclaimerOnStart ? 18 : 2 }} />
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* ─── Step 2: Style & Layout ─── */}
          {currentStep === 2 && (
            <div className="space-y-6 max-w-2xl">
              <h2 className="text-lg font-semibold text-gray-900">Style & Layout</h2>
              <div>
                <label className="text-sm font-medium text-gray-700 mb-3 block">Theme</label>
                <div className="flex gap-3 flex-wrap">
                  {THEMES.map(t => <button key={t.key} onClick={() => setSelectedTheme(t.key)} className="flex flex-col items-center gap-2 p-3 rounded-xl border-2 w-[100px]" style={{ borderColor: selectedTheme === t.key ? '#091A7A' : '#E5E7EB', background: selectedTheme === t.key ? '#F8FAFC' : 'white' }}><div className="flex gap-1"><div className="w-6 h-6 rounded-full" style={{ background: t.primary }} /><div className="w-6 h-6 rounded-full" style={{ background: t.secondary }} /></div><span className="text-xs font-medium text-gray-700">{t.label}</span></button>)}
                </div>
                {selectedTheme === 'custom' && <div className="flex gap-4 mt-4"><div><label className="text-xs text-gray-500 mb-1 block">Primary</label><Input value={customPrimary} onChange={e => setCustomPrimary(e.target.value)} className="w-32" /></div><div><label className="text-xs text-gray-500 mb-1 block">Secondary</label><Input value={customSecondary} onChange={e => setCustomSecondary(e.target.value)} className="w-32" /></div></div>}
              </div>

              {/* Screens */}
              <div className="rounded-xl border-2 border-gray-100 bg-gray-50/50 p-4">
                <div className="flex items-center justify-between mb-3">
                  <div>
                    <label className="text-sm font-semibold text-gray-800">Screens ({screens.length})</label>
                    <p className="text-xs text-gray-500 mt-0.5">Configure each screen and its template</p>
                  </div>
                  <button onClick={() => setShowTemplatePicker(true)} className="flex items-center gap-1 text-xs font-medium px-2.5 py-1 rounded-md" style={{ background: '#091A7A10', color: '#091A7A' }}><Plus size={14} /> Add Screen</button>
                </div>
                <div className="space-y-2">
                  {screens.map((screen, si) => {
                    const isActive = si === activeScreenIndex;
                    const tmpl = screen.templateId ? getScreenTemplate(screen.templateId) : null;
                    return (
                      <div key={screen.id} className={`rounded-xl border overflow-hidden ${screen.isHome ? 'bg-indigo-50/40' : 'bg-white'}`} style={{ borderColor: isActive ? '#091A7A' : screen.isHome ? '#C7D2FE' : '#E5E7EB', boxShadow: isActive ? '0 0 0 1px #091A7A' : 'none' }}>
                        <button onClick={() => setActiveScreenIndex(si)} className="w-full flex items-center gap-2 px-4 py-2.5 text-left" style={{ background: isActive ? '#091A7A08' : screen.isHome ? '#EEF2FF' : '#FAFAFA' }}>
                          {screen.isHome ? (
                            <div className="w-5 h-5 rounded-md flex items-center justify-center shrink-0" style={{ background: '#091A7A' }}>
                              <Home size={11} className="text-white" />
                            </div>
                          ) : (
                            <div className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0" style={{ background: isActive ? '#091A7A' : '#E5E7EB', color: isActive ? 'white' : '#9CA3AF' }}>{si}</div>
                          )}
                          <span className="text-sm font-medium text-gray-900 flex-1 truncate">{screen.isHome ? (recipeName || 'Home') : (screen.title || tmpl?.name || 'Untitled')}</span>
                          {screen.isHome && <span className="text-[9px] px-1.5 py-0.5 rounded font-medium" style={{ background: '#091A7A20', color: '#091A7A' }}>Dashboard</span>}
                          {tmpl && !screen.isHome && <span className="text-[9px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-500">{tmpl.emoji} {tmpl.name}</span>}
                          <ChevronDown size={14} className="text-gray-400" style={{ transform: isActive ? 'rotate(180deg)' : 'rotate(0)' }} />
                          {!screen.isHome && screens.length > 1 && <button onClick={e => { e.stopPropagation(); removeScreen(si); }} className="text-gray-300 hover:text-red-500"><Trash2 size={13} /></button>}
                        </button>

                        {isActive && (
                          <div className="px-4 py-3 border-t border-gray-100 bg-white space-y-3">
                            {/* Non-home: editable title */}
                            {!screen.isHome && (
                              <div><label className="text-[10px] font-medium text-gray-500 uppercase tracking-wider mb-1 block">Screen Title</label><Input value={screen.title} onChange={e => updateScreenTitle(si, e.target.value)} className="h-8 text-sm" /></div>
                            )}

                            {/* Home: grid column selector */}
                            {screen.isHome && screens.length > 1 && (
                              <div>
                                <label className="text-[10px] font-medium text-gray-500 uppercase tracking-wider mb-2 block">Navigation Grid Layout</label>
                                <div className="flex gap-2">
                                  {[1, 2, 3].map(cols => {
                                    const selected = screen.gridColumns === cols;
                                    const otherCount = screens.filter(s => !s.isHome).length;
                                    const previewItems = Math.min(otherCount, cols * 2);
                                    return (
                                      <button key={cols} onClick={() => updateGridColumns(cols)}
                                        className="flex-1 rounded-lg border-2 p-3 flex flex-col items-center gap-2 transition-all"
                                        style={{ borderColor: selected ? '#091A7A' : '#E5E7EB', background: selected ? '#091A7A08' : 'white' }}>
                                        <div className="gap-1" style={{ display: 'grid', gridTemplateColumns: `repeat(${cols}, 1fr)`, width: '100%' }}>
                                          {Array.from({ length: previewItems }).map((_, j) => (
                                            <div key={j} className="h-4 rounded" style={{ background: selected ? '#091A7A' : '#E5E7EB' }} />
                                          ))}
                                        </div>
                                        <span className="text-[10px] font-medium" style={{ color: selected ? '#091A7A' : '#9CA3AF' }}>{cols} col{cols > 1 ? 's' : ''}</span>
                                      </button>
                                    );
                                  })}
                                </div>
                              </div>
                            )}

                            {/* Home: toggle Ask AI section */}
                            {screen.isHome && (
                              <div className="flex items-center gap-3 rounded-lg bg-gray-50 px-3 py-2">
                                <span className="text-xs text-gray-600 flex-1">Ask AI section below grid</span>
                                <button onClick={toggleHomeAsk}
                                  className="relative w-9 h-5 rounded-full transition-colors"
                                  style={{ background: screen.templateId ? '#091A7A' : '#D1D5DB' }}>
                                  <div className="absolute top-0.5 w-4 h-4 rounded-full bg-white shadow transition-transform"
                                    style={{ left: screen.templateId ? 18 : 2 }} />
                                </button>
                              </div>
                            )}

                            {/* Screen template editor */}
                            {renderScreenEditor(screen, si)}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* ─── Step 3: Knowledge ─── */}
          {currentStep === 3 && (
            <div className="space-y-5 max-w-2xl">
              <h2 className="text-lg font-semibold text-gray-900">Knowledge Base</h2>
              <p className="text-sm text-gray-500 -mt-3">Upload documents to give your recipe domain expertise.</p>
              <div onClick={() => fileInputRef.current?.click()} className="border-2 border-dashed border-gray-300 rounded-xl p-8 flex flex-col items-center gap-3 hover:border-gray-400 cursor-pointer">
                <div className="w-14 h-14 rounded-xl flex items-center justify-center" style={{ background: '#091A7A10' }}><Upload size={28} style={{ color: '#091A7A' }} /></div>
                <p className="text-sm font-medium text-gray-700">Drop files here or click to browse</p>
                <p className="text-xs text-gray-400">PDF, TXT, CSV up to 10MB</p>
              </div>
              <input ref={fileInputRef} type="file" accept=".pdf,.txt,.csv,.md" multiple className="hidden" onChange={handleFileUpload} />
              {knowledgeFiles.length > 0 && <div className="space-y-2">{knowledgeFiles.map((f, i) => <div key={i} className="flex items-center gap-3 p-3 rounded-lg border border-gray-100 bg-white"><FileText size={20} className={f.status === 'ready' ? 'text-green-600' : 'text-gray-400'} /><div className="flex-1 min-w-0"><p className="text-sm font-medium text-gray-800 truncate">{f.name}</p><div className="flex items-center gap-2 mt-0.5"><p className="text-xs text-gray-400">{f.size}</p>{f.status === 'ready' && f.chunks && <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-green-50 text-green-700">{f.chunks} chunks</span>}{f.status !== 'ready' && <span className="text-[10px] text-amber-600">{f.status}...</span>}</div>{f.status !== 'ready' && <Progress value={f.status === 'uploading' ? 40 : 75} className="mt-1.5 h-1.5" />}{f.summary && <p className="text-[11px] text-gray-500 mt-1.5 line-clamp-2">{f.summary}</p>}</div><button onClick={() => removeFile(i)} className="text-gray-400 hover:text-gray-600 shrink-0"><X size={16} /></button></div>)}</div>}
              {knowledgeFiles.some(f => f.status === 'ready') && <div className="rounded-xl border border-gray-200 bg-white p-4"><div className="flex items-center justify-between mb-2"><div><p className="text-sm font-medium text-gray-700">Always-Loaded Context</p><p className="text-xs text-gray-500">Concise brief always in memory.</p></div><button onClick={() => ensureApiKey(generateKnowledgeSummary)} disabled={aiLoading} className="flex items-center gap-1 px-3 py-1.5 rounded-md text-xs font-medium" style={{ background: '#091A7A10', color: '#091A7A' }}>{aiLoading ? <Loader2 size={12} className="animate-spin" /> : <Sparkles size={12} />} Generate Brief</button></div>{knowledgeSummary ? <Textarea value={knowledgeSummary} onChange={e => setKnowledgeSummary(e.target.value)} rows={6} className="text-xs" /> : <div className="rounded-lg p-3 bg-gray-50 text-xs text-gray-400 text-center">Click "Generate Brief" to create a summary</div>}</div>}
            </div>
          )}

          {/* ─── Step 4: Review ─── */}
          {currentStep === 4 && (
            <div className="space-y-5 max-w-2xl">
              <h2 className="text-lg font-semibold text-gray-900">Review Recipe</h2>
              <div className="rounded-xl border border-gray-200 bg-white overflow-hidden">
                <div className="px-5 py-4 flex items-center gap-3 border-b border-gray-100">
                  <span className="text-2xl">{recipeIcon}</span>
                  <div><p className="text-sm font-semibold text-gray-900">{recipeName || 'Untitled'}</p><p className="text-xs text-gray-500">{recipeDescription || 'No description'}</p></div>
                  <span className="ml-auto text-xs font-medium px-2.5 py-1 rounded-full bg-gray-100 text-gray-600">{category}</span>
                </div>
                <div className="px-5 py-3 grid grid-cols-3 gap-4 text-center border-b border-gray-100">
                  <div><p className="text-lg font-bold text-gray-900">{screens.length}</p><p className="text-[11px] text-gray-500">Screens</p></div>
                  <div><p className="text-lg font-bold text-gray-900">{screens.filter(s => s.templateId).length}</p><p className="text-[11px] text-gray-500">Templates</p></div>
                  <div><p className="text-lg font-bold text-gray-900">{selectedLanguages.length}</p><p className="text-[11px] text-gray-500">Languages</p></div>
                </div>
                <div className="px-5 py-3">
                  {screens.map(s => {
                    const pd = s.templateId ? getScreenTemplate(s.templateId) : null;
                    return (
                      <div key={s.id} className="flex items-center gap-2 py-1">
                        <span className="text-xs font-medium text-gray-700">{screenTitle(s)}</span>
                        {pd && <span className="text-[10px] text-gray-400">{pd.emoji} {pd.name}</span>}
                        {s.isHome && screens.length > 1 && <span className="text-[10px] text-gray-400">+ Grid ({s.gridColumns} col)</span>}
                      </div>
                    );
                  })}
                </div>
              </div>
              {/* Formula summary for calculator */}
              {screens.some(s => s.templateId === 'calculator') && (
                <div className="rounded-xl border border-gray-200 bg-white p-4">
                  <p className="text-xs font-medium text-gray-500 mb-2">Calculator Formula</p>
                  {screens.filter(s => s.templateId === 'calculator').map(s => (
                    <div key={s.id} className="flex items-center gap-2">
                      <span className="text-xs font-medium text-gray-700">{s.title}:</span>
                      <code className="text-xs font-mono text-gray-600 bg-gray-50 px-2 py-0.5 rounded">{resolveFormula(s)}</code>
                    </div>
                  ))}
                </div>
              )}
              {selectedLanguages.length > 0 && <div className="rounded-xl border border-gray-200 bg-white p-4"><p className="text-xs font-medium text-gray-500 mb-2">Languages</p><div className="flex flex-wrap gap-1.5">{selectedLanguages.map(c => <span key={c} className="text-xs px-2 py-0.5 rounded-md bg-gray-100 text-gray-700">{ALL_LANGUAGES.find(l => l.code === c)?.label || c}</span>)}</div></div>}
            </div>
          )}
        </div>

        {/* Bottom bar */}
        <div className="sticky bottom-0 border-t border-gray-200 bg-white px-8 py-4 flex items-center justify-between">
          <div>{currentStep > 1 && <button onClick={() => setCurrentStep(s => s - 1)} className="flex items-center gap-1 text-sm font-medium text-gray-600 hover:text-gray-900"><ChevronLeft size={16} /> Previous</button>}</div>
          <div className="flex items-center gap-3">
            {currentStep === 4 && <>
              <button onClick={() => setShowYamlPreview(true)} className="flex items-center gap-1.5 px-4 py-2 rounded-lg border border-gray-200 text-sm font-medium text-gray-700 hover:bg-gray-50"><Eye size={16} /> Preview YAML</button>
              <button onClick={downloadYaml} className="flex items-center gap-1.5 px-5 py-2 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: '#10B981' }}><Download size={16} /> Download YAML</button>
            </>}
            {currentStep < 4 && <button onClick={() => setCurrentStep(s => s + 1)} className="flex items-center gap-1 px-5 py-2 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: '#091A7A' }}>Next Step <ChevronRight size={16} /></button>}
          </div>
        </div>
      </div>

      {/* ─── Right: Live Preview ─── */}
      <div className="flex-[2] flex flex-col items-center bg-gray-50 p-6 overflow-y-auto">
        <div className="flex items-center gap-2 mb-4 self-start">
          <div className="w-2 h-2 rounded-full bg-red-500 animate-pulse" />
          <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Live Preview</span>
          {screens.length > 1 && <span className="text-[10px] text-gray-400 ml-2">{screenTitle(activeScreen)}</span>}
        </div>
        <div className="rounded-[2.5rem] p-3 shadow-xl" style={{ background: '#1F2937', width: 280 }}>
          <div className="rounded-[2rem] overflow-hidden flex flex-col" style={{ background: activeSecondary, height: 560 }}>
            <div className="flex items-center justify-between px-5 pt-3 pb-1">
              <span className="text-[10px] font-medium" style={{ color: activePrimary }}>9:41</span>
              <div className="flex gap-1"><div className="w-3 h-2 rounded-sm" style={{ background: activePrimary }} /><div className="w-3 h-2 rounded-sm" style={{ background: activePrimary, opacity: 0.5 }} /></div>
            </div>
            <div className="flex items-center gap-2 px-5 py-3">
              {activeScreenIndex > 0 && <button onClick={() => setActiveScreenIndex(0)} className="opacity-50 hover:opacity-100"><ChevronLeft size={14} style={{ color: activePrimary }} /></button>}
              <span className="text-lg">{recipeIcon}</span>
              <span className="text-sm font-semibold truncate" style={{ color: activePrimary }}>{screenTitle(activeScreen)}</span>
            </div>
            <div className="flex-1 px-4 pb-4 overflow-y-auto">
              <div className="space-y-2">{previewWidgets.map((w, i) => renderWidget(w, i))}</div>
            </div>
            {screens.length > 1 && <div className="flex gap-1 justify-center pb-2 px-3">{screens.map((s, i) => <button key={s.id} onClick={() => setActiveScreenIndex(i)} className="px-2 py-0.5 rounded-full text-[7px] font-medium cursor-pointer" style={{ background: i === activeScreenIndex ? activePrimary : activePrimary + '20', color: i === activeScreenIndex ? 'white' : activePrimary }}>{s.isHome ? (recipeName || 'Home') : s.title}</button>)}</div>}
            <div className="flex justify-center pb-2"><div className="w-24 h-1 rounded-full" style={{ background: activePrimary + '40' }} /></div>
          </div>
        </div>
      </div>

      {/* ─── Dialogs ─── */}
      <Dialog open={showYamlPreview} onOpenChange={setShowYamlPreview}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Recipe YAML</DialogTitle><DialogDescription>Generated DSL configuration</DialogDescription></DialogHeader>
          <pre className="bg-gray-900 text-green-400 p-4 rounded-lg text-xs leading-relaxed overflow-x-auto whitespace-pre-wrap">{generateYaml()}</pre>
          <button onClick={downloadYaml} className="flex items-center justify-center gap-2 w-full py-2.5 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: '#091A7A' }}><Download size={16} /> Download YAML File</button>
        </DialogContent>
      </Dialog>

      <Dialog open={showApiKeyDialog} onOpenChange={setShowApiKeyDialog}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>Google AI Studio API Key</DialogTitle><DialogDescription>Enter your Gemini API key to enable AI features.</DialogDescription></DialogHeader>
          <Input value={apiKey} onChange={e => setApiKey(e.target.value)} placeholder="AIza..." type="password" />
          <button onClick={() => { setShowApiKeyDialog(false); localStorage.setItem(API_KEY_STORAGE, apiKey); toast.success('API key saved'); }} disabled={!apiKey} className="w-full py-2.5 rounded-lg text-white text-sm font-medium hover:opacity-90" style={{ background: apiKey ? '#091A7A' : '#9CA3AF' }}>Save & Continue</button>
        </DialogContent>
      </Dialog>

      <Dialog open={showTemplatePicker} onOpenChange={setShowTemplatePicker}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Add Screen</DialogTitle><DialogDescription>Choose a screen type for the new screen</DialogDescription></DialogHeader>
          <div className="grid grid-cols-2 gap-3">
            {SCREEN_TEMPLATES.map(pkg => (
              <button key={pkg.id} onClick={() => addScreenFromTemplate(pkg.id)}
                className="flex flex-col gap-2 p-4 rounded-xl border-2 border-gray-200 text-left hover:border-gray-300 hover:bg-gray-50 transition-all">
                <div className="flex items-center gap-2">
                  <span className="text-xl">{pkg.emoji}</span>
                  <span className="text-sm font-semibold text-gray-900">{pkg.name}</span>
                </div>
                <p className="text-xs text-gray-500">{pkg.description}</p>
                <div className="flex flex-wrap gap-1">
                  {pkg.widgets.filter(w => w.defaultOn).map(w => (
                    <span key={w.wid} className="text-[9px] px-1 py-0.5 rounded bg-gray-100 text-gray-500">{w.type.replace(/_/g, ' ')}</span>
                  ))}
                </div>
              </button>
            ))}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
