document.addEventListener("DOMContentLoaded", (event) => {
    console.log("DOM fully loaded and parsed");
    // Hide toast after 20 seconds
    setTimeout(() => {
        hideToast();
    }, 20000);
});


function hideToast() {
    const toastLive = document.getElementById('liveToast');
    const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLive);
    toastBootstrap.hide();
}