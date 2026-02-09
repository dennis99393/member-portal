// Seasonal effect date ranges (modify dates here)
// Hash overrides: #snow, #hearts, #fireworks
window.SEASONAL_DATE_RANGES = {
    winter: [
        { startMonth: 12, startDay: 24, endMonth: 12, endDay: 31 },
        { startMonth: 1, startDay: 1, endMonth: 1, endDay: 1 }
    ],
    valentines: [
        { startMonth: 2, startDay: 13, endMonth: 2, endDay: 14 }
    ],
    independence: [
        { startMonth: 7, startDay: 3, endMonth: 7, endDay: 4 }
    ]
};

function shouldLoadSeasonalEffect() {
    // Check hash overrides
    if (['#snow', '#hearts', '#fireworks'].includes(window.location.hash)) {
        return true;
    }
    // Check date ranges
    const now = new Date();
    const month = now.getMonth() + 1;
    const day = now.getDate();
    for (const ranges of Object.values(window.SEASONAL_DATE_RANGES)) {
        for (const r of ranges) {
            if (r.startMonth === r.endMonth) {
                if (month === r.startMonth && day >= r.startDay && day <= r.endDay) return true;
            } else {
                if ((month === r.startMonth && day >= r.startDay) ||
                (month === r.endMonth && day <= r.endDay)) return true;
            }
        }
    }
    return false;
}

// Helper function to get platform/browser info for PWA tracking
function getPWAContext() {
    const ua = navigator.userAgent;
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches
                       || window.navigator.standalone
                       || document.referrer.includes('android-app://');

    return {
        platform: /Android/i.test(ua) ? 'android'
                : /iPhone|iPad|iPod/i.test(ua) ? 'ios'
                : /Windows/i.test(ua) ? 'windows'
                : /Mac/i.test(ua) ? 'macos'
                : /Linux/i.test(ua) ? 'linux'
                : 'unknown',
        browser: /Chrome/i.test(ua) && !/Edg/i.test(ua) ? 'chrome'
               : /Edg/i.test(ua) ? 'edge'
               : /Firefox/i.test(ua) ? 'firefox'
               : /Safari/i.test(ua) && !/Chrome/i.test(ua) ? 'safari'
               : 'other',
        isStandalone: isStandalone,
        displayMode: isStandalone ? 'standalone' : 'browser'
    };
}

document.addEventListener("DOMContentLoaded", (event) => {
    printDMSItBanner();
    // Hide toast after 20 seconds
    setTimeout(() => {
        hideToast();
    }, 20000);
    if (shouldLoadSeasonalEffect()) {
        loadScript('/static/js/falling-effect.js');
    }
});

// Service Worker Registration
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js')
            .then(registration => {
                console.log('ServiceWorker registered:', registration.scope);

                // Track successful registration
                if (typeof track === 'function') {
                    track('pwa_service_worker_registered', {
                        scope: registration.scope,
                        ...getPWAContext()
                    });
                }

                // Check for updates every hour
                setInterval(() => {
                    registration.update();
                }, 60 * 60 * 1000);

                // Listen for updates
                registration.addEventListener('updatefound', () => {
                    const newWorker = registration.installing;

                    if (typeof track === 'function') {
                        track('pwa_service_worker_update_found', {
                            state: newWorker.state
                        });
                    }

                    newWorker.addEventListener('statechange', () => {
                        if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                            console.log('New service worker available');

                            if (typeof track === 'function') {
                                track('pwa_service_worker_update_available', {
                                    previousController: !!navigator.serviceWorker.controller
                                });
                            }
                            // Optionally notify user about update
                        }
                    });
                });
            })
            .catch(err => {
                console.log('ServiceWorker registration failed:', err);

                // Track registration failures
                if (typeof track === 'function') {
                    track('pwa_service_worker_registration_failed', {
                        error: err.message || 'Unknown error',
                        ...getPWAContext()
                    });
                }
            });
    });
}

// PWA Install Tracking
let deferredPrompt;
let installPromptShownAt = null;

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    installPromptShownAt = Date.now();

    if (typeof track === 'function') {
        track('pwa_install_prompt_shown', {
            source: 'browser_native',
            ...getPWAContext()
        });
    }

    // Track user's choice (accept or dismiss)
    e.userChoice.then((choiceResult) => {
        if (typeof track === 'function') {
            track('pwa_install_prompt_result', {
                outcome: choiceResult.outcome, // 'accepted' or 'dismissed'
                source: 'browser_native',
                ...getPWAContext()
            });
        }
    });
});

window.addEventListener('appinstalled', () => {
    const installDuration = installPromptShownAt
        ? Date.now() - installPromptShownAt
        : null;

    if (typeof track === 'function') {
        track('pwa_installed', {
            source: 'browser_native',
            installDurationMs: installDuration,
            ...getPWAContext()
        });
    }

    deferredPrompt = null;
    installPromptShownAt = null;
});

// Track PWA launch source (once per session)
window.addEventListener('load', () => {
    const context = getPWAContext();

    if (context.isStandalone && !sessionStorage.getItem('pwa_launch_tracked')) {
        if (typeof track === 'function') {
            track('pwa_launched', {
                displayMode: 'standalone',
                launchSource: 'pwa_icon',
                ...context
            });
            sessionStorage.setItem('pwa_launch_tracked', 'true');
        }
    }
});

function loadScript(src) {
    const script = document.createElement('script');
    script.src = src;
    document.head.appendChild(script);
}

function printDMSItBanner() {
    var titleCss = "font-size: 40px;color: red;font-weight: bold;";

    console.log("%cDMS IT Infra", titleCss);
    console.log("%cThis site is built by DMS IT Infra volunteers. Come say hi at one of our meetings - https://calendar.dallasmakerspace.org/?category=103", 'color: green');
    console.log("%cWe're always looking for IT volunteers to help with the infrastructure, networking and more.", 'color: green');
}

function hideToast() {
    const toastLive = document.getElementById('liveToast');
    if (toastLive) {
        const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLive);
        toastBootstrap.hide();
    }
}

/**
 * Escape HTML to prevent XSS attacks
 * @param {string} text - The text to escape
 * @returns {string} The escaped HTML string
 */
function escapeHtml(text) {
    if (text == null) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}