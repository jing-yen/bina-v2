import { motion } from 'motion/react';
import { TrendingUp, Download, Globe, Users, BarChart3, Award } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Cell } from 'recharts';

const DOWNLOAD_DATA = [
  { month: 'Jan', downloads: 1200 },
  { month: 'Feb', downloads: 1900 },
  { month: 'Mar', downloads: 2500 },
  { month: 'Apr', downloads: 3200 },
  { month: 'May', downloads: 4100 },
  { month: 'Jun', downloads: 5400 },
];

const REGIONAL_DATA = [
  { region: 'Indonesia', percentage: 45, count: 6800 },
  { region: 'Philippines', percentage: 28, count: 4200 },
  { region: 'Vietnam', percentage: 18, count: 2700 },
  { region: 'Thailand', percentage: 9, count: 1300 },
];

const MY_RECIPES = [
  { 
    id: '1', 
    name: 'Bidan Pintar', 
    downloads: 12340, 
    rating: 4.8,
    growth: '+24%',
    dialects: ['Javanese', 'Indonesian']
  },
  { 
    id: '2', 
    name: 'Farm Buddy', 
    downloads: 9870, 
    rating: 4.6,
    growth: '+18%',
    dialects: ['Vietnamese', 'Thai']
  },
];

export function Analytics() {
  const totalDownloads = MY_RECIPES.reduce((sum, r) => sum + r.downloads, 0);
  const avgRating = (MY_RECIPES.reduce((sum, r) => sum + r.rating, 0) / MY_RECIPES.length).toFixed(1);

  return (
    <div className="min-h-full pb-6">
      <div className="px-6 pt-6 pb-4">
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '8px' }}>
          Analytics Dashboard
        </h2>
        <p style={{ fontSize: '14px', color: '#6B7280' }}>
          Track your published recipes' performance
        </p>
      </div>

      {/* Key Metrics */}
      <div className="px-6 pb-4">
        <div className="grid grid-cols-2 gap-4">
          {/* Total Downloads */}
          <motion.div
            whileTap={{ scale: 0.98 }}
            className="p-4 rounded-3xl border border-white/20"
            style={{
              background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(16, 185, 129, 0.05) 100%)'
            }}
          >
            <div className="flex items-center gap-2 mb-2">
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ background: '#10B981' }}
              >
                <Download size={20} className="text-white" />
              </div>
            </div>
            <p style={{ fontSize: '24px', fontWeight: 600, color: '#091A7A', marginBottom: '2px' }}>
              {(totalDownloads / 1000).toFixed(1)}K
            </p>
            <p style={{ fontSize: '12px', color: '#6B7280' }}>
              Total Downloads
            </p>
            <div className="flex items-center gap-1 mt-2">
              <TrendingUp size={14} className="text-green-600" />
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#10B981' }}>
                +21% this month
              </span>
            </div>
          </motion.div>

          {/* Average Rating */}
          <motion.div
            whileTap={{ scale: 0.98 }}
            className="p-4 rounded-3xl border border-white/20"
            style={{
              background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(245, 158, 11, 0.05) 100%)'
            }}
          >
            <div className="flex items-center gap-2 mb-2">
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ background: '#F59E0B' }}
              >
                <Award size={20} className="text-white" />
              </div>
            </div>
            <p style={{ fontSize: '24px', fontWeight: 600, color: '#091A7A', marginBottom: '2px' }}>
              {avgRating}
            </p>
            <p style={{ fontSize: '12px', color: '#6B7280' }}>
              Average Rating
            </p>
            <div className="flex gap-0.5 mt-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <span key={star} style={{ color: '#F59E0B', fontSize: '14px' }}>★</span>
              ))}
            </div>
          </motion.div>

          {/* Active Users */}
          <motion.div
            whileTap={{ scale: 0.98 }}
            className="p-4 rounded-3xl border border-white/20"
            style={{
              background: 'linear-gradient(135deg, rgba(9, 26, 122, 0.15) 0%, rgba(9, 26, 122, 0.05) 100%)'
            }}
          >
            <div className="flex items-center gap-2 mb-2">
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ background: '#091A7A' }}
              >
                <Users size={20} className="text-white" />
              </div>
            </div>
            <p style={{ fontSize: '24px', fontWeight: 600, color: '#091A7A', marginBottom: '2px' }}>
              8.4K
            </p>
            <p style={{ fontSize: '12px', color: '#6B7280' }}>
              Active Users
            </p>
            <div className="flex items-center gap-1 mt-2">
              <TrendingUp size={14} className="text-green-600" />
              <span style={{ fontSize: '12px', fontWeight: 600, color: '#10B981' }}>
                +15% weekly
              </span>
            </div>
          </motion.div>

          {/* Regions */}
          <motion.div
            whileTap={{ scale: 0.98 }}
            className="p-4 rounded-3xl border border-white/20"
            style={{
              background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.05) 100%)'
            }}
          >
            <div className="flex items-center gap-2 mb-2">
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ background: '#3B82F6' }}
              >
                <Globe size={20} className="text-white" />
              </div>
            </div>
            <p style={{ fontSize: '24px', fontWeight: 600, color: '#091A7A', marginBottom: '2px' }}>
              12
            </p>
            <p style={{ fontSize: '12px', color: '#6B7280' }}>
              Countries Reached
            </p>
            <p style={{ fontSize: '10px', color: '#3B82F6', marginTop: '4px' }}>
              Southeast Asia focused
            </p>
          </motion.div>
        </div>
      </div>

      {/* Downloads Chart */}
      <div className="px-6 pb-4">
        <div 
          className="p-6 rounded-3xl border border-white/20"
          style={{
            background: 'rgba(255, 255, 255, 0.9)',
            backdropFilter: 'blur(20px)'
          }}
        >
          <div className="flex items-center justify-between mb-4">
            <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A' }}>
              Download Trends
            </h3>
            <BarChart3 size={20} className="text-gray-400" />
          </div>
          
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={DOWNLOAD_DATA}>
                <XAxis 
                  dataKey="month" 
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#6B7280', fontSize: 12 }}
                />
                <YAxis 
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#6B7280', fontSize: 12 }}
                />
                <Bar dataKey="downloads" radius={[8, 8, 0, 0]}>
                  {DOWNLOAD_DATA.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill="#091A7A" />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Regional Distribution */}
      <div className="px-6 pb-4">
        <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '12px' }}>
          Regional Distribution
        </h3>
        
        <div className="space-y-3">
          {REGIONAL_DATA.map((region, index) => (
            <div 
              key={index}
              className="p-4 rounded-2xl border border-white/20"
              style={{
                background: 'rgba(255, 255, 255, 0.9)',
                backdropFilter: 'blur(20px)'
              }}
            >
              <div className="flex items-center justify-between mb-2">
                <span style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                  {region.region}
                </span>
                <span style={{ fontSize: '12px', color: '#6B7280' }}>
                  {region.count.toLocaleString()} downloads
                </span>
              </div>
              
              <div className="flex items-center gap-3">
                <div 
                  className="flex-1 h-2 rounded-full overflow-hidden"
                  style={{ background: '#E5E7EB' }}
                >
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${region.percentage}%` }}
                    transition={{ duration: 1, delay: index * 0.1 }}
                    className="h-full rounded-full"
                    style={{ background: '#091A7A' }}
                  />
                </div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: '#091A7A', minWidth: '40px' }}>
                  {region.percentage}%
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* My Published Recipes */}
      <div className="px-6 pb-4">
        <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '12px' }}>
          My Published Recipes
        </h3>
        
        <div className="space-y-3">
          {MY_RECIPES.map((recipe) => (
            <motion.div
              key={recipe.id}
              whileTap={{ scale: 0.98 }}
              className="p-4 rounded-3xl border border-white/20"
              style={{
                background: 'rgba(255, 255, 255, 0.9)',
                backdropFilter: 'blur(20px)'
              }}
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex-1">
                  <h4 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '4px' }}>
                    {recipe.name}
                  </h4>
                  <div className="flex items-center gap-2">
                    <div className="flex gap-0.5">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <span 
                          key={star} 
                          style={{ 
                            color: star <= recipe.rating ? '#F59E0B' : '#E5E7EB',
                            fontSize: '12px'
                          }}
                        >
                          ★
                        </span>
                      ))}
                    </div>
                    <span style={{ fontSize: '12px', color: '#6B7280' }}>
                      {recipe.rating}
                    </span>
                  </div>
                </div>
                
                <div 
                  className="px-3 py-1 rounded-full flex items-center gap-1"
                  style={{ background: '#10B98120' }}
                >
                  <TrendingUp size={12} className="text-green-600" />
                  <span style={{ fontSize: '12px', fontWeight: 600, color: '#10B981' }}>
                    {recipe.growth}
                  </span>
                </div>
              </div>
              
              <div className="flex items-center gap-4 mb-3">
                <div className="flex items-center gap-2">
                  <Download size={16} className="text-gray-400" />
                  <span style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                    {recipe.downloads.toLocaleString()}
                  </span>
                </div>
                
                <div className="flex items-center gap-2">
                  <Globe size={16} className="text-gray-400" />
                  <span style={{ fontSize: '14px', color: '#6B7280' }}>
                    {recipe.dialects.length} dialects
                  </span>
                </div>
              </div>
              
              <div className="flex gap-2 flex-wrap">
                {recipe.dialects.map((dialect, index) => (
                  <div 
                    key={index}
                    className="px-3 py-1 rounded-full"
                    style={{ background: '#091A7A15' }}
                  >
                    <span style={{ fontSize: '10px', fontWeight: 500, color: '#091A7A' }}>
                      {dialect}
                    </span>
                  </div>
                ))}
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Achievement Badge */}
      <div className="px-6">
        <div 
          className="p-6 rounded-3xl border border-white/20"
          style={{
            background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(245, 158, 11, 0.05) 100%)'
          }}
        >
          <div className="flex gap-4">
            <div 
              className="w-16 h-16 rounded-2xl flex items-center justify-center flex-shrink-0"
              style={{ background: '#F59E0B' }}
            >
              <Award size={32} className="text-white" />
            </div>
            <div>
              <h4 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '4px' }}>
                Top Contributor
              </h4>
              <p style={{ fontSize: '12px', color: '#6B7280', lineHeight: 1.5 }}>
                Your recipes have reached over 20K users across Southeast Asia. 
                Thank you for empowering grassroots communities!
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
