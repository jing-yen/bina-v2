import type { RecipeConfig } from './types';
import { defaultIntroPage } from './types';
import { createScreen } from './screenTemplates';

export const PAKAR_TANI_RECIPE: RecipeConfig = {
  recipeName: 'Pakar Sawit',
  recipeDescription: 'Diagnosis penyakit sawit, rawatan & pesanan bekalan — tanpa internet.',
  recipeIcon: '\u{1F334}',
  systemPrompt: `Anda adalah Pakar Sawit, pakar diagnosis penyakit kelapa sawit untuk pekebun kecil di Malaysia.

Keupayaan anda:
1. Kenal pasti penyakit sawit dari gambar daun, batang, dan buah
2. Tentukan tahap keterukan (Ringan / Sederhana / Teruk)
3. Syorkan rawatan khusus menggunakan produk yang tersedia di kedai agro tempatan
4. Panduan langkah demi langkah rawatan dalam bahasa mudah

Penyakit utama sawit Malaysia:
- Ganoderma (Reput Pangkal Batang): cendawan putih di pangkal, daun layu, buah tidak masak
- Reput Pucuk (Bud Rot): pucuk muda reput, bau busuk, daun baru tidak membuka
- Bintik Daun (Leaf Spot): bintik coklat/hitam pada daun, biasa masa musim hujan
- Kerosakan Kumbang Badak (Rhinoceros Beetle): lubang pada pucuk, serat daun koyak berbentuk V
- Ulat Bungkus (Bagworm): daun dimakan, tinggal tulang daun sahaja
- Tikus (Rat Damage): kesan gigitan pada buah dan batang muda
- Kekurangan Nutrient: daun kuning (N), hujung daun kering (K), daun kecil (Mg)

Peraturan:
- Sentiasa balas dalam Bahasa Melayu (boleh guna dialek Sabah/Sarawak jika diminta)
- Rujuk produk yang diluluskan MARDI dan MPOB
- Jika tidak pasti, nasihatkan hubungi pegawai pertanian daerah
- Jangan cadangkan racun terlarang atau kaedah berbahaya
- Sertakan nama produk sebenar (Trichoderma, Gano-Care, Metarhizium, Bt)`,
  blockedKeywords: 'racun terlarang, paraquat, endosulfan, DDT, bakar ladang',
  disclaimer: 'Panduan pertanian AI berdasarkan penyelidikan MARDI. Rujuk pegawai pertanian untuk pengesahan.',
  category: 'Agriculture',
  selectedLanguages: ['ms', 'en', 'id'],
  selectedTheme: 'forest',
  customPrimary: '#15803D',
  customSecondary: '#BBF7D0',
  screens: [
    {
      id: 'home', title: 'Pakar Sawit', isHome: true, gridColumns: 2,
      templateId: null,
      fieldValues: {},
      disabledWidgets: [],
    },
    {
      id: 'diagnosis_daun', title: 'Diagnosis Daun', isHome: false, gridColumns: 2, screenIcon: '\u{1F50D}',
      templateId: 'camera_analysis',
      fieldValues: {
        camera_label: 'Ambil Gambar Daun/Batang',
        button_label: 'Diagnosis',
        ai_instruction: 'vision_ask:Analisis gambar sawit ini. {{user_text}}\n\n1. PENYAKIT: Kenal pasti penyakit (Ganoderma/Reput Pucuk/Bintik Daun/Kumbang Badak/Ulat Bungkus/Kekurangan Nutrient/Lain-lain)\n2. KETERUKAN: Ringan / Sederhana / Teruk\n3. RAWATAN SEGERA: Langkah pertama yang perlu diambil sekarang\n4. PRODUK DIPERLUKAN: Nama produk + anggaran kuantiti + anggaran harga\n5. PENCEGAHAN: Langkah elak jangkitan merebak\n\nAkhir jawapan, nyatakan:\n- "Untuk rawatan langkah demi langkah, pergi ke skrin Rawatan Ganoderma"\n- "Untuk beli bekalan, pergi ke skrin Pesan Bekalan"\n\nJika gambar tidak jelas atau bukan sawit, nyatakan dan minta gambar lebih dekat.',
      },
      disabledWidgets: [],
      description: 'SCREEN: Diagnosis Daun | FUNCTION: Camera-based palm oil disease identification with vision AI and treatment recommendations | INPUTS: photo_path, user_text | TRIGGERS: penyakit, daun kuning, daun layu, cendawan, lubang, reput, kumbang, ulat, bintik, rosak, sakit pokok',
      prefillHints: { keterangan_pokok: 'user_text' },
    },
    {
      id: 'senarai_rawatan', title: 'Rawatan Ganoderma', isHome: false, gridColumns: 2, screenIcon: '\u{2705}',
      ...createScreen('checklist', {
        steps: 'Tandakan pokok berpenyakit dengan reben merah|text\nGali dan buang bahagian pangkal yang dijangkiti|text\nSapukan Trichoderma harzianum pada kawasan potong|text\nTabur 250g baja Gano-Care A di sekeliling pangkal (radius 1m)|text\nPasang penghadang tanah (soil mounding) 60cm tinggi|text\nRekod tarikh, lokasi pokok, dan rawatan yang diberi|text\nSemak semula dalam 2 minggu — cari cendawan baru|text\nJika tiada perubahan selepas 4 minggu, hubungi pegawai MPOB daerah|text',
        completion_action: 'none',
      }),
      description: 'SCREEN: Rawatan Ganoderma | FUNCTION: Step-by-step Ganoderma treatment checklist with product application guide | TRIGGERS: rawatan, checklist, langkah, macam mana nak rawat, ganoderma, reput pangkal, cara rawat',
      prefillHints: {},
    },
    {
      id: 'kedai_agro', title: 'Kedai Agro Terdekat', isHome: false, gridColumns: 2, screenIcon: '\u{1F4CD}',
      ...createScreen('nearby_places', {
        heading: 'Kedai Agro & Koperasi Berhampiran',
      }),
      description: 'SCREEN: Kedai Agro Terdekat | FUNCTION: Find nearby agricultural supply shops sorted by distance | TRIGGERS: kedai, beli, baja, racun, agro, koperasi, bekalan, di mana',
      prefillHints: {},
    },
    {
      id: 'pesan_bekalan', title: 'Pesan Bekalan', isHome: false, gridColumns: 2, screenIcon: '\u{1F4E6}',
      ...createScreen('sms_dispatch', {
        heading: 'Pesan Bekalan Sawit',
        contact_type: 'sms',
        contacts: '\u{1F3EA} Koperasi Sawit Daerah | +60131234567\n\u{1F6D2} AgriMart Seremban | +60191234567\n\u{1F333} Tani Supply Melaka | +60171234567',
        sms_template: 'PESANAN BEKALAN SAWIT\nDari: Pekebun Bina\nLokasi ladang: https://maps.google.com/?q={{user_location}}\n\nDiagnosis AI: {{ai_response}}\n\nSila hubungi untuk pengesahan harga dan penghantaran.\nTerima kasih.',
      }),
      description: 'SCREEN: Pesan Bekalan | FUNCTION: SMS order to agricultural cooperative with diagnosis context and GPS location | TRIGGERS: pesan, beli, order, bekalan, baja, racun, koperasi, hantar, gano-care',
      prefillHints: {},
    },
    {
      id: 'tanya_pakar', title: 'Tanya Pakar', isHome: false, gridColumns: 2, screenIcon: '\u{1F4AC}',
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'chat', heading: 'Tanya Pakar Sawit',
        hint: 'Contoh: Berapa kerap perlu baja?',
        q1: 'Macam mana nak kawal kumbang badak?',
        q2: 'Apa tanda awal Ganoderma?',
        q3: 'Berapa kos rawatan satu pokok?',
        q4: 'Bila masa terbaik untuk membaja?',
        button_label: 'Tanya',
        ai_instruction: 'ask:Konteks diagnosis terkini pengguna:\n{{ai_response}}\n\nSoalan pengguna: {{user_text}}',
      },
      disabledWidgets: [],
      description: 'SCREEN: Tanya Pakar | FUNCTION: Agricultural Q&A with context from recent diagnosis | INPUTS: user_text | TRIGGERS: tanya, soalan, macam mana, apa, kenapa, berapa, nasihat, tips, baja, racun, harga, musim',
      prefillHints: { soalan: 'user_text' },
    },
  ],
  knowledgeSummary: `Pakar Sawit adalah alat diagnosis penyakit kelapa sawit untuk pekebun kecil di Malaysia.
Malaysia adalah pengeluar minyak sawit kedua terbesar dunia dengan ~5.7 juta hektar ladang.
Ganoderma boninense (reput pangkal batang) adalah penyakit paling serius — boleh bunuh pokok dalam 2-3 tahun jika tidak dirawat.
Rawatan Ganoderma: Trichoderma harzianum (agen biokawal), Gano-Care A (baja khas), soil mounding (penghadang tanah).
Kumbang badak (Oryctes rhinoceros): lubang pada pucuk, rawatan dengan Metarhizium anisopliae atau perangkap feromon.
Ulat bungkus (Metisa plana): rawatan dengan Bacillus thuringiensis (Bt) atau pelepasan parasitoid.
Kekurangan Nitrogen (N): daun kuning merata. Kekurangan Kalium (K): hujung daun kering. Kekurangan Magnesium (Mg): daun kecil, jalur kuning.
MARDI dan MPOB adalah badan penyelidikan pertanian utama Malaysia.
Musim pembajaan sawit: 2x setahun (Mac-April dan Sep-Okt).
Kos rawatan Ganoderma per pokok: RM 15-30 bergantung keterukan.
Harga produk: Trichoderma ~RM 25/kg, Gano-Care A ~RM 18/250g, Metarhizium ~RM 30/kg, Bt spray ~RM 45/liter.`,
  maxClarifications: 2,
  fallbackScreen: 'tanya_pakar',
  introPage: {
    ...defaultIntroPage('Panduan pertanian AI berdasarkan penyelidikan MARDI. Rujuk pegawai pertanian untuk pengesahan.'),
    enabled: true,
    authorName: 'MARDI',
    authorOrg: 'Institut Penyelidikan & Kemajuan Pertanian Malaysia',
    authorVerified: true,
    links: [
      { label: 'Portal MARDI', url: 'https://www.mardi.gov.my' },
      { label: 'Info Sawit MPOB', url: 'https://www.mpob.gov.my' },
    ],
  },
};
