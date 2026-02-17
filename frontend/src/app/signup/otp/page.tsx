'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { otpApi } from '@/lib/api-client';

const STEPS = ['Terms', 'Identity', 'Contact', 'OTP', 'PIN', 'Done'];
const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60;

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

export default function OtpPage() {
    const router = useRouter();
    const [otp, setOtp] = useState<string[]>(Array(OTP_LENGTH).fill(''));
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [resendTimer, setResendTimer] = useState(RESEND_COOLDOWN);
    const [canResend, setCanResend] = useState(false);
    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

    // Resend countdown timer
    useEffect(() => {
        if (resendTimer > 0) {
            const timer = setTimeout(() => setResendTimer(resendTimer - 1), 1000);
            return () => clearTimeout(timer);
        } else {
            setCanResend(true);
        }
    }, [resendTimer]);

    // Focus first input on mount
    useEffect(() => {
        inputRefs.current[0]?.focus();
    }, []);

    const handleChange = (index: number, value: string) => {
        if (!/^\d?$/.test(value)) return;

        const newOtp = [...otp];
        newOtp[index] = value;
        setOtp(newOtp);
        setError('');

        // Auto-advance to next digit
        if (value && index < OTP_LENGTH - 1) {
            inputRefs.current[index + 1]?.focus();
        }
    };

    const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
        if (e.key === 'Backspace' && !otp[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        }
    };

    const handlePaste = (e: React.ClipboardEvent) => {
        e.preventDefault();
        const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
        if (pasted.length === OTP_LENGTH) {
            const newOtp = pasted.split('');
            setOtp(newOtp);
            inputRefs.current[OTP_LENGTH - 1]?.focus();
        }
    };

    const handleVerify = useCallback(async () => {
        const otpCode = otp.join('');
        if (otpCode.length !== OTP_LENGTH) {
            setError('Please enter all 6 digits');
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

            const response = await otpApi.verify({
                userId,
                otpCode,
                channel: 'SMS',
            });

            if (response.data.status === 'OTP_VERIFIED') {
                router.push('/signup/create-pin');
            } else {
                setError('Invalid OTP. Please try again.');
                setOtp(Array(OTP_LENGTH).fill(''));
                inputRefs.current[0]?.focus();
            }
        } catch (error: any) {
            const message = error.response?.data?.message || 'Verification failed. Please try again.';
            setError(message);
            setOtp(Array(OTP_LENGTH).fill(''));
            inputRefs.current[0]?.focus();
        } finally {
            setLoading(false);
        }
    }, [otp, router]);

    const handleResend = async () => {
        if (!canResend) return;

        try {
            const userId = sessionStorage.getItem('signup_userId') || '';
            await otpApi.send(userId, 'SMS');
            setResendTimer(RESEND_COOLDOWN);
            setCanResend(false);
            setError('');
            setOtp(Array(OTP_LENGTH).fill(''));
            inputRefs.current[0]?.focus();
        } catch (error: any) {
            setError(error.response?.data?.message || 'Failed to resend OTP.');
        }
    };

    // Auto-submit when all digits entered
    useEffect(() => {
        const otpCode = otp.join('');
        if (otpCode.length === OTP_LENGTH && !loading) {
            handleVerify();
        }
    }, [otp, loading, handleVerify]);

    return (
        <div className="page-container">
            <div className="signup-layout">
                <div className="brand-header">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                <StepIndicator current={3} />

                <div className="glass-card animate-in">
                    <h1 className="heading-lg" style={{ marginBottom: 'var(--space-sm)', textAlign: 'center' }}>
                        Verify Your Phone
                    </h1>
                    <p className="text-body" style={{ marginBottom: 'var(--space-md)', textAlign: 'center' }}>
                        Enter the 6-digit code sent to your phone number
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

                    {/* OTP Input */}
                    <div className="otp-container" onPaste={handlePaste}>
                        {otp.map((digit, index) => (
                            <input
                                key={index}
                                ref={(el) => { inputRefs.current[index] = el; }}
                                type="text"
                                inputMode="numeric"
                                className={`otp-digit ${digit ? 'filled' : ''} ${error ? 'error' : ''}`}
                                value={digit}
                                onChange={(e) => handleChange(index, e.target.value)}
                                onKeyDown={(e) => handleKeyDown(index, e)}
                                maxLength={1}
                                aria-label={`OTP digit ${index + 1}`}
                                autoComplete="one-time-code"
                                disabled={loading}
                            />
                        ))}
                    </div>

                    <button
                        className={`btn btn-primary ${loading ? 'btn-loading' : ''}`}
                        onClick={handleVerify}
                        disabled={loading || otp.join('').length !== OTP_LENGTH}
                        aria-label="Verify OTP code"
                    >
                        <span className="btn-text-content">
                            {loading ? '' : 'Verify Code'}
                        </span>
                    </button>

                    {/* Resend Timer */}
                    <div className="resend-timer">
                        {canResend ? (
                            <button className="btn btn-text" onClick={handleResend}>
                                Resend Code
                            </button>
                        ) : (
                            <span className="timer-text">
                                Resend code in <span className="timer-count">{resendTimer}s</span>
                            </span>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
