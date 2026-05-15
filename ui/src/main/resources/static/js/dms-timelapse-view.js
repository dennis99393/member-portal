/**
 * <dms-timelapse-view stream="id" [buffer-secs="60"] [interval="1500"] [variant="thumbnail"]>
 *
 * Attributes:
 *   stream      — chrono-ring stream ID (required)
 *   buffer-secs — rolling buffer duration in seconds (default 60)
 *   interval    — live poll interval in ms (default 1500)
 *   variant     — "thumbnail" for minimalist single-frame view; absent = live mode
 *
 * URLs are routed through the member-portal proxy at /cameras-api/...
 */

// Polls /cameras-api/streams once every 5s while at least one live component
// is active, dispatching 'cameras-streams-update' on document each time.
const StreamsPoller = (() => {
  let timer = null;
  let count = 0;

  async function poll() {
    try {
      const res = await fetch('/cameras-api/streams');
      if (!res.ok) return;
      const streams = await res.json();
      document.dispatchEvent(new CustomEvent('cameras-streams-update', { detail: streams }));
    } catch (_) {}
  }

  return {
    subscribe()   { if (++count === 1) { poll(); timer = setInterval(poll, 5000); } },
    unsubscribe() { if (--count === 0) { clearInterval(timer); timer = null; } },
  };
})();

class DmsTimelapseView extends HTMLElement {
  static get observedAttributes() {
    return ['stream', 'buffer-secs', 'interval', 'variant'];
  }

  constructor() {
    super();
    this._root = this.attachShadow({ mode: 'open' });
    this._timer = null;
    this._clockTimer = null;
    this._streamsListener = null;
    this._buffer = [];
    this._scrubbing = false;
    this._connected = true;
    this._onFsChange = null;
    this._rendered = false;
  }

  connectedCallback() { if (!this._rendered) this._render(); }
  disconnectedCallback() { this._teardown(); }
  attributeChangedCallback() { this._teardown(); this._render(); }

  _teardown() {
    clearInterval(this._timer);
    clearInterval(this._clockTimer);
    this._timer = null;
    this._clockTimer = null;
    this._scrubbing = false;
    this._rendered = false;
    if (this._streamsListener) {
      document.removeEventListener('cameras-streams-update', this._streamsListener);
      this._streamsListener = null;
      StreamsPoller.unsubscribe();
    }
    if (this._onFsChange) {
      this.removeEventListener('fullscreenchange', this._onFsChange);
      this._onFsChange = null;
    }
  }

  _render() {
    const stream = this.getAttribute('stream');
    if (!stream) return;
    this._rendered = true;
    if (this.getAttribute('variant') === 'thumbnail') {
      this._renderThumbnail(stream);
      return;
    }
    this._renderLive(stream);
  }

  // ── Live mode ─────────────────────────────────────────────────────────────

  _renderLive(stream) {
    this._root.innerHTML = `
      <style>
        :host { display: block; background: #000; }
        .frame-wrap {
          position: relative; line-height: 0; overflow: hidden;
          aspect-ratio: 16/9;
        }
        img.main { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: contain; }
        .spinner {
          position: absolute; inset: 0;
          display: flex; align-items: center; justify-content: center;
          color: #888; font-size: .8rem; gap: 8px;
        }
        .spinner.hidden { display: none; }
        .error { color: #f55; font-size: .8rem; word-break: break-word; }
        .badge {
          position: absolute; bottom: 6px; right: 8px;
          background: rgba(0,0,0,.55); color: #0f0; font-size: .65rem;
          padding: 2px 6px; border-radius: 4px; font-family: monospace;
          display: none; align-items: center; gap: 4px;
        }
        .badge.visible { display: flex; }
        .badge.offline { color: #f55; }
        .dot {
          width: 7px; height: 7px; border-radius: 50%; background: #0f0;
          animation: pulse 1.4s ease-in-out infinite;
        }
        .badge.offline .dot { background: #f55; animation: none; }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.3} }
        .ts {
          position: absolute; bottom: 6px; left: 8px; display: none;
          color: #fff; font-size: .65rem; font-family: monospace;
          padding: 2px 6px; border-radius: 4px;
          text-shadow: 0 1px 3px rgba(0,0,0,.9);
          filter: drop-shadow(0 1px 4px rgba(0,0,0,.8));
        }
        .ts.visible { display: block; }
        .ts.stale { color: #aaa; }
        .timeline {
          position: relative; height: 18px; background: #e8e8e8;
          cursor: crosshair; user-select: none; touch-action: none;
          display: none;
        }
        .timeline.visible { display: block; }
        .ticks { position: absolute; inset: 0; }
        .ticks span { position: absolute; top: 0; bottom: 0; width: 1px; background: #4af; opacity: .7; }
        .cursor { position: absolute; top: -2px; bottom: -2px; width: 2px; background: #fff; pointer-events: none; }
        .btn-fs {
          position: absolute; top: 6px; right: 8px;
          background: rgba(0,0,0,.45); border: none; color: #fff;
          border-radius: 4px; padding: 4px 5px; cursor: pointer;
          line-height: 0; display: none;
        }
        .btn-fs.visible { display: block; }
        .btn-fs:hover { background: rgba(0,0,0,.75); }
        :host(:fullscreen), :host(:-webkit-full-screen) {
          display: flex; flex-direction: column; background: #000;
          width: 100vw; height: 100vh;
        }
        :host(:fullscreen) .frame-wrap, :host(:-webkit-full-screen) .frame-wrap {
          flex: 1; aspect-ratio: unset; overflow: hidden;
        }
        :host(:fullscreen) .timeline, :host(:-webkit-full-screen) .timeline {
          flex-shrink: 0;
        }
      </style>
      <div class="frame-wrap">
        <img class="main" style="display:none" alt="live feed"/>
        <img class="preload" style="display:none" alt=""/>
        <div class="spinner"><span>⏳</span><span>Loading…</span></div>
        <div class="ts"></div>
        <div class="badge"><div class="dot"></div><span class="badge-label">LIVE</span></div>
        <button class="btn-fs" title="Enter fullscreen">
          <svg class="icon-enter" viewBox="0 0 16 16" width="14" height="14" fill="currentColor">
            <path d="M1.5 1h4v1.5h-2.5v2.5h-1.5zm9 0h4v4h-1.5v-2.5h-2.5v-1.5zm-9 9h1.5v2.5h2.5v1.5h-4zm10.5 2.5v-2.5h1.5v4h-4v-1.5h2.5z"/>
          </svg>
          <svg class="icon-exit" viewBox="0 0 16 16" width="14" height="14" fill="currentColor" style="display:none">
            <path d="M5.5 0v4h-4v-1.5h2.5v-2.5zm5 0h1.5v2.5h2.5v1.5h-4zm-9 9.5h4v4h-1.5v-2.5h-2.5zm8.5 1.5v-2.5h1.5v4h-4v-1.5h2.5z"/>
          </svg>
        </button>
      </div>
      <div class="timeline" title="Hover to preview · release to resume live">
        <div class="ticks"></div>
        <div class="cursor" style="display:none"></div>
      </div>
    `;

    const mainImg    = this._root.querySelector('img.main');
    const preload    = this._root.querySelector('img.preload');
    const spinner    = this._root.querySelector('.spinner');
    const badge      = this._root.querySelector('.badge');
    const badgeLabel = this._root.querySelector('.badge-label');
    const tsEl       = this._root.querySelector('.ts');
    const timeline   = this._root.querySelector('.timeline');
    const ticksEl    = this._root.querySelector('.ticks');
    const cursor     = this._root.querySelector('.cursor');
    const fsBtn      = this._root.querySelector('.btn-fs');
    const iconEnter  = fsBtn.querySelector('.icon-enter');
    const iconExit   = fsBtn.querySelector('.icon-exit');

    fsBtn.addEventListener('click', () => {
      if (!document.fullscreenElement) {
        this.requestFullscreen().catch(() => {});
      } else {
        document.exitFullscreen();
      }
    });
    this._onFsChange = () => {
      const isFs = !!document.fullscreenElement;
      iconEnter.style.display = isFs ? 'none' : '';
      iconExit.style.display  = isFs ? '' : 'none';
      fsBtn.title = isFs ? 'Exit fullscreen' : 'Enter fullscreen';
      if (!isFs && this.getAttribute('variant') === 'thumbnail') {
        this._teardown();
        this._renderThumbnail(stream);
      }
    };
    this.addEventListener('fullscreenchange', this._onFsChange);

    const bufferMs = () =>
      (parseInt(this.getAttribute('buffer-secs') || '60', 10)) * 1000;

    const updateTicks = (frames) => {
      const now = Date.now();
      const start = now - bufferMs();
      ticksEl.innerHTML = '';
      for (const f of frames) {
        const t = new Date(f.timestamp).getTime();
        const pos = (t - start) / bufferMs();
        if (pos < 0 || pos > 1) continue;
        const tick = document.createElement('span');
        tick.style.left = (pos * 100).toFixed(2) + '%';
        ticksEl.appendChild(tick);
      }
    };

    const poll = async () => {
      try {
        const res = await fetch(`/cameras-api/streams/${encodeURIComponent(stream)}/timelapse`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const frames = await res.json();
        if (!frames || !frames.length) return;
        this._buffer = frames;
        updateTicks(frames);

        const latest = frames[frames.length - 1];
        const src = `/cameras-api/frame/${encodeURIComponent(latest.id)}`;

        preload.onload = () => {
          if (this._scrubbing) return;
          mainImg.src = preload.src;
          mainImg.style.display = '';
          spinner.classList.add('hidden');
          badge.classList.add('visible');
          tsEl.classList.add('visible');
          timeline.classList.add('visible');
          fsBtn.classList.add('visible');
        };
        preload.onerror = () => {};
        preload.src = src;
      } catch (e) {
        if (!spinner.classList.contains('hidden')) {
          spinner.innerHTML = `<span class="error">Error: ${escHtml(e.message)}</span>`;
        }
      }
    };

    // Scrub interactions
    const onScrub = (clientX) => {
      if (!this._buffer.length) return;
      const rect = timeline.getBoundingClientRect();
      const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
      cursor.style.left = (x * 100) + '%';
      cursor.style.display = '';
      const targetMs = (Date.now() - bufferMs()) + x * bufferMs();
      let best = null, bestDiff = Infinity;
      for (const f of this._buffer) {
        const diff = Math.abs(new Date(f.timestamp).getTime() - targetMs);
        if (diff < bestDiff) { bestDiff = diff; best = f; }
      }
      if (best) {
        this._scrubbing = true;
        mainImg.src = `/cameras-api/frame/${encodeURIComponent(best.id)}`;
        tsEl.textContent = new Date(targetMs).toLocaleTimeString();
      }
    };

    const onRelease = () => {
      this._scrubbing = false;
      cursor.style.display = 'none';
    };

    timeline.addEventListener('mousemove', e => onScrub(e.clientX));
    timeline.addEventListener('mouseleave', onRelease);
    timeline.addEventListener('touchstart', e => { e.preventDefault(); onScrub(e.touches[0].clientX); }, { passive: false });
    timeline.addEventListener('touchmove',  e => { e.preventDefault(); onScrub(e.touches[0].clientX); }, { passive: false });
    timeline.addEventListener('touchend', onRelease);

    // Wall-clock timer: advance timestamp every second unless scrubbing or offline
    this._clockTimer = setInterval(() => {
      if (this._scrubbing) return;
      if (this._connected === false) return;
      if (tsEl.classList.contains('visible')) {
        tsEl.textContent = new Date().toLocaleTimeString();
      }
    }, 1000);

    // Subscribe to the shared streams poller for connected-state updates
    this._streamsListener = (e) => {
      const entry = e.detail.find(s => s.id === stream);
      if (!entry) return;
      this._connected = entry.connected;
      badge.classList.toggle('offline', !this._connected);
      badgeLabel.textContent = this._connected ? 'LIVE' : 'OFFLINE';
      tsEl.classList.toggle('stale', !this._connected);
    };
    document.addEventListener('cameras-streams-update', this._streamsListener);
    StreamsPoller.subscribe();

    poll();
    const intervalMs = parseInt(this.getAttribute('interval') || '1500', 10);
    this._timer = setInterval(poll, intervalMs);
  }

  // ── Thumbnail mode ────────────────────────────────────────────────────────

  async _renderThumbnail(stream) {
    this._root.innerHTML = `
      <style>
        :host { display: block; background: #000; }
        .thumb-wrap {
          position: relative; aspect-ratio: 16/9; overflow: hidden; line-height: 0;
        }
        img.thumb { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: contain; }
        .spinner {
          position: absolute; inset: 0;
          display: flex; align-items: center; justify-content: center;
          color: #888; font-size: .8rem;
        }
        .spinner.hidden { display: none; }
        .status-dot {
          position: absolute; bottom: 6px; right: 8px;
          width: 10px; height: 10px; border-radius: 50%;
          background: #0f0; box-shadow: 0 0 4px rgba(0,200,0,.7);
          animation: pulse 1.4s ease-in-out infinite;
          display: none;
        }
        .status-dot.visible { display: block; }
        .status-dot.offline { background: #f55; box-shadow: 0 0 4px rgba(200,0,0,.7); animation: none; }
        @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.3} }
        .btn-expand {
          position: absolute; top: 50%; left: 50%;
          transform: translate(-50%, -50%);
          background: rgba(0,0,0,.55); border: 2px solid rgba(255,255,255,.55);
          color: #fff; border-radius: 8px; padding: 10px 12px;
          cursor: pointer; line-height: 0; display: none;
        }
        .btn-expand.visible { display: block; }
        .btn-expand:hover { background: rgba(0,0,0,.85); border-color: #fff; }
      </style>
      <div class="thumb-wrap">
        <img class="thumb" style="display:none" alt="camera thumbnail"/>
        <div class="spinner"><span>⏳</span></div>
        <div class="status-dot"></div>
        <button class="btn-expand" title="Enter fullscreen">
          <svg viewBox="0 0 16 16" width="22" height="22" fill="currentColor">
            <path d="M1.5 1h4v1.5h-2.5v2.5h-1.5zm9 0h4v4h-1.5v-2.5h-2.5v-1.5zm-9 9h1.5v2.5h2.5v1.5h-4zm10.5 2.5v-2.5h1.5v4h-4v-1.5h2.5z"/>
          </svg>
        </button>
      </div>
    `;

    const thumbImg  = this._root.querySelector('img.thumb');
    const spinner   = this._root.querySelector('.spinner');
    const statusDot = this._root.querySelector('.status-dot');
    const expandBtn = this._root.querySelector('.btn-expand');

    // Check connected state
    try {
      const res = await fetch('/cameras-api/streams');
      if (res.ok) {
        const streams = await res.json();
        const entry = streams.find(s => s.id === stream);
        if (entry) {
          this._connected = entry.connected;
          statusDot.classList.toggle('offline', !entry.connected);
          statusDot.classList.add('visible');
        }
      }
    } catch (_) {}

    // Load latest frame
    try {
      const res = await fetch(`/cameras-api/streams/${encodeURIComponent(stream)}/timelapse`);
      if (res.ok) {
        const frames = await res.json();
        if (frames && frames.length) {
          const latest = frames[frames.length - 1];
          thumbImg.onload = () => {
            thumbImg.style.display = '';
            spinner.classList.add('hidden');
            expandBtn.classList.add('visible');
          };
          thumbImg.onerror = () => {
            spinner.innerHTML = '<span style="color:#f55">No image</span>';
          };
          thumbImg.src = `/cameras-api/frame/${encodeURIComponent(latest.id)}`;
        } else {
          spinner.innerHTML = '<span>No frames</span>';
        }
      }
    } catch (_) {
      spinner.innerHTML = '<span style="color:#f55">Error</span>';
    }

    expandBtn.addEventListener('click', () => {
      this._teardown();
      this._rendered = true;
      this._renderLive(stream);
      this.requestFullscreen().catch(() => {});
    });
  }
}

function escHtml(str) {
  return String(str).replace(/[&<>"']/g, c =>
    ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

customElements.define('dms-timelapse-view', DmsTimelapseView);
