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

function loadScript(src) {
    const script = document.createElement('script');
    script.src = src;
    document.head.appendChild(script);
}

function printDMSItBanner() {
    var titleCss = "font-size: 40px;color: red;font-weight: bold;";

    console.log("%cDMS IT Infra", titleCss);
    console.log("%cThis site is built by DMS IT Infra volunteers. Come say hi at one of our meetings - https://calendar.dallasmakerspace.org/?category=84", 'color: green');
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