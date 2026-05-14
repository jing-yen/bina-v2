import type { RecipeConfig } from './types';
import { createScreen } from './screenTemplates';

export const FARM_BUDDY_RECIPE: RecipeConfig = {
  recipeName: 'Farm Buddy',
  recipeDescription: 'Diagnose crops, calculate profit, find nearby agro shops.',
  recipeIcon: '\u{1F33E}',
  systemPrompt: `You are Farm Buddy (Pakar Tani), an agricultural education assistant
for Southeast Asian smallholder farmers.
Give simple, practical, safe guidance.
Do not claim certainty in diagnoses.
Ask clarifying questions about crop type, symptoms, weather.
If the issue seems severe, advise contacting a local agriculture officer.
Never recommend restricted or dangerous chemicals.`,
  blockedKeywords: 'mix pesticide, poison, kill pest with fuel, dangerous spray, drink chemical',
  disclaimer: 'AI-generated guidance. Not a professional consultation.',
  category: 'Agriculture',
  selectedLanguages: ['en', 'ms'],
  selectedTheme: 'forest',
  customPrimary: '#6B21A8',
  customSecondary: '#E9D5FF',
  screens: [
    {
      id: 'home', title: '', isHome: true, gridColumns: 2,
      ...createScreen('ask_ai', {
        heading: 'Ask Farm Buddy',
        hint: 'Or ask anything...',
        button_label: 'Ask Farm Buddy',
        ai_instruction: 'ask:{{user_text}}',
      }),
    },
    {
      id: 'diagnose', title: 'Leaf Diagnosis', isHome: false, gridColumns: 2, screenIcon: '\u{1F331}',
      ...createScreen('camera_analysis', {
        camera_label: 'Take Photo',
        button_label: 'Diagnose',
        ai_instruction: 'vision_ask:Diagnose this crop leaf. Describe the disease, likely cause, and treatment. {{user_text}}',
      }),
      description: 'SCREEN: Leaf Diagnosis | FUNCTION: Take a photo for AI analysis | INPUTS: photo_path, user_text | TRIGGERS: leaf problem, crop disease, yellow leaves, wilting, spots on leaf, pest damage',
      prefillHints: { crop_name: 'user_text' },
    },
    {
      id: 'profit', title: 'Profit Calculator', isHome: false, gridColumns: 2, screenIcon: '\u{1F4B0}',
      ...createScreen('calculator', {
        field_a_label: 'Total Revenue (RM)',
        field_a_hint: 'e.g. 5000',
        field_b_label: 'Total Costs (RM)',
        field_b_hint: 'e.g. 2000',
        slider_label: 'Estimated Tax',
        slider_min: '0',
        slider_max: '30',
        formula_template: 'Profit: (A−B)×(1−Rate%)',
        result_label: 'Net Profit',
        result_prefix: 'RM ',
      }),
      description: 'SCREEN: Profit Calculator | FUNCTION: Calculate profit from revenue and costs | INPUTS: calc_a, calc_b, calc_rate | TRIGGERS: how much profit, revenue minus cost, calculate earnings, untung berapa',
      prefillHints: { total_revenue: 'calc_a', total_costs: 'calc_b', tax_rate: 'calc_rate' },
    },
    {
      id: 'nearby', title: 'Nearest Agro Shop', isHome: false, gridColumns: 2, screenIcon: '\u{1F3EA}',
      ...createScreen('nearby_places', {
        heading: 'Find fertiliser and supply shops near you',
      }),
      description: 'SCREEN: Nearest Agro Shop | FUNCTION: Find nearby places on a map | TRIGGERS: where to buy fertiliser, nearest shop, find supplier, kedai baja',
      prefillHints: {},
    },
  ],
  knowledgeSummary: '',
  maxClarifications: 2,
};
