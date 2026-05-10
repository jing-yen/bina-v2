import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useOutletContext, useNavigate } from 'react-router';
import { UserMode } from './Root';
import { Heart, Download, CheckCircle2, TrendingUp, AlertCircle, Wheat, Stethoscope, GraduationCap, ShoppingCart, ChevronRight, Verified } from 'lucide-react';
import { ImageWithFallback } from '../figma/ImageWithFallback';
import { PreInstallConfigurator } from './PreInstallConfigurator';
import Slider from "react-slick";
import "slick-carousel/slick/slick.css";
import "slick-carousel/slick/slick-theme.css";

interface Recipe {
  id: string;
  name: string;
  description: string;
  domain: string;
  dialect: string;
  size: string;
  downloads: number;
  verified: boolean;
  image: string;
  organization: string;
  trending?: boolean;
  emergency?: boolean;
}

const MOCK_RECIPES: Recipe[] = [
  {
    id: '1',
    name: 'Offline Flood Triage Agent',
    description: 'Emergency medical triage for flood disasters. Works 100% offline.',
    domain: 'Health',
    dialect: 'Bahasa Indonesia',
    size: '1.2KB',
    downloads: 15420,
    verified: true,
    image: 'https://images.unsplash.com/photo-1619719341796-44c4d2e0eb5a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxlbWVyZ2VuY3klMjBtZWRpY2FsJTIwdHJpYWdlJTIwbW9iaWxlJTIwYXBwfGVufDF8fHx8MTc3ODA4ODA5Mnww&ixlib=rb-4.1.0&q=80&w=1080',
    organization: 'WHO Southeast Asia',
    emergency: true,
    trending: true
  },
  {
    id: '2',
    name: 'Buku Kira-Kira',
    description: 'Smart bookkeeping for small businesses. Track sales, inventory, and expenses.',
    domain: 'Business',
    dialect: 'Tagalog',
    size: '0.9KB',
    downloads: 8760,
    verified: true,
    image: 'https://images.unsplash.com/photo-1760532928897-36ba23c0ba41?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzbWFsbCUyMGJ1c2luZXNzJTIwcmV0YWlsJTIwYXNpYXxlbnwxfHx8fDE3NzgwODgwOTJ8MA&ixlib=rb-4.1.0&q=80&w=1080',
    organization: 'Asian Development Bank'
  },
  {
    id: '3',
    name: 'Bidan Pintar',
    description: 'Rural midwife assistant. Prenatal care guidance and emergency protocols.',
    domain: 'Health',
    dialect: 'Javanese',
    size: '1.5KB',
    downloads: 12340,
    verified: true,
    image: 'https://images.unsplash.com/photo-1775795693558-13b8c9afbbc5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxydXJhbCUyMGhlYWx0aGNhcmUlMjB3b3JrZXIlMjBhc2lhfGVufDF8fHx8MTc3ODA4ODA5Mnww&ixlib=rb-4.1.0&q=80&w=1080',
    organization: 'Ministry of Health Indonesia',
    trending: true
  },
  {
    id: '4',
    name: 'Farm Buddy',
    description: 'Crop disease detection and treatment recommendations for small farmers.',
    domain: 'Agriculture',
    dialect: 'Vietnamese',
    size: '2.1KB',
    downloads: 9870,
    verified: true,
    image: 'https://images.unsplash.com/photo-1623927976626-002bd460db22?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxhZ3JpY3VsdHVyZSUyMGZhcm1pbmclMjBzbWFydHBob25lfGVufDF8fHx8MTc3ODA4ODA5Mnww&ixlib=rb-4.1.0&q=80&w=1080',
    organization: 'IRRI Philippines'
  }
];

const DOMAIN_COLORS: Record<string, string> = {
  'Health': '#EF4444',
  'Business': '#10B981',
  'Agriculture': '#F59E0B',
  'Education': '#3B82F6'
};

const DOMAIN_ICONS: Record<string, any> = {
  'Health': Stethoscope,
  'Business': ShoppingCart,
  'Agriculture': Wheat,
  'Education': GraduationCap
};

export function RecipeHub() {
  const { userMode } = useOutletContext<{ userMode: UserMode }>();
  const navigate = useNavigate();
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null);
  const [showConfigurator, setShowConfigurator] = useState(false);
  const [filterDomain, setFilterDomain] = useState<string>('All');

  const sliderSettings = {
    dots: true,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 4000,
    arrows: false,
    dotsClass: "slick-dots custom-dots"
  };

  const domains = ['All', 'Health', 'Agriculture', 'Business', 'Education'];
  
  const filteredRecipes = filterDomain === 'All' 
    ? MOCK_RECIPES 
    : MOCK_RECIPES.filter(r => r.domain === filterDomain);

  const featuredRecipes = MOCK_RECIPES.filter(r => r.trending || r.emergency);

  return (
    <div className="min-h-full pb-6">
      {/* Hero Carousel */}
      <div className="px-6 pt-6 pb-4">
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '12px' }}>
          {userMode === 'architect' ? 'Recipe Marketplace' : 'Discover AI Recipes'}
        </h2>
        
        <div className="rounded-3xl overflow-hidden shadow-elevated">
          <Slider {...sliderSettings}>
            {featuredRecipes.map((recipe) => (
              <div key={recipe.id} className="outline-none">
                <motion.div
                  whileTap={{ scale: 0.98 }}
                  onClick={() => setSelectedRecipe(recipe)}
                  className="relative h-[200px] rounded-3xl overflow-hidden"
                >
                  <ImageWithFallback
                    src={recipe.image}
                    alt={recipe.name}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
                  
                  <div className="absolute bottom-0 left-0 right-0 p-6">
                    {recipe.emergency && (
                      <div className="flex items-center gap-2 mb-2">
                        <div className="px-3 py-1 rounded-full bg-red-500 flex items-center gap-1">
                          <AlertCircle size={14} className="text-white" />
                          <span style={{ fontSize: '10px', color: 'white', fontWeight: 600 }}>EMERGENCY</span>
                        </div>
                      </div>
                    )}
                    <h3 style={{ fontSize: '18px', fontWeight: 600, color: 'white', marginBottom: '4px' }}>
                      {recipe.name}
                    </h3>
                    <p style={{ fontSize: '12px', color: 'rgba(255,255,255,0.9)' }}>
                      {recipe.description}
                    </p>
                  </div>
                </motion.div>
              </div>
            ))}
          </Slider>
        </div>
      </div>

      {/* Domain Filters */}
      <div className="px-6 pb-4">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-2">
          {domains.map((domain) => {
            const isActive = filterDomain === domain;
            return (
              <motion.button
                key={domain}
                whileTap={{ scale: 0.95 }}
                onClick={() => setFilterDomain(domain)}
                className="px-4 py-2 rounded-full whitespace-nowrap border border-white/20 min-h-[44px]"
                style={{
                  background: isActive 
                    ? 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)'
                    : 'rgba(255, 255, 255, 0.6)',
                  backdropFilter: 'blur(12px)',
                  fontSize: '12px',
                  fontWeight: 500,
                  color: isActive ? 'white' : '#091A7A'
                }}
              >
                {domain}
              </motion.button>
            );
          })}
        </div>
      </div>

      {/* Recipe Grid */}
      <div className="px-6 space-y-4">
        {userMode === 'architect' && (
          <div className="flex items-center justify-between mb-2">
            <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A' }}>All Recipes</h3>
            <motion.button
              whileTap={{ scale: 0.95 }}
              className="px-4 py-2 rounded-full min-h-[44px]"
              style={{
                background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
                fontSize: '12px',
                fontWeight: 600,
                color: 'white'
              }}
            >
              My Published
            </motion.button>
          </div>
        )}

        {filteredRecipes.map((recipe) => {
          const DomainIcon = DOMAIN_ICONS[recipe.domain];
          const domainColor = DOMAIN_COLORS[recipe.domain];
          
          return (
            <motion.div
              key={recipe.id}
              whileTap={{ scale: 0.98 }}
              onClick={() => setSelectedRecipe(recipe)}
              className="rounded-3xl p-4 border border-white/20 shadow-card"
              style={{
                background: 'rgba(255, 255, 255, 0.9)',
                backdropFilter: 'blur(20px)'
              }}
            >
              <div className="flex gap-4">
                <div className="w-20 h-20 rounded-2xl overflow-hidden flex-shrink-0">
                  <ImageWithFallback
                    src={recipe.image}
                    alt={recipe.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2 mb-1">
                    <h4 style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                      {recipe.name}
                    </h4>
                    {recipe.verified && (
                      <CheckCircle2 size={16} className="text-blue-500 flex-shrink-0" />
                    )}
                  </div>
                  
                  <p style={{ fontSize: '12px', color: '#6B7280', marginBottom: '8px' }}>
                    {recipe.description.substring(0, 60)}...
                  </p>
                  
                  <div className="flex items-center gap-2 flex-wrap">
                    <div 
                      className="px-2 py-1 rounded-lg flex items-center gap-1"
                      style={{ background: `${domainColor}20` }}
                    >
                      <DomainIcon size={12} style={{ color: domainColor }} />
                      <span style={{ fontSize: '10px', color: domainColor, fontWeight: 600 }}>
                        {recipe.domain}
                      </span>
                    </div>
                    
                    <div className="flex items-center gap-1">
                      <Download size={12} className="text-gray-500" />
                      <span style={{ fontSize: '10px', color: '#6B7280' }}>
                        {(recipe.downloads / 1000).toFixed(1)}K
                      </span>
                    </div>
                    
                    <span style={{ fontSize: '10px', color: '#6B7280' }}>•</span>
                    
                    <span style={{ fontSize: '10px', color: '#6B7280' }}>
                      {recipe.size}
                    </span>
                  </div>
                </div>
                
                <ChevronRight size={20} className="text-gray-400 flex-shrink-0" />
              </div>
            </motion.div>
          );
        })}
      </div>

      {/* Recipe Detail Modal */}
      {selectedRecipe && !showConfigurator && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/60 z-50 flex items-end"
          onClick={() => setSelectedRecipe(null)}
        >
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            className="w-full rounded-t-3xl overflow-hidden"
            style={{ background: 'white', maxHeight: '80vh' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="relative h-[200px]">
              <ImageWithFallback
                src={selectedRecipe.image}
                alt={selectedRecipe.name}
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
            </div>
            
            <div className="p-6 space-y-4">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h3 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '4px' }}>
                    {selectedRecipe.name}
                  </h3>
                  <div className="flex items-center gap-2">
                    <CheckCircle2 size={14} className="text-blue-500" />
                    <span style={{ fontSize: '12px', color: '#6B7280' }}>
                      Verified by {selectedRecipe.organization}
                    </span>
                  </div>
                </div>
                
                <motion.button whileTap={{ scale: 0.95 }} className="p-2">
                  <Heart size={24} className="text-gray-400" />
                </motion.button>
              </div>
              
              <p style={{ fontSize: '14px', color: '#6B7280', lineHeight: 1.6 }}>
                {selectedRecipe.description}
              </p>
              
              <div className="grid grid-cols-3 gap-4 py-4">
                <div className="text-center">
                  <p style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A' }}>
                    {(selectedRecipe.downloads / 1000).toFixed(1)}K
                  </p>
                  <p style={{ fontSize: '12px', color: '#6B7280' }}>Downloads</p>
                </div>
                <div className="text-center">
                  <p style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A' }}>
                    {selectedRecipe.size}
                  </p>
                  <p style={{ fontSize: '12px', color: '#6B7280' }}>Recipe Size</p>
                </div>
                <div className="text-center">
                  <p style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A' }}>4.8</p>
                  <p style={{ fontSize: '12px', color: '#6B7280' }}>Rating</p>
                </div>
              </div>
              
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span style={{ fontSize: '12px', color: '#6B7280' }}>Domain:</span>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: '#091A7A' }}>
                    {selectedRecipe.domain}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span style={{ fontSize: '12px', color: '#6B7280' }}>Dialect:</span>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: '#091A7A' }}>
                    {selectedRecipe.dialect}
                  </span>
                </div>
              </div>
              
              <motion.button
                whileTap={{ scale: 0.95 }}
                className="w-full py-4 rounded-2xl shadow-lg min-h-[56px]"
                style={{
                  background: 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)',
                  fontSize: '16px',
                  fontWeight: 600,
                  color: 'white'
                }}
                onClick={() => setShowConfigurator(true)}
              >
                Configure & Install
              </motion.button>
            </div>
          </motion.div>
        </motion.div>
      )}

      {/* Pre-Install Configurator */}
      <AnimatePresence>
        {showConfigurator && selectedRecipe && (
          <PreInstallConfigurator
            recipeName={selectedRecipe.name}
            recipeOrganization={selectedRecipe.organization}
            onInstall={(selectedFeatures) => {
              setShowConfigurator(false);
              setSelectedRecipe(null);
              // Navigate to My Pocket after successful installation
              navigate('/my-pocket');
            }}
            onCancel={() => setShowConfigurator(false)}
          />
        )}
      </AnimatePresence>

      {/* Custom Slider Dots Styling */}
      <style>{`
        .custom-dots {
          bottom: 12px !important;
        }
        .custom-dots li button:before {
          color: white !important;
          opacity: 0.5 !important;
        }
        .custom-dots li.slick-active button:before {
          opacity: 1 !important;
        }
      `}</style>
    </div>
  );
}
