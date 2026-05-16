import { initializeApp } from 'firebase/app';
import { getFirestore, doc, updateDoc, Timestamp } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: 'AIzaSyCKMwZ0iFo9Mxt9iIX497ZanQEUrxGxsT0',
  authDomain: 'bina-ai-a2d96.firebaseapp.com',
  projectId: 'bina-ai-a2d96',
  storageBucket: 'bina-ai-a2d96.firebasestorage.app',
  messagingSenderId: '260186437518',
  appId: '1:260186437518:web:ed891835707efbdfce16df',
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

// Bidan Pintar — single-screen ask_ai (no grid, just chat)
const bidanYaml = `id: bidan_pintar
name: "Bidan Pintar"
description: "Rural midwife assistant. Prenatal care guidance and emergency protocols, fully offline."
icon: "👩‍⚕️"
version: "1.0.0"
category: Health

cover_image: ""
featured: true
emergency: false
dialect: "Bahasa Indonesia"
tags:
  - prenatal
  - maternal-health
  - rural
  - emergency-protocols

author:
  name: Ministry of Health Indonesia
  organisation: Public Health Outreach
  verified: true

model:
  model_id: gemma-4-e2b-it
  backend: cpu
  system_prompt: |
    You are Bidan Pintar, an assistant for community midwives serving rural
    villages. Provide clear, practical, evidence-based guidance on prenatal
    visits, danger signs, and emergency referrals.
    Always use simple language. Never substitute for a doctor.
    If symptoms suggest emergency (heavy bleeding, severe headache,
    convulsions, no fetal movement), immediately advise referral to the
    nearest puskesmas or hospital.
  sampler:
    temperature: 0.25
    top_k: 40
    top_p: 0.9
    max_tokens: 512

theme:
  primary: "#BE185D"
  secondary: "#F472B6"
  text_size: standard

localisation:
  supported:
    - id
    - en
    - ms
  default: id

variables:
  user_text:    { type: string, default: "" }
  ai_response:  { type: string, default: "" }

screens:
  - id: home
    title: "Bidan Pintar"
    body:
      - text_label:
          text: "Apa yang ingin Anda tanyakan?"
          style: subheading

      - text_input:
          bind: user_text
          hint: "Tulis pertanyaan Anda..."

      - action_button:
          label: "Tanya Bidan Pintar"
          action: "ask:{{user_text}}"
          style: primary

      - markdown_output:
          source: ai_response
          empty_text: "Jawaban akan muncul di sini..."
          streaming: true

safety:
  blocked_keywords:
    - obat aborsi
    - induksi tanpa bidan
  escalation_message: "Mohon segera rujuk ke puskesmas atau bidan setempat. Jangan tangani sendiri."
  disclaimer: "Panduan dari AI. Bukan pengganti pemeriksaan medis langsung."

permissions: []

setup:
  intro_page:
    accept_label: "Saya Mengerti"
    disclaimer: "Bidan Pintar memberikan panduan berbasis AI untuk tujuan edukasi. Bukan pengganti konsultasi medis langsung. Selalu periksa ke bidan atau dokter terdekat."
    cover_photo: false
    author:
      name: Ministry of Health Indonesia
      organisation: Public Health Outreach
      verified: true

triage:
  home_mode: "chat"
  max_clarifications: 2
  fallback: "show_all"
`;

// Farm Buddy — grid home with 3 sub-screens
const farmYaml = `id: farm_buddy
name: "Farm Buddy"
description: "Diagnose crops, calculate profit, find nearby agro shops."
icon: "🌾"
version: "1.0.0"
category: Agriculture

cover_image: ""
featured: true
emergency: false
dialect: "Bahasa Malaysia"
tags:
  - farming
  - crops
  - profit
  - southeast-asia

author:
  name: Universiti Putra Malaysia
  organisation: AgriTech Faculty
  verified: true

model:
  model_id: gemma-4-e2b-it
  backend: cpu
  system_prompt: |
    You are Farm Buddy (Pakar Tani), an agricultural education assistant
    for Southeast Asian smallholder farmers.
    Give simple, practical, safe guidance.
    Do not claim certainty in diagnoses.
    Ask clarifying questions about crop type, symptoms, weather.
    If the issue seems severe, advise contacting a local agriculture officer.
    Never recommend restricted or dangerous chemicals.
  sampler:
    temperature: 0.3
    top_k: 40
    top_p: 0.95
    max_tokens: 512

theme:
  primary: "#2E7D32"
  secondary: "#A5D6A7"
  text_size: standard

localisation:
  supported:
    - ms
    - en
    - id
  default: ms

variables:
  user_text:    { type: string, default: "" }
  photo_path:   { type: string, default: "" }
  ai_response:  { type: string, default: "" }
  calc_a:       { type: number, default: "0" }
  calc_b:       { type: number, default: "0" }
  calc_rate:    { type: number, default: "5" }
  calc_result:  { type: number, default: "0" }

screens:
  - id: home
    title: "Farm Buddy"
    body:
      - text_label:
          text: "What would you like to do?"
          style: subheading

      - macro_grid:
          columns: 2
          buttons:
            - { label: "Leaf Diagnosis", action: "go:diagnose", icon: "🔬" }
            - { label: "Profit Calculator", action: "go:profit", icon: "💰" }
            - { label: "Nearest Agro Shop", action: "go:nearby", icon: "📍" }
            - { label: "Ask Farm Buddy", action: "go:ask", icon: "💬" }

  - id: diagnose
    title: "Leaf Diagnosis"
    body:
      - text_label:
          text: "Take a photo of the affected leaf"
          style: subheading

      - camera_input:
          bind: photo_path
          label: "Take Photo"
          preview: true

      - text_input:
          bind: user_text
          hint: "Describe symptoms (optional)"

      - action_button:
          label: "Diagnose"
          action: "vision_ask:Diagnose this crop leaf. Describe the disease, likely cause, and treatment. {{user_text}}"
          style: primary

      - markdown_output:
          source: ai_response
          streaming: true

  - id: profit
    title: "Profit Calculator"
    body:
      - text_label:
          text: "Calculate your farming profit"
          style: subheading

      - text_input:
          bind: calc_a
          hint: "e.g., 5000"
          label: "Total Revenue (RM)"
          input_type: number

      - text_input:
          bind: calc_b
          hint: "e.g., 2000"
          label: "Total Costs (RM)"
          input_type: number

      - slider:
          bind: calc_rate
          min: 0
          max: 30
          step: 1
          label: "Estimated Tax"
          left_label: "0%"
          right_label: "30%"

      - action_button:
          label: "Calculate"
          action: "formula:calc"
          style: primary

      - metric_card:
          source: calc_result
          label: "Net Profit"
          prefix: "RM "
          format: decimal_2

      - action_button:
          label: "Get AI advice on my profit"
          action: "ask:I made RM{{calc_a}} with RM{{calc_b}} costs and {{calc_rate}}% tax. Net: RM{{calc_result}}. Advice?"
          style: secondary

      - markdown_output:
          source: ai_response
          streaming: true

  - id: nearby
    title: "Nearest Agro Shop"
    body:
      - text_label:
          text: "Find fertiliser and supply shops near you"
          style: subheading

      - action_button:
          label: "Get My Location"
          action: "geolocate"
          style: primary

      - geo_display:
          data: agro_shops
          limit: 5
          show_distance: true
          empty_text: "Tap above to find nearby shops"

  - id: ask
    title: "Ask Farm Buddy"
    body:
      - text_label:
          text: "Ask anything about farming"
          style: subheading

      - text_input:
          bind: user_text
          hint: "Ask about crops, pests, soil..."

      - action_button:
          label: "Ask"
          action: "ask:{{user_text}}"
          style: primary

      - markdown_output:
          source: ai_response
          empty_text: "Responses will appear here..."
          streaming: true

formulas:
  calc:
    expression: "({{calc_a}} - {{calc_b}}) * (1 - {{calc_rate}} / 100)"
    output: calc_result

data:
  agro_shops:
    type: points
    items:
      - { name: "Kedai Baja Pak Ali", lat: 3.139, lng: 101.687, info: "Open 8am-5pm" }
      - { name: "AgriMart Seremban", lat: 2.725, lng: 101.938, info: "Fertiliser specialist" }
      - { name: "Tani Supply Melaka", lat: 2.189, lng: 102.250, info: "Seeds and tools" }
      - { name: "Koperasi Tani Johor", lat: 1.485, lng: 103.761, info: "Wholesale pricing" }

safety:
  blocked_keywords:
    - mix pesticide
    - poison
    - kill pest with fuel
  escalation_message: "This may be dangerous. Please contact your local agriculture officer."
  disclaimer: "AI-generated guidance. Not a professional consultation."

permissions:
  - camera
  - location

setup:
  intro_page:
    accept_label: "I Understand & Accept"
    disclaimer: "Farm Buddy provides AI-generated agricultural guidance for educational purposes only. It does not replace professional agronomist advice. Always verify recommendations with your local agriculture department before applying treatments."
    cover_photo: false
    author:
      name: Universiti Putra Malaysia
      organisation: AgriTech Faculty
      verified: true
    links:
      - { label: "UPM AgriTech Website", url: "https://agriculture.upm.edu.my" }
      - { label: "Malaysian DOA Guidelines", url: "https://www.doa.gov.my" }

knowledge:
  always_loaded: |
    Common Southeast Asian crop diseases:
    - Rice blast (Pyricularia oryzae): grey-green spots on leaves, spread in humid conditions. Treat with tricyclazole fungicide.
    - Bacterial leaf blight (Xanthomonas oryzae): yellow-white lesions along leaf veins. Use resistant varieties, avoid excess nitrogen.
    - Tungro virus: yellow-orange discolouration from base. Control green leafhopper vector. No direct cure.
    - Brown planthopper: honeydew deposits, hopper burn. Use resistant varieties, avoid excessive insecticides.
    Common crops: rice (padi), oil palm (kelapa sawit), rubber (getah), cocoa, durian, pepper.
  chunks: 0

screen_catalog:
  - id: diagnose
    title: "Leaf Diagnosis"
    template: camera_analysis
    icon: "🔬"
    description: "Take a photo of a diseased crop leaf to get an AI-powered diagnosis with treatment recommendations."
    accepted_inputs: [photo, text]
  - id: profit
    title: "Profit Calculator"
    template: calculator
    icon: "💰"
    description: "Calculate farming profit after costs and tax. Get AI advice on improving margins."
    accepted_inputs: [number]
  - id: nearby
    title: "Nearest Agro Shop"
    template: nearby_places
    icon: "📍"
    description: "Find fertiliser and agricultural supply shops near your location."
    accepted_inputs: [location]
  - id: ask
    title: "Ask Farm Buddy"
    template: ask_ai
    icon: "💬"
    description: "Ask any farming question and get AI-powered advice."
    accepted_inputs: [text]

triage:
  home_mode: "grid"
  max_clarifications: 2
  fallback: "show_all"
`;

// Buku Kira-Kira — grid home with profit sub-screen + ask sub-screen
const bukuYaml = `id: buku_kira_kira
name: "Buku Kira-Kira"
description: "Smart bookkeeping for sari-sari stores. Track sales, costs, and daily profit."
icon: "📒"
version: "1.0.0"
category: Business

cover_image: ""
featured: false
emergency: false
dialect: "Tagalog"
tags:
  - bookkeeping
  - sari-sari
  - small-business
  - philippines

author:
  name: Maria Lopez
  organisation: Community Author
  verified: false

model:
  model_id: gemma-4-e2b-it
  backend: cpu
  system_prompt: |
    You are Buku Kira-Kira, a friendly bookkeeping helper for sari-sari
    store owners and small Filipino businesses. Use simple Tagalog or
    Taglish. Help users understand cash flow, margins, and inventory.
    Never give regulated tax advice; suggest consulting a BIR officer
    for filing questions.
  sampler:
    temperature: 0.3
    top_k: 40
    top_p: 0.95
    max_tokens: 384

theme:
  primary: "#0EA5E9"
  secondary: "#34D399"
  text_size: standard

localisation:
  supported:
    - tl
    - en
  default: tl

variables:
  user_text:    { type: string, default: "" }
  ai_response:  { type: string, default: "" }
  calc_a:       { type: number, default: "0" }
  calc_b:       { type: number, default: "0" }
  calc_result:  { type: number, default: "0" }

screens:
  - id: home
    title: "Buku Kira-Kira"
    body:
      - text_label:
          text: "Anong gusto mong gawin ngayon?"
          style: subheading

      - macro_grid:
          columns: 2
          buttons:
            - { label: "Daily Profit", action: "go:profit", icon: "💰" }
            - { label: "Ask Buku", action: "go:ask", icon: "💬" }

  - id: profit
    title: "Daily Profit"
    body:
      - text_label:
          text: "Ilagay ang sales at gastos para sa araw na ito"
          style: subheading

      - text_input:
          bind: calc_a
          hint: "hal. 1500"
          label: "Total Sales (PHP)"
          input_type: number

      - text_input:
          bind: calc_b
          hint: "hal. 900"
          label: "Total Gastos (PHP)"
          input_type: number

      - action_button:
          label: "Compute Profit"
          action: "formula:calc"
          style: primary

      - metric_card:
          source: calc_result
          label: "Net Profit"
          prefix: "PHP "
          format: decimal_2

      - action_button:
          label: "Tanungin si Buku tungkol sa profit"
          action: "ask:Kumita ako ng PHP{{calc_a}} at gumastos ng PHP{{calc_b}}. Net ko ay PHP{{calc_result}}. Ano payo mo?"
          style: secondary

      - markdown_output:
          source: ai_response
          streaming: true

  - id: ask
    title: "Ask Buku"
    body:
      - text_label:
          text: "Magtanong ng kahit ano tungkol sa negosyo"
          style: subheading

      - text_input:
          bind: user_text
          hint: "Magtanong sa Buku Kira-Kira..."

      - action_button:
          label: "Tanungin"
          action: "ask:{{user_text}}"
          style: primary

      - markdown_output:
          source: ai_response
          empty_text: "Lalabas dito ang sagot..."
          streaming: true

formulas:
  calc:
    expression: "{{calc_a}} - {{calc_b}}"
    output: calc_result

safety:
  blocked_keywords: []
  escalation_message: ""
  disclaimer: "AI advice lang ito. Para sa BIR/tax, kumonsulta sa propesyonal."

permissions: []

setup:
  intro_page:
    accept_label: "Naiintindihan Ko"
    disclaimer: "Ang Buku Kira-Kira ay nagbibigay ng AI-generated na payo para sa edukasyon lamang. Hindi ito kapalit ng propesyonal na accountant."

screen_catalog:
  - id: profit
    title: "Daily Profit"
    template: calculator
    icon: "💰"
    description: "Calculate your daily profit from sales and expenses."
    accepted_inputs: [number]
  - id: ask
    title: "Ask Buku"
    template: ask_ai
    icon: "💬"
    description: "Ask any business or bookkeeping question."
    accepted_inputs: [text]

triage:
  home_mode: "grid"
  max_clarifications: 2
  fallback: "show_all"
`;

const updates = [
  { id: 'WLOzoXBJAxPt7nWKy0qc', name: 'Bidan Pintar', yaml: bidanYaml },
  { id: 'Hk4FUtclBQodWy193cMA', name: 'Farm Buddy', yaml: farmYaml },
  { id: 'csbLvXpMdRMmBzWu9g26', name: 'Buku Kira-Kira', yaml: bukuYaml },
];

async function update() {
  for (const { id, name, yaml } of updates) {
    await updateDoc(doc(db, 'recipes', id), {
      generatedYaml: yaml,
      updatedAt: Timestamp.now(),
    });
    console.log(`Updated "${name}" (${id})`);
  }
  console.log('Done.');
  process.exit(0);
}

update().catch(e => { console.error(e); process.exit(1); });
