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
        className="flex flex-col justify-between p-4 transition-all duration-200"
        style={{ transitionTimingFunction: 'cubic-bezier(0.16, 1, 0.3, 1)', background: '#1C1917', width: collapsed ? 64 : 240, minWidth: collapsed ? 64 : 240 }}
      >
        <div>
          <div className={`flex items-center ${collapsed ? 'justify-center' : 'gap-3 px-3'} pt-2 pb-6`}>
            <div className="w-9 h-9 rounded-full bg-white/20 flex items-center justify-center shrink-0">
              <span className="text-white font-bold text-lg">B</span>
            </div>
            {!collapsed && <span className="text-white font-semibold text-lg tracking-tight">Bina.ai</span>}
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
                    background: isActive ? 'rgba(255,255,255,0.1)' : 'transparent',
                    color: isActive ? '#FAF8F5' : 'rgba(255,255,255,0.6)',
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
