document.addEventListener("DOMContentLoaded", (event) => {
    printDMSItBanner();
    // Hide toast after 20 seconds
    setTimeout(() => {
        hideToast();
    }, 20000);
});

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