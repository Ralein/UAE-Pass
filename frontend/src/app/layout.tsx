import type { Metadata } from 'next';
import './globals.css';
import { I18nProvider, LanguageToggle } from '@/lib/i18n';

export const metadata: Metadata = {
    title: 'UAE Digital Identity',
    description: 'UAE National Digital Identity Platform — Secure registration and authentication',
};

export default function RootLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return (
        <html lang="en" dir="ltr" suppressHydrationWarning>
            <body suppressHydrationWarning>
                <I18nProvider defaultLocale="en">
                    <LanguageToggle />
                    {children}
                </I18nProvider>
            </body>
        </html>
    );
}

