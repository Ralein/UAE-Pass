'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';

const STEPS = ['Terms', 'Identity', 'Contact', 'OTP', 'PIN', 'Done'];

function StepIndicator({ current }: { current: number }) {
    return (
        <div className="step-indicator" role="progressbar" aria-valuenow={current + 1} aria-valuemin={1} aria-valuemax={STEPS.length}>
            {STEPS.map((step, index) => (
                <div key={step} style={{ display: 'contents' }}>
                    <div
                        className={`step-dot ${index < current ? 'completed' : index === current ? 'active' : 'inactive'
                            }`}
                        aria-label={`Step ${index + 1}: ${step}${index < current ? ' (completed)' : index === current ? ' (current)' : ''}`}
                    >
                        {index < current ? '✓' : index + 1}
                    </div>
                    {index < STEPS.length - 1 && (
                        <div className={`step-line ${index < current ? 'completed' : ''}`} />
                    )}
                </div>
            ))}
        </div>
    );
}

export default function TermsPage() {
    const router = useRouter();
    const [accepted, setAccepted] = useState(false);

    return (
        <div className="page-container">
            <div className="signup-layout">
                <div className="brand-header">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                <StepIndicator current={0} />

                <div className="glass-card animate-in">
                    <h1 className="heading-lg" style={{ marginBottom: 'var(--space-lg)' }}>
                        Terms & Conditions
                    </h1>

                    <div
                        style={{
                            maxHeight: '300px',
                            overflowY: 'auto',
                            padding: 'var(--space-lg)',
                            background: 'var(--bg-glass)',
                            borderRadius: 'var(--radius-sm)',
                            border: '1px solid var(--border-subtle)',
                            marginBottom: 'var(--space-xl)',
                        }}
                    >
                        <h3 className="heading-md" style={{ marginBottom: 'var(--space-md)' }}>
                            UAE Digital Identity Service Agreement
                        </h3>
                        <div className="text-body" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-md)' }}>
                            <p>
                                <strong>1. Acceptance of Terms</strong><br />
                                By creating an account with the UAE Digital Identity Platform, you agree to these Terms and
                                Conditions, our Privacy Policy, and all applicable UAE federal laws governing digital identity services.
                            </p>
                            <p>
                                <strong>2. Identity Verification</strong><br />
                                You consent to the verification of your identity using official UAE government-issued documents.
                                All personal information will be handled in accordance with UAE Federal Law regarding personal data protection.
                            </p>
                            <p>
                                <strong>3. Account Security</strong><br />
                                You are responsible for maintaining the confidentiality of your PIN and access credentials.
                                You must notify us immediately of any unauthorized use of your account.
                            </p>
                            <p>
                                <strong>4. Data Protection</strong><br />
                                Your personal data is encrypted, hashed, and stored securely in compliance with international
                                security standards. We will never share your data without your explicit consent.
                            </p>
                            <p>
                                <strong>5. Service Availability</strong><br />
                                The UAE Digital Identity Platform is provided on an &quot;as available&quot; basis.
                                We reserve the right to modify or discontinue the service with reasonable notice.
                            </p>
                            <p>
                                <strong>6. Prohibited Activities</strong><br />
                                You may not use the platform for any fraudulent, deceptive, or illegal purposes.
                                Attempting to bypass security measures will result in account suspension.
                            </p>
                        </div>
                    </div>

                    <label className="checkbox-container" style={{ marginBottom: 'var(--space-xl)' }}>
                        <input
                            type="checkbox"
                            className="checkbox-input"
                            checked={accepted}
                            onChange={(e) => setAccepted(e.target.checked)}
                            aria-label="Accept terms and conditions"
                        />
                        <span className="checkbox-label">
                            I have read and agree to the Terms & Conditions and Privacy Policy of the UAE Digital Identity Platform.
                        </span>
                    </label>

                    <button
                        className="btn btn-primary"
                        disabled={!accepted}
                        onClick={() => router.push('/signup/id-entry')}
                        aria-label="Continue to identity verification"
                    >
                        Continue
                    </button>
                </div>
            </div>
        </div>
    );
}
