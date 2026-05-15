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
  blockedKeywords: 'ubat, medication, prescribe, preskripsi, dos, dosage, paracetamol, aspirin',
  disclaimer: 'AMARAN: Ini BUKAN penggantian doktor. Untuk panduan kecemasan sahaja. Hubungi 999 untuk kecemasan sebenar.',
  category: 'Health',
  selectedLanguages: ['ms', 'en'],
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
        form_field_count: '4',
        f1_label: 'Umur pesakit', f1_type: 'number', f1_options: '',
        f2_label: 'Minggu mengandung', f2_type: 'number', f2_options: '',
        f3_label: 'Gejala utama', f3_type: 'dropdown', f3_options: 'Bengkak muka/kaki, Sakit kepala teruk, Penglihatan kabur, Pendarahan, Kontraksi awal, Demam tinggi, Pening/pengsan, Sakit perut teruk, Pecah ketuban, Lain-lain',
        f4_label: 'Keterangan lanjut (suara/teks)', f4_type: 'text', f4_options: '',
        hint: '', q1: '', q2: '', q3: '', q4: '',
        button_label: 'Saringan',
        ai_instruction: 'ask:TRIAGE KEHAMILAN:\nUmur: {{form_f1}} tahun\nMinggu mengandung: {{form_f2}}\nGejala utama: {{form_f3}}\nKeterangan: {{form_f4}}\n\nTentukan tahap risiko [HIJAU/KUNING/MERAH]. Jika MERAH, senaraikan langkah penstabilan segera dan nasihatkan hubungi 999. Terjemah sebarang dialek ke istilah perubatan formal.',
      },
      disabledWidgets: [],
      description: 'SCREEN: Saringan Gejala | FUNCTION: Form-based pregnancy symptom triage with risk level assessment | INPUTS: form_f1, form_f2, form_f3, form_f4 | TRIGGERS: gejala, sakit, bengkak, pendarahan, sakit kepala, penglihatan kabur, demam, kontraksi, pening, muntah, pecah air',
      prefillHints: { umur_pesakit: 'form_f1', minggu_mengandung: 'form_f2', gejala_utama: 'form_f3', keterangan: 'form_f4' },
    },
    {
      id: 'senarai_semak', title: 'Senarai Semak Kecemasan', isHome: false, gridColumns: 2, screenIcon: '\u{2705}',
      ...createScreen('checklist', {
        steps: 'Baringkan pesakit ke posisi kiri|text\nUkur tekanan darah (jika ada alat)|text\nPeriksa bengkak pada muka, tangan, kaki|text\nTanya: Penglihatan kabur atau bintik-bintik?|text\nTanya: Sakit kepala teruk atau pening?|text\nPeriksa pendarahan — berapa banyak?|text\nUkur suhu badan|text\nJika 2+ gejala MERAH: Hubungi 999 SEGERA|text',
        completion_action: 'none',
      }),
      description: 'SCREEN: Senarai Semak Kecemasan | FUNCTION: Step-by-step emergency stabilization checklist for midwives | TRIGGERS: senarai semak, checklist, langkah kecemasan, apa perlu buat, stabilkan pesakit',
      prefillHints: {},
    },
    {
      id: 'panggil_ambulans', title: 'Panggil Ambulans', isHome: false, gridColumns: 2, screenIcon: '\u{1F6D1}',
      ...createScreen('sms_dispatch', {
        heading: 'Hantar Maklumat Kecemasan',
        contact_type: 'sms',
        contacts: '\u{1F691} Ambulans 999 | 999\n\u{1F3E5} Klinik Desa | +60131234567',
        sms_template: 'BINA TRIAGE - KOD MERAH\nKecemasan Kehamilan\nLokasi GPS: {{user_location}}\nPesakit: {{form_f1}} tahun, {{form_f2}} minggu mengandung\nGejala: {{form_f3}}\nKeterangan: {{form_f4}}\nDihantar oleh bidan via Bina.ai',
      }),
      description: 'SCREEN: Panggil Ambulans | FUNCTION: Send emergency SMS with GPS location and patient data to ambulance/clinic | TRIGGERS: ambulans, 999, kecemasan, hantar maklumat, panggil bantuan, hospital, klinik',
      prefillHints: {},
    },
    {
      id: 'rujukan_cepat', title: 'Rujukan Cepat', isHome: false, gridColumns: 2, screenIcon: '\u{1F4D6}',
      ...createScreen('info_display', {
        text: 'TANDA PRE-EKLAMPSIA (MERAH):\n\u{2022} Bengkak muka dan tangan (bukan kaki sahaja)\n\u{2022} Sakit kepala teruk yang tidak hilang\n\u{2022} Penglihatan kabur atau nampak bintik\n\u{2022} Sakit bahagian atas perut (bawah rusuk)\n\u{2022} Tekanan darah >140/90\n\nTANDA PENDARAHAN (MERAH):\n\u{2022} Darah merah terang dari faraj\n\u{2022} Lebih dari satu pad sejam\n\u{2022} Pening atau hampir pengsan\n\u{2022} Jantung berdegup laju\n\nTANDA JANGKITAN (KUNING/MERAH):\n\u{2022} Demam melebihi 38\u{00B0}C\n\u{2022} Menggigil\n\u{2022} Sakit ketika kencing\n\u{2022} Keputihan berbau\n\nBILA HUBUNGI 999:\nJika 2 atau lebih gejala MERAH hadir, atau pesakit tidak sedarkan diri.\nBaringkan pesakit ke KIRI sambil menunggu ambulans.\nJangan bagi makan atau minum jika pesakit hendak pengsan.',
        style: 'body',
      }),
      description: 'SCREEN: Rujukan Cepat | FUNCTION: Quick reference card for emergency warning signs | TRIGGERS: rujukan, panduan, tanda bahaya, apa tanda, senarai gejala, info kecemasan',
      prefillHints: {},
    },
  ],
  knowledgeSummary: `Panduan triage kecemasan kehamilan untuk bidan kampung di Malaysia.
Pre-eklampsia adalah komplikasi kehamilan yang dicirikan oleh tekanan darah tinggi dan kerosakan organ (biasanya buah pinggang/hati), biasanya selepas 20 minggu mengandung.
Eklampsia ialah sawan yang berlaku dalam pre-eklampsia — kecemasan perubatan.
Pendarahan ante-partum (sebelum bersalin) memerlukan tindakan segera jika melebihi 500ml atau disertai tanda kejutan.
Bidan kampung di kawasan luar bandar Malaysia sering menjadi responder pertama kerana klinik desa mungkin 30-60 minit jauhnya.
Nombor kecemasan Malaysia: 999 (ambulans), 112 (pelbagai kecemasan).
Posisi pemulihan kehamilan: miring ke kiri (left lateral) untuk mengelakkan tekanan pada vena kava.`,
  maxClarifications: 1,
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
