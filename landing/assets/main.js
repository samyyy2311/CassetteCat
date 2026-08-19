document.addEventListener('DOMContentLoaded', () => {
  const captions = {
    home: "Your library at a glance: song, artist, and album counts, plus quick picks.",
    now: "Full playback controls, scrubber, and album art for whatever's playing.",
    library: 'Browse by song, artist, album, genre, or playlist.',
  };

  const lists = {
    home: [
      'Local library with folder filtering',
      'Library snapshot: song, artist, and album counts',
      'Quick picks and recently played',
      'Shuffle your entire library instantly',
    ],
    now: [
      'Graphic equalizer with AutoEq calibration',
      'Synced lyrics from LRCLIB or embedded tags',
      'Sleep timer to pause playback automatically',
      'Server credentials encrypted with hardware-backed AES-256',
    ],
    library: [
      'Subsonic and Jellyfin streaming, offline caching',
      'Browse by song, artist, album, genre, or playlist',
      'Sort and filter your library your way',
      'Backup and restore for your library and playlists',
    ],
  };

  const tabs = document.querySelectorAll('.app-tab');
  const shots = document.querySelectorAll('.app-shot');
  const caption = document.getElementById('app-caption');
  const list = document.getElementById('app-list');

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      const key = tab.dataset.tab;
      tabs.forEach((t) => t.classList.toggle('active', t === tab));
      shots.forEach((s) => s.classList.toggle('active', s.dataset.tab === key));
      if (caption && captions[key]) caption.textContent = captions[key];
      if (list && lists[key]) {
        list.innerHTML = lists[key]
          .map((item) => `<li style="padding:10px 0; font-size:14px; color:var(--text-2);">${item}</li>`)
          .join('');
      }
    });
  });

  const heroShots = document.querySelectorAll('.hero-shot');
  if (heroShots.length > 1 && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    let i = 0;
    setInterval(() => {
      heroShots[i].classList.remove('active');
      i = (i + 1) % heroShots.length;
      heroShots[i].classList.add('active');
    }, 3400);
  }

  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', () => {
      const root = document.documentElement;
      const isLight = root.getAttribute('data-theme') === 'light';
      const next = isLight ? 'dark' : 'light';
      if (next === 'light') {
        root.setAttribute('data-theme', 'light');
      } else {
        root.removeAttribute('data-theme');
      }
      try { localStorage.setItem('cc-theme', next); } catch (e) {}
      const metaTheme = document.querySelector('meta[name="theme-color"]');
      if (metaTheme) metaTheme.setAttribute('content', next === 'light' ? '#F4F1EB' : '#000000');
    });
  }
});
