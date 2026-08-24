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

/* ─────────────────────────────────────────────
   Radio Browser
   ───────────────────────────────────────────── */

document.addEventListener('DOMContentLoaded', () => {
  const searchInput = document.getElementById('radio-search');
  const searchButton = document.getElementById('radio-search-button');
  const results = document.getElementById('radio-results');
  const status = document.getElementById('radio-status');
  const audio = document.getElementById('radio-audio');
  const current = document.getElementById('radio-current');
  const meta = document.getElementById('radio-meta');
  const art = document.getElementById('radio-art');
  const mainButton = document.getElementById('radio-main-button');
  const playIcon = document.getElementById('radio-play-icon');
  const pauseIcon = document.getElementById('radio-pause-icon');
  const stopButton = document.getElementById('radio-stop');
  const volume = document.getElementById('radio-volume');
  const live = document.getElementById('radio-live');
  const resultsTitle = document.getElementById('radio-results-title');

  if (!searchInput || !searchButton || !results || !audio) return;

  const apiServers = [
    'https://de1.api.radio-browser.info',
    'https://de2.api.radio-browser.info',
    'https://at1.api.radio-browser.info'
  ];

  function escapeHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  async function radioRequest(path) {
    let lastError;

    for (const server of apiServers) {
      try {
        const response = await fetch(server + path, {
          headers: {
            'Accept': 'application/json'
          }
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        return await response.json();
      } catch (error) {
        lastError = error;
      }
    }

    throw lastError || new Error('Radio Browser unavailable');
  }

  function setPlayingUI(playing) {
    if (playing) {
      playIcon.style.display = 'none';
      pauseIcon.style.display = 'block';
      live.classList.add('playing');
      live.innerHTML = '<span></span> LIVE';
    } else {
      playIcon.style.display = 'block';
      pauseIcon.style.display = 'none';
      live.classList.remove('playing');
      live.innerHTML = '<span></span> READY';
    }
  }

  function setArtwork(url) {
    if (!url) {
      art.innerHTML = '<div class="cc-radio-art-placeholder">♪</div>';
      return;
    }

    art.innerHTML = `
      <img
        src="${escapeHtml(url)}"
        alt=""
        onerror="this.parentElement.innerHTML='<div class=&quot;cc-radio-art-placeholder&quot;>♪</div>';"
      >
    `;
  }

  function stationMeta(station) {
    return [
      station.country,
      station.codec,
      station.bitrate ? `${station.bitrate} kbps` : null
    ].filter(Boolean).join(' · ');
  }

  function renderStations(stations) {
    results.innerHTML = '';

    if (!stations.length) {
      resultsTitle.textContent = 'No stations found';
      status.textContent = '';
      return;
    }

    resultsTitle.textContent = 'Stations';
    status.textContent = `${stations.length} result${stations.length === 1 ? '' : 's'}`;

    stations.forEach((station) => {
      const item = document.createElement('button');

      item.type = 'button';
      item.className = 'cc-radio-station';
      item.dataset.uuid = station.stationuuid || '';

      const artwork = station.favicon
        ? `
          <img
            src="${escapeHtml(station.favicon)}"
            alt=""
            onerror="this.style.display='none';"
          >
        `
        : '♪';

      item.innerHTML = `
        <span class="cc-radio-station-art">${artwork}</span>

        <span class="cc-radio-station-copy">
          <span class="cc-radio-station-name">
            ${escapeHtml(station.name || 'Unknown station')}
          </span>

          <span class="cc-radio-station-meta">
            ${escapeHtml(stationMeta(station) || 'Internet radio')}
          </span>
        </span>

        <span class="cc-radio-station-play">▶</span>
      `;

      item.addEventListener('click', () => playStation(station, item));

      results.appendChild(item);
    });
  }

  async function searchStations(queryOverride = null) {
    const query = queryOverride ?? searchInput.value.trim();

    if (!query) {
      results.innerHTML = '';
      resultsTitle.textContent = 'Popular stations';
      status.textContent = '';
      return;
    }

    searchButton.disabled = true;
    status.textContent = 'Searching…';
    results.innerHTML = '';

    try {
      const encoded = encodeURIComponent(query);

      const stations = await radioRequest(
        `/json/stations/search?name=${encoded}&limit=30&order=clickcount&reverse=true`
      );

      renderStations(stations);
    } catch (error) {
      console.error('Radio Browser search failed:', error);
      resultsTitle.textContent = 'Radio Browser unavailable';
      status.textContent = 'Please try again.';
    } finally {
      searchButton.disabled = false;
    }
  }

  async function playStation(station, item) {
    if (!station.stationuuid) {
      status.textContent = 'This station has no valid ID.';
      return;
    }

    document.querySelectorAll('.cc-radio-station.active')
      .forEach((element) => element.classList.remove('active'));

    item?.classList.add('active');

    current.textContent = station.name || 'Unknown station';
    meta.textContent = stationMeta(station) || 'Internet radio';
    setArtwork(station.favicon);
    status.textContent = 'Connecting…';

    try {
      const data = await radioRequest(
        `/json/url/${encodeURIComponent(station.stationuuid)}`
      );

      if (!data || !data.ok || !data.url) {
        throw new Error('No playable stream URL returned');
      }

      audio.src = data.url;
      await audio.play();

      setPlayingUI(true);
      status.textContent = 'Streaming live';
    } catch (error) {
      console.error('Radio Browser playback failed:', error);
      setPlayingUI(false);
      status.textContent = 'Unable to play this station.';
      audio.removeAttribute('src');
      audio.load();
    }
  }

  mainButton?.addEventListener('click', async () => {
    if (!audio.src) {
      status.textContent = 'Choose a station first.';
      return;
    }

    if (audio.paused) {
      try {
        await audio.play();
      } catch (error) {
        console.error('Playback failed:', error);
        status.textContent = 'Unable to resume playback.';
      }
    } else {
      audio.pause();
    }
  });

  stopButton?.addEventListener('click', () => {
    audio.pause();
    audio.removeAttribute('src');
    audio.load();

    current.textContent = 'Choose a station';
    meta.textContent = 'Search below or pick a station to start listening.';
    setArtwork(null);

    document.querySelectorAll('.cc-radio-station.active')
      .forEach((element) => element.classList.remove('active'));

    setPlayingUI(false);
    status.textContent = '';
  });

  volume?.addEventListener('input', () => {
    audio.volume = Number(volume.value);
  });

  audio.volume = Number(volume?.value ?? 0.85);

  audio.addEventListener('play', () => setPlayingUI(true));
  audio.addEventListener('pause', () => setPlayingUI(false));

  searchButton.addEventListener('click', () => searchStations());

  searchInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      searchStations();
    }
  });

  /* Initial stations */
  searchStations('rock');
});
