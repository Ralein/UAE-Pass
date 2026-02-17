'use client';

import { useState } from 'react';
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

export default function IdEntryPage() {
    const router = useRouter();
    const [emiratesId, setEmiratesId] = useState('');
    const [fullName, setFullName] = useState('');
    const [errors, setErrors] = useState<Record<string, string>>({});

    const validateEmiratesId = (value: string) => {
        return /^784-\d{4}-\d{7}-\d$/.test(value);
    };

    const formatEmiratesId = (value: string) => {
        // Auto-format as user types: 784-YYYY-NNNNNNN-C
        const digits = value.replace(/[^\d]/g, '');
        let formatted = '';
        if (digits.length > 0) formatted += digits.slice(0, 3);
        if (digits.length > 3) formatted += '-' + digits.slice(3, 7);
        if (digits.length > 7) formatted += '-' + digits.slice(7, 14);
        if (digits.length > 14) formatted += '-' + digits.slice(14, 15);
        return formatted;
    };

    const handleSubmit = () => {
        const newErrors: Record<string, string> = {};

        if (!emiratesId || !validateEmiratesId(emiratesId)) {
            newErrors.emiratesId = 'Enter a valid Emirates ID (784-YYYY-NNNNNNN-C)';
        }
        if (!fullName || fullName.trim().length < 2) {
            newErrors.fullName = 'Enter your full name (at least 2 characters)';
        }

        setErrors(newErrors);

        if (Object.keys(newErrors).length === 0) {
            // Store in sessionStorage for the next step (not sensitive — just format-validated input)
            sessionStorage.setItem('signup_emiratesId', emiratesId);
            sessionStorage.setItem('signup_fullName', fullName);
            router.push('/signup/contact');
        }
    };

    return (
        <div className="page-container">
            <div className="signup-layout">
                <div className="brand-header">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                <StepIndicator current={1} />

                <div className="glass-card animate-in">
                    <h1 className="heading-lg" style={{ marginBottom: 'var(--space-sm)' }}>
                        Verify Your Identity
                    </h1>
                    <p className="text-body" style={{ marginBottom: 'var(--space-xl)' }}>
                        Enter your Emirates ID and full name as shown on your official ID card.
                    </p>

                    <div className="form-group">
                        <label htmlFor="emiratesId" className="form-label">Emirates ID Number</label>
                        <input
                            id="emiratesId"
                            type="text"
                            className={`form-input ${errors.emiratesId ? 'error' : ''}`}
                            placeholder="784-0000-0000000-0"
                            value={emiratesId}
                            onChange={(e) => {
                                setEmiratesId(formatEmiratesId(e.target.value));
                                setErrors((prev) => ({ ...prev, emiratesId: '' }));
                            }}
                            maxLength={18}
                            aria-label="Emirates ID number"
                            aria-describedby="emiratesId-hint"
                            autoComplete="off"
                        />
                        <div id="emiratesId-hint" className="form-hint">Format: 784-YYYY-NNNNNNN-C</div>
                        {errors.emiratesId && <div className="form-error">⚠ {errors.emiratesId}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="fullName" className="form-label">Full Name (as on ID)</label>
                        <input
                            id="fullName"
                            type="text"
                            className={`form-input ${errors.fullName ? 'error' : ''}`}
                            placeholder="Enter your full name"
                            value={fullName}
                            onChange={(e) => {
                                setFullName(e.target.value);
                                setErrors((prev) => ({ ...prev, fullName: '' }));
                            }}
                            maxLength={200}
                            aria-label="Full name"
                            autoComplete="name"
                        />
                        {errors.fullName && <div className="form-error">⚠ {errors.fullName}</div>}
                    </div>

                    <button
                        className="btn btn-primary"
                        onClick={handleSubmit}
                        aria-label="Continue to contact information"
                    >
                        Continue
                    </button>
                </div>
            </div>
        </div>
    );
}
