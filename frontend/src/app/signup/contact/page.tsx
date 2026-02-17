'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { registrationApi } from '@/lib/api-client';

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

export default function ContactPage() {
    const router = useRouter();
    const [phone, setPhone] = useState('+971');
    const [email, setEmail] = useState('');
    const [gender, setGender] = useState('');
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(false);
    const [serverError, setServerError] = useState('');

    const handleSubmit = async () => {
        const newErrors: Record<string, string> = {};

        if (!phone || !/^\+971[0-9]{8,9}$/.test(phone)) {
            newErrors.phone = 'Enter a valid UAE phone number (+971XXXXXXXXX)';
        }
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            newErrors.email = 'Enter a valid email address';
        }
        if (!gender) {
            newErrors.gender = 'Please select your gender';
        }

        setErrors(newErrors);
        if (Object.keys(newErrors).length > 0) return;

        setLoading(true);
        setServerError('');

        try {
            // Retrieve identity data stored from previous step
            const emiratesId = sessionStorage.getItem('signup_emiratesId') || '';
            const fullName = sessionStorage.getItem('signup_fullName') || '';

            if (!emiratesId || !fullName) {
                router.push('/signup/id-entry');
                return;
            }

            const response = await registrationApi.start({
                emiratesId,
                fullName,
                email,
                phone,
                gender,
            });

            // Store userId for OTP step
            sessionStorage.setItem('signup_userId', response.data.userId);
            router.push('/signup/otp');
        } catch (error: any) {
            const message = error.response?.data?.message || 'Registration failed. Please try again.';
            setServerError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="page-container">
            <div className="signup-layout">
                <div className="brand-header">
                    <div className="brand-icon">ID</div>
                    <span className="brand-name">UAE PASS</span>
                </div>

                <StepIndicator current={2} />

                <div className="glass-card animate-in">
                    <h1 className="heading-lg" style={{ marginBottom: 'var(--space-sm)' }}>
                        Contact Information
                    </h1>
                    <p className="text-body" style={{ marginBottom: 'var(--space-xl)' }}>
                        We&apos;ll send a verification code to confirm your contact details.
                    </p>

                    {serverError && (
                        <div
                            className="form-error animate-in"
                            style={{
                                padding: 'var(--space-md)',
                                background: 'rgba(239, 68, 68, 0.1)',
                                borderRadius: 'var(--radius-sm)',
                                marginBottom: 'var(--space-lg)',
                            }}
                        >
                            ⚠ {serverError}
                        </div>
                    )}

                    <div className="form-group">
                        <label htmlFor="phone" className="form-label">Phone Number</label>
                        <input
                            id="phone"
                            type="tel"
                            className={`form-input ${errors.phone ? 'error' : ''}`}
                            placeholder="+971501234567"
                            value={phone}
                            onChange={(e) => {
                                setPhone(e.target.value);
                                setErrors((prev) => ({ ...prev, phone: '' }));
                            }}
                            maxLength={13}
                            aria-label="UAE phone number"
                        />
                        <div className="form-hint">UAE mobile number starting with +971</div>
                        {errors.phone && <div className="form-error">⚠ {errors.phone}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="email" className="form-label">Email Address</label>
                        <input
                            id="email"
                            type="email"
                            className={`form-input ${errors.email ? 'error' : ''}`}
                            placeholder="you@example.com"
                            value={email}
                            onChange={(e) => {
                                setEmail(e.target.value);
                                setErrors((prev) => ({ ...prev, email: '' }));
                            }}
                            maxLength={254}
                            aria-label="Email address"
                            autoComplete="email"
                        />
                        {errors.email && <div className="form-error">⚠ {errors.email}</div>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="gender" className="form-label">Gender</label>
                        <select
                            id="gender"
                            className={`form-input form-select ${errors.gender ? 'error' : ''}`}
                            value={gender}
                            onChange={(e) => {
                                setGender(e.target.value);
                                setErrors((prev) => ({ ...prev, gender: '' }));
                            }}
                            aria-label="Select gender"
                        >
                            <option value="">Select gender</option>
                            <option value="MALE">Male</option>
                            <option value="FEMALE">Female</option>
                        </select>
                        {errors.gender && <div className="form-error">⚠ {errors.gender}</div>}
                    </div>

                    <button
                        className={`btn btn-primary ${loading ? 'btn-loading' : ''}`}
                        onClick={handleSubmit}
                        disabled={loading}
                        aria-label="Submit and receive OTP"
                    >
                        <span className="btn-text-content">
                            {loading ? '' : 'Continue & Verify'}
                        </span>
                    </button>
                </div>
            </div>
        </div>
    );
}
