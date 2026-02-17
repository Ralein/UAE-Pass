'use client';

import { useRouter } from 'next/navigation';

export default function SignupPage() {
    const router = useRouter();

    return (
        <div className="page-container">
            <div className="signup-layout">
                {/* Brand Header */}
                <div className="brand-header animate-in">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                {/* Hero Section */}
                <div className="glass-card animate-in animate-delay-1">
                    <h1 className="heading-xl" style={{ marginBottom: 'var(--space-md)' }}>
                        Your Digital Identity,<br />One Secure Platform
                    </h1>
                    <p className="text-body" style={{ marginBottom: 'var(--space-2xl)' }}>
                        Create your UAE Digital Identity to securely access government
                        and private services across the UAE. Verify once, use everywhere.
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-md)' }}>
                        <button
                            className="btn btn-primary"
                            onClick={() => router.push('/signup/terms')}
                            aria-label="Create your digital identity account"
                        >
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M10 4V16M4 10H16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                            Create Account
                        </button>

                        <button
                            className="btn btn-secondary"
                            onClick={() => {/* Future: login flow */ }}
                            aria-label="Sign in to existing account"
                        >
                            Sign In
                        </button>
                    </div>
                </div>

                {/* Features */}
                <div className="animate-in animate-delay-2" style={{ marginTop: 'var(--space-xl)' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 'var(--space-md)' }}>
                        {[
                            { icon: '🔒', label: 'Bank-Grade Security' },
                            { icon: '⚡', label: 'Instant Verification' },
                            { icon: '🛡️', label: 'Privacy First' },
                        ].map((feature) => (
                            <div
                                key={feature.label}
                                style={{
                                    textAlign: 'center',
                                    padding: 'var(--space-lg) var(--space-sm)',
                                    background: 'var(--bg-glass)',
                                    borderRadius: 'var(--radius-md)',
                                    border: '1px solid var(--border-subtle)',
                                }}
                            >
                                <div style={{ fontSize: '1.5rem', marginBottom: 'var(--space-xs)' }}>{feature.icon}</div>
                                <div className="text-small">{feature.label}</div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Footer */}
                <div className="animate-in animate-delay-3" style={{ textAlign: 'center', marginTop: 'var(--space-2xl)' }}>
                    <p className="text-small">
                        Powered by UAE National Digital Infrastructure
                    </p>
                </div>
            </div>
        </div>
    );
}
