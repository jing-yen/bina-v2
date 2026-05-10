import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Camera, Mic, MessageSquare, MapPin, Database, Share2, Bell, CheckCircle2, X } from 'lucide-react';

interface Feature {
  id: string;
  icon: any;
  title: string;
  description: string;
  sizeKB: number;
  enabled: boolean;
  recommended?: boolean;
}

interface PreInstallConfiguratorProps {
  recipeName: string;
  recipeOrganization: string;
  onInstall: (selectedFeatures: string[]) => void;
  onCancel: () => void;
}

const DEFAULT_FEATURES: Feature[] = [
  {
    id: 'camera',
    icon: Camera,
    title: 'Camera Scanner',
    description: 'Allows the AI to see crop diseases',
    sizeKB: 0.4,
    enabled: true,
    recommended: true
  },
  {
    id: 'voice',
    icon: Mic,
    title: 'Voice Assistant',
    description: 'Allows you to speak your questions',
    sizeKB: 0.3,
    enabled: true,
    recommended: true
  },
  {
    id: 'sms',
    icon: MessageSquare,
    title: 'SMS Dispatcher',
    description: 'Allows the AI to text the nearest supplier',
    sizeKB: 0.2,
    enabled: false
  },
  {
    id: 'gps',
    icon: MapPin,
    title: 'GPS Tracker',
    description: 'Saves the location of infected trees',
    sizeKB: 0.1,
    enabled: true
  },
  {
    id: 'offline-storage',
    icon: Database,
    title: 'Offline Data Storage',
    description: 'Stores your history for later review',
    sizeKB: 0.3,
    enabled: true,
    recommended: true
  },
  {
    id: 'p2p-sharing',
    icon: Share2,
    title: 'P2P Recipe Sharing',
    description: 'Share this tool with neighbors offline',
    sizeKB: 0.2,
    enabled: false
  },
  {
    id: 'notifications',
    icon: Bell,
    title: 'Smart Notifications',
    description: 'Get alerts for important updates',
    sizeKB: 0.1,
    enabled: false
  }
];

export function PreInstallConfigurator({
  recipeName,
  recipeOrganization,
  onInstall,
  onCancel
}: PreInstallConfiguratorProps) {
  const [features, setFeatures] = useState<Feature[]>(DEFAULT_FEATURES);
  const [showSuccess, setShowSuccess] = useState(false);

  const toggleFeature = (featureId: string) => {
    setFeatures(prev =>
      prev.map(f =>
        f.id === featureId ? { ...f, enabled: !f.enabled } : f
      )
    );
  };

  const totalSize = features.reduce((sum, f) => f.enabled ? sum + f.sizeKB : sum, 0.8);
  const enabledCount = features.filter(f => f.enabled).length;

  const handleInstall = () => {
    setShowSuccess(true);
    setTimeout(() => {
      const selectedFeatures = features.filter(f => f.enabled).map(f => f.id);
      onInstall(selectedFeatures);
    }, 1500);
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/70 z-[60] flex items-end"
      onClick={onCancel}
    >
      <motion.div
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ type: 'spring', damping: 30, stiffness: 300 }}
        className="w-full rounded-t-[32px] overflow-hidden flex flex-col"
        style={{
          background: 'linear-gradient(180deg, #ADC8FF 0%, #FFFFFF 100%)',
          maxHeight: '88vh'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="p-6 pb-4 flex-shrink-0">
          <div className="flex items-start justify-between mb-4">
            <div className="flex-1">
              <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '4px' }}>
                Customize Installation
              </h2>
              <p style={{ fontSize: '12px', color: '#6B7280' }}>
                Choose features you need for {recipeName}
              </p>
            </div>

            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={onCancel}
              className="p-2 rounded-full min-h-[44px] min-w-[44px] flex items-center justify-center"
              style={{ background: 'rgba(255, 255, 255, 0.8)' }}
            >
              <X size={20} className="text-gray-600" />
            </motion.button>
          </div>

          {/* Data Footprint Indicator */}
          <motion.div
            className="rounded-2xl p-4 border border-white/40 shadow-card"
            style={{ background: 'rgba(255, 255, 255, 0.9)', backdropFilter: 'blur(20px)' }}
            animate={{ scale: [1, 1.02, 1] }}
            transition={{ duration: 0.3 }}
            key={totalSize}
          >
            <div className="flex items-center justify-between">
              <div>
                <p style={{ fontSize: '12px', color: '#6B7280', marginBottom: '4px' }}>
                  Total Download Size
                </p>
                <p style={{ fontSize: '24px', fontWeight: 700, color: '#091A7A' }}>
                  {totalSize.toFixed(1)} KB
                </p>
              </div>

              <div className="text-right">
                <p style={{ fontSize: '12px', color: '#6B7280', marginBottom: '4px' }}>
                  Active Features
                </p>
                <p style={{ fontSize: '24px', fontWeight: 700, color: '#10B981' }}>
                  {enabledCount}/{features.length}
                </p>
              </div>
            </div>
          </motion.div>
        </div>

        {/* Scrollable Feature List */}
        <div className="flex-1 overflow-y-auto px-6 pb-6 space-y-3">
          {features.map((feature) => {
            const Icon = feature.icon;
            const isEnabled = feature.enabled;

            return (
              <motion.div
                key={feature.id}
                whileTap={{ scale: 0.98 }}
                className="rounded-2xl p-4 border border-white/30 shadow-card transition-all duration-200"
                style={{
                  background: isEnabled
                    ? 'rgba(255, 255, 255, 0.95)'
                    : 'rgba(255, 255, 255, 0.5)',
                  backdropFilter: 'blur(16px)',
                  opacity: isEnabled ? 1 : 0.7
                }}
              >
                <div className="flex items-start gap-4">
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 transition-all duration-200"
                    style={{
                      background: isEnabled
                        ? 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)'
                        : 'rgba(107, 114, 128, 0.3)',
                    }}
                  >
                    <Icon size={22} className={isEnabled ? 'text-white' : 'text-gray-400'} />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-start gap-2 mb-1">
                      <h4 style={{
                        fontSize: '14px',
                        fontWeight: 600,
                        color: isEnabled ? '#091A7A' : '#9CA3AF',
                        flex: 1
                      }}>
                        {feature.title}
                      </h4>
                      {feature.recommended && (
                        <div
                          className="px-2 py-0.5 rounded-full"
                          style={{ background: '#10B98120' }}
                        >
                          <span style={{ fontSize: '9px', fontWeight: 600, color: '#10B981' }}>
                            RECOMMENDED
                          </span>
                        </div>
                      )}
                    </div>

                    <p style={{
                      fontSize: '12px',
                      color: isEnabled ? '#6B7280' : '#9CA3AF',
                      marginBottom: '6px',
                      lineHeight: 1.4
                    }}>
                      {feature.description}
                    </p>

                    <div className="flex items-center gap-2">
                      <span style={{
                        fontSize: '10px',
                        color: isEnabled ? '#091A7A' : '#9CA3AF',
                        fontWeight: 600
                      }}>
                        +{feature.sizeKB.toFixed(1)} KB
                      </span>
                    </div>
                  </div>

                  {/* Large Toggle Switch */}
                  <motion.button
                    whileTap={{ scale: 0.9 }}
                    onClick={() => toggleFeature(feature.id)}
                    className="relative rounded-full transition-all duration-200 min-h-[44px] min-w-[44px] flex items-center justify-center"
                    style={{
                      width: '56px',
                      height: '32px',
                      background: isEnabled
                        ? 'linear-gradient(135deg, #10B981 0%, #059669 100%)'
                        : 'rgba(203, 206, 212, 0.6)',
                      padding: '4px'
                    }}
                  >
                    <motion.div
                      className="rounded-full shadow-md"
                      style={{
                        width: '24px',
                        height: '24px',
                        background: 'white',
                        position: 'absolute'
                      }}
                      animate={{ x: isEnabled ? 12 : -12 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                    />
                  </motion.button>
                </div>
              </motion.div>
            );
          })}
        </div>

        {/* Sticky Bottom CTA */}
        <div
          className="p-6 border-t border-white/40 flex-shrink-0"
          style={{ background: 'rgba(255, 255, 255, 0.95)', backdropFilter: 'blur(20px)' }}
        >
          <motion.button
            whileTap={{ scale: 0.98 }}
            onClick={handleInstall}
            disabled={enabledCount === 0}
            className="w-full py-4 rounded-2xl shadow-elevated min-h-[56px] flex items-center justify-center gap-2 transition-opacity duration-200"
            style={{
              background: enabledCount === 0
                ? 'rgba(107, 114, 128, 0.5)'
                : 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)',
              fontSize: '16px',
              fontWeight: 600,
              color: 'white',
              opacity: enabledCount === 0 ? 0.5 : 1
            }}
          >
            <CheckCircle2 size={20} />
            Install to Pocket
          </motion.button>

          <p style={{ fontSize: '11px', color: '#9CA3AF', textAlign: 'center', marginTop: '12px' }}>
            You can change these settings anytime in My Pocket
          </p>
        </div>

        {/* Success Animation Overlay */}
        <AnimatePresence>
          {showSuccess && (
            <motion.div
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              className="absolute inset-0 flex items-center justify-center"
              style={{ background: 'rgba(9, 26, 122, 0.95)', backdropFilter: 'blur(10px)' }}
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: [0, 1.2, 1] }}
                transition={{ duration: 0.5, times: [0, 0.6, 1] }}
                className="text-center"
              >
                <div
                  className="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-4"
                  style={{ background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)' }}
                >
                  <CheckCircle2 size={48} className="text-white" />
                </div>
                <h3 style={{ fontSize: '20px', fontWeight: 600, color: 'white', marginBottom: '8px' }}>
                  Installation Complete!
                </h3>
                <p style={{ fontSize: '14px', color: 'rgba(255,255,255,0.8)' }}>
                  {recipeName} is ready in your Pocket
                </p>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </motion.div>
  );
}
