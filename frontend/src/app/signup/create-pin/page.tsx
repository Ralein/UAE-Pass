'use client';

import { useState, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { pinApi } from '@/lib/api-client';

const STEPS = ['Terms', 'Identity', 'Contact', 'OTP', 'PIN', 'Done'];
const PIN_LENGTH = 6;

function StepIndicator({ current }: { current: number }) {
    return (
        <div className="step-indicator" role="progressbar" aria-valuenow={current + 1} aria-valuemin={1} aria-valuemax={STEPS.length}>
            {STEPS.map((step, index) => (
                <div key={step} style={{ display: 'contents' }}>
                    <div className={`step-dot ${index < current ? 'completed' : index === current ? 'active' : 'inactive'}`}>
                        {index < current ? '✓' : index + 1}
                    </div>
                    {index < STEPS.length - 1 && <div className={`step-line ${index < current ? 'completed' : ''}`} />}
                </div>
            ))}
        </div>
    );
}

type PinPhase = 'create' | 'confirm';

export default function CreatePinPage() {
    const router = useRouter();
    const [phase, setPhase] = useState<PinPhase>('create');
    const [pin, setPin] = useState<string[]>(Array(PIN_LENGTH).fill(''));
    const [confirmPin, setConfirmPin] = useState<string[]>(Array(PIN_LENGTH).fill(''));
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

    const currentPin = phase === 'create' ? pin : confirmPin;
    const setCurrentPin = phase === 'create' ? setPin : setConfirmPin;

    useEffect(() => {
        inputRefs.current[0]?.focus();
    }, [phase]);

    const handleChange = (index: number, value: string) => {
        if (!/^\d?$/.test(value)) return;

        const newPin = [...currentPin];
        newPin[index] = value;
        setCurrentPin(newPin);
        setError('');

        if (value && index < PIN_LENGTH - 1) {
            inputRefs.current[index + 1]?.focus();
        }

        // Auto-advance to confirm phase
        if (phase === 'create' && value && index === PIN_LENGTH - 1) {
            const fullPin = newPin.join('');
            if (fullPin.length === PIN_LENGTH) {
                setTimeout(() => {
                    setPhase('confirm');
                }, 300);
            }
        }
    };

    const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
        if (e.key === 'Backspace' && !currentPin[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        }
    };

    const handleSubmit = async () => {
        const pinCode = pin.join('');
        const confirmCode = confirmPin.join('');

        if (pinCode !== confirmCode) {
            setError('PINs do not match. Please try again.');
            setPhase('create');
            setPin(Array(PIN_LENGTH).fill(''));
            setConfirmPin(Array(PIN_LENGTH).fill(''));
            return;
        }

        setLoading(true);
        setError('');

        try {
            const userId = sessionStorage.getItem('signup_userId') || '';
            if (!userId) {
                router.push('/signup');
                return;
            }

            await pinApi.create({
                userId,
                pin: pinCode,
                pinConfirm: confirmCode,
            });

            // Clear sensitive data from session storage
            sessionStorage.removeItem('signup_emiratesId');
            sessionStorage.removeItem('signup_fullName');
            sessionStorage.removeItem('signup_userId');

            router.push('/signup/success');
        } catch (error: any) {
            const message = error.response?.data?.message || 'Failed to create PIN. Please try again.';
            setError(message);
            setPin(Array(PIN_LENGTH).fill(''));
            setConfirmPin(Array(PIN_LENGTH).fill(''));
            setPhase('create');
        } finally {
            setLoading(false);
        }
    };

    // Auto-submit when confirm PIN is complete
    useEffect(() => {
        if (phase === 'confirm' && confirmPin.join('').length === PIN_LENGTH) {
            handleSubmit();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [confirmPin, phase]);

    const pinRules = [
        { label: 'Exactly 6 digits', valid: pin.join('').length === PIN_LENGTH },
        { label: 'Not all same digit', valid: pin.join('').length === PIN_LENGTH && new Set(pin).size > 1 },
        { label: 'Not sequential (e.g., 123456)', valid: true }, // Enforced server-side
    ];

    return (
        <div className="page-container">
            <div className="signup-layout">
                <div className="brand-header">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                <StepIndicator current={4} />

                <div className="glass-card animate-in">
                    <h1 className="heading-lg" style={{ marginBottom: 'var(--space-sm)', textAlign: 'center' }}>
                        {phase === 'create' ? 'Create Your PIN' : 'Confirm Your PIN'}
                    </h1>
                    <p className="text-body" style={{ marginBottom: 'var(--space-md)', textAlign: 'center' }}>
                        {phase === 'create'
                            ? 'Choose a 6-digit PIN to secure your account'
                            : 'Re-enter your PIN to confirm'}
                    </p>

                    {error && (
                        <div
                            className="form-error animate-in"
                            style={{
                                padding: 'var(--space-md)',
                                background: 'rgba(239, 68, 68, 0.1)',
                                borderRadius: 'var(--radius-sm)',
                                marginBottom: 'var(--space-md)',
                                justifyContent: 'center',
                            }}
                        >
                            ⚠ {error}
                        </div>
                    )}

                    {/* PIN Input */}
                    <div className="pin-container">
                        {currentPin.map((digit, index) => (
                            <input
                                key={`${phase}-${index}`}
                                ref={(el) => { inputRefs.current[index] = el; }}
                                type="password"
                                inputMode="numeric"
                                className={`pin-digit ${digit ? 'filled' : ''}`}
                                value={digit}
                                onChange={(e) => handleChange(index, e.target.value)}
                                onKeyDown={(e) => handleKeyDown(index, e)}
                                maxLength={1}
                                aria-label={`${phase === 'create' ? 'PIN' : 'Confirm PIN'} digit ${index + 1}`}
                                autoComplete="off"
                                disabled={loading}
                            />
                        ))}
                    </div>

                    {/* PIN Rules (only in create phase) */}
                    {phase === 'create' && (
                        <div style={{ marginBottom: 'var(--space-xl)' }}>
                            {pinRules.map((rule) => (
                                <div
                                    key={rule.label}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 'var(--space-sm)',
                                        marginBottom: 'var(--space-xs)',
                                    }}
                                >
                                    <span style={{
                                        color: rule.valid ? 'var(--color-primary-light)' : 'var(--text-muted)',
                                        fontSize: '0.8125rem',
                                    }}>
                                        {rule.valid ? '✓' : '○'} {rule.label}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}

                    {phase === 'confirm' && (
                        <button
                            className={`btn btn-primary ${loading ? 'btn-loading' : ''}`}
                            onClick={handleSubmit}
                            disabled={loading || confirmPin.join('').length !== PIN_LENGTH}
                            aria-label="Create PIN and activate account"
                        >
                            <span className="btn-text-content">
                                {loading ? '' : 'Create PIN'}
                            </span>
                        </button>
                    )}

                    {phase === 'confirm' && (
                        <button
                            className="btn btn-text"
                            onClick={() => {
                                setPhase('create');
                                setPin(Array(PIN_LENGTH).fill(''));
                                setConfirmPin(Array(PIN_LENGTH).fill(''));
                                setError('');
                            }}
                            style={{ marginTop: 'var(--space-md)' }}
                        >
                            Start Over
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
