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
      id: 'diagnose', title: 'Leaf Diagnosis', isHome: false, gridColumns: 2,
      ...createScreen('camera_analysis', {
        camera_label: 'Take Photo',
        button_label: 'Diagnose',
        ai_instruction: 'vision_ask:Diagnose this crop leaf. Describe the disease, likely cause, and treatment. {{user_text}}',
      }),
    },
    {
      id: 'profit', title: 'Profit Calculator', isHome: false, gridColumns: 2,
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
    },
    {
      id: 'nearby', title: 'Nearest Agro Shop', isHome: false, gridColumns: 2,
      ...createScreen('nearby_places', {
        heading: 'Find fertiliser and supply shops near you',
      }),
    },
  ],
  knowledgeSummary: '',
};
