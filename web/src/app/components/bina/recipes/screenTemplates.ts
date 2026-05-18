import type {
  ScreenTemplate, ScreenConfig, WidgetConfig, WidgetProps,
} from './types';

export const FORMULA_TEMPLATES: { label: string; expr: string }[] = [
  { label: 'A × B', expr: '{{calc_a}} * {{calc_b}}' },
  { label: 'A + B', expr: '{{calc_a}} + {{calc_b}}' },
  { label: 'A − B', expr: '{{calc_a}} - {{calc_b}}' },
  { label: 'A / B', expr: '{{calc_a}} / {{calc_b}}' },
  { label: 'A × B × Rate%', expr: '{{calc_a}} * {{calc_b}} * {{calc_rate}} / 100' },
  { label: '(A + B) × Rate%', expr: '({{calc_a}} + {{calc_b}}) * {{calc_rate}} / 100' },
  { label: 'Profit: (A−B)×(1−Rate%)', expr: '({{calc_a}} - {{calc_b}}) * (1 - {{calc_rate}} / 100)' },
  { label: 'A × B × C', expr: '{{calc_a}} * {{calc_b}} * {{calc_c}}' },
  { label: '(A + B) × (C − D)', expr: '({{calc_a}} + {{calc_b}}) * ({{calc_c}} - {{calc_d}})' },
  { label: 'Custom', expr: '' },
];

export function resolveFormula(screen: ScreenConfig): string {
  const tmpl = screen.fieldValues.formula_template || 'A × B';
  if (tmpl === 'Custom') return screen.fieldValues.custom_formula || '{{calc_a}} * {{calc_b}}';
  return FORMULA_TEMPLATES.find(t => t.label === tmpl)?.expr || '{{calc_a}} * {{calc_b}}';
}

export const SCREEN_TEMPLATES: ScreenTemplate[] = [
  {
    id: 'ask_ai', name: 'Ask AI', emoji: '\u{1F4AC}', description: 'Multi-question flow or structured form with AI response',
    widgets: [
      { wid: 'text_label', type: 'text_label', optional: false, defaultOn: true, staticProps: { style: 'subheading' }, fieldMap: { heading: 'text' } },
      { wid: 'text_input', type: 'text_input', optional: false, defaultOn: true, staticProps: { bind: 'user_text' }, fieldMap: { hint: 'hint' } },
      { wid: 'voice_input', type: 'voice_input', optional: true, defaultOn: true, staticProps: { bind: 'user_text', mode: 'tap' }, fieldMap: { hint: 'hint' } },
      { wid: 'action_button', type: 'action_button', optional: false, defaultOn: true, staticProps: { style: 'primary' }, fieldMap: { button_label: 'label', ai_instruction: 'action' } },
      { wid: 'markdown_output', type: 'markdown_output', optional: false, defaultOn: true, staticProps: { source: 'ai_response', streaming: 'true' }, fieldMap: {} },
    ],
    fields: [
      { key: 'mode', label: 'Mode', placeholder: '', type: 'select', options: ['form', 'chat'], defaultValue: 'form' },
      { key: 'heading', label: 'Heading', placeholder: 'How can I help?', type: 'text', defaultValue: 'How can I help?' },
      { key: 'hint', label: 'Input hint', placeholder: 'Ask a question...', type: 'text', defaultValue: 'Ask a question...', showWhen: { field: 'mode', value: 'chat' } },
      { key: 'q1', label: 'Question 1', placeholder: 'e.g. What is your name?', type: 'text', defaultValue: '', showWhen: { field: 'mode', value: 'chat' } },
      { key: 'q2', label: 'Question 2', placeholder: 'e.g. What do you need help with?', type: 'text', defaultValue: '', showWhen: { field: 'mode', value: 'chat' } },
      { key: 'q3', label: 'Question 3', placeholder: '', type: 'text', defaultValue: '', showWhen: { field: 'mode', value: 'chat' } },
      { key: 'q4', label: 'Question 4', placeholder: '', type: 'text', defaultValue: '', showWhen: { field: 'mode', value: 'chat' } },
      { key: 'button_label', label: 'Button label', placeholder: 'Ask', type: 'text', defaultValue: 'Ask' },
      { key: 'ai_instruction', label: 'AI instruction', placeholder: 'ask:{{user_text}}', type: 'textarea', defaultValue: 'ask:{{user_text}}' },
    ],
  },
  {
    id: 'camera_analysis', name: 'Camera Analysis', emoji: '\u{1F4F7}', description: 'Take photo, get AI analysis',
    widgets: [
      { wid: 'camera_input', type: 'camera_input', optional: false, defaultOn: true, staticProps: { bind: 'photo_path', preview: 'true' }, fieldMap: { camera_label: 'label' } },
      { wid: 'text_input', type: 'text_input', optional: true, defaultOn: true, staticProps: { bind: 'user_text', hint: 'Add context (optional)' }, fieldMap: {} },
      { wid: 'voice_input', type: 'voice_input', optional: true, defaultOn: false, staticProps: { bind: 'user_text', mode: 'tap' }, fieldMap: {} },
      { wid: 'action_button', type: 'action_button', optional: false, defaultOn: true, staticProps: { style: 'primary' }, fieldMap: { button_label: 'label', ai_instruction: 'action' } },
      { wid: 'markdown_output', type: 'markdown_output', optional: false, defaultOn: true, staticProps: { source: 'ai_response', streaming: 'true' }, fieldMap: {} },
    ],
    fields: [
      { key: 'camera_label', label: 'Camera button', placeholder: 'Take Photo', type: 'text', defaultValue: 'Take Photo' },
      { key: 'button_label', label: 'Analyse button', placeholder: 'Analyse', type: 'text', defaultValue: 'Analyse' },
      { key: 'ai_instruction', label: 'AI instruction', placeholder: 'vision_ask:Analyse this image. {{user_text}}', type: 'textarea', defaultValue: 'vision_ask:Analyse this image. {{user_text}}' },
    ],
  },
  {
    id: 'calculator', name: 'Calculator', emoji: '\u{1F9EE}', description: 'Input fields, formula, result display',
    widgets: [
      { wid: 'input_a', type: 'text_input', optional: false, defaultOn: true, staticProps: { bind: 'calc_a', input_type: 'number' }, fieldMap: { field_a_label: 'label', field_a_hint: 'hint' } },
      { wid: 'input_b', type: 'text_input', optional: true, defaultOn: true, staticProps: { bind: 'calc_b', input_type: 'number' }, fieldMap: { field_b_label: 'label', field_b_hint: 'hint' } },
      { wid: 'input_c', type: 'text_input', optional: true, defaultOn: false, staticProps: { bind: 'calc_c', input_type: 'number' }, fieldMap: { field_c_label: 'label', field_c_hint: 'hint' } },
      { wid: 'input_d', type: 'text_input', optional: true, defaultOn: false, staticProps: { bind: 'calc_d', input_type: 'number' }, fieldMap: { field_d_label: 'label', field_d_hint: 'hint' } },
      { wid: 'slider', type: 'slider', optional: true, defaultOn: true, staticProps: { bind: 'calc_rate', step: '1', show_value: 'true' }, fieldMap: { slider_label: 'label', slider_min: 'min', slider_max: 'max' } },
      { wid: 'action_button', type: 'action_button', optional: false, defaultOn: true, staticProps: { label: 'Calculate', action: 'formula:calc', style: 'primary' }, fieldMap: {} },
      { wid: 'metric_card', type: 'metric_card', optional: false, defaultOn: true, staticProps: { source: 'calc_result', format: 'decimal_2' }, fieldMap: { result_label: 'label', result_prefix: 'prefix', result_suffix: 'suffix' } },
    ],
    fields: [
      { key: 'field_a_label', label: 'Field A', placeholder: 'e.g. Area', type: 'text', defaultValue: 'Value A' },
      { key: 'field_a_hint', label: 'Hint A', placeholder: 'e.g. Enter area in hectares', type: 'text', defaultValue: 'Enter value' },
      { key: 'field_b_label', label: 'Field B', placeholder: 'e.g. Yield per ha', type: 'text', defaultValue: 'Value B' },
      { key: 'field_b_hint', label: 'Hint B', placeholder: 'e.g. Expected kg per hectare', type: 'text', defaultValue: 'Enter value' },
      { key: 'field_c_label', label: 'Field C', placeholder: 'e.g. Quantity', type: 'text', defaultValue: 'Value C' },
      { key: 'field_c_hint', label: 'Hint C', placeholder: 'Enter value', type: 'text', defaultValue: 'Enter value' },
      { key: 'field_d_label', label: 'Field D', placeholder: 'e.g. Price', type: 'text', defaultValue: 'Value D' },
      { key: 'field_d_hint', label: 'Hint D', placeholder: 'Enter value', type: 'text', defaultValue: 'Enter value' },
      { key: 'slider_label', label: 'Slider', placeholder: 'Rate %', type: 'text', defaultValue: 'Rate %' },
      { key: 'slider_min', label: 'Slider min', placeholder: '0', type: 'text', defaultValue: '0' },
      { key: 'slider_max', label: 'Slider max', placeholder: '100', type: 'text', defaultValue: '100' },
      { key: 'formula_template', label: 'Formula', placeholder: '', type: 'select', options: FORMULA_TEMPLATES.map(t => t.label), defaultValue: 'A × B' },
      { key: 'custom_formula', label: 'Expression', placeholder: '{{calc_a}} * {{calc_b}} * {{calc_rate}} / 100', type: 'text', defaultValue: '', showWhen: { field: 'formula_template', value: 'Custom' } },
      { key: 'result_label', label: 'Result', placeholder: 'Total', type: 'text', defaultValue: 'Result' },
      { key: 'result_prefix', label: 'Prefix', placeholder: 'e.g. $', type: 'text', defaultValue: '' },
      { key: 'result_suffix', label: 'Suffix', placeholder: 'e.g. kg', type: 'text', defaultValue: '' },
    ],
  },
  {
    id: 'nearby_places', name: 'Nearby Places', emoji: '\u{1F4CD}', description: 'Find and display nearby locations',
    widgets: [
      { wid: 'text_label', type: 'text_label', optional: false, defaultOn: true, staticProps: { style: 'subheading' }, fieldMap: { heading: 'text' } },
      { wid: 'action_button', type: 'action_button', optional: false, defaultOn: true, staticProps: { label: 'Get My Location', action: 'geolocate', style: 'primary' }, fieldMap: {} },
      { wid: 'geo_display', type: 'geo_display', optional: false, defaultOn: true, staticProps: { data: 'places', limit: '5', show_distance: 'true', empty_text: 'Tap above to find nearby places' }, fieldMap: {} },
    ],
    fields: [
      { key: 'heading', label: 'Heading text', placeholder: 'Find places near you', type: 'text', defaultValue: 'Find places near you' },
    ],
  },
  {
    id: 'info_display', name: 'Info Text', emoji: '\u{1F4DD}', description: 'Static text heading or paragraph',
    widgets: [
      { wid: 'text_label', type: 'text_label', optional: false, defaultOn: true, staticProps: {}, fieldMap: { text: 'text', style: 'style' } },
    ],
    fields: [
      { key: 'text', label: 'Text', placeholder: 'Welcome', type: 'text', defaultValue: 'Welcome' },
      { key: 'style', label: 'Style', placeholder: 'heading', type: 'select', options: ['heading', 'subheading', 'body', 'caption'], defaultValue: 'heading' },
    ],
  },
  {
    id: 'checklist', name: 'Checklist', emoji: '\u{2705}', description: 'Step-by-step workflow with progress tracking',
    widgets: [
      { wid: 'progress_bar', type: 'progress_bar', optional: false, defaultOn: true, staticProps: { bind: 'checklist_step' }, fieldMap: {} },
      { wid: 'checklist_items', type: 'checklist_items', optional: false, defaultOn: true, staticProps: { bind: 'checklist_step' }, fieldMap: {} },
      { wid: 'action_button', type: 'action_button', optional: false, defaultOn: true, staticProps: { label: 'Next Step', action: 'increment:checklist_step', style: 'primary' }, fieldMap: {} },
      { wid: 'markdown_output', type: 'markdown_output', optional: true, defaultOn: false, staticProps: { source: 'ai_response', streaming: 'true' }, fieldMap: {} },
    ],
    fields: [
      { key: 'steps', label: 'Steps (one per line)', placeholder: 'Measure pH|number\nTake photo|photo\nCheck moisture|toggle', type: 'textarea', defaultValue: 'Step 1|text\nStep 2|text\nStep 3|text' },
      { key: 'completion_action', label: 'On completion', placeholder: '', type: 'select', options: ['none', 'ai_summary', 'show_result'], defaultValue: 'none' },
    ],
  },
  {
    id: 'sms_dispatch', name: 'SMS / Phone', emoji: '\u{1F4F1}', description: 'Pre-configured contacts for SMS or phone calls',
    widgets: [
      { wid: 'text_label', type: 'text_label', optional: false, defaultOn: true, staticProps: { style: 'subheading' }, fieldMap: { heading: 'text' } },
      { wid: 'geolocate_btn', type: 'action_button', optional: true, defaultOn: true, staticProps: { label: '\u{1F4CD} Get My Location', action: 'geolocate', style: 'secondary' }, fieldMap: {} },
      { wid: 'contact_list', type: 'action_button', optional: false, defaultOn: true, staticProps: { style: 'primary' }, fieldMap: {} },
    ],
    fields: [
      { key: 'heading', label: 'Heading', placeholder: 'Emergency Contacts', type: 'text', defaultValue: 'Emergency Contacts' },
      { key: 'contact_type', label: 'Action type', placeholder: '', type: 'select', options: ['sms', 'call'], defaultValue: 'sms' },
      { key: 'contacts', label: 'Contacts (one per line: Name | Phone number)', placeholder: 'Ambulance | 999\nFire | 994\nPoison Control | +60123456789', type: 'textarea', defaultValue: 'Contact 1 | +60123456789\nContact 2 | +60198765432' },
      { key: 'sms_template', label: 'SMS message template', placeholder: 'Help needed at my location. Situation: {{user_text}}', type: 'textarea', defaultValue: 'Help needed. Please respond.', showWhen: { field: 'contact_type', value: 'sms' } },
    ],
  },
];

export const getScreenTemplate = (id: string) => SCREEN_TEMPLATES.find(t => t.id === id);

export function createScreen(templateId: string, fieldOverrides?: Record<string, string>): Pick<ScreenConfig, 'templateId' | 'fieldValues' | 'disabledWidgets'> {
  const def = getScreenTemplate(templateId);
  const fieldValues: Record<string, string> = {};
  def?.fields.forEach(f => { fieldValues[f.key] = fieldOverrides?.[f.key] || f.defaultValue; });
  const disabledWidgets = def?.widgets.filter(w => w.optional && !w.defaultOn).map(w => w.wid) || [];
  return { templateId, fieldValues, disabledWidgets };
}

export function generateScreenDescription(screen: ScreenConfig): string {
  if (!screen.templateId) return '';
  const def = getScreenTemplate(screen.templateId);
  if (!def) return '';
  const title = screen.title || def.name;
  const widgets = def.widgets.filter(w => !screen.disabledWidgets.includes(w.wid));
  const inputs = widgets.filter(w => w.staticProps.bind).map(w => w.staticProps.bind);
  const fieldSummary = def.fields
    .filter(f => screen.fieldValues[f.key] && !['mode', 'formula_template', 'completion_action'].includes(f.key))
    .map(f => {
      const val = screen.fieldValues[f.key];
      if (!val || val === f.defaultValue) return null;
      return `${f.label}: ${val}`;
    })
    .filter(Boolean)
    .slice(0, 3)
    .join(', ');
  const parts = [`SCREEN: ${title}`, `FUNCTION: ${def.description}`];
  if (inputs.length) parts.push(`INPUTS: ${inputs.join(', ')}`);
  if (fieldSummary) parts.push(`CONTEXT: ${fieldSummary}`);
  return parts.join(' | ');
}

export function generatePrefillHints(screen: ScreenConfig): Record<string, string> {
  if (!screen.templateId) return {};
  const def = getScreenTemplate(screen.templateId);
  if (!def) return {};
  const hints: Record<string, string> = {};
  for (const widget of def.widgets) {
    if (screen.disabledWidgets.includes(widget.wid)) continue;
    const bind = widget.staticProps.bind;
    if (!bind) continue;
    for (const [fieldKey, propKey] of Object.entries(widget.fieldMap)) {
      if (propKey !== 'label' && propKey !== 'hint') continue;
      const label = screen.fieldValues[fieldKey];
      if (!label) continue;
      const key = label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
      if (key) hints[key] = bind;
    }
  }
  if (screen.templateId === 'ask_ai' && screen.fieldValues.mode === 'form') {
    const fieldCount = parseInt(screen.fieldValues.form_field_count || '2') || 2;
    for (let i = 1; i <= fieldCount; i++) {
      const label = screen.fieldValues[`f${i}_label`];
      if (!label) continue;
      const key = label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
      if (key) hints[key] = `form_f${i}`;
    }
  }
  return hints;
}

export function getScreenAcceptedInputs(screen: ScreenConfig): string[] {
  if (!screen.templateId) return [];
  const def = getScreenTemplate(screen.templateId);
  if (!def) return [];
  const inputs: string[] = [];
  for (const widget of def.widgets) {
    if (screen.disabledWidgets.includes(widget.wid)) continue;
    if (widget.staticProps.bind) inputs.push(widget.staticProps.bind);
  }
  if (screen.templateId === 'ask_ai' && screen.fieldValues.mode === 'form') {
    const fieldCount = parseInt(screen.fieldValues.form_field_count || '2') || 2;
    for (let i = 1; i <= fieldCount; i++) {
      if (screen.fieldValues[`f${i}_label`]) inputs.push(`form_f${i}`);
    }
  }
  return [...new Set(inputs)];
}

export function parseChecklistSteps(stepsStr: string): { label: string; type: string }[] {
  return stepsStr.split('\n').filter(Boolean).map(line => {
    const [label, type] = line.split('|', 2).map(s => s.trim());
    return { label: label || 'Step', type: type || 'text' };
  });
}

export function resolveTemplateWidgets(screen: ScreenConfig): WidgetConfig[] {
  if (!screen.templateId) return [];
  const def = getScreenTemplate(screen.templateId);
  if (!def) return [];

  let widgets = def.widgets
    .filter(pw => !screen.disabledWidgets.includes(pw.wid))
    .map(pw => {
      const props = { ...pw.staticProps };
      for (const [fieldKey, propKey] of Object.entries(pw.fieldMap)) {
        const val = screen.fieldValues[fieldKey];
        if (val !== undefined && val !== '') props[propKey] = val;
      }
      return { type: pw.type, props };
    });

  // ask_ai form mode: replace single text_input with per-field inputs
  if (screen.templateId === 'ask_ai' && screen.fieldValues.mode === 'form') {
    const filtered = widgets.filter(w => !(w.type === 'text_input' && w.props.bind === 'user_text'));
    const btnIdx = filtered.findIndex(w => w.type === 'action_button');
    const formWidgets: WidgetConfig[] = [];
    const fieldCount = parseInt(screen.fieldValues.form_field_count || '2') || 2;
    for (let i = 1; i <= fieldCount; i++) {
      const label = screen.fieldValues[`f${i}_label`] || '';
      const fType = screen.fieldValues[`f${i}_type`] || 'text';
      const options = screen.fieldValues[`f${i}_options`] || '';
      formWidgets.push({
        type: 'text_input',
        props: {
          bind: `form_f${i}`,
          label: label || `Field ${i}`,
          hint: label || `Field ${i}`,
          input_type: fType,
          ...(fType === 'dropdown' && options ? { options } : {}),
        },
      });
    }
    filtered.splice(btnIdx >= 0 ? btnIdx : filtered.length, 0, ...formWidgets);
    widgets = filtered;
  }

  // sms_dispatch: expand contacts into individual action buttons
  if (screen.templateId === 'sms_dispatch') {
    const contactType = screen.fieldValues.contact_type || 'sms';
    const contacts = (screen.fieldValues.contacts || '').split('\n').map(l => l.trim()).filter(Boolean);
    const smsTemplate = screen.fieldValues.sms_template || '';
    const contactWidgets: WidgetConfig[] = contacts.map(line => {
      const [name, phone] = line.split('|').map(s => s.trim());
      if (!name || !phone) return null;
      const action = contactType === 'call' ? `tel:${phone}` : `sms:${phone}:${smsTemplate}`;
      return {
        type: 'action_button' as const,
        props: { label: `${contactType === 'call' ? '\u{1F4DE}' : '\u{1F4E8}'} ${name}`, action, style: 'secondary' },
      };
    }).filter((w): w is WidgetConfig => w !== null);
    widgets = widgets.filter(w => !(w.type === 'action_button' && w.props.action !== 'geolocate'));
    widgets.push(...contactWidgets);
  }

  // checklist: inject parsed steps into widget props
  if (screen.templateId === 'checklist') {
    const steps = parseChecklistSteps(screen.fieldValues.steps || '');
    widgets = widgets.map(w => {
      if (w.type === 'progress_bar') return { ...w, props: { ...w.props, total: String(steps.length) } };
      if (w.type === 'checklist_items') return { ...w, props: { ...w.props, items: JSON.stringify(steps) } };
      return w;
    });
  }

  return widgets;
}

export function resolveScreenWidgets(screen: ScreenConfig, allScreens: ScreenConfig[]): WidgetConfig[] {
  const widgets: WidgetConfig[] = [];
  const nonHomeScreens = allScreens.filter(s => !s.isHome);
  if (screen.isHome && nonHomeScreens.length > 0 && !screen.templateId) {
    // Emit a heading text_label if the home screen has a heading field
    if (screen.fieldValues?.heading) {
      widgets.push({
        type: 'text_label',
        props: { text: screen.fieldValues.heading, style: 'subheading' },
      });
    }
    widgets.push({
      type: 'macro_grid',
      props: {
        columns: String(screen.gridColumns),
        _allScreens: JSON.stringify(nonHomeScreens.map(s => ({ id: s.id, title: s.title, icon: s.screenIcon }))),
      },
    });
  }
  if (screen.templateId) {
    widgets.push(...resolveTemplateWidgets(screen));
  }
  return widgets;
}
