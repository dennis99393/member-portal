/**
 * Snowflake Effect - Self-contained falling snow animation
 * Displays between Dec 24 and Jan 1, or with ?snow=true override
 */
(function() {
  'use strict';

  const SNOWFLAKE_COUNT = 50;
  const SNOWFLAKE_CHARS = ['❄', '❅', '❆', '✦', '✧'];

  function isHolidaySeason() {
    const now = new Date();
    const month = now.getMonth(); // 0-indexed (11 = December, 0 = January)
    const day = now.getDate();

    // Dec 24-31 OR Jan 1
    return (month === 11 && day >= 24) || (month === 0 && day === 1);
  }

  function hasOverride() {
    return window.location.hash === '#snow';
  }

  function shouldShowSnow() {
    return isHolidaySeason() || hasOverride();
  }

  function injectStyles() {
    const css = `
      .snowflake-container {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        pointer-events: none;
        z-index: 1000;
        overflow: hidden;
      }
      .snowflake {
        position: absolute;
        top: -20px;
        color: rgba(255, 255, 255, 0.8);
        text-shadow: 0 0 3px rgba(200, 220, 255, 0.8);
        user-select: none;
        animation: snowfall linear infinite;
      }
      @keyframes snowfall {
        0% {
          transform: translateY(0) rotate(0deg);
          opacity: 1;
        }
        100% {
          transform: translateY(100vh) rotate(360deg);
          opacity: 0.3;
        }
      }
    `;
    const style = document.createElement('style');
    style.textContent = css;
    document.head.appendChild(style);
  }

  function createSnowflakes() {
    const container = document.createElement('div');
    container.className = 'snowflake-container';
    container.setAttribute('aria-hidden', 'true');

    for (let i = 0; i < SNOWFLAKE_COUNT; i++) {
      const flake = document.createElement('span');
      flake.className = 'snowflake';
      flake.textContent = SNOWFLAKE_CHARS[Math.floor(Math.random() * SNOWFLAKE_CHARS.length)];
      flake.style.left = Math.random() * 100 + '%';
      flake.style.fontSize = (Math.random() * 10 + 10) + 'px';
      flake.style.animationDuration = (Math.random() * 5 + 8) + 's';
      flake.style.animationDelay = (Math.random() * 10) + 's';
      container.appendChild(flake);
    }

    document.body.appendChild(container);
  }

  function init() {
    if (!shouldShowSnow()) return;
    injectStyles();
    createSnowflakes();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
