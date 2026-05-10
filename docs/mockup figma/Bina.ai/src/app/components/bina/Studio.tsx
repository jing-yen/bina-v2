import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { FileText, Upload, CheckSquare, Camera, MessageSquare, FileUp, Smartphone, Zap, Eye } from 'lucide-react';

type StudioStep = 1 | 2 | 3;

interface WidgetOption {
  id: string;
  name: string;
  icon: any;
  description: string;
  enabled: boolean;
}

const WIDGET_OPTIONS: WidgetOption[] = [
  { id: 'camera', name: 'Camera', icon: Camera, description: 'Allow photo capture', enabled: false },
  { id: 'sms', name: 'SMS Dispatch', icon: MessageSquare, description: 'Send text messages', enabled: false },
  { id: 'voice', name: 'Voice Input', icon: Zap, description: 'Voice commands', enabled: true },
  { id: 'scanner', name: 'Document Scanner', icon: FileUp, description: 'Scan documents', enabled: false },
];

export function Studio() {
  const [currentStep, setCurrentStep] = useState<StudioStep>(1);
  const [systemPrompt, setSystemPrompt] = useState('');
  const [widgets, setWidgets] = useState<WidgetOption[]>(WIDGET_OPTIONS);
  const [showSimulator, setShowSimulator] = useState(false);

  const toggleWidget = (id: string) => {
    setWidgets(widgets.map(w => w.id === id ? { ...w, enabled: !w.enabled } : w));
  };

  const steps = [
    { id: 1, label: 'Identity', icon: FileText },
    { id: 2, label: 'Knowledge', icon: Upload },
    { id: 3, label: 'Widgets', icon: CheckSquare },
  ];

  return (
    <div className="min-h-full pb-6">
      <div className="px-6 pt-6 pb-4">
        <h2 style={{ fontSize: '20px', fontWeight: 600, color: '#091A7A', marginBottom: '8px' }}>
          Recipe Studio
        </h2>
        <p style={{ fontSize: '14px', color: '#6B7280' }}>
          Create AI recipes for grassroots users
        </p>
      </div>

      {/* Progress Steps */}
      <div className="px-6 pb-6">
        <div className="flex items-center justify-between">
          {steps.map((step, index) => {
            const StepIcon = step.icon;
            const isActive = currentStep === step.id;
            const isCompleted = currentStep > step.id;
            
            return (
              <div key={step.id} className="flex items-center flex-1">
                <motion.button
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setCurrentStep(step.id as StudioStep)}
                  className="flex flex-col items-center gap-2"
                >
                  <div 
                    className="w-12 h-12 rounded-full flex items-center justify-center border-2 transition-all"
                    style={{
                      background: isActive || isCompleted ? '#091A7A' : 'rgba(255, 255, 255, 0.9)',
                      borderColor: isActive || isCompleted ? '#091A7A' : '#E5E7EB'
                    }}
                  >
                    <StepIcon 
                      size={20} 
                      style={{ color: isActive || isCompleted ? 'white' : '#6B7280' }} 
                    />
                  </div>
                  <span style={{ 
                    fontSize: '10px', 
                    fontWeight: isActive ? 600 : 400,
                    color: isActive ? '#091A7A' : '#6B7280'
                  }}>
                    {step.label}
                  </span>
                </motion.button>
                
                {index < steps.length - 1 && (
                  <div 
                    className="flex-1 h-0.5 mx-2 transition-all"
                    style={{ 
                      background: currentStep > step.id ? '#091A7A' : '#E5E7EB',
                      marginTop: '-24px'
                    }}
                  />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Step Content */}
      <AnimatePresence mode="wait">
        <motion.div
          key={currentStep}
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: -20 }}
          transition={{ duration: 0.3 }}
          className="px-6"
        >
          {/* Step 1: Identity */}
          {currentStep === 1 && (
            <div className="space-y-4">
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '8px' }}>
                  Define System Prompt & Guardrails
                </h3>
                <p style={{ fontSize: '12px', color: '#6B7280', marginBottom: '12px' }}>
                  Set the AI's personality, expertise, and safety boundaries
                </p>
              </div>

              <div 
                className="rounded-3xl p-4 border border-white/20"
                style={{
                  background: 'rgba(255, 255, 255, 0.9)',
                  backdropFilter: 'blur(20px)'
                }}
              >
                <label style={{ fontSize: '12px', fontWeight: 500, color: '#091A7A', marginBottom: '8px', display: 'block' }}>
                  System Prompt
                </label>
                <textarea
                  value={systemPrompt}
                  onChange={(e) => setSystemPrompt(e.target.value)}
                  placeholder="You are a helpful medical assistant for rural midwives in Indonesia. You provide evidence-based prenatal care guidance..."
                  rows={8}
                  className="w-full rounded-2xl p-4 border border-gray-200 resize-none"
                  style={{
                    fontSize: '14px',
                    color: '#091A7A',
                    background: 'white'
                  }}
                />
              </div>

              <div 
                className="p-4 rounded-2xl border border-white/20"
                style={{ background: 'rgba(173, 200, 255, 0.2)' }}
              >
                <p style={{ fontSize: '12px', color: '#091A7A', lineHeight: 1.5 }}>
                  💡 <strong>Tip:</strong> Be specific about the user's context, language level, and safety constraints. 
                  This prompt runs entirely on-device.
                </p>
              </div>
            </div>
          )}

          {/* Step 2: Knowledge */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '8px' }}>
                  Upload Knowledge Base
                </h3>
                <p style={{ fontSize: '12px', color: '#6B7280', marginBottom: '12px' }}>
                  Add PDFs, text files, or databases to enhance the AI's knowledge
                </p>
              </div>

              {/* Upload Area */}
              <motion.div
                whileTap={{ scale: 0.98 }}
                className="rounded-3xl p-8 border-2 border-dashed border-gray-300 flex flex-col items-center gap-3"
                style={{ background: 'rgba(255, 255, 255, 0.6)' }}
              >
                <div 
                  className="w-16 h-16 rounded-2xl flex items-center justify-center"
                  style={{ background: '#091A7A20' }}
                >
                  <Upload size={32} className="text-[#091A7A]" />
                </div>
                <div className="text-center">
                  <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A', marginBottom: '4px' }}>
                    Drop files here or tap to browse
                  </p>
                  <p style={{ fontSize: '12px', color: '#6B7280' }}>
                    PDF, TXT, CSV up to 10MB
                  </p>
                </div>
              </motion.div>

              {/* Uploaded Files */}
              <div className="space-y-2">
                {[
                  { name: 'WHO_Prenatal_Guidelines.pdf', size: '2.4 MB' },
                  { name: 'Emergency_Protocols_ID.pdf', size: '1.8 MB' }
                ].map((file, index) => (
                  <div 
                    key={index}
                    className="flex items-center gap-3 p-4 rounded-2xl border border-white/20"
                    style={{ background: 'rgba(255, 255, 255, 0.9)' }}
                  >
                    <div 
                      className="w-10 h-10 rounded-xl flex items-center justify-center"
                      style={{ background: '#10B98120' }}
                    >
                      <FileText size={20} className="text-green-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {file.name}
                      </p>
                      <p style={{ fontSize: '12px', color: '#6B7280' }}>
                        {file.size}
                      </p>
                    </div>
                    <CheckSquare size={20} className="text-green-600" />
                  </div>
                ))}
              </div>

              <div 
                className="p-4 rounded-2xl border border-white/20"
                style={{ background: 'rgba(173, 200, 255, 0.2)' }}
              >
                <p style={{ fontSize: '12px', color: '#091A7A', lineHeight: 1.5 }}>
                  📚 Files are embedded locally using LiteRT. The AI can reference this knowledge even offline.
                </p>
              </div>
            </div>
          )}

          {/* Step 3: Widget Arsenal */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A', marginBottom: '8px' }}>
                  Widget Permissions
                </h3>
                <p style={{ fontSize: '12px', color: '#6B7280', marginBottom: '12px' }}>
                  Choose which physical widgets the AI can use
                </p>
              </div>

              <div className="space-y-3">
                {widgets.map((widget) => {
                  const WidgetIcon = widget.icon;
                  return (
                    <motion.button
                      key={widget.id}
                      whileTap={{ scale: 0.98 }}
                      onClick={() => toggleWidget(widget.id)}
                      className="w-full p-4 rounded-2xl border border-white/20 flex items-center gap-4"
                      style={{
                        background: widget.enabled 
                          ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(16, 185, 129, 0.05) 100%)'
                          : 'rgba(255, 255, 255, 0.9)',
                        borderColor: widget.enabled ? '#10B98140' : 'rgba(255, 255, 255, 0.2)'
                      }}
                    >
                      <div 
                        className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
                        style={{ 
                          background: widget.enabled ? '#10B981' : '#E5E7EB'
                        }}
                      >
                        <WidgetIcon size={24} style={{ color: widget.enabled ? 'white' : '#6B7280' }} />
                      </div>
                      <div className="flex-1 text-left">
                        <p style={{ fontSize: '14px', fontWeight: 600, color: '#091A7A' }}>
                          {widget.name}
                        </p>
                        <p style={{ fontSize: '12px', color: '#6B7280' }}>
                          {widget.description}
                        </p>
                      </div>
                      <div 
                        className="w-12 h-6 rounded-full transition-all flex-shrink-0"
                        style={{ 
                          background: widget.enabled ? '#10B981' : '#E5E7EB',
                          position: 'relative'
                        }}
                      >
                        <motion.div 
                          animate={{ x: widget.enabled ? 24 : 0 }}
                          transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                          className="w-6 h-6 rounded-full bg-white shadow-lg"
                        />
                      </div>
                    </motion.button>
                  );
                })}
              </div>
            </div>
          )}
        </motion.div>
      </AnimatePresence>

      {/* Bottom Actions */}
      <div className="px-6 pt-6 space-y-3">
        {/* Simulator Button */}
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => setShowSimulator(!showSimulator)}
          className="w-full py-4 rounded-2xl border border-white/20 flex items-center justify-center gap-2 min-h-[56px]"
          style={{
            background: 'rgba(255, 255, 255, 0.9)',
            backdropFilter: 'blur(20px)'
          }}
        >
          <Eye size={20} className="text-[#091A7A]" />
          <span style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A' }}>
            Test in Simulator
          </span>
        </motion.button>

        {/* Navigation Buttons */}
        <div className="flex gap-3">
          {currentStep > 1 && (
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => setCurrentStep((currentStep - 1) as StudioStep)}
              className="flex-1 py-4 rounded-2xl border border-white/20 min-h-[56px]"
              style={{
                background: 'rgba(255, 255, 255, 0.9)',
                fontSize: '16px',
                fontWeight: 600,
                color: '#091A7A'
              }}
            >
              Previous
            </motion.button>
          )}
          
          {currentStep < 3 ? (
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => setCurrentStep((currentStep + 1) as StudioStep)}
              className="flex-1 py-4 rounded-2xl shadow-lg min-h-[56px]"
              style={{
                background: 'linear-gradient(135deg, #091A7A 0%, #1E3A8A 100%)',
                fontSize: '16px',
                fontWeight: 600,
                color: 'white'
              }}
            >
              Next Step
            </motion.button>
          ) : (
            <motion.button
              whileTap={{ scale: 0.95 }}
              className="flex-1 py-4 rounded-2xl shadow-lg min-h-[56px]"
              style={{
                background: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
                fontSize: '16px',
                fontWeight: 600,
                color: 'white'
              }}
            >
              Publish to Hub
            </motion.button>
          )}
        </div>
      </div>

      {/* Simulator Preview */}
      {showSimulator && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4"
          onClick={() => setShowSimulator(false)}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="w-full max-w-sm rounded-3xl overflow-hidden"
            style={{ background: 'white' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b border-gray-200 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Smartphone size={20} className="text-[#091A7A]" />
                <span style={{ fontSize: '16px', fontWeight: 600, color: '#091A7A' }}>
                  Simulator Preview
                </span>
              </div>
              <motion.button 
                whileTap={{ scale: 0.9 }}
                onClick={() => setShowSimulator(false)}
                className="w-8 h-8 rounded-full flex items-center justify-center"
                style={{ background: '#F3F4F6' }}
              >
                ✕
              </motion.button>
            </div>
            <div className="p-6">
              <p style={{ fontSize: '14px', color: '#6B7280', textAlign: 'center' }}>
                Simulator will show how your recipe renders the UI based on the system prompt and enabled widgets.
              </p>
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  );
}
