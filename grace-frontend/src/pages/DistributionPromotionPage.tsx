import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Icon } from '@/components/ui/Icon'

// Step types
type StepId = 1 | 2 | 3

interface StepConfig {
  id: StepId
  label: string
}

const STEPS: StepConfig[] = [
  { id: 1, label: '确认信息' },
  { id: 2, label: '选择平台' },
  { id: 3, label: '推广配置' },
]

// StepIndicator Component
interface StepIndicatorProps {
  steps: StepConfig[]
  currentStep: StepId
}

function StepIndicator({ steps, currentStep }: StepIndicatorProps) {
  const getStepState = (stepId: StepId): 'completed' | 'active' | 'future' => {
    if (stepId < currentStep) return 'completed'
    if (stepId === currentStep) return 'active'
    return 'future'
  }

  const getDotClasses = (state: 'completed' | 'active' | 'future'): string => {
    switch (state) {
      case 'completed':
        return 'w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white'
      case 'active':
        return 'w-10 h-10 rounded-full bg-primary ring-4 ring-primary-fixed flex items-center justify-center text-white font-bold shadow-lg shadow-primary/20'
      case 'future':
        return 'w-10 h-10 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant font-medium'
    }
  }

  const getLineClasses = (stepId: StepId): string => {
    const state = getStepState(stepId + 1 as StepId)
    if (state === 'completed' || state === 'active') {
      return 'h-0.5 w-24 bg-primary'
    }
    return 'h-0.5 w-24 bg-surface-container-high'
  }

  const getLabelClasses = (state: 'completed' | 'active' | 'future'): string => {
    switch (state) {
      case 'completed':
        return 'text-sm font-medium text-on-surface'
      case 'active':
        return 'text-sm font-bold text-primary'
      case 'future':
        return 'text-sm font-medium text-on-surface-variant'
    }
  }

  return (
    <div className="flex items-center justify-center mb-12">
      <div className="flex items-center">
        {steps.map((step, index) => {
          const state = getStepState(step.id)
          const isLast = index === steps.length - 1

          return (
            <div key={step.id} className="flex items-center">
              {/* Step Dot with Label */}
              <div className="flex flex-col items-center gap-2 bg-background px-4">
                <div className={getDotClasses(state)}>
                  {state === 'completed' ? (
                    <Icon name="check" size={20} />
                  ) : (
                    <span>{step.id}</span>
                  )}
                </div>
                <span className={getLabelClasses(state)}>{step.label}</span>
              </div>

              {/* Connecting Line (except for last step) */}
              {!isLast && (
                <div className={getLineClasses(step.id)} />
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

// Step 1: VideoConfirmation Component
function VideoConfirmation() {
  return (
    <div className="max-w-2xl mx-auto">
      <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
        确认视频信息
      </h2>
      <div className="bg-surface-container-lowest rounded-lg p-8 text-center">
        <p className="text-on-surface-variant">
          视频信息确认内容将在后续任务中实现
        </p>
      </div>
    </div>
  )
}

// Step 2: PlatformSelection Component
function PlatformSelection() {
  return (
    <div className="max-w-4xl mx-auto">
      <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
        选择发布平台
      </h2>
      <div className="bg-surface-container-lowest rounded-lg p-8 text-center">
        <p className="text-on-surface-variant">
          平台选择内容将在后续任务中实现
        </p>
      </div>
    </div>
  )
}

// Step 3: PromotionConfig Component
function PromotionConfig() {
  return (
    <div className="max-w-6xl mx-auto">
      <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
        配置推广文案
      </h2>
      <div className="bg-surface-container-lowest rounded-lg p-8 text-center">
        <p className="text-on-surface-variant">
          推广配置内容将在后续任务中实现
        </p>
      </div>
    </div>
  )
}

// NavigationButtons Component
interface NavigationButtonsProps {
  currentStep: StepId
  onBack: () => void
  onNext: () => void
  isLastStep: boolean
}

function NavigationButtons({
  currentStep,
  onBack,
  onNext,
  isLastStep,
}: NavigationButtonsProps) {
  return (
    <footer className="flex items-center justify-between mt-8 pt-8 border-t border-surface-container-high">
      <button
        onClick={onBack}
        disabled={currentStep === 1}
        className="px-8 py-3 rounded-lg text-on-surface-variant font-bold flex items-center gap-2 hover:bg-surface-container-low transition-all disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <Icon name="arrow_back" size={18} />
        返回
      </button>

      {isLastStep ? (
        <button
          onClick={onNext}
          className="px-12 py-3.5 rounded-lg bg-gradient-to-r from-primary to-primary-container text-white font-bold flex items-center gap-3 shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/40 transition-all"
        >
          确认发布
          <Icon name="rocket_launch" size={18} />
        </button>
      ) : (
        <button
          onClick={onNext}
          className="px-8 py-3 rounded-lg bg-primary text-white font-bold flex items-center gap-2 hover:opacity-90 transition-all"
        >
          下一步
          <Icon name="arrow_forward" size={18} />
        </button>
      )}
    </footer>
  )
}

// Main Page Component
export default function DistributionPromotionPage() {
  const { videoId } = useParams<{ videoId: string }>()
  const [currentStep, setCurrentStep] = useState<StepId>(1)

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => (prev - 1) as StepId)
    }
  }

  const handleNext = () => {
    if (currentStep < 3) {
      setCurrentStep((prev) => (prev + 1) as StepId)
    } else {
      // Last step - handle publish
      console.log('Publishing video:', videoId)
    }
  }

  const renderStepContent = () => {
    switch (currentStep) {
      case 1:
        return <VideoConfirmation />
      case 2:
        return <PlatformSelection />
      case 3:
        return <PromotionConfig />
      default:
        return null
    }
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">
            分发与推广
          </h1>
          <p className="font-body text-sm text-on-surface-variant mt-1">
            一键发布视频到多平台并执行推广
          </p>
        </div>
      </div>

      {/* Step Wizard */}
      <div className="bg-surface-container-lowest rounded-xl p-8">
        {/* Step Indicator */}
        <StepIndicator steps={STEPS} currentStep={currentStep} />

        {/* Step Content */}
        <div className="min-h-[300px]">{renderStepContent()}</div>

        {/* Navigation Buttons */}
        <NavigationButtons
          currentStep={currentStep}
          onBack={handleBack}
          onNext={handleNext}
          isLastStep={currentStep === 3}
        />
      </div>
    </div>
  )
}
