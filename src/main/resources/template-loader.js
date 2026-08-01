// Template loader for including header and footer components
async function loadTemplate(templateName, elementId) {
    try {
        const response = await fetch(`./${templateName}.html`);
        if (!response.ok) {
            throw new Error(`Failed to load ${templateName}.html`);
        }
        const html = await response.text();
        document.getElementById(elementId).innerHTML = html;
        return true;
    } catch (error) {
        console.error(`Error loading template ${templateName}:`, error);
        return false;
    }
}

// Load templates when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    const headerElement = document.getElementById('header-placeholder');
    const footerElement = document.getElementById('footer-placeholder');

    if (headerElement) {
        loadTemplate('header', 'header-placeholder').then(function() {
            var currentPage = window.location.pathname.split('/').pop() || 'home.html';
            // Highlight top-level nav links
            var links = document.querySelectorAll('#header-placeholder nav ul > li > a');
            links.forEach(function(link) {
                var href = link.getAttribute('href');
                if (!href || href === '#') return;
                var linkPage = href.split('/').pop();
                if (linkPage === currentPage) {
                    link.classList.add('active');
                }
            });
            // Highlight dropdown parent if a child page is active
            var dropdownLinks = document.querySelectorAll('#header-placeholder .dropdown-menu a');
            dropdownLinks.forEach(function(link) {
                var href = link.getAttribute('href');
                if (!href) return;
                var linkPage = href.split('/').pop();
                if (linkPage === currentPage) {
                    var parent = link.closest('.nav-dropdown');
                    if (parent) {
                        parent.querySelector(':scope > a').classList.add('active');
                    }
                }
            });
            // Click-to-toggle dropdown
            var dropdowns = document.querySelectorAll('#header-placeholder .nav-dropdown > a');
            dropdowns.forEach(function(trigger) {
                trigger.addEventListener('click', function(e) {
                    e.preventDefault();
                    var parent = trigger.closest('.nav-dropdown');
                    var wasOpen = parent.classList.contains('open');
                    // Close all dropdowns first
                    document.querySelectorAll('#header-placeholder .nav-dropdown').forEach(function(d) {
                        d.classList.remove('open');
                    });
                    if (!wasOpen) {
                        parent.classList.add('open');
                    }
                });
            });
            // Close dropdown when clicking outside
            document.addEventListener('click', function(e) {
                if (!e.target.closest('.nav-dropdown')) {
                    document.querySelectorAll('#header-placeholder .nav-dropdown').forEach(function(d) {
                        d.classList.remove('open');
                    });
                }
            });
        });
    }
    if (footerElement) {
        loadTemplate('footer', 'footer-placeholder');
    }
});

