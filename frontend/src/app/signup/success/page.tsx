'use client';

import { useRouter } from 'next/navigation';

const STEPS = ['Terms', 'Identity', 'Contact', 'OTP', 'PIN', 'Done'];

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

export default function SuccessPage() {
    const router = useRouter();

    return (
        <div className="page-container">
            <div className="signup-layout">
                <div className="brand-header">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                <StepIndicator current={5} />

                <div className="glass-card animate-in" style={{ textAlign: 'center' }}>
                    {/* Success Icon */}
                    <div className="success-icon">
                        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path
                                d="M5 13L9 17L19 7"
                                stroke="white"
                                strokeWidth="2.5"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                        </svg>
                    </div>

                    <h1 className="heading-xl" style={{ marginBottom: 'var(--space-md)' }}>
                        Account Created!
                    </h1>

                    <p className="text-body" style={{ marginBottom: 'var(--space-2xl)' }}>
                        Your UAE Digital Identity has been successfully created.
                        You can now use your credentials to sign in to government and private services across the UAE.
                    </p>

                    {/* Account Summary */}
                    <div
                        style={{
                            background: 'var(--bg-glass)',
                            borderRadius: 'var(--radius-sm)',
                            padding: 'var(--space-lg)',
                            marginBottom: 'var(--space-2xl)',
                            border: '1px solid var(--border-subtle)',
                            textAlign: 'left',
                        }}
                    >
                        <div className="heading-md" style={{ marginBottom: 'var(--space-md)' }}>
                            Account Details
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-sm)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span className="text-small">Account Level</span>
                                <span style={{
                                    fontSize: '0.8125rem',
                                    fontWeight: 600,
                                    color: 'var(--color-accent-gold)',
                                }}>
                                    SOP 1
                                </span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span className="text-small">Status</span>
                                <span style={{
                                    fontSize: '0.8125rem',
                                    fontWeight: 600,
                                    color: 'var(--text-accent)',
                                }}>
                                    Active ✓
                                </span>
                            </div>
                            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                <span className="text-small">Verification</span>
                                <span style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                                    Phone Verified
                                </span>
                            </div>
                        </div>
                    </div>

                    <button
                        className="btn btn-primary"
                        onClick={() => router.push('/')}
                        aria-label="Go to sign in"
                    >
                        Go to Sign In
                    </button>

                    <div style={{ marginTop: 'var(--space-lg)' }}>
                        <p className="text-small">
                            Upgrade your account to SOP 2 by verifying your Emirates ID at any smart service center.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}
