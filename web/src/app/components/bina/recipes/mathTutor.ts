import type { RecipeConfig } from './types';
import { defaultIntroPage } from './types';

export const MATH_TUTOR_RECIPE: RecipeConfig = {
  recipeName: 'Math Tutor',
  recipeDescription: 'Step-by-step math help for primary & secondary students',
  recipeIcon: '\u{1F4DA}',
  systemPrompt: `You are a patient, encouraging math tutor for students aged 8-16. Explain step by step, using simple language. When solving problems, show each step clearly. Use analogies students can relate to (money, food, games). Celebrate correct answers. If a student is stuck, give hints before the full answer. Never just give the answer — always teach the method.`,
  blockedKeywords: '',
  disclaimer: 'Educational tool to supplement learning. Not a replacement for classroom teaching.',
  category: 'Education',
  selectedLanguages: ['en', 'ms', 'id', 'ta', 'zh'],
  selectedTheme: 'amber',
  customPrimary: '#D97706',
  customSecondary: '#FDE68A',
  screens: [
    {
      id: 'home', title: '', isHome: true, gridColumns: 2,
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'chat', heading: 'What math do you need help with?',
        hint: 'Type your math question...',
        q1: 'How do I add fractions?',
        q2: 'Explain long division',
        q3: 'What is the area of a circle?',
        q4: '',
        button_label: 'Help Me', ai_instruction: 'ask:{{user_text}}',
      },
      disabledWidgets: [],
    },
    {
      id: 'practice', title: 'Practice Calculator', isHome: false, gridColumns: 2,
      templateId: 'calculator',
      fieldValues: {
        field_a_label: 'Number A', field_a_hint: 'First number',
        field_b_label: 'Number B', field_b_hint: 'Second number',
        field_c_label: '', field_c_hint: '', field_d_label: '', field_d_hint: '',
        slider_label: 'Rate %', slider_min: '0', slider_max: '100',
        formula_template: 'A × B', custom_formula: '',
        result_label: 'Answer', result_prefix: '', result_suffix: '',
      },
      disabledWidgets: ['input_c', 'input_d', 'slider'],
    },
    {
      id: 'explainer', title: 'Photo Solver', isHome: false, gridColumns: 2,
      templateId: 'camera_analysis',
      fieldValues: {
        camera_label: 'Take Photo of Problem',
        button_label: 'Solve Step by Step',
        ai_instruction: 'vision_ask:Look at this math problem. Solve it step by step, explaining each step clearly for a student. {{user_text}}',
      },
      disabledWidgets: [],
    },
  ],
  knowledgeSummary: '',
  introPage: {
    ...defaultIntroPage('Educational tool to supplement learning. Not a replacement for classroom teaching.'),
    enabled: true,
    authorName: 'Bina Education',
    authorOrg: 'Bina.ai',
    authorVerified: true,
    links: [],
  },
};
