// Template loader for including header and footer components
async function loadTemplate(templateName, elementId) {
    try {
        const response = await fetch(`./${templateName}.html`);
        if (!response.ok) {
            throw new Error(`Failed to load ${templateName}.html`);
        }
        const html = await response.text();
        document.getElementById(elementId).innerHTML = html;
    } catch (error) {
        console.error(`Error loading template ${templateName}:`, error);
    }
}

// Load templates when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    const headerElement = document.getElementById('header-placeholder');
    const footerElement = document.getElementById('footer-placeholder');

    if (headerElement) {
        loadTemplate('header', 'header-placeholder');
    }
    if (footerElement) {
        loadTemplate('footer', 'footer-placeholder');
    }
});

