import type { RecipeConfig } from './types';
import { defaultIntroPage } from './types';
import { createScreen } from './screenTemplates';

export const DOKTOR_POKOK_RECIPE: RecipeConfig = {
  recipeName: 'Doktor Pokok',
  recipeDescription: 'AI crop diagnosis — snap a photo of your plant, get instant disease identification and treatment',
  recipeIcon: '\u{1F33F}',
  systemPrompt: `You are Doktor Pokok (Plant Doctor), an AI agricultural assistant for smallholder farmers in Southeast Asia. You help diagnose plant diseases from photos and descriptions, assess soil conditions, recommend fertilizers, and provide crop management advice.

Always:
- Be concise — farmers need quick, actionable answers
- When analyzing a plant photo, identify the most likely disease first, then give treatment steps
- Recommend locally available treatments and pesticides
- Consider tropical climate and Southeast Asian crop varieties
- Use bullet points and bold for key information

Never:
- Recommend banned or restricted pesticides
- Ignore signs of serious crop disease that could spread
- Give advice outside agriculture`,
  blockedKeywords: 'harmful',
  disclaimer: 'Alat ini membantu diagnosis tanaman tetapi tidak menggantikan nasihat pakar pertanian profesional.',
  category: 'Agriculture',
  selectedLanguages: ['ms', 'en'],
  selectedTheme: 'forest',
  customPrimary: '#2D7D46',
  customSecondary: '#F0F7F2',
  screens: [
    {
      id: 'home', title: 'Doktor Pokok', isHome: true, gridColumns: 2,
      templateId: null,
      fieldValues: {},
      disabledWidgets: [],
    },
    {
      id: 'diagnose', title: 'Diagnos Penyakit', isHome: false, gridColumns: 2, screenIcon: '\u{1F4F8}',
      templateId: 'camera_analysis',
      fieldValues: {
        camera_label: 'Ambil Gambar Tanaman',
        button_label: 'Diagnos',
        ai_instruction: 'vision_ask:You are a plant pathologist. Analyze this photo of a crop/plant. Identify: 1) The plant species if visible, 2) The disease or pest affecting it, 3) Severity (mild/moderate/severe), 4) Recommended treatment using locally available products in Southeast Asia, 5) Prevention measures. If the image is unclear, ask for a better photo. Additional context from farmer: {{user_text}}',
      },
      disabledWidgets: [],
      description: 'SCREEN: Disease Diagnosis | FUNCTION: Camera + voice + text input for plant disease identification with vision AI | INPUTS: photo_path, user_text | TRIGGERS: diagnose, penyakit, sakit, daun kuning, bintik, rosak, layu, kulat',
      prefillHints: { keterangan_tanaman: 'user_text' },
    },
    {
      id: 'soil', title: 'Semakan Tanah', isHome: false, gridColumns: 2, screenIcon: '\u{1F9EA}',
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'chat', heading: 'Penilaian Tanah & Baja',
        hint: 'Nyatakan jenis tanah, pH, dan tanaman anda...',
        q1: 'Tanah liat pH 5 untuk padi — apa baja?',
        q2: 'Tanah berpasir untuk sayur — tips?',
        q3: 'Bagaimana nak betulkan pH tanah?',
        q4: 'Baja organik vs kimia?',
        button_label: 'Analisis Tanah',
        ai_instruction: 'ask:You are a soil scientist advising a Southeast Asian farmer. Provide: 1) Soil health assessment, 2) pH correction if needed, 3) Fertilizer recommendation (NPK ratio and local brands), 4) Organic amendment suggestions, 5) Planting readiness verdict. Question: {{user_text}}',
      },
      disabledWidgets: [],
      description: 'SCREEN: Soil Check | FUNCTION: Soil & fertilizer assessment with pH slider, soil type, crop type inputs | TRIGGERS: tanah, baja, pH, saliran, banjir, nutrient',
      prefillHints: {},
    },
    {
      id: 'crop_guide', title: 'Panduan Tanaman', isHome: false, gridColumns: 2, screenIcon: '\u{1F331}',
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'chat', heading: 'Panduan Tanaman',
        hint: 'Soalan tentang penanaman, tuaian, racun perosak...',
        q1: 'Bila masa terbaik tanam cili?',
        q2: 'Macam mana kawal ulat sayur?',
        q3: 'Berapa lama durian berbuah?',
        q4: 'Tips tanam padi organik?',
        button_label: 'Dapatkan Nasihat',
        ai_instruction: 'ask:You are a crop management advisor for Southeast Asian smallholders. Provide advice including local varieties, tropical climate considerations, planting calendar for Malaysia, common pests and organic solutions. Question: {{user_text}}',
      },
      disabledWidgets: [],
      description: 'SCREEN: Crop Guide | FUNCTION: Crop-specific advice with dropdown for crop selection | TRIGGERS: tanaman, panduan, tanam, tuai, racun, perosak, musim, benih',
      prefillHints: {},
    },
    {
      id: 'shops', title: 'Kedai Agro Terdekat', isHome: false, gridColumns: 2, screenIcon: '\u{1F3EA}',
      ...createScreen('nearby_places', {
        heading: 'Kedai Agro Terdekat',
      }),
      description: 'SCREEN: Nearby Agro Shops | FUNCTION: Find agricultural supply shops sorted by GPS distance, with per-shop call buttons and ministry hotline SMS | TRIGGERS: kedai, beli, baja, racun, bekalan, terdekat, lokasi',
      prefillHints: {},
    },
  ],
  knowledgeSummary: `Doktor Pokok adalah alat diagnosis tanaman AI untuk petani kecil di Asia Tenggara.
Tanaman utama Malaysia: Padi, Sawit, Getah, Sayur-sayuran, Buah-buahan (Durian, Rambutan), Jagung, Ubi Kayu.
Penyakit biasa: Blas (padi), Ganoderma (sawit), Cladosporium (getah), Antraknos (cili/buah), Hawar daun, Bintik daun.
Rawatan tersedia: Trichoderma, Mancozeb, Copper fungicide, Bt spray, Neem oil, baja NPK.
MARDI dan Jabatan Pertanian adalah badan rujukan utama.
Musim penanaman Malaysia: 2 musim utama (musim hujan Okt-Feb, musim kering Mac-Sep).
Jabatan Pertanian hotline: +60197654321.
Kedai agro di Kelantan: Kota Bharu, Bachok, Tumpat, Pasir Mas, Tanah Merah.`,
  maxClarifications: 2,
  fallbackScreen: 'crop_guide',
  introPage: {
    ...defaultIntroPage('Alat ini membantu diagnosis tanaman tetapi tidak menggantikan nasihat pakar pertanian profesional. Sentiasa rujuk pegawai pertanian untuk kes kritikal.'),
    enabled: true,
    authorName: 'Jabatan Pertanian Malaysia (Dept. of Agriculture)',
    authorOrg: 'Jabatan Pertanian Malaysia (Dept. of Agriculture)',
    authorVerified: true,
    acceptLabel: 'Saya Faham — Mula',
    links: [
      { label: 'Portal Jabatan Pertanian', url: 'https://www.doa.gov.my' },
      { label: 'Panduan Tanaman MARDI', url: 'https://www.mardi.gov.my' },
    ],
  },
};
