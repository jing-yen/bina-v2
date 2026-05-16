import type { RecipeConfig } from './types';
import { defaultIntroPage } from './types';
import { createScreen } from './screenTemplates';

export const RURAL_TRIAGE_RECIPE: RecipeConfig = {
  recipeName: 'Triage Ibu Hamil',
  recipeDescription: 'Panduan kecemasan kehamilan untuk bidan kampung — berfungsi luar talian.',
  recipeIcon: '\u{1F3E5}',
  systemPrompt: `Anda adalah pembantu triage kecemasan kehamilan untuk bidan kampung di kawasan luar bandar Malaysia.

PERATURAN KETAT:
- JANGAN sesekali memberi diagnosis perubatan
- JANGAN cadangkan ubat atau dos
- HANYA kenal pasti gejala berisiko dan panduan penstabilan
- Sentiasa nasihatkan dapatkan bantuan profesional

Aliran kerja:
1. Terima gejala (mungkin dalam dialek tempatan)
2. Terjemah ke istilah perubatan formal
3. Tentukan tahap risiko:
   - HIJAU: Pantau di rumah, lawatan klinik biasa
   - KUNING: Perlu perhatian dalam 24 jam, hubungi klinik
   - MERAH: Kecemasan — penstabilan segera + hubungi 999
4. Jika MERAH, senaraikan langkah penstabilan

Gejala MERAH (kecemasan):
- Pre-eklampsia: bengkak muka/tangan + sakit kepala teruk + penglihatan kabur + tekanan darah tinggi
- Pendarahan: darah banyak dari faraj (lebih dari pad biasa)
- Eklampsia: sawan/kejang semasa mengandung
- Kelahiran pramatang: kontraksi sebelum 37 minggu
- Pecah ketuban pramatang
- Tali pusat terkeluar

Sentiasa balas dalam Bahasa Melayu.
Guna format ringkas dengan bullet points.
Mulakan jawapan dengan tahap risiko: [HIJAU], [KUNING], atau [MERAH].`,
  blockedKeywords: 'ubat, medication, prescribe, preskripsi, dos, dosage, paracetamol, aspirin, ubat sendiri, abaikan simptom',
  disclaimer: 'AMARAN: Ini BUKAN penggantian doktor. Untuk panduan kecemasan sahaja. Hubungi 999 untuk kecemasan sebenar.',
  category: 'Health',
  selectedLanguages: ['ms', 'en', 'id'],
  selectedTheme: 'coral',
  customPrimary: '#DC2626',
  customSecondary: '#FECACA',
  screens: [
    {
      id: 'home', title: 'Triage Ibu Hamil', isHome: true, gridColumns: 2,
      templateId: null,
      fieldValues: {},
      disabledWidgets: [],
    },
    {
      id: 'saringan_gejala', title: 'Saringan Gejala', isHome: false, gridColumns: 2, screenIcon: '\u{1FA7A}',
      templateId: 'ask_ai',
      fieldValues: {
        mode: 'form', heading: 'Saringan Gejala Kehamilan',
        form_field_count: '6',
        f1_label: 'Umur pesakit', f1_type: 'number', f1_options: '',
        f2_label: 'Minggu mengandung', f2_type: 'number', f2_options: '',
        f3_label: 'Gejala utama', f3_type: 'dropdown', f3_options: 'Bengkak muka/kaki, Sakit kepala teruk, Penglihatan kabur, Pendarahan, Kontraksi awal, Demam tinggi, Pening/pengsan, Sakit perut teruk, Pecah ketuban, Sawan/kejang',
        f4_label: 'Tekanan darah (jika ada)', f4_type: 'text', f4_options: '',
        f5_label: 'Gerakan bayi', f5_type: 'dropdown', f5_options: 'Aktif seperti biasa, Berkurangan hari ini, Tiada gerakan 12+ jam, Tidak pasti',
        f6_label: 'Keterangan lanjut', f6_type: 'text', f6_options: '',
        hint: '', q1: '', q2: '', q3: '', q4: '',
        button_label: 'Saringan',
        ai_instruction: 'ask:TRIAGE KEHAMILAN:\nUmur: {{form_f1}} tahun\nMinggu mengandung: {{form_f2}}\nGejala utama: {{form_f3}}\nTekanan darah: {{form_f4}}\nGerakan bayi: {{form_f5}}\nKeterangan: {{form_f6}}\nMaklumat tambahan (suara): {{user_text}}\n\nTentukan tahap risiko [HIJAU/KUNING/MERAH].\n\nJika MERAH:\n1. Nyatakan sebab MERAH\n2. Senaraikan 3-5 langkah penstabilan SEGERA\n3. Nasihatkan hubungi 999\n4. Nyatakan: "Pergi ke skrin Panggil 999 untuk hubungi ambulans"\n\nJika KUNING, nasihatkan lawatan klinik dalam 24 jam.\nJika HIJAU, beri panduan pemantauan di rumah.',
      },
      disabledWidgets: [],
      description: 'SCREEN: Saringan Gejala | FUNCTION: Pregnancy symptom triage with BP, fetal movement, and risk level | INPUTS: form_f1, form_f2, form_f3, form_f4, form_f5, form_f6 | TRIGGERS: gejala, sakit, bengkak, pendarahan, sakit kepala, penglihatan kabur, demam, kontraksi, pening, muntah, pecah air, tekanan darah, gerakan bayi',
      prefillHints: { umur_pesakit: 'form_f1', minggu_mengandung: 'form_f2', gejala_utama: 'form_f3', tekanan_darah: 'form_f4', gerakan_bayi: 'form_f5', keterangan: 'form_f6' },
    },
    {
      id: 'senarai_semak', title: 'Senarai Semak Kecemasan', isHome: false, gridColumns: 2, screenIcon: '\u{2705}',
      ...createScreen('checklist', {
        steps: 'Baringkan pesakit ke posisi KIRI (elak tekanan vena kava)|text\nUkur tekanan darah jika ada alat — catat bacaan|text\nPeriksa bengkak: muka, tangan, kaki — catat lokasi|text\nTanya: Penglihatan kabur atau nampak bintik?|text\nTanya: Sakit kepala teruk atau pening?|text\nPeriksa pendarahan — anggaran jumlah (berapa pad?)|text\nUkur suhu badan — catat bacaan|text\nPeriksa gerakan bayi — minta ibu kira tendangan 10 minit|text\nJika 2+ gejala MERAH: Pergi ke skrin Panggil 999 SEGERA|text',
        completion_action: 'none',
      }),
      description: 'SCREEN: Senarai Semak Kecemasan | FUNCTION: Step-by-step emergency stabilization checklist for midwives | TRIGGERS: senarai semak, checklist, langkah kecemasan, apa perlu buat, stabilkan pesakit',
      prefillHints: {},
    },
    {
      id: 'panggil_999', title: 'Panggil 999', isHome: false, gridColumns: 2, screenIcon: '\u{1F6D1}',
      ...createScreen('sms_dispatch', {
        heading: 'KECEMASAN — Panggil Ambulans',
        contact_type: 'call',
        contacts: '\u{1F691} Ambulans 999 | 999\n\u{1F3E5} Klinik Desa | +60131234567\n\u{1F46E} Balai Polis | 112',
      }),
      description: 'SCREEN: Panggil 999 | FUNCTION: One-tap emergency call to ambulance, clinic, or police | TRIGGERS: ambulans, 999, kecemasan, panggil bantuan, hospital, tolong',
      prefillHints: {},
    },
    {
      id: 'hantar_info', title: 'Hantar Info Pesakit', isHome: false, gridColumns: 2, screenIcon: '\u{1F4E8}',
      ...createScreen('sms_dispatch', {
        heading: 'Hantar maklumat pesakit ke klinik',
        contact_type: 'sms',
        contacts: '\u{1F3E5} Klinik Desa | +60131234567\n\u{1F691} Ambulans (SMS) | 999',
        sms_template: 'BINA TRIAGE - KOD MERAH\nKecemasan Kehamilan\nLokasi: https://maps.google.com/?q={{user_location}}\nPesakit: {{form_f1}} tahun, {{form_f2}} minggu mengandung\nGejala: {{form_f3}}\nTekanan darah: {{form_f4}}\nGerakan bayi: {{form_f5}}\nKeterangan: {{form_f6}}\nDihantar oleh bidan via Bina',
      }),
      description: 'SCREEN: Hantar Info | FUNCTION: Send patient data with GPS location via SMS to clinic | TRIGGERS: hantar, SMS, maklumat pesakit, klinik, info',
      prefillHints: {},
    },
    {
      id: 'rujukan_preeklampsia', title: 'Tanda Pre-Eklampsia', isHome: false, gridColumns: 2, screenIcon: '\u{26A0}\u{FE0F}',
      ...createScreen('info_display', {
        text: '**TANDA PRE-EKLAMPSIA (MERAH)**\n\n- Bengkak muka dan tangan (bukan kaki sahaja)\n- Sakit kepala teruk yang tidak hilang\n- Penglihatan kabur atau nampak bintik\n- Sakit bahagian atas perut (bawah rusuk)\n- Tekanan darah melebihi 140/90\n- Loya atau muntah tiba-tiba\n\n**Julat Normal:**\n- Tekanan darah: 90/60 hingga 120/80\n- Bengkak kaki sahaja = biasa pada trimester 3\n- Sakit kepala ringan = biasa, pantau\n\n**Tindakan:** Jika 2+ gejala di atas, ini MERAH. Baringkan ke KIRI, hubungi 999.',
        style: 'body',
      }),
      description: 'SCREEN: Tanda Pre-Eklampsia | FUNCTION: Quick reference for pre-eclampsia warning signs with normal ranges | TRIGGERS: pre-eklampsia, bengkak, tekanan darah tinggi, penglihatan kabur',
      prefillHints: {},
    },
    {
      id: 'rujukan_pendarahan', title: 'Tanda Pendarahan', isHome: false, gridColumns: 2, screenIcon: '\u{1FA78}',
      ...createScreen('info_display', {
        text: '**TANDA PENDARAHAN (MERAH)**\n\n- Darah merah terang dari faraj\n- Lebih dari satu pad sejam\n- Pening atau hampir pengsan\n- Jantung berdegup laju\n- Kulit pucat, sejuk, berpeluh\n\n**TANDA JANGKITAN (KUNING/MERAH)**\n\n- Demam melebihi 38\u{00B0}C\n- Menggigil\n- Sakit ketika kencing\n- Keputihan berbau busuk\n\n**Julat Normal:**\n- Sedikit tompok (spotting) = biasa trimester 1\n- Suhu badan: 36.5 hingga 37.5\u{00B0}C\n\n**Tindakan:** Pendarahan banyak = MERAH. Jangan beri makan/minum. Angkat kaki. Hubungi 999.',
        style: 'body',
      }),
      description: 'SCREEN: Tanda Pendarahan | FUNCTION: Quick reference for bleeding and infection warning signs | TRIGGERS: pendarahan, darah, jangkitan, demam, keputihan',
      prefillHints: {},
    },
    {
      id: 'bila_999', title: 'Bila Hubungi 999', isHome: false, gridColumns: 2, screenIcon: '\u{1F198}',
      ...createScreen('info_display', {
        text: '**HUBUNGI 999 SEGERA JIKA:**\n\n- 2 atau lebih gejala MERAH hadir\n- Pesakit tidak sedarkan diri\n- Sawan atau kejang\n- Pendarahan tidak berhenti\n- Tali pusat terkeluar\n\n**SAMBIL MENUNGGU AMBULANS:**\n\n1. Baringkan pesakit ke posisi KIRI\n2. Jangan bagi makan atau minum\n3. Jika pendarahan — angkat kaki lebih tinggi dari jantung\n4. Jika sawan — lindungi kepala, jangan masukkan apa-apa dalam mulut\n5. Catat masa sawan mula dan berapa lama\n6. Bersedia beri maklumat kepada paramedik\n\n**NOMBOR PENTING:**\n- Ambulans: 999\n- Pelbagai kecemasan: 112\n- Talian KKM: 03-8881 0200',
        style: 'body',
      }),
      description: 'SCREEN: Bila Hubungi 999 | FUNCTION: When to call emergency and stabilization steps while waiting | TRIGGERS: 999, bila panggil, kecemasan, tidak sedar, sawan, ambulans',
      prefillHints: {},
    },
  ],
  knowledgeSummary: `Panduan triage kecemasan kehamilan untuk bidan kampung di Malaysia.
Pre-eklampsia adalah komplikasi kehamilan yang dicirikan oleh tekanan darah tinggi dan kerosakan organ (biasanya buah pinggang/hati), biasanya selepas 20 minggu mengandung.
Eklampsia ialah sawan yang berlaku dalam pre-eklampsia — kecemasan perubatan.
Pendarahan ante-partum (sebelum bersalin) memerlukan tindakan segera jika melebihi 500ml atau disertai tanda kejutan.
Bidan kampung di kawasan luar bandar Malaysia sering menjadi responder pertama kerana klinik desa mungkin 30-60 minit jauhnya.
Nombor kecemasan Malaysia: 999 (ambulans), 112 (pelbagai kecemasan).
Posisi pemulihan kehamilan: miring ke kiri (left lateral) untuk mengelakkan tekanan pada vena kava.
Tekanan darah normal kehamilan: 90/60 hingga 120/80. Lebih 140/90 = risiko tinggi.
Gerakan bayi normal: 10+ tendangan dalam 2 jam. Kurang dari itu = perlu pemeriksaan segera.`,
  maxClarifications: 2,
  fallbackScreen: 'saringan_gejala',
  introPage: {
    ...defaultIntroPage('AMARAN: Ini BUKAN penggantian doktor. Untuk panduan kecemasan sahaja. Hubungi 999 untuk kecemasan sebenar.'),
    enabled: true,
    authorName: 'Kementerian Kesihatan',
    authorOrg: 'KKM Malaysia',
    authorVerified: true,
    links: [
      { label: 'Talian Kecemasan KKM', url: 'tel:999' },
    ],
  },
};
