'use client';

import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import enMessages from '@/locales/en.json';
import arMessages from '@/locales/ar.json';

type Locale = 'en' | 'ar';

interface I18nContextType {
    locale: Locale;
    dir: 'ltr' | 'rtl';
    t: (key: string, params?: Record<string, string | number>) => string;
    setLocale: (locale: Locale) => void;
}

const messages: Record<Locale, Record<string, any>> = {
    en: enMessages,
    ar: arMessages,
};

const I18nContext = createContext<I18nContextType | null>(null);

/**
 * Get a nested value from an object by dot-separated key path.
 */
function getNestedValue(obj: Record<string, any>, path: string): string | undefined {
    const keys = path.split('.');
    let current: any = obj;
    for (const key of keys) {
        if (current == null || typeof current !== 'object') return undefined;
        current = current[key];
    }
    return typeof current === 'string' ? current : undefined;
}

/**
 * Interpolate parameters into a message string.
 * Replaces {{key}} with the corresponding value.
 */
function interpolate(message: string, params?: Record<string, string | number>): string {
    if (!params) return message;
    return message.replace(/\{\{(\w+)\}\}/g, (_, key) => {
        return params[key] !== undefined ? String(params[key]) : `{{${key}}}`;
    });
}

export function I18nProvider({ children, defaultLocale = 'en' }: { children: ReactNode; defaultLocale?: Locale }) {
    const [locale, setLocaleState] = useState<Locale>(defaultLocale);

    const setLocale = useCallback((newLocale: Locale) => {
        setLocaleState(newLocale);
        // Update document direction for RTL support
        if (typeof document !== 'undefined') {
            document.documentElement.dir = newLocale === 'ar' ? 'rtl' : 'ltr';
            document.documentElement.lang = newLocale;
        }
    }, []);

    const t = useCallback((key: string, params?: Record<string, string | number>) => {
        const value = getNestedValue(messages[locale], key);
        if (!value) {
            console.warn(`Missing translation: ${key} for locale: ${locale}`);
            return key;
        }
        return interpolate(value, params);
    }, [locale]);

    const dir = locale === 'ar' ? 'rtl' : 'ltr';

    return (
        <I18nContext.Provider value={{ locale, dir, t, setLocale }}>
            {children}
        </I18nContext.Provider>
    );
}

export function useI18n() {
    const context = useContext(I18nContext);
    if (!context) {
        throw new Error('useI18n must be used within I18nProvider');
    }
    return context;
}

/**
 * Language toggle component.
 */
export function LanguageToggle() {
    const { locale, setLocale } = useI18n();

    return (
        <button
            onClick={() => setLocale(locale === 'en' ? 'ar' : 'en')}
            className="language-toggle"
            aria-label={locale === 'en' ? 'Switch to Arabic' : 'Switch to English'}
            style={{
                position: 'fixed',
                top: '1rem',
                right: locale === 'ar' ? 'auto' : '1rem',
                left: locale === 'ar' ? '1rem' : 'auto',
                zIndex: 1000,
                background: 'var(--glass-bg)',
                border: '1px solid var(--glass-border)',
                borderRadius: '0.5rem',
                padding: '0.5rem 1rem',
                color: 'var(--text-primary)',
                cursor: 'pointer',
                backdropFilter: 'blur(10px)',
                fontSize: '0.875rem',
                fontWeight: 600,
                transition: 'all 0.2s ease',
            }}
        >
            {locale === 'en' ? 'العربية' : 'English'}
        </button>
    );
}
