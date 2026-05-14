import { motion } from 'motion/react';
import { UserMode } from './Root';
import { Sparkles, Hammer } from 'lucide-react';

interface TopHeaderProps {
  userMode: UserMode;
  onToggleMode: () => void;
}

export function TopHeader({ userMode, onToggleMode }: TopHeaderProps) {
  return (
    <div className="relative z-40 px-6 py-4 bg-gradient-to-b from-white/80 to-transparent backdrop-blur-lg border-b border-stone-200/30">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <motion.div
            whileTap={{ scale: 0.9 }}
            className="w-10 h-10 rounded-2xl flex items-center justify-center shadow-lg"
            style={{ background: '#C45A3A' }}
          >
            <span className="text-white font-bold" style={{ fontSize: '18px' }}>B</span>
          </motion.div>
          <div>
            <h1 style={{ fontSize: '20px', fontWeight: 600, color: '#1C1917', margin: 0 }}>Bina.ai</h1>
            <p style={{ fontSize: '10px', color: '#78716C', margin: 0 }}>Edge-Native AI Platform</p>
          </div>
        </div>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={onToggleMode}
          className="flex items-center gap-2 px-4 py-2 rounded-full border border-stone-200/30 shadow-lg min-h-[44px]"
          style={{
            background: userMode === 'builder'
              ? 'linear-gradient(135deg, #059669 0%, #047857 100%)'
              : 'linear-gradient(135deg, #C45A3A 0%, #A34428 100%)'
          }}
        >
          {userMode === 'builder' ? (
            <>
              <Sparkles size={16} className="text-white" />
              <span style={{ fontSize: '12px', color: 'white', fontWeight: 500 }}>Builder</span>
            </>
          ) : (
            <>
              <Hammer size={16} className="text-white" />
              <span style={{ fontSize: '12px', color: 'white', fontWeight: 500 }}>Architect</span>
            </>
          )}
        </motion.button>
      </div>
    </div>
  );
}
