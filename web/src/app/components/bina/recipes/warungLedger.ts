import type { RecipeConfig } from './types';
import { defaultIntroPage } from './types';
import { createScreen } from './screenTemplates';

export const WARUNG_LEDGER_RECIPE: RecipeConfig = {
  recipeName: 'Kira Mikro',
  recipeDescription: 'Kira jualan & hutang warung anda — ketik atau cakap, tak perlu internet.',
  recipeIcon: '\u{1F4B0}',
  systemPrompt: `Anda adalah Kira Mikro, pembantu kewangan warung untuk peniaga kecil di Asia Tenggara.

Peranan anda:
- Bantu rekod jualan harian (item, kuantiti, harga)
- Kira untung rugi dan jumlah jualan
- Baca dan digitalkan buku hutang tulisan tangan (Buku 555)
- Beri ringkasan kewangan mudah faham

Peraturan:
- Sentiasa balas dalam Bahasa Melayu
- Guna RM untuk mata wang
- Ringkas dan jelas — peniaga sibuk
- Format nombor dengan koma (contoh: RM 1,250.00)
- Jangan beri nasihat pelaburan atau pinjaman
- Jika diminta baca tulisan tangan, senaraikan setiap baris: Nama — Jumlah
- Semak matematik dan tunjuk jumlah keseluruhan`,
  blockedKeywords: 'pinjaman, loan, invest, saham, stock, gambling, judi',
  disclaimer: 'Alat bantuan kewangan warung. Bukan nasihat kewangan profesional.',
  category: 'Finance',
  selectedLanguages: ['ms', 'en', 'id'],
  selectedTheme: 'amber',
  customPrimary: '#D97706',
  customSecondary: '#FDE68A',
  screens: [
    {
      id: 'home', title: 'Kira Mikro', isHome: true, gridColumns: 2,
      templateId: null,
      fieldValues: {},
      disabledWidgets: [],
    },
    {
      id: 'rekod_jualan', title: 'Rekod Jualan', isHome: false, gridColumns: 2, screenIcon: '\u{1F4DD}',
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'form', heading: 'Rekod Jualan Hari Ini',
        form_field_count: '5',
        f1_label: 'Item', f1_type: 'dropdown', f1_options: 'Nasi Lemak, Karipap, Teh O, Teh Tarik, Roti Canai, Mee Goreng, Nasi Goreng, Air Sirap, Kuih Muih, Lain-lain',
        f2_label: 'Kuantiti', f2_type: 'number', f2_options: '',
        f3_label: 'Harga seunit (RM)', f3_type: 'number', f3_options: '',
        f4_label: 'Item lain / Nota', f4_type: 'text', f4_options: '',
        f5_label: 'Jumlah item sebelum ini (RM)', f5_type: 'number', f5_options: '',
        hint: '', q1: '', q2: '', q3: '', q4: '',
        button_label: 'Rekod',
        ai_instruction: 'ask:Rekod jualan ini:\nItem: {{form_f1}} {{form_f4}}\nKuantiti: {{form_f2}}\nHarga seunit: RM{{form_f3}}\nJumlah sebelum ini: RM{{form_f5}}\n\nKirakan:\n1. Jumlah item ini: kuantiti x harga\n2. Jumlah terkumpul: jumlah sebelum + jumlah item ini\n\nBalas dalam format:\n**Item:** [nama] x[kuantiti] = RM [jumlah]\n**Jumlah Terkumpul Hari Ini: RM [jumlah baru]**\n\nIngatkan pengguna masukkan jumlah terkumpul ini sebagai "Jumlah item sebelum ini" untuk rekod seterusnya.',
      },
      disabledWidgets: [],
      description: 'SCREEN: Rekod Jualan | FUNCTION: Form to record daily sales with item, quantity, price, and running total | INPUTS: form_f1, form_f2, form_f3, form_f4, form_f5 | TRIGGERS: rekod, jualan, jual, nasi lemak, karipap, teh, berapa jumlah, hari ini',
      prefillHints: { item: 'form_f1', kuantiti: 'form_f2', harga: 'form_f3', nota: 'form_f4', jumlah_sebelum: 'form_f5' },
    },
    {
      id: 'kira_untung', title: 'Kira Untung', isHome: false, gridColumns: 2, screenIcon: '\u{1F4B5}',
      ...createScreen('calculator', {
        field_a_label: 'Jumlah Jualan Hari Ini (RM)',
        field_a_hint: 'cth. 500 — dari Rekod Jualan',
        field_b_label: 'Jumlah Kos (RM)',
        field_b_hint: 'cth. 200 — bahan, gas, sewa',
        slider_label: 'Cukai SST (%)',
        slider_min: '0',
        slider_max: '15',
        formula_template: 'Profit: (A−B)×(1−Rate%)',
        result_label: 'Untung Bersih',
        result_prefix: 'RM ',
      }),
      description: 'SCREEN: Kira Untung | FUNCTION: Calculate net profit from revenue minus costs with SST tax slider | INPUTS: calc_a, calc_b, calc_rate | TRIGGERS: untung, profit, kira, berapa untung, jualan tolak kos, margin',
      prefillHints: { jumlah_jualan: 'calc_a', jumlah_kos: 'calc_b', cukai: 'calc_rate' },
    },
    {
      id: 'imbas_hutang', title: 'Imbas Hutang', isHome: false, gridColumns: 2, screenIcon: '\u{1F4F7}',
      ...createScreen('camera_analysis', {
        camera_label: 'Ambil Gambar Buku Hutang',
        button_label: 'Imbas & Baca',
        ai_instruction: 'vision_ask:Baca buku hutang tulisan tangan ini. Untuk setiap baris:\n1. Senaraikan: Nama — RM [jumlah]\n2. Jika ada tarikh, nyatakan\n3. Semak jumlah keseluruhan\n4. Beri jumlah besar: "JUMLAH HUTANG: RM ___"\n\nJika tulisan tidak jelas, nyatakan "tidak pasti" dan beri anggaran terbaik.',
      }),
      description: 'SCREEN: Imbas Hutang | FUNCTION: Camera scan of handwritten debt notebook (Buku 555) with AI OCR | INPUTS: photo_path, user_text | TRIGGERS: hutang, buku 555, siapa hutang, baca buku, imbas, scan hutang, orang tak bayar',
      prefillHints: { nama_pelanggan: 'user_text' },
    },
    {
      id: 'tanya_kira', title: 'Tanya Kira', isHome: false, gridColumns: 2, screenIcon: '\u{1F4AC}',
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'chat', heading: 'Tanya apa-apa tentang warung',
        hint: 'Contoh: Macam mana nak jimat kos?',
        q1: 'Macam mana nak kurangkan pembaziran?',
        q2: 'Apa item paling laris warung biasa?',
        q3: 'Berapa harga pasaran nasi lemak sekarang?',
        q4: 'Kena daftar SST ke kalau jual bawah RM500k?',
        button_label: 'Tanya',
        ai_instruction: 'ask:Konteks kewangan warung pengguna:\n- Jumlah Jualan: RM{{calc_a}}\n- Jumlah Kos: RM{{calc_b}}\n- Untung Bersih: RM{{calc_result}}\n- Item terakhir direkod: {{form_f1}}\n\nSoalan pengguna: {{user_text}}',
      },
      disabledWidgets: [],
      description: 'SCREEN: Tanya Kira | FUNCTION: Financial Q&A with context from calculator and sales records | INPUTS: user_text | TRIGGERS: tanya, soalan, macam mana, apa, kenapa, berapa, nasihat, tips',
      prefillHints: { soalan: 'user_text' },
    },
  ],
  knowledgeSummary: `Kira Mikro adalah alat POS (Point-of-Sale) mudah alih untuk peniaga kecil warung dan gerai di Malaysia.
Sasaran pengguna: Peniaga jalanan, warung nasi lemak, gerai kuih, kedai kopi — kebanyakan tanpa rekod digital.
Buku 555 adalah buku nota murah yang digunakan secara meluas di Malaysia untuk merekod hutang pelanggan secara tulisan tangan.
Cukai perkhidmatan Malaysia (SST) ialah 6-8% untuk F&B. Kebanyakan peniaga mikro tidak perlu daftar SST jika pendapatan bawah RM500,000/tahun.
Mata wang: Ringgit Malaysia (RM). Format: RM 1,250.00`,
  maxClarifications: 2,
  fallbackScreen: 'tanya_kira',
  introPage: {
    ...defaultIntroPage('Alat bantuan kewangan warung. Bukan nasihat kewangan profesional.'),
    enabled: true,
    authorName: 'Bina Finance',
    authorOrg: 'Bina',
    authorVerified: true,
    links: [],
  },
};
