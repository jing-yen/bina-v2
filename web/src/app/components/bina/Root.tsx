import { Outlet, useLocation, useNavigate } from 'react-router';
import { LayoutDashboard, Wand2, BarChart3, Cloud } from 'lucide-react';
import { Toaster } from 'sonner';

const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/studio', label: 'Recipe Studio', icon: Wand2 },
  { path: '/analytics', label: 'Analytics', icon: BarChart3 },
  { path: '/sync', label: 'Sync', icon: Cloud },
];

export default function Root() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <div className="flex h-screen w-screen overflow-hidden">
      {/* Sidebar */}
      <aside
        className="flex flex-col justify-between w-[240px] min-w-[240px] p-4"
        style={{ background: '#091A7A' }}
      >
        {/* Top section */}
        <div>
          {/* Logo */}
          <div className="flex items-center gap-3 px-3 pt-2 pb-6">
            <div className="w-9 h-9 rounded-full bg-white/20 flex items-center justify-center">
              <span className="text-white font-bold text-lg">B</span>
            </div>
            <span className="text-white font-semibold text-lg tracking-tight">Bina.ai</span>
          </div>

          {/* Nav items */}
          <nav className="flex flex-col gap-1">
            {NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              const isActive =
                item.path === '/'
                  ? location.pathname === '/'
                  : location.pathname.startsWith(item.path);

              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-left transition-colors"
                  style={{
                    background: isActive ? 'rgba(255,255,255,0.1)' : 'transparent',
                    color: isActive ? '#FFFFFF' : 'rgba(255,255,255,0.6)',
                  }}
                >
                  <Icon size={20} />
                  <span className="text-sm font-medium">{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Bottom badge */}
        <div className="px-3 pb-2">
          <div className="rounded-lg px-3 py-2 text-center" style={{ background: 'rgba(255,255,255,0.08)' }}>
            <span className="text-[11px] text-white/50">Powered by Gemma 4 31B</span>
          </div>
        </div>
      </aside>

      {/* Content area */}
      <main className="flex-1 overflow-y-auto" style={{ background: '#F8FAFC' }}>
        <Outlet />
      </main>

      <Toaster richColors position="top-right" />
    </div>
  );
}
