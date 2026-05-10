import { useState } from 'react';
import { motion } from 'motion/react';
import { QrCode, Bluetooth, Share2, CheckCircle2, Wifi, WifiOff } from 'lucide-react';

export function OfflineSync() {
  const [isOffline, setIsOffline] = useState(true);
  const [shareMethod, setShareMethod] = useState<'qr' | 'bluetooth' | null>(null);

  return (
    <div className="min-h-full pb-6">
      <div className="px-6 pt-6 pb-4">
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '8px' }}>
          Offline Sync
        </h2>
        <p style={{ fontSize: '14px', color: '#6B7280' }}>
          Share your customized apps with others nearby
        </p>
      </div>

      {/* Network Status */}
      <div className="px-6 pb-4">
        <div 
          className="flex items-center gap-3 p-4 rounded-2xl border border-white/20"
          style={{
            background: isOffline 
              ? 'linear-gradient(135deg, rgba(239, 68, 68, 0.1) 0%, rgba(239, 68, 68, 0.05) 100%)'
              : 'linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(16, 185, 129, 0.05) 100%)'
          }}
        >
          {isOffline ? (
            <>
              <WifiOff size={24} className="text-red-500" />
              <div className="flex-1">
                <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                  Offline Mode Active
                </p>
                <p style={{ fontSize: '12px', color: '#6B7280' }}>
                  Peer-to-peer sharing available
                </p>
              </div>
            </>
          ) : (
            <>
              <Wifi size={24} className="text-green-500" />
              <div className="flex-1">
                <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                  Connected
                </p>
                <p style={{ fontSize: '12px', color: '#6B7280' }}>
                  All sync methods available
                </p>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Share Method Selection */}
      <div className="px-6 pb-4">
        <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '12px' }}>
          Choose Share Method
        </h3>
        
        <div className="grid grid-cols-2 gap-4">
          {/* QR Code Method */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => setShareMethod('qr')}
            className="p-6 rounded-3xl border border-white/20 flex flex-col items-center gap-3 min-h-[140px]"
            style={{
              background: shareMethod === 'qr'
                ? 'linear-gradient(135deg, rgba(9, 26, 122, 0.15) 0%, rgba(9, 26, 122, 0.05) 100%)'
                : 'rgba(255, 255, 255, 0.9)',
              backdropFilter: 'blur(20px)',
              borderColor: shareMethod === 'qr' ? '#091A7A40' : 'rgba(255, 255, 255, 0.2)'
            }}
          >
            <div 
              className="w-16 h-16 rounded-2xl flex items-center justify-center"
              style={{ background: shareMethod === 'qr' ? '#091A7A' : '#E5E7EB' }}
            >
              <QrCode size={32} style={{ color: shareMethod === 'qr' ? 'white' : '#6B7280' }} />
            </div>
            <div className="text-center">
              <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                QR Code
              </p>
              <p style={{ fontSize: '10px', color: '#6B7280' }}>
                Scan to share
              </p>
            </div>
          </motion.button>

          {/* Bluetooth Method */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={() => setShareMethod('bluetooth')}
            className="p-6 rounded-3xl border border-white/20 flex flex-col items-center gap-3 min-h-[140px]"
            style={{
              background: shareMethod === 'bluetooth'
                ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(16, 185, 129, 0.05) 100%)'
                : 'rgba(255, 255, 255, 0.9)',
              backdropFilter: 'blur(20px)',
              borderColor: shareMethod === 'bluetooth' ? '#10B98140' : 'rgba(255, 255, 255, 0.2)'
            }}
          >
            <div 
              className="w-16 h-16 rounded-2xl flex items-center justify-center"
              style={{ background: shareMethod === 'bluetooth' ? '#10B981' : '#E5E7EB' }}
            >
              <Bluetooth size={32} style={{ color: shareMethod === 'bluetooth' ? 'white' : '#6B7280' }} />
            </div>
            <div className="text-center">
              <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                Bluetooth
              </p>
              <p style={{ fontSize: '10px', color: '#6B7280' }}>
                Nearby Share
              </p>
            </div>
          </motion.button>
        </div>
      </div>

      {/* QR Code Display */}
      {shareMethod === 'qr' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="px-6 pb-4"
        >
          <div 
            className="p-8 rounded-3xl border border-white/20 flex flex-col items-center"
            style={{
              background: 'rgba(255, 255, 255, 0.9)',
              backdropFilter: 'blur(20px)'
            }}
          >
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A', marginBottom: '16px' }}>
              Scan this QR Code
            </p>
            
            {/* QR Code Placeholder */}
            <div 
              className="w-64 h-64 rounded-2xl flex items-center justify-center mb-4"
              style={{ 
                background: 'white',
                border: '2px solid #E5E7EB',
                backgroundImage: `
                  repeating-linear-gradient(0deg, #091A7A 0px, #091A7A 4px, transparent 4px, transparent 12px),
                  repeating-linear-gradient(90deg, #091A7A 0px, #091A7A 4px, transparent 4px, transparent 12px)
                `,
                backgroundSize: '12px 12px'
              }}
            >
              <QrCode size={48} className="text-gray-400" />
            </div>
            
            <p style={{ fontSize: '12px', color: '#6B7280', textAlign: 'center', marginBottom: '12px' }}>
              This QR contains your app state and customizations
            </p>
            
            <div className="flex items-center gap-2 px-4 py-2 rounded-full" style={{ background: '#10B98120' }}>
              <CheckCircle2 size={16} className="text-green-600" />
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#10B981' }}>
                Ready to Share
              </span>
            </div>
          </div>
        </motion.div>
      )}

      {/* Bluetooth Sharing */}
      {shareMethod === 'bluetooth' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="px-6 pb-4"
        >
          <div 
            className="p-6 rounded-3xl border border-white/20"
            style={{
              background: 'rgba(255, 255, 255, 0.9)',
              backdropFilter: 'blur(20px)'
            }}
          >
            <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A', marginBottom: '16px' }}>
              Nearby Devices
            </p>
            
            {/* Scanning Animation */}
            <div className="flex flex-col items-center py-8">
              <motion.div
                animate={{
                  scale: [1, 1.2, 1],
                  opacity: [0.5, 1, 0.5]
                }}
                transition={{
                  duration: 2,
                  repeat: Infinity,
                  ease: "easeInOut"
                }}
                className="w-20 h-20 rounded-full flex items-center justify-center mb-4"
                style={{ background: '#10B98120' }}
              >
                <Bluetooth size={40} className="text-green-600" />
              </motion.div>
              
              <p style={{ fontSize: '14px', color: '#091A7A', marginBottom: '4px' }}>
                Searching for devices...
              </p>
              <p style={{ fontSize: '12px', color: '#6B7280' }}>
                Make sure Bluetooth is enabled on both devices
              </p>
            </div>

            {/* Mock Device List */}
            <div className="space-y-3 mt-4">
              {[
                { name: 'Ahmad\'s Phone', distance: '2m away' },
                { name: 'Maria\'s Tablet', distance: '5m away' }
              ].map((device, index) => (
                <motion.button
                  key={index}
                  whileTap={{ scale: 0.98 }}
                  className="w-full p-4 rounded-2xl border border-white/20 flex items-center justify-between"
                  style={{ background: 'rgba(255, 255, 255, 0.6)' }}
                >
                  <div className="flex items-center gap-3">
                    <div 
                      className="w-10 h-10 rounded-full flex items-center justify-center"
                      style={{ background: '#10B98120' }}
                    >
                      <Bluetooth size={20} className="text-green-600" />
                    </div>
                    <div className="text-left">
                      <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                        {device.name}
                      </p>
                      <p style={{ fontSize: '12px', color: '#6B7280' }}>
                        {device.distance}
                      </p>
                    </div>
                  </div>
                  <Share2 size={20} className="text-gray-400" />
                </motion.button>
              ))}
            </div>
          </div>
        </motion.div>
      )}

      {/* Info Card */}
      <div className="px-6 pt-4">
        <div 
          className="p-4 rounded-2xl border border-white/20"
          style={{ background: 'rgba(173, 200, 255, 0.2)' }}
        >
          <div className="flex gap-3">
            <div className="flex-shrink-0">
              <div 
                className="w-10 h-10 rounded-full flex items-center justify-center"
                style={{ background: '#091A7A' }}
              >
                <span style={{ fontSize: '20px' }}>💾</span>
              </div>
            </div>
            <div>
              <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A', marginBottom: '4px' }}>
                Tiny Data Footprint
              </p>
              <p style={{ fontSize: '12px', color: '#6B7280', lineHeight: 1.5 }}>
                Your customized app state is compressed to less than 5KB. 
                Perfect for offline sharing in low-bandwidth areas.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
