'use client';

import Image from 'next/image';
import Link from 'next/link';
import './landing.css';

export default function LandingPage() {
    return (
        <div className="landing">
            {/* ─── Top Bar ─── */}
            <div className="top-bar">
                <nav className="top-bar-nav">
                    <Link href="/about">About</Link>
                    <Link href="/faq">FAQs</Link>
                    <Link href="/tutorials">Tutorials</Link>
                    <Link href="/support">Support</Link>
                </nav>
                <div className="top-bar-actions">
                    <button className="accessibility-btn" aria-label="Accessibility">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="4.5" r="2.5" />
                            <path d="M12 7v5l-3 5m6-5-3-5" />
                            <path d="M6 10h12" />
                        </svg>
                    </button>
                    <button className="lang-btn">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="12" cy="12" r="10" />
                            <path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10A15.3 15.3 0 0 1 12 2z" />
                        </svg>
                    </button>
                </div>
            </div>

            {/* ─── Main Nav ─── */}
            <header className="main-nav">
                <div className="main-nav-inner">
                    <Link href="/" className="nav-logo">
                        <Image src="/full-logo.svg" alt="UAE PASS" width={180} height={50} priority />
                    </Link>
                    <nav className="nav-links">
                        <Link href="/" className="nav-link active">Home</Link>
                        <Link href="/kiosk" className="nav-link">Kiosk Locations</Link>
                        <Link href="/partners" className="nav-link">Partners</Link>
                        <Link href="/developers" className="nav-link">Developers</Link>
                    </nav>
                    <Link href="/signup" className="login-btn">Login</Link>
                </div>
            </header>

            {/* ─── Hero ─── */}
            <section className="hero">
                <div className="hero-inner">
                    <div className="hero-content">
                        <span className="hero-badge">UAE PASS</span>
                        <h1 className="hero-title">
                            The first national digital identity for citizens residents and visitors in UAE
                        </h1>
                        <p className="hero-desc">
                            UAE PASS is the first secure national digital identity for citizens, residents
                            and visitors in UAE, enabling them to access many online services across various
                            sectors, in addition to providing features such as signing and verifying documents
                            digitally, requesting digital versions of official documents, and using the same in
                            applying for services from our partners.
                        </p>
                        <div className="hero-actions">
                            <button className="hero-btn">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" /><path d="m15 5 4 4" /></svg>
                                Sign Document
                            </button>
                            <button className="hero-btn">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><path d="m9 12 2 2 4-4" /></svg>
                                Verify Document
                            </button>
                            <button className="hero-btn-text">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><polygon points="10 8 16 12 10 16 10 8" fill="currentColor" /></svg>
                                Watch Video
                            </button>
                        </div>
                        <div className="hero-collab">
                            <span>In Collaboration With</span>
                            <div className="collab-logos">
                                <Image src="/logo/tdra.svg" alt="TDRA" width={80} height={32} />
                                <Image src="/logo/adda.svg" alt="ADDA" width={80} height={32} />
                                <Image src="/logo/dda.svg" alt="DDA" width={80} height={32} />
                            </div>
                        </div>
                    </div>
                    <div className="hero-visual">
                        <div className="phone-mockup">
                            <Image
                                src="/u-pass-mobile.png"
                                alt="UAE PASS App"
                                width={380}
                                height={560}
                                priority
                            />
                        </div>
                        <div className="qr-section">
                            <Image src="/qr-code.png" alt="Download QR" width={160} height={160} className="qr-img" />
                            <p className="qr-label">Download</p>
                            <p className="qr-brand">UAE PASS</p>
                        </div>
                    </div>
                </div>
            </section>

            {/* ─── Feature Highlights ─── */}
            <section className="features">
                <div className="features-inner">
                    <h2 className="section-heading">UAE PASS is your right choice</h2>
                    <p className="section-subheading">
                        The first secure national digital identity for citizens, residents, and visitors.
                    </p>
                    <div className="features-grid">
                        <div className="feature-card">
                            <div className="feature-icon">
                                <Image src="/secure-sign.png" alt="Secure Sign In" width={220} height={220} />
                            </div>
                            <h3>Secure Sign In</h3>
                            <p>Login and sign up to many websites and applications with one account.</p>
                            <Link href="/about" className="feature-link">Learn More →</Link>
                        </div>
                        <div className="feature-card">
                            <div className="feature-icon">
                                <Image src="/digital-signatures.png" alt="Sign Documents Digitally" width={220} height={220} />
                            </div>
                            <h3>Sign Documents Digitally</h3>
                            <p>Sign and verify documents digitally through UAE PASS.</p>
                            <Link href="/about" className="feature-link">Learn More →</Link>
                        </div>
                        <div className="feature-card">
                            <div className="feature-icon">
                                <Image src="/document-sharing.png" alt="Documents Sharing" width={220} height={220} />
                            </div>
                            <h3>Documents Sharing</h3>
                            <p>Request and share official documents with various partners.</p>
                            <Link href="/about" className="feature-link">Learn More →</Link>
                        </div>
                    </div>
                </div>
            </section>

            {/* ─── Sign Documents Digitally ─── */}
            <section className="detail-section">
                <div className="detail-inner">
                    <div className="detail-image">
                        <Image src="/digital-signatures.png" alt="Sign Documents Digitally" width={480} height={480} />
                    </div>
                    <div className="detail-content">
                        <span className="detail-badge">THROUGH YOUR SMARTPHONE</span>
                        <h2>Sign Documents Digitally</h2>
                        <p>
                            You can now easily sign a document digitally with UAE PASS. Just download a
                            document, sign it through the UAE PASS app or selfcare portal, then share it. You
                            can also verify digital signatures by uploading a document and validating it as
                            needed from the verify feature available on all channels.
                        </p>
                        <p className="detail-cta-text">
                            learn more about the services you can reach through UAE PASS digital id
                        </p>
                        <div className="detail-actions">
                            <button className="detail-btn">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" /><path d="m15 5 4 4" /></svg>
                                Sign Document
                            </button>
                            <button className="detail-btn">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /><path d="m9 12 2 2 4-4" /></svg>
                                Verify Document
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            {/* ─── Request & Share Documents ─── */}
            <section className="detail-section alt">
                <div className="detail-inner reverse">
                    <div className="detail-image">
                        <Image src="/document-sharing.png" alt="Request Documents" width={480} height={480} />
                    </div>
                    <div className="detail-content">
                        <span className="detail-badge">THROUGH YOUR SMARTPHONE</span>
                        <h2>Request and Share Official Documents</h2>
                        <p>
                            You can request digital versions of official documents through UAE PASS app and
                            through partner apps. You can also use the same in applying for services. Download
                            the UAE PASS app and explore the document wallet feature.
                        </p>
                        <div className="detail-actions">
                            <button className="detail-btn">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="m8 12 2 2 4-4" /></svg>
                                Explore Services
                            </button>
                            <button className="detail-btn-text">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><polygon points="10 8 16 12 10 16 10 8" fill="currentColor" /></svg>
                                Watch Video
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            {/* ─── 3 Steps ─── */}
            <section className="steps-section">
                <div className="steps-inner">
                    <h2 className="section-heading">Sign up in 3 Simple Steps</h2>
                    <div className="steps-grid">
                        <div className="step-card">
                            <span className="step-number">1</span>
                            <div className="step-img">
                                <Image src="/download-step.png" alt="Download" width={140} height={160} />
                            </div>
                            <h3>Download</h3>
                            <p>Download the UAE PASS application from the app store.</p>
                            <div className="store-badges">
                                <a href="https://apps.apple.com/ae/app/uae-pass/id1377158818" target="_blank" rel="noopener noreferrer">
                                    <Image src="/apple-store.png" alt="App Store" width={120} height={40} />
                                </a>
                                <a href="https://play.google.com/store/apps/details?id=ae.uaepass.mainapp" target="_blank" rel="noopener noreferrer">
                                    <Image src="/google-play.png" alt="Google Play" width={120} height={40} />
                                </a>
                            </div>
                        </div>
                        <div className="step-card">
                            <span className="step-number">2</span>
                            <div className="step-img">
                                <Image src="/register-step.png" alt="Register" width={140} height={160} />
                            </div>
                            <h3>Register</h3>
                            <p>Register through Emirates ID, GCC ID, or Passport.</p>
                            <Link href="/tutorials" className="step-link">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><polygon points="10 8 16 12 10 16 10 8" fill="currentColor" /></svg>
                                Watch Video
                            </Link>
                        </div>
                        <div className="step-card">
                            <span className="step-number">3</span>
                            <div className="step-img">
                                <Image src="/verify-step.png" alt="Verify" width={140} height={160} />
                            </div>
                            <h3>Verify</h3>
                            <p>Verify your identity through face verification or at the nearest kiosk.</p>
                            <Link href="/kiosk" className="step-link">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" /></svg>
                                Nearest Kiosk
                            </Link>
                        </div>
                    </div>
                </div>
            </section>

            {/* ─── Contact ─── */}
            <section className="contact-section">
                <div className="contact-inner">
                    <h2>Still have a question? Contact us</h2>
                    <div className="contact-info">
                        <a href="tel:60056111" className="contact-item">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" /></svg>
                            600 56 1111
                        </a>
                        <a href="mailto:support@uaepass.ae" className="contact-item">
                            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect width="20" height="16" x="2" y="4" rx="2" /><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" /></svg>
                            support@uaepass.ae
                        </a>
                    </div>
                </div>
            </section>

            {/* ─── Footer ─── */}
            <footer className="site-footer">
                <div className="footer-inner">
                    <div className="footer-brand">
                        <Image src="/full-logo.svg" alt="UAE PASS" width={140} height={40} className="footer-logo" />
                        <p>Your secure national digital identity in the UAE</p>
                    </div>
                    <div className="footer-col">
                        <h4>Company</h4>
                        <Link href="/about">About</Link>
                        <Link href="/faq">FAQs</Link>
                        <Link href="/support">Support</Link>
                    </div>
                    <div className="footer-col">
                        <h4>Services</h4>
                        <Link href="/kiosk">Kiosk Locations</Link>
                        <Link href="/partners">Partners</Link>
                        <Link href="/developers">Developers</Link>
                    </div>
                    <div className="footer-col">
                        <h4>Location</h4>
                        <p>United Arab Emirates</p>
                    </div>
                </div>
                <div className="footer-bottom">
                    <p>All rights reserved. © {new Date().getFullYear()} UAE PASS</p>
                </div>
            </footer>
        </div>
    );
}
