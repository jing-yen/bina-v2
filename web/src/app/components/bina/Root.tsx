import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { LayoutDashboard, Wand2, BarChart3, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { Toaster } from 'sonner';

const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/studio', label: 'Recipe Studio', icon: Wand2 },
  { path: '/analytics', label: 'Analytics', icon: BarChart3 },
];

export default function Root() {
  const location = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="flex min-h-[100dvh] w-full overflow-hidden">
      <aside
        className="relative flex flex-col justify-between p-4 transition-all duration-200 overflow-hidden"
        style={{ transitionTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)', background: '#1C1917', width: collapsed ? 64 : 240, minWidth: collapsed ? 64 : 240 }}
      >
        <div className="absolute bottom-0 left-0 right-0 h-32 pointer-events-none" style={{ background: 'linear-gradient(to top, rgba(196,90,58,0.12) 0%, transparent 100%)' }} />
        <div>
          <div className={`flex ${collapsed ? 'items-center justify-center' : 'items-center px-3 gap-3'} pt-2 pb-6`}>
            <div
              className={`${collapsed ? 'w-10 h-10 rounded-xl' : 'w-[64px] h-[64px] rounded-2xl'} shrink-0 overflow-hidden`}
              style={{ boxShadow: '0 0 20px rgba(255,255,255,0.25), 0 0 40px rgba(196,90,58,0.15)' }}
            >
              <img src="/bina-icon.png" alt="Bina" className="w-full h-full object-cover scale-[1.25]" />
            </div>
            {!collapsed && <span className="text-white font-bold text-4xl" style={{ fontFamily: "'Playfair Display', serif", letterSpacing: '-0.02em' }}>Bina</span>}
          </div>

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
                  title={collapsed ? item.label : undefined}
                  className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3 px-3'} py-2.5 rounded-lg text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/30`}
                  style={{
                    background: isActive ? 'rgba(196,90,58,0.15)' : 'transparent',
                    color: isActive ? '#FAF8F5' : 'rgba(255,255,255,0.5)',
                    boxShadow: isActive ? 'inset 3px 0 0 #C45A3A' : 'none',
                  }}
                >
                  <Icon size={20} className="shrink-0" />
                  {!collapsed && <span className="text-sm font-medium">{item.label}</span>}
                </button>
              );
            })}
          </nav>
        </div>

        <div className="flex flex-col gap-2">
          {!collapsed && (
            <div className="px-3">
              <div className="rounded-lg px-3 py-2 text-center" style={{ background: 'rgba(255,255,255,0.08)' }}>
                <span className="text-[11px] text-white/50">Powered by Gemma 4 31B</span>
              </div>
            </div>
          )}
          <button
            onClick={() => setCollapsed(c => !c)}
            className="flex items-center justify-center py-2 rounded-lg transition-colors hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/30"
            style={{ color: 'rgba(255,255,255,0.5)' }}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
          </button>
        </div>
      </aside>

      <main className="flex-1 overflow-y-auto" style={{ background: '#FAF8F5' }}>
        <Outlet />
      </main>

      <Toaster richColors position="top-right" />
    </div>
  );
}
