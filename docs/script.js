const NAV = [
  {
    title: 'Getting Started',
    links: [
      { label: 'Home', file: 'README.md' },
      { label: 'Installation', file: 'installation.md' },
      { label: 'Features', file: 'features.md' },
      { label: 'Rank Buffs', file: 'rank-buffs.md' },
    ]
  },
  {
    title: 'Usage',
    links: [
      { label: 'Commands', file: 'commands.md' },
      { label: 'Permissions', file: 'permissions.md' },
      { label: 'Configuration', file: 'configuration.md' },
      { label: 'Messages', file: 'messages.md' },
    ]
  },
  {
    title: 'Features',
    links: [
      { label: 'Hierarchy Hammer', file: 'hierarchy-hammer.md' },
      { label: 'TAB Integration', file: 'tab-integration.md' },
      { label: 'PlaceholderAPI', file: 'placeholders.md' },
    ]
  },
  {
    title: 'Integration',
    links: [
      { label: 'Discord Bot', file: 'discord.md' },
    ]
  },
  {
    title: 'Help',
    links: [
      { label: 'FAQ', file: 'faq.md' },
      { label: 'Support', file: 'support.md' },
    ]
  },
];

function renderSidebar() {
  const navEl = document.getElementById('sidebarNav');
  navEl.innerHTML = '';

  NAV.forEach(section => {
    const div = document.createElement('div');
    div.className = 'section';

    const title = document.createElement('div');
    title.className = 'section-title';
    title.textContent = section.title;
    div.appendChild(title);

    section.links.forEach(link => {
      const a = document.createElement('a');
      a.href = '#' + link.file;
      a.textContent = link.label;
      a.dataset.file = link.file;
      div.appendChild(a);
    });

    navEl.appendChild(div);
  });
}

function getTheme() {
  return localStorage.getItem('theme') || 'dark';
}

function setTheme(theme) {
  document.documentElement.dataset.theme = theme;
  localStorage.setItem('theme', theme);
  const icon = theme === 'dark' ? '🌙' : '☀️';
  document.getElementById('themeToggle').textContent = icon;
  const mobileBtnToggle = document.getElementById('themeToggleMobile');
  if (mobileBtnToggle) {
    mobileBtnToggle.textContent = icon;
  }
}

function toggleTheme() {
  const currentTheme = getTheme();
  const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
  setTheme(newTheme);
}

function toggleMobileMenu() {
  document.getElementById('sidebar').classList.toggle('open');
}

function closeMobileMenu(e) {
  if (e.target.tagName === 'A') {
    document.getElementById('sidebar').classList.remove('open');
  }
}

let currentFile = '';
const contentEl = document.getElementById('pageContent');

async function loadPage(file) {
  if (file === currentFile) return;
  currentFile = file;
  contentEl.innerHTML = '<div class="loading">Loading...</div>';

  try {
    const response = await fetch(file);
    if (!response.ok) throw new Error('Not found');

    const markdown = await response.text();
    const html = marked.parse(markdown);

    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;
    tempDiv.querySelectorAll('a[href$=".md"]').forEach(a => {
      const href = a.getAttribute('href');
      if (href && !href.startsWith('http') && !href.startsWith('#') && href.endsWith('.md')) {
        a.href = '#' + href;
      }
    });

    contentEl.innerHTML = tempDiv.innerHTML;

    if (typeof Prism !== 'undefined') {
      contentEl.querySelectorAll('pre code').forEach(block => {
        Prism.highlightElement(block);
      });
    }

    document.querySelectorAll('.sidebar-nav a').forEach(a => {
      a.classList.toggle('active', a.dataset.file === file);
    });

    const activeLink = document.querySelector('.sidebar-nav a.active');
    const pageName = activeLink ? activeLink.textContent : file;
    document.title = file !== 'README.md' 
      ? `RankedSMPX Docs — ${pageName}` 
      : 'RankedSMPX Docs';

    generateTableOfContents();

  } catch (error) {
    contentEl.innerHTML = `<div class="error">Could not load <strong>${file}</strong>. The file may not exist yet.</div>`;
    clearTableOfContents();
  }
}

function generateTableOfContents() {
  const tocNav = document.getElementById('tocNav');
  const headings = contentEl.querySelectorAll('h2, h3, h4');
  
  if (headings.length === 0) {
    tocNav.innerHTML = '<div class="toc-empty">No headings found</div>';
    return;
  }

  tocNav.innerHTML = '';
  
  headings.forEach((heading, index) => {
    if (!heading.id) {
      heading.id = heading.textContent
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)/g, '');
    }

    const link = document.createElement('a');
    link.href = '#' + heading.id;
    link.textContent = heading.textContent;
    link.className = 'toc-' + heading.tagName.toLowerCase();
    link.dataset.target = heading.id;
    
    link.addEventListener('click', (e) => {
      e.preventDefault();
      heading.scrollIntoView({ behavior: 'smooth', block: 'start' });
      updateTocActive(heading.id);
    });
    
    tocNav.appendChild(link);
  });

  setupScrollSpy();
}

function clearTableOfContents() {
  const tocNav = document.getElementById('tocNav');
  tocNav.innerHTML = '<div class="toc-empty">No headings found</div>';
}

let scrollSpyObserver = null;

function setupScrollSpy() {
  if (scrollSpyObserver) {
    scrollSpyObserver.disconnect();
  }

  const headings = contentEl.querySelectorAll('h2, h3, h4');
  
  if (headings.length === 0) return;

  const observerOptions = {
    root: null,
    rootMargin: '-80px 0px -80% 0px',
    threshold: 0
  };

  scrollSpyObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        updateTocActive(entry.target.id);
      }
    });
  }, observerOptions);

  headings.forEach(heading => {
    if (heading.id) {
      scrollSpyObserver.observe(heading);
    }
  });
}

function updateTocActive(activeId) {
  document.querySelectorAll('.toc-nav a').forEach(link => {
    link.classList.toggle('active', link.dataset.target === activeId);
  });
}

function onHashChange() {
  const file = location.hash.slice(1) || 'README.md';
  loadPage(file);
}

function initializeEventListeners() {
  document.getElementById('themeToggle').addEventListener('click', toggleTheme);
  const mobileBtnToggle = document.getElementById('themeToggleMobile');
  if (mobileBtnToggle) {
    mobileBtnToggle.addEventListener('click', toggleTheme);
  }

  const hamburger = document.getElementById('hamburger');
  if (hamburger) {
    hamburger.addEventListener('click', toggleMobileMenu);
  }

  document.getElementById('sidebar').addEventListener('click', closeMobileMenu);

  window.addEventListener('hashchange', onHashChange);
  window.addEventListener('load', onHashChange);
}

document.addEventListener('DOMContentLoaded', () => {
  renderSidebar();
  setTheme(getTheme());
  initializeEventListeners();
});
