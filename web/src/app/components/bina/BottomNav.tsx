import { motion } from 'motion/react';
import { useNavigate, useLocation } from 'react-router';
import { UserMode } from './Root';
import { Store, Pocket, Hammer, BarChart3 } from 'lucide-react';

interface BottomNavProps {
  userMode: UserMode;
}

export function BottomNav({ userMode }: BottomNavProps) {
  const navigate = useNavigate();
  const location = useLocation();

  const builderTabs = [
    { id: 'hub', label: 'Hub', icon: Store, path: '/' },
    { id: 'pocket', label: 'My Pocket', icon: Pocket, path: '/pocket' },
  ];

  const architectTabs = [
    { id: 'hub', label: 'Hub', icon: Store, path: '/' },
    { id: 'studio', label: 'Studio', icon: Hammer, path: '/studio' },
    { id: 'analytics', label: 'Analytics', icon: BarChart3, path: '/analytics' },
  ];

  const tabs = userMode === 'builder' ? builderTabs : architectTabs;

  return (
    <div className="absolute bottom-0 left-0 right-0 z-40 pb-6">
      <motion.div 
        initial={{ y: 100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.3, duration: 0.5, ease: "easeOut" }}
        className="mx-4 rounded-3xl backdrop-blur-lg border border-white/20 shadow-elevated overflow-hidden"
        style={{
          background: 'rgba(255, 255, 255, 0.9)'
        }}
      >
        <div className="flex items-center justify-around px-2 py-3">
          {tabs.map((tab) => {
            const isActive = location.pathname === tab.path;
            const Icon = tab.icon;
            
            return (
              <motion.button
                key={tab.id}
                whileTap={{ scale: 0.95 }}
                onClick={() => navigate(tab.path)}
                className="flex flex-col items-center gap-1 px-6 py-2 rounded-2xl transition-all relative min-h-[44px] min-w-[44px]"
                style={{
                  background: isActive ? 'rgba(173, 200, 255, 0.3)' : 'transparent'
                }}
              >
                {isActive && (
                  <motion.div
                    layoutId={`activeTab-${userMode}`}
                    className="absolute inset-0 rounded-2xl"
                    style={{
                      background: 'linear-gradient(135deg, rgba(173, 200, 255, 0.4) 0%, rgba(173, 200, 255, 0.2) 100%)',
                      border: '1px solid rgba(255, 255, 255, 0.3)'
                    }}
                    transition={{ type: "spring", bounce: 0.2, duration: 0.6 }}
                  />
                )}
                <Icon 
                  size={20} 
                  className="relative z-10"
                  style={{ 
                    color: isActive ? '#091A7A' : '#6B7280',
                    strokeWidth: isActive ? 2.5 : 2
                  }} 
                />
                <span 
                  className="relative z-10"
                  style={{ 
                    fontSize: '10px',
                    fontWeight: isActive ? 600 : 400,
                    color: isActive ? '#091A7A' : '#6B7280'
                  }}
                >
                  {tab.label}
                </span>
              </motion.button>
            );
          })}
        </div>
      </motion.div>
    </div>
  );
}
