import { useState } from 'react';
import { RECIPES, resolveScreenWidgets, resolveFormula, generateScreenDescription, generatePrefillHints, getScreenAcceptedInputs } from './recipes';
import type { RecipeConfig, ScreenConfig, WidgetConfig, IntroPageConfig } from './recipes';
import { createRecipe } from '../../lib/recipeService';

function widgetToYaml(w: WidgetConfig, allScreens: { id: string; title: string; icon?: string }[]): string {
  const p = w.props;
  const esc = (s: string) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  const line = (k: string, v: string, q = false) => q ? `\n          ${k}: "${esc(v)}"` : `\n          ${k}: ${v}`;
  const opt = (k: string, v: string | undefined, q = false) => v ? line(k, v, q) : '';

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
            const icon = s.icon;
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
      try { items = JSON.parse(p.items || '[]'); } catch { /* empty */ }
      const itemsYaml = items.map(it => `            - { label: "${it.label}", type: "${it.type}" }`).join('\n');
      return `      - checklist_items:${line('bind', p.bind || 'checklist_step')}\n          items:\n${itemsYaml}`;
    }
    default:
      return `      - ${w.type}: {}`;
  }
}

function generateYaml(recipe: RecipeConfig): string {
  const yamlEsc = (s: string) => s.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  const name = recipe.recipeName || 'My Recipe';
  const desc = recipe.recipeDescription || 'A custom AI recipe';
  const id = name.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '') || 'my_recipe';
  const { screens } = recipe;

  const sysPromptText = recipe.systemPrompt.trim();
  const sysPrompt = sysPromptText
    ? `  system_prompt: |\n    ${sysPromptText.split('\n').join('\n    ')}`
    : '  system_prompt: "You are a helpful assistant."';

  const blocked = recipe.blockedKeywords.split(',').map(k => k.trim()).filter(Boolean);
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
    if (!s.templateId || s.templateId !== 'ask_ai') return '';
    const qs = [s.fieldValues.q1, s.fieldValues.q2, s.fieldValues.q3, s.fieldValues.q4]
      .filter(q => q && q.trim())
      .map(q => `    - "${yamlEsc(q)}"`);
    if (qs.length === 0) return '';
    return `  ${s.id}:\n${qs.join('\n')}`;
  }).filter(Boolean).join('\n');

  const nonHomeScreens = screens.filter(s => !s.isHome).map(s => ({ id: s.id, title: s.title, icon: s.screenIcon }));
  const screensYaml = screens.map((screen, si) => {
    const widgets = allResolved[si];
    const body = widgets.map(w => widgetToYaml(w, nonHomeScreens)).join('\n');
    let routingYaml = '';
    if (screen.routing && screen.routing.field && screen.routing.rules.length > 0) {
      const rulesStr = screen.routing.rules.map(r => `        - { value: "${yamlEsc(r.value)}", goto: "${r.goto}" }`).join('\n');
      routingYaml = `\n    next:\n      field: ${screen.routing.field}\n      rules:\n${rulesStr}${screen.routing.fallback ? `\n      fallback: ${screen.routing.fallback}` : ''}`;
    }
    return `  - id: ${screen.id}\n    title: "${yamlEsc(screen.title)}"\n    body:\n${body}${routingYaml}`;
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
  const loc = recipe.selectedLanguages.length > 0 ? `\nlocalisation:\n  supported:\n${recipe.selectedLanguages.map(l => `    - ${l}`).join('\n')}\n  default: ${recipe.selectedLanguages[0] || 'en'}\n` : '';
  const know = recipe.knowledgeSummary ? `\nknowledge:\n  always_loaded: |\n    ${recipe.knowledgeSummary.split('\n').join('\n    ')}\n  chunks: 0\n` : '';
  const questionsBlock = questionsYaml ? `\nquestions:\n${questionsYaml}\n` : '';

  const introPage = recipe.introPage;
  let setupBlock = '';
  if (introPage) {
    let introYaml = `\nsetup:\n  intro_page:\n    accept_label: "${yamlEsc(introPage.acceptLabel || 'I Understand')}"`;
    if (introPage.disclaimer) introYaml += `\n    disclaimer: "${yamlEsc(introPage.disclaimer)}"`;
    if (introPage.authorName) introYaml += `\n    author:\n      name: "${yamlEsc(introPage.authorName)}"${introPage.authorOrg ? `\n      organisation: "${yamlEsc(introPage.authorOrg)}"` : ''}${introPage.authorVerified ? '\n      verified: true' : ''}`;
    if (introPage.links && introPage.links.length > 0) {
      introYaml += `\n    links:`;
      introPage.links.forEach(l => { if (l.label && l.url) introYaml += `\n      - { label: "${yamlEsc(l.label)}", url: "${l.url}" }`; });
    }
    setupBlock = introYaml + '\n';
  }

  const catalogScreens = screens.filter(s => !s.isHome && s.templateId);
  const screenCatalog = catalogScreens.length > 0 ? `\nscreen_catalog:\n${catalogScreens.map(s => {
    const desc2 = s.description || generateScreenDescription(s);
    const accepted = getScreenAcceptedInputs(s);
    const hints = s.prefillHints || generatePrefillHints(s);
    const hintEntries = Object.entries(hints);
    let entry = `  - id: ${s.id}\n    title: "${yamlEsc(s.title)}"\n    template: ${s.templateId}`;
    if (s.screenIcon) entry += `\n    icon: "${s.screenIcon}"`;
    if (desc2) entry += `\n    description: "${yamlEsc(desc2)}"`;
    if (accepted.length) entry += `\n    accepted_inputs: [${accepted.join(', ')}]`;
    if (hintEntries.length) {
      entry += `\n    prefill_hints:`;
      hintEntries.forEach(([k, v]) => { entry += `\n      ${k}: ${v}`; });
    }
    return entry;
  }).join('\n')}\n` : '';

  const homeScreen = screens.find(s => s.isHome);
  const homeMode = homeScreen?.templateId ? 'chat' : 'grid';
  const triageBlock = `\ntriage:\n  home_mode: "${homeMode}"\n  max_clarifications: ${recipe.maxClarifications ?? 2}\n  fallback: "${recipe.fallbackScreen || 'show_all'}"\n`;

  return `id: ${id}\nname: "${yamlEsc(name)}"\ndescription: "${yamlEsc(desc)}"\nicon: "${recipe.recipeIcon}"\nversion: "1.0.0"\ncategory: ${recipe.category}\n\nauthor:\n  name: ${yamlEsc(introPage?.authorName || 'User')}\n  organisation: "${yamlEsc(introPage?.authorOrg || '')}"\n  verified: ${introPage?.authorVerified ? 'true' : 'false'}\n\nmodel:\n  model_id: gemma-4-e2b-it\n  backend: cpu\n${sysPrompt}\n\ntheme:\n  primary: "${recipe.customPrimary}"\n  secondary: "${recipe.customSecondary}"\n\nvariables:\n${vars.join('\n')}\n\nscreens:\n${screensYaml}\n${formulas}${data}\nsafety:\n  blocked_keywords:\n${blockedYaml}\n  escalation_message: "This request has been blocked for safety."\n  disclaimer: "${yamlEsc(recipe.disclaimer || 'AI-generated content.')}"\n\npermissions:\n${perms.length > 0 ? perms.join('\n') : '  []'}${loc}${know}${questionsBlock}${setupBlock}${screenCatalog}${triageBlock}`;
}

interface SeedResult {
  name: string;
  status: 'pending' | 'seeding' | 'done' | 'error';
  firestoreId?: string;
  error?: string;
}

export function SeedFirestore() {
  const [results, setResults] = useState<SeedResult[]>(
    Object.keys(RECIPES).map(name => ({ name, status: 'pending' }))
  );
  const [seeding, setSeeding] = useState(false);

  const seedAll = async () => {
    setSeeding(true);
    const entries = Object.entries(RECIPES);
    for (let i = 0; i < entries.length; i++) {
      const [name, recipe] = entries[i];
      setResults(prev => prev.map((r, ri) => ri === i ? { ...r, status: 'seeding' } : r));
      try {
        const yaml = generateYaml(recipe);
        const config: RecipeConfig = {
          ...recipe,
          generatedYaml: yaml,
        };
        const docId = await createRecipe(config);
        setResults(prev => prev.map((r, ri) => ri === i ? { ...r, status: 'done', firestoreId: docId } : r));
      } catch (e) {
        setResults(prev => prev.map((r, ri) => ri === i ? { ...r, status: 'error', error: String(e) } : r));
      }
    }
    setSeeding(false);
  };

  const seedOne = async (index: number) => {
    const [name, recipe] = Object.entries(RECIPES)[index];
    setResults(prev => prev.map((r, ri) => ri === index ? { ...r, status: 'seeding' } : r));
    try {
      const yaml = generateYaml(recipe);
      const config: RecipeConfig = {
        ...recipe,
        generatedYaml: yaml,
      };
      const docId = await createRecipe(config);
      setResults(prev => prev.map((r, ri) => ri === index ? { ...r, status: 'done', firestoreId: docId } : r));
    } catch (e) {
      setResults(prev => prev.map((r, ri) => ri === index ? { ...r, status: 'error', error: String(e) } : r));
    }
  };

  return (
    <div style={{ maxWidth: 600, margin: '40px auto', fontFamily: 'system-ui' }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, marginBottom: 8 }}>Seed Recipes to Firestore</h1>
      <p style={{ color: '#666', marginBottom: 24, fontSize: 14 }}>
        Push all pre-built recipe configs + generated YAML to Firestore so Android Hub can fetch them.
      </p>

      <button
        onClick={seedAll}
        disabled={seeding}
        style={{
          padding: '10px 24px', borderRadius: 8, border: 'none', cursor: seeding ? 'not-allowed' : 'pointer',
          background: seeding ? '#ccc' : '#15803D', color: 'white', fontWeight: 600, fontSize: 14, marginBottom: 24,
        }}
      >
        {seeding ? 'Seeding...' : 'Seed All Recipes'}
      </button>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {results.map((r, i) => {
          const recipe = Object.values(RECIPES)[i];
          return (
            <div key={r.name} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: 16, borderRadius: 12,
              border: '1px solid #e5e5e5', background: r.status === 'done' ? '#f0fdf4' : r.status === 'error' ? '#fef2f2' : 'white',
            }}>
              <span style={{ fontSize: 28 }}>{recipe.recipeIcon}</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: 14 }}>{recipe.recipeName}</div>
                <div style={{ fontSize: 12, color: '#666' }}>{recipe.recipeDescription}</div>
                <div style={{ fontSize: 11, color: '#999', marginTop: 2 }}>
                  {recipe.screens.length} screens · {recipe.category}
                </div>
                {r.status === 'done' && r.firestoreId && (
                  <div style={{ fontSize: 11, color: '#15803D', marginTop: 2 }}>
                    Firestore ID: {r.firestoreId}
                  </div>
                )}
                {r.status === 'error' && (
                  <div style={{ fontSize: 11, color: '#DC2626', marginTop: 2 }}>{r.error}</div>
                )}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                {r.status === 'pending' && (
                  <button onClick={() => seedOne(i)} style={{
                    padding: '6px 12px', borderRadius: 6, border: '1px solid #d1d5db',
                    background: 'white', cursor: 'pointer', fontSize: 12,
                  }}>Seed</button>
                )}
                {r.status === 'seeding' && <span style={{ fontSize: 12, color: '#D97706' }}>Pushing...</span>}
                {r.status === 'done' && <span style={{ fontSize: 16 }}>{'✅'}</span>}
                {r.status === 'error' && <span style={{ fontSize: 16 }}>{'❌'}</span>}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
