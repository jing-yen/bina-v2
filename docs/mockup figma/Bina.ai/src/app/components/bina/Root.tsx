import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { motion, AnimatePresence } from 'motion/react';
import { BottomNav } from './BottomNav';
import { TopHeader } from './TopHeader';

export type UserMode = 'builder' | 'architect';

export default function Root() {
  const [userMode, setUserMode] = useState<UserMode>('builder');
  const location = useLocation();
  const navigate = useNavigate();

  const toggleMode = () => {
    const newMode = userMode === 'builder' ? 'architect' : 'builder';
    setUserMode(newMode);
    // Navigate to the first tab of the new mode
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-50 via-pink-50 to-purple-50 flex items-center justify-center p-2 md:p-4">
      {/* Mobile Device Mockup */}
      <motion.div
        initial={{ scale: 0.85, opacity: 0, y: 30 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        transition={{ duration: 1, ease: "easeOut" }}
        className="relative scale-[0.7] origin-center"
      >
        {/* Device Shadow Layers */}
        <div className="absolute inset-0 bg-black/20 rounded-[3.5rem] blur-3xl transform translate-y-12 scale-110" />
        <div className="absolute inset-0 bg-black/15 rounded-[3.5rem] blur-2xl transform translate-y-8 scale-105" />
        
        {/* Mobile Frame - Adjusted for Android */}
        <div className="relative w-[428px] h-[926px] bg-gradient-to-br from-slate-800 via-slate-700 to-slate-900 rounded-[3.5rem] shadow-2xl">
          {/* Frame Highlights */}
          <div className="absolute inset-0 bg-gradient-to-br from-white/10 via-transparent to-transparent rounded-[3.5rem] pointer-events-none" />
          
          {/* Volume/Power Buttons */}
          <div className="absolute -right-0.5 top-[200px] w-2 h-16 bg-slate-700 rounded-l-md shadow-inner" />
          
          {/* Inner Screen Bezel */}
          <div className="absolute inset-2 bg-black rounded-[3.2rem] shadow-inner">
            {/* Screen Area */}
            <div className="absolute inset-1 bg-gradient-to-b from-[#ADC8FF] via-[#E8F2FF]/95 to-white rounded-[3rem] overflow-hidden flex flex-col shadow-inner border border-gray-200/20">
              {/* Top Header with Mode Toggle */}
              <TopHeader userMode={userMode} onToggleMode={toggleMode} />
              
              {/* Main Content Area - Scrollable */}
              <div className="flex-1 overflow-y-auto scrollbar-hide relative">
                <AnimatePresence mode="wait">
                  <motion.div
                    key={location.pathname + userMode}
                    initial={{ opacity: 0, y: 20, filter: 'blur(8px)' }}
                    animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
                    exit={{ opacity: 0, y: -20, filter: 'blur(8px)' }}
                    transition={{ duration: 0.4, ease: "easeInOut" }}
                    className="min-h-full"
                  >
                    <Outlet context={{ userMode }} />
                  </motion.div>
                </AnimatePresence>
                
                {/* Bottom padding for navigation */}
                <div className="h-24" />
              </div>
              
              {/* Bottom Navigation */}
              <BottomNav userMode={userMode} />
              
              {/* Home Indicator */}
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.5 }}
                className="absolute bottom-2 left-1/2 transform -translate-x-1/2 z-50"
              >
                <div className="w-36 h-1 bg-black/60 rounded-full shadow-sm" />
              </motion.div>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
