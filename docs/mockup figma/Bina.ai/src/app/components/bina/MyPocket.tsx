import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Settings2, Mic, Camera, Plus, X, Grid3x3, LayoutGrid, Languages, Trash2, MapPin, Phone, Ambulance, CheckCircle2, Circle } from 'lucide-react';
import { ImageWithFallback } from '../figma/ImageWithFallback';

interface InstalledAgent {
  id: string;
  name: string;
  icon: string;
  color: string;
}

interface Widget {
  id: string;
  type: 'button' | 'slider' | 'bilingual';
  label: string;
  labelAlt?: string;
  value?: number;
  position: { row: number; col: number };
  size: { rows: number; cols: number };
}

const INSTALLED_AGENTS: InstalledAgent[] = [
  { id: '1', name: 'Buku Kira-Kira', icon: '📊', color: '#10B981' },
  { id: '2', name: 'Bidan Pintar', icon: '🏥', color: '#EF4444' },
  { id: '3', name: 'Farm Buddy', icon: '🌾', color: '#F59E0B' },
];

const INITIAL_WIDGETS: Widget[] = [
  { id: '1', type: 'button', label: 'Jual / Sell', labelAlt: 'ขาย', position: { row: 0, col: 0 }, size: { rows: 1, cols: 2 } },
  { id: '2', type: 'button', label: 'Stok / Stock', labelAlt: 'สต๊อก', position: { row: 0, col: 2 }, size: { rows: 1, cols: 2 } },
  { id: '3', type: 'button', label: 'Untung / Profit', labelAlt: 'กำไร', position: { row: 1, col: 0 }, size: { rows: 1, cols: 2 } },
  { id: '4', type: 'button', label: 'Hutang / Debt', labelAlt: 'หนี้', position: { row: 1, col: 2 }, size: { rows: 1, cols: 2 } },
  { id: '5', type: 'slider', label: 'Context Slider', position: { row: 2, col: 0 }, size: { rows: 1, cols: 4 }, value: 50 },
];

export function MyPocket() {
  const [selectedAgent, setSelectedAgent] = useState<InstalledAgent>(INSTALLED_AGENTS[0]);
  const [customizeMode, setCustomizeMode] = useState(false);
  const [widgets, setWidgets] = useState<Widget[]>(INITIAL_WIDGETS);
  const [jiggleMode, setJiggleMode] = useState(false);

  const toggleCustomize = () => {
    setCustomizeMode(!customizeMode);
    if (!customizeMode) {
      setJiggleMode(true);
    } else {
      setJiggleMode(false);
    }
  };

  const deleteWidget = (id: string) => {
    setWidgets(widgets.filter(w => w.id !== id));
  };

  const addWidget = (type: 'button' | 'slider') => {
    const newWidget: Widget = {
      id: Date.now().toString(),
      type,
      label: type === 'button' ? 'New Button' : 'New Slider',
      position: { row: widgets.length, col: 0 },
      size: { rows: 1, cols: type === 'button' ? 2 : 4 },
      value: type === 'slider' ? 50 : undefined
    };
    setWidgets([...widgets, newWidget]);
  };

  return (
    <div className="min-h-full pb-6">
      {/* Top Agent Carousel */}
      <div className="px-6 pt-6 pb-4">
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '12px' }}>
          My Pocket
        </h2>
        
        <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2">
          {INSTALLED_AGENTS.map((agent) => {
            const isActive = selectedAgent.id === agent.id;
            return (
              <motion.button
                key={agent.id}
                whileTap={{ scale: 0.95 }}
                onClick={() => setSelectedAgent(agent)}
                className="flex flex-col items-center gap-2 p-4 rounded-3xl border border-white/20 min-w-[100px] min-h-[100px]"
                style={{
                  background: isActive 
                    ? `linear-gradient(135deg, ${agent.color}20 0%, ${agent.color}10 100%)`
                    : 'rgba(255, 255, 255, 0.6)',
                  backdropFilter: 'blur(12px)',
                  borderColor: isActive ? `${agent.color}40` : 'rgba(255, 255, 255, 0.2)'
                }}
              >
                <div 
                  className="w-12 h-12 rounded-2xl flex items-center justify-center shadow-lg"
                  style={{ background: agent.color }}
                >
                  <span style={{ fontSize: '24px' }}>{agent.icon}</span>
                </div>
                <span style={{ 
                  fontSize: '12px', 
                  fontWeight: isActive ? 600 : 500,
                  color: isActive ? '#091A7A' : '#6B7280',
                  textAlign: 'center'
                }}>
                  {agent.name}
                </span>
              </motion.button>
            );
          })}
        </div>
      </div>

      {/* Customize Toggle */}
      <div className="px-6 pb-4">
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={toggleCustomize}
          className="flex items-center gap-2 px-4 py-3 rounded-2xl border border-white/20 min-h-[44px]"
          style={{
            background: customizeMode 
              ? 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)'
              : 'rgba(255, 255, 255, 0.9)',
            backdropFilter: 'blur(20px)'
          }}
        >
          <Settings2 size={18} style={{ color: customizeMode ? 'white' : '#091A7A' }} />
          <span style={{ 
            fontSize: '14px', 
            fontWeight: 600, 
            color: customizeMode ? 'white' : '#091A7A' 
          }}>
            {customizeMode ? 'Done Customizing' : 'Customize Interface'}
          </span>
        </motion.button>
      </div>

      {/* Generative UI Canvas */}
      <div className="px-6 pb-4">
        <div
          className="rounded-3xl p-6 border border-white/20 shadow-card"
          style={{
            background: 'rgba(255, 255, 255, 0.9)',
            backdropFilter: 'blur(20px)'
          }}
        >
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2">
              <div
                className="w-8 h-8 rounded-xl flex items-center justify-center"
                style={{ background: selectedAgent.color }}
              >
                <span style={{ fontSize: '16px' }}>{selectedAgent.icon}</span>
              </div>
              <span style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                {selectedAgent.name}
              </span>
            </div>
            <Languages size={18} className="text-gray-400" />
          </div>

          {/* Screen 1: Buku Kira-Kira - Warung POS */}
          {selectedAgent.id === '1' && (
            <div className="space-y-4">
              {/* Widget 1: Macro_Grid - 2x2 POS Grid */}
              <div className="grid grid-cols-2 gap-3">
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  className="rounded-2xl shadow-card min-h-[100px] flex flex-col items-center justify-center gap-2"
                  style={{ background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)' }}
                >
                  <span style={{ fontSize: '18px', fontWeight: 700, color: 'white' }}>
                    + Nasi Lemak
                  </span>
                  <span style={{ fontSize: '14px', fontWeight: 500, color: 'rgba(255,255,255,0.9)' }}>
                    RM 3.50
                  </span>
                </motion.button>

                <motion.button
                  whileTap={{ scale: 0.95 }}
                  className="rounded-2xl shadow-card min-h-[100px] flex flex-col items-center justify-center gap-2"
                  style={{ background: 'linear-gradient(135deg, #F97316 0%, #EA580C 100%)' }}
                >
                  <span style={{ fontSize: '18px', fontWeight: 700, color: 'white' }}>
                    + Karipap
                  </span>
                  <span style={{ fontSize: '14px', fontWeight: 500, color: 'rgba(255,255,255,0.9)' }}>
                    RM 1.00
                  </span>
                </motion.button>

                <motion.button
                  whileTap={{ scale: 0.95 }}
                  className="rounded-2xl shadow-card min-h-[100px] flex flex-col items-center justify-center gap-2"
                  style={{ background: 'linear-gradient(135deg, #FBBF24 0%, #F59E0B 100%)' }}
                >
                  <span style={{ fontSize: '18px', fontWeight: 700, color: 'white' }}>
                    + Teh O
                  </span>
                  <span style={{ fontSize: '14px', fontWeight: 500, color: 'rgba(255,255,255,0.9)' }}>
                    RM 1.50
                  </span>
                </motion.button>

                <motion.button
                  whileTap={{ scale: 0.95 }}
                  className="rounded-2xl shadow-card min-h-[100px] flex flex-col items-center justify-center gap-2"
                  style={{ background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)' }}
                >
                  <span style={{ fontSize: '18px', fontWeight: 700, color: 'white' }}>
                    + Kopi O
                  </span>
                  <span style={{ fontSize: '14px', fontWeight: 500, color: 'rgba(255,255,255,0.9)' }}>
                    RM 2.00
                  </span>
                </motion.button>
              </div>

              {/* Widget 2: Vision_EdgeScanner */}
              <motion.button
                whileTap={{ scale: 0.95 }}
                className="w-full py-5 rounded-2xl border-2 border-dashed shadow-card flex items-center justify-center gap-3 min-h-[64px]"
                style={{
                  background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(16, 185, 129, 0.05) 100%)',
                  borderColor: '#10B981'
                }}
              >
                <Camera size={28} style={{ color: '#10B981' }} />
                <span style={{ fontSize: '16px', fontWeight: 700, color: '#10B981' }}>
                  Imbas Buku Hutang
                </span>
              </motion.button>
            </div>
          )}

          {/* Screen 2: Bidan Pintar - Rural Triage */}
          {selectedAgent.id === '2' && (
            <div className="space-y-4">
              {/* Red Warning Banner */}
              <div
                className="rounded-2xl p-4 shadow-card"
                style={{ background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)' }}
              >
                <div className="flex items-center gap-2 mb-1">
                  <div className="w-2 h-2 rounded-full bg-white animate-pulse" />
                  <span style={{ fontSize: '12px', fontWeight: 700, color: 'white', letterSpacing: '0.05em' }}>
                    KOD MERAH
                  </span>
                </div>
                <p style={{ fontSize: '16px', fontWeight: 700, color: 'white' }}>
                  Risiko Pra-Eklampsia
                </p>
              </div>

              {/* Widget 1: Dynamic_ActionList - Stabilization SOP */}
              <div className="space-y-2">
                <p style={{ fontSize: '12px', fontWeight: 600, color: '#6B7280', marginBottom: '8px' }}>
                  Langkah Stabilisasi:
                </p>

                <motion.div
                  whileTap={{ scale: 0.98 }}
                  className="flex items-start gap-3 p-4 rounded-2xl"
                  style={{ background: 'rgba(16, 185, 129, 0.1)' }}
                >
                  <CheckCircle2 size={24} style={{ color: '#10B981', flexShrink: 0 }} />
                  <span style={{ fontSize: '14px', fontWeight: 500, color: '#091A7A' }}>
                    Baringkan pesakit mengiring
                  </span>
                </motion.div>

                <motion.div
                  whileTap={{ scale: 0.98 }}
                  className="flex items-start gap-3 p-4 rounded-2xl"
                  style={{ background: 'rgba(107, 114, 128, 0.1)' }}
                >
                  <Circle size={24} style={{ color: '#6B7280', flexShrink: 0 }} />
                  <span style={{ fontSize: '14px', fontWeight: 500, color: '#091A7A' }}>
                    Sediakan oksigen jika ada
                  </span>
                </motion.div>

                <motion.div
                  whileTap={{ scale: 0.98 }}
                  className="flex items-start gap-3 p-4 rounded-2xl"
                  style={{ background: 'rgba(107, 114, 128, 0.1)' }}
                >
                  <Circle size={24} style={{ color: '#6B7280', flexShrink: 0 }} />
                  <span style={{ fontSize: '14px', fontWeight: 500, color: '#091A7A' }}>
                    Ukur tekanan darah setiap 5 minit
                  </span>
                </motion.div>
              </div>

              {/* Widget 2: Native_Comm_Dispatcher - Emergency Button */}
              <motion.button
                whileTap={{ scale: 0.98 }}
                animate={{
                  boxShadow: [
                    '0 0 0 0 rgba(239, 68, 68, 0.4)',
                    '0 0 0 20px rgba(239, 68, 68, 0)',
                    '0 0 0 0 rgba(239, 68, 68, 0)'
                  ]
                }}
                transition={{
                  duration: 2,
                  repeat: Infinity,
                  ease: 'easeInOut'
                }}
                className="w-full py-8 rounded-2xl shadow-elevated flex flex-col items-center justify-center gap-3 min-h-[120px]"
                style={{ background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)' }}
              >
                <Ambulance size={48} className="text-white" />
                <span style={{ fontSize: '18px', fontWeight: 700, color: 'white', textAlign: 'center' }}>
                  Panggil Ambulans & Hantar GPS
                </span>
              </motion.button>
            </div>
          )}

          {/* Screen 3: Farm Buddy - Agri-Expert */}
          {selectedAgent.id === '3' && (
            <div className="space-y-4">
              {/* Widget 1: Geo_Locator Tag */}
              <div
                className="rounded-2xl p-4 border border-white/20"
                style={{ background: 'rgba(245, 158, 11, 0.1)' }}
              >
                <div className="flex items-center gap-2">
                  <MapPin size={20} style={{ color: '#F59E0B' }} />
                  <span style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                    Jangkitan: Titik 042 Disimpan
                  </span>
                </div>
              </div>

              {/* Widget 2: Diagnosis & Image */}
              <div className="space-y-3">
                {/* Disease Image Thumbnail */}
                <div className="rounded-2xl overflow-hidden border border-white/20 shadow-card">
                  <ImageWithFallback
                    src="https://images.unsplash.com/photo-1464226184884-fa280b87c399?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                    alt="Diseased palm leaf"
                    className="w-full h-40 object-cover"
                  />
                </div>

                {/* Diagnosis */}
                <div
                  className="rounded-2xl p-4"
                  style={{ background: 'rgba(239, 68, 68, 0.1)' }}
                >
                  <p style={{ fontSize: '12px', fontWeight: 600, color: '#6B7280', marginBottom: '4px' }}>
                    Penyakit Dikesan:
                  </p>
                  <p style={{ fontSize: '16px', fontWeight: 700, color: '#EF4444' }}>
                    Ganoderma (Stem Rot)
                  </p>
                </div>

                {/* Checklist */}
                <div className="space-y-2">
                  <p style={{ fontSize: '12px', fontWeight: 600, color: '#6B7280', marginBottom: '8px' }}>
                    Tindakan Segera:
                  </p>

                  <motion.div
                    whileTap={{ scale: 0.98 }}
                    className="flex items-start gap-3 p-4 rounded-2xl"
                    style={{ background: 'rgba(107, 114, 128, 0.1)' }}
                  >
                    <Circle size={24} style={{ color: '#6B7280', flexShrink: 0 }} />
                    <span style={{ fontSize: '14px', fontWeight: 500, color: '#091A7A' }}>
                      Ikat reben merah pada pokok
                    </span>
                  </motion.div>

                  <motion.div
                    whileTap={{ scale: 0.98 }}
                    className="flex items-start gap-3 p-4 rounded-2xl"
                    style={{ background: 'rgba(107, 114, 128, 0.1)' }}
                  >
                    <Circle size={24} style={{ color: '#6B7280', flexShrink: 0 }} />
                    <span style={{ fontSize: '14px', fontWeight: 500, color: '#091A7A' }}>
                      Asingkan kawasan jangkitan
                    </span>
                  </motion.div>
                </div>

                {/* Widget 3: Procurement Button */}
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  className="w-full py-5 rounded-2xl shadow-card flex items-center justify-center gap-3 min-h-[64px]"
                  style={{ background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)' }}
                >
                  <span style={{ fontSize: '16px', fontWeight: 700, color: 'white' }}>
                    Pesan Baja Gano-Care A - RM45
                  </span>
                  <Phone size={20} className="text-white" />
                </motion.button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Bottom Action Dock - Context-Specific */}
      <div className="px-6">
        {/* Buku Kira-Kira: Voice-First Input */}
        {selectedAgent.id === '1' && (
          <motion.button
            whileTap={{ scale: 0.95 }}
            className="w-full py-6 rounded-2xl shadow-elevated flex items-center justify-center gap-3 min-h-[72px]"
            style={{
              background: 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)'
            }}
          >
            <Mic size={36} className="text-white" />
            <span style={{ fontSize: '18px', fontWeight: 700, color: 'white' }}>
              Pesanan Suara
            </span>
          </motion.button>
        )}

        {/* Bidan Pintar: Camera + Voice */}
        {selectedAgent.id === '2' && (
          <div className="flex gap-3">
            <motion.button
              whileTap={{ scale: 0.95 }}
              className="flex-1 py-5 rounded-2xl shadow-elevated flex items-center justify-center gap-2 min-h-[56px]"
              style={{
                background: 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)'
              }}
            >
              <Mic size={24} className="text-white" />
              <span style={{ fontSize: '16px', fontWeight: 600, color: 'white' }}>
                Rekod Simptom
              </span>
            </motion.button>

            <motion.button
              whileTap={{ scale: 0.95 }}
              className="w-16 h-16 rounded-2xl shadow-elevated flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)'
              }}
            >
              <Camera size={28} className="text-white" />
            </motion.button>
          </div>
        )}

        {/* Farm Buddy: Camera-First for Crop Scanning */}
        {selectedAgent.id === '3' && (
          <div className="flex gap-3">
            <motion.button
              whileTap={{ scale: 0.95 }}
              className="flex-1 py-5 rounded-2xl shadow-elevated flex items-center justify-center gap-2 min-h-[56px]"
              style={{
                background: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)'
              }}
            >
              <Camera size={28} className="text-white" />
              <span style={{ fontSize: '16px', fontWeight: 600, color: 'white' }}>
                Imbas Tanaman
              </span>
            </motion.button>

            <motion.button
              whileTap={{ scale: 0.95 }}
              className="w-16 h-16 rounded-2xl shadow-elevated flex items-center justify-center"
              style={{
                background: 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)'
              }}
            >
              <Mic size={24} className="text-white" />
            </motion.button>
          </div>
        )}
      </div>

      {/* Helper Text - Context-Specific */}
      <div className="px-6 pt-4">
        <div className="p-4 rounded-2xl" style={{ background: 'rgba(173, 200, 255, 0.2)' }}>
          {selectedAgent.id === '1' && (
            <p style={{ fontSize: '12px', color: '#091A7A', textAlign: 'center', lineHeight: 1.5 }}>
              💡 Tekan butang makanan untuk tambah pesanan. Gunakan suara untuk pesanan pantas.
            </p>
          )}
          {selectedAgent.id === '2' && (
            <p style={{ fontSize: '12px', color: '#091A7A', textAlign: 'center', lineHeight: 1.5 }}>
              💡 Ikut senarai langkah stabilisasi. Tekan butang merah untuk kecemasan sahaja.
            </p>
          )}
          {selectedAgent.id === '3' && (
            <p style={{ fontSize: '12px', color: '#091A7A', textAlign: 'center', lineHeight: 1.5 }}>
              💡 Imbas tanaman untuk diagnosis. GPS auto-simpan lokasi jangkitan.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
