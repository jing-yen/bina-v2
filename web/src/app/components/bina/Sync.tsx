import { RefreshCw, Wifi, Smartphone, Laptop, Tablet, Monitor, Clock, CheckCircle2, AlertCircle } from 'lucide-react';

const DEVICES = [
  { name: 'iPhone 15 Pro', type: 'phone', lastSynced: '2 min ago', recipes: 48, status: 'synced' },
  { name: 'MacBook Pro', type: 'laptop', lastSynced: '5 min ago', recipes: 48, status: 'synced' },
  { name: 'iPad Air', type: 'tablet', lastSynced: '1 hour ago', recipes: 45, status: 'pending' },
  { name: 'Kitchen Display', type: 'monitor', lastSynced: '3 hours ago', recipes: 42, status: 'synced' },
];

const SYNC_LOG = [
  { time: '10:32 AM', event: 'Synced 3 new recipes to iPhone 15 Pro' },
  { time: '10:30 AM', event: 'Synced 3 new recipes to MacBook Pro' },
  { time: '9:15 AM', event: 'Updated "Nasi Goreng Kampung" across all devices' },
  { time: '8:45 AM', event: 'Synced recipe edits to iPad Air' },
  { time: 'Yesterday', event: 'Full sync completed — 48 recipes across 4 devices' },
  { time: 'Yesterday', event: 'New device added: Kitchen Display' },
];

const DEVICE_ICONS: Record<string, React.ReactNode> = {
  phone: <Smartphone className="w-5 h-5" />,
  laptop: <Laptop className="w-5 h-5" />,
  tablet: <Tablet className="w-5 h-5" />,
  monitor: <Monitor className="w-5 h-5" />,
};

export function Sync() {
  return (
    <div className="p-8 max-w-6xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Sync Center</h1>
        <p className="text-sm text-gray-500 mt-1">Keep your recipes in sync across all your devices</p>
      </div>

      {/* Status Card */}
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-6 mb-8">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-6">
            <div className="w-12 h-12 rounded-xl flex items-center justify-center" style={{ background: '#091A7A15' }}>
              <RefreshCw className="w-6 h-6" style={{ color: '#091A7A' }} />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-medium text-gray-900">Last synced:</span>
                <span className="text-sm text-gray-500">Today at 10:32 AM</span>
              </div>
              <div className="flex items-center gap-2">
                <Wifi className="w-4 h-4 text-green-500" />
                <span className="text-sm text-green-600 font-medium">Online — All systems operational</span>
              </div>
            </div>
          </div>
          <button
            className="px-5 py-2.5 rounded-lg text-white text-sm font-medium flex items-center gap-2 hover:opacity-90 transition-opacity cursor-pointer"
            style={{ background: '#091A7A' }}
          >
            <RefreshCw className="w-4 h-4" />
            Sync Now
          </button>
        </div>
      </div>

      {/* Devices */}
      <div className="mb-8">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Connected Devices</h2>
        <div className="grid grid-cols-2 gap-4">
          {DEVICES.map((device) => (
            <div key={device.name} className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg flex items-center justify-center bg-gray-50 text-gray-600">
                    {DEVICE_ICONS[device.type]}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{device.name}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{device.recipes} recipes</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  {device.status === 'synced' ? (
                    <CheckCircle2 className="w-4 h-4 text-green-500" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-amber-500" />
                  )}
                  <span className={`text-xs font-medium ${device.status === 'synced' ? 'text-green-600' : 'text-amber-600'}`}>
                    {device.status === 'synced' ? 'Synced' : 'Pending'}
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-1.5 mt-3 pt-3 border-t border-gray-50">
                <Clock className="w-3.5 h-3.5 text-gray-400" />
                <span className="text-xs text-gray-500">Last synced {device.lastSynced}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Sync Log */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h2>
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm divide-y divide-gray-50">
          {SYNC_LOG.map((entry, i) => (
            <div key={i} className="flex items-center gap-4 px-5 py-3.5">
              <span className="text-xs text-gray-400 w-20 shrink-0 font-medium">{entry.time}</span>
              <span className="text-sm text-gray-700">{entry.event}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
