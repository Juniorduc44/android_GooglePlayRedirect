# Source: https://docs.turnkey.com/api-reference/overview/intro












.base-ui-disable-scrollbar{scrollbar-width:none}.base-ui-disable-scrollbar::-webkit-scrollbar{display:none}

                                     

Introduction - Turnkey
































    





   

Documentation Index

Fetch the complete documentation index at:
 
/llms.txt


Use this file to discover all available pages before exploring further.


:root{--banner-height:0px!important}
 
:root {
    --primary: 76 72 255;
    --primary-light: 76 72 255;
    --primary-dark: 5 10 11;
    --tooltip-foreground: 255 255 255;
    --background-light: 255 255 255;
    --background-dark: 10 10 16;
    --gray-50: 245 245 250;
    --gray-100: 240 240 245;
    --gray-200: 224 224 230;
    --gray-300: 208 208 213;
    --gray-400: 160 160 166;
    --gray-500: 114 114 119;
    --gray-600: 82 82 87;
    --gray-700: 64 64 70;
    --gray-800: 39 39 45;
    --gray-900: 25 24 30;
    --gray-950: 12 12 17;
  }

  




Skip to main content
 












Turnkey
 home page
















Search...


⌘
K









Ask Assistant








Support





Blog





Contact us





Get started








Get started























































Search...





















Navigation












REST API






Introduction







Home



Solutions



Documentation



API & SDK reference



Security





:root{--topbar-tabs-height:3rem}
















REST API








Introduction









Stamps









Errors








Activities













Queries













Auth Proxy















SDK reference








Introduction








React wallet kit













React Native wallet kit













TypeScript core














Flutter









Swift









Kotlin









TypeScript server









Go









Ruby









Rust









Python








Web3 libraries













Advanced















Changelogs







SDK changelogs













API changelog













TVC changelog











 






  


/* Custom Dropdown for SMS Price Lookup */
.sms-price-lookup-container {
  position: relative;
  margin-top: 1rem;
  margin-bottom: 1rem;
}

.sms-country-trigger {
  display: block;
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 1rem;
  font-family: inherit;
  line-height: 1.5;
  color: #495057;
  background-color: #fff;
  background-clip: padding-box;
  border: 1px solid #ced4da;
  border-radius: 0.25rem;
  text-align: left;
  cursor: pointer;
  padding-right: 2rem;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3e%3cpath fill='none' stroke='%23343a40' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M2 5l6 6 6-6'/%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 16px 12px;
}

.sms-country-list {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  max-height: 200px;
  overflow-y: auto;
  background-color: #fff;
  border: 1px solid #ced4da;
  border-top: none;
  border-radius: 0 0 0.25rem 0.25rem;
  z-index: 1000;
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.15);
  margin: 0;
  padding: 0;
  list-style: none;
}

.sms-country-item {
  display: block;
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 1rem;
  font-family: inherit;
  color: #495057;
  text-align: left;
  background-color: transparent;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}

.sms-country-item:hover,
.sms-country-item:focus {
  background-color: #f8f9fa;
  color: #16181b;
  outline: none;
}

#hidden-content-simple[data-open="false"] { display: none; }
#hidden-content-simple[data-open="true"]  { display: block; }

/* ============================================================
   ABC Favorit — hero headline only
   ============================================================ */

@font-face {
  font-family: 'ABC Favorit';
  src: url('/fonts/ABCFavorit-Regular.woff2') format('woff2');
  font-weight: 400;
  font-style: normal;
  font-display: swap;
}

@font-face {
  font-family: 'ABC Favorit';
  src: url('/fonts/ABCFavorit-Bold.woff2') format('woff2'),
       url('/fonts/ABCFavorit-Bold.woff') format('woff');
  font-weight: 700;
  font-style: normal;
  font-display: swap;
}

/* Home tab — Untitled UI icon via mask (matches feature cards) */
a.nav-tabs-item[href="/welcome"] > img {
  display: none !important;
}

a.nav-tabs-item[href="/welcome"]::before {
  content: "";
  display: block;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
  background-color: currentColor;
  mask-image: url(/images/icons/home-02.svg);
  mask-size: contain;
  mask-repeat: no-repeat;
  mask-position: center;
  -webkit-mask-image: url(/images/icons/home-02.svg);
  -webkit-mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
}

a.nav-tabs-item[data-active="true"],
a.nav-tabs-item[data-active="true"].text-gray-800 {
  color: #4c48ff !important;
  text-shadow: none !important;
}

.dark a.nav-tabs-item[data-active="true"],
.dark a.nav-tabs-item[data-active="true"].dark\:text-gray-200 {
  color: #5B68FF !important;
  text-shadow: none !important;
}

a.nav-tabs-item[href="/welcome"][data-active="true"]::before {
  background-color: #4c48ff !important;
}

.dark a.nav-tabs-item[href="/welcome"][data-active="true"]::before {
  background-color: #5B68FF !important;
}

/* ============================================================
   Welcome page — align with navbar max-w-8xl + px-12
   Mintlify wraps main content in .scroll-mt with peer-based max-w-3xl / max-w-8xl;
   force that wrapper full width so .tk-page can use the full 92rem column.
   ============================================================ */

.scroll-mt-\[var\(--scroll-mt\)\]:has(.tk-page) {
  max-width: none !important;
  width: 100% !important;
  margin-inline: 0 !important;
  padding-inline: 0 !important;
}

#content-container:has(.tk-page) > .flex.flex-row-reverse {
  flex-direction: row !important;
}

#content-container:has(.tk-page) #content-area {
  width: 100% !important;
  max-width: none !important;
  padding: 0 !important;
  margin: 0 !important;
}

#content-container:has(.tk-page) #sidebar {
  display: none !important;
}

#content-container:has(.tk-page) #content {
  max-width: none !important;
  padding: 0 !important;
}

.tk-page {
  width: 100%;
  max-width: 92rem;
  margin-inline: auto;
  padding: 0 3rem 5rem; /* px-12 — aligns with nav tabs */
  box-sizing: border-box;
}

@media (max-width: 1000px) {
  .tk-page {
    padding-inline: 40px;
  }
}

@media (max-width: 868px) {
  .tk-page {
    padding-inline: 20px;
    padding-bottom: 2.5rem;
  }
}

/* ============================================================
   Light / dark image switching
   ============================================================ */

.tk-img-light { display: block !important; }
.tk-img-dark  { display: none  !important; }
.dark .tk-img-light { display: none  !important; }
.dark .tk-img-dark  { display: block !important; }

.dark .tk-page {
  --tk-surface: #0B0B0F;
}

/* ============================================================
   Asset placeholders
   ============================================================ */

.tk-placeholder {
  background: #f3f4f6;
  border: 2px dashed #d1d5db;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 1.5rem;
  text-align: center;
  font-size: 0.75rem;
  color: #9ca3af;
  box-sizing: border-box;
}

.dark .tk-placeholder {
  background: #1f2937;
  border-color: #374151;
  color: #6b7280;
}

/* ============================================================
   Section divider
   ============================================================ */

.section-divider {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 0;
}

.dark .section-divider {
  border-top-color: rgba(255, 255, 255, 0.06);
}

/* ============================================================
   Hero
   ============================================================ */

.tk-hero {
  position: relative;
  overflow: hidden;
  padding: 2.5rem 0 3rem;
  min-height: 360px;
  display: flex;
  align-items: stretch;
  justify-content: flex-start;
  gap: 2.5rem;
}

.tk-hero-content {
  flex: 0 1 720px;
  max-width: 62%;
  min-width: 0;
  align-self: flex-start;
  position: relative;
  z-index: 2;
}

.tk-hero-headline {
  color: var(--Black, #111);
  font-family: 'ABC Favorit', sans-serif;
  font-size: 82px;
  font-style: normal;
  font-weight: 400;
  line-height: 95%;
  letter-spacing: -4.1px;
  font-synthesis: none;
  margin: 0 0 1.25rem;
  max-width: 720px;
}

.dark .tk-hero-headline {
  color: #f9fafb;
}

.tk-hero-body {
  color: #414146;
  font-family: Inter, sans-serif;
  font-size: 18px;
  font-style: normal;
  font-weight: 400;
  line-height: 28px;
  font-feature-settings: 'ss01' on, 'ss03' on;
  margin: 0 0 1.5rem;
  max-width: 720px;
}

.dark .tk-hero-body {
  color: #a1a1aa;
}

/* Shared description / body copy (welcome page, not hero) */
.tk-build-body,
.tk-whitepaper-text {
  color: #414146;
  font-family: Inter, sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: 400;
  line-height: 28px;
}

.dark .tk-build-body,
.dark .tk-whitepaper-text {
  color: #a1a1aa;
}

.tk-search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border-radius: 12px;
  border: 1px solid var(--Black-20, rgba(0, 0, 0, 0.20));
  background: var(--Black-4, rgba(0, 0, 0, 0.04));
  padding: 12px 16px;
  cursor: pointer;
  max-width: 500px;
  transition: border-color 0.15s, background 0.15s;
}

.tk-search-bar:hover {
  border-color: rgba(0, 0, 0, 0.28);
  background: rgba(0, 0, 0, 0.06);
}

.tk-search-icon {
  flex-shrink: 0;
  color: #737277;
}

.tk-search-placeholder {
  flex: 1;
  color: #737277;
  text-align: left;
  font-family: Inter, sans-serif;
  font-size: 16px;
  font-style: normal;
  font-weight: 400;
  line-height: 24px;
}

.dark .tk-search-icon,
.dark .tk-search-placeholder {
  color: #a1a1aa;
}

.dark .tk-search-bar {
  border-color: rgba(255, 255, 255, 0.20);
  background: rgba(255, 255, 255, 0.04);
}

.dark .tk-search-bar:hover {
  border-color: rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.06);
}

.tk-search-kbd {
  color: #737277;
  text-align: center;
  font-family: "SF Pro", -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 12px;
  font-style: normal;
  font-weight: 590;
  line-height: 16px;
  border: none;
  border-radius: 0;
  padding: 0;
  background: transparent;
}

.dark .tk-search-kbd {
  color: #a1a1aa;
  background: transparent;
}

.tk-hero-illustration {
  position: relative;
  flex: 0 0 657px;
  width: 657px;
  min-height: 329px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  z-index: 1;
  pointer-events: none;
  user-select: none;
}

/* Grid is a direct child of .tk-hero — keep it out of flex flow */
.tk-hero > .tk-hero-grid {
  flex: none;
}

/* Force a fixed pixel width — this locks the height; illustration never shrinks vertically */
.tk-hero-illustration > * {
  pointer-events: none;
  cursor: default;
}

.tk-hero-grid {
  position: absolute;
  top: 0;
  right: 0;
  width: 640px;
  height: 512px;
  z-index: 0;
  pointer-events: none;
}

.tk-hero-grid img {
  display: block;
  width: 640px;
  height: 512px;
}

.tk-hero-img {
  position: relative;
  display: block;
  width: 657px;
  height: 329px;
  flex-shrink: 0;
  pointer-events: none;
  border: none;
}

.tk-hero-img svg {
  display: block;
  width: 657px;
  height: 329px;
}

/* Hero tablet — Figma 169:4781 (869–1440px): copy left, graphic right, crop at viewport */
@media (max-width: 1440px) and (min-width: 869px) {
  .tk-hero {
    position: relative;
    flex-direction: row;
    align-items: flex-start;
    justify-content: flex-start;
    gap: 1rem;
    width: 100vw;
    max-width: 100vw;
    margin-left: calc(50% - 50vw);
    padding: 2.3125rem 0 2.5rem 3rem;
    min-height: auto;
    overflow: hidden;
    box-sizing: border-box;
  }

  .tk-hero-grid {
    top: -3.3125rem;
    right: 0;
    left: auto;
    width: 640px;
    height: 512px;
  }

  .tk-hero-grid img {
    width: 640px;
    height: 512px;
    max-height: none;
    object-fit: none;
  }

  .tk-hero-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
    flex: 0 0 612px;
    max-width: 612px;
    width: 612px;
    align-self: flex-start;
    position: relative;
    z-index: 2;
    isolation: isolate;
  }

  .tk-hero-content::before {
    content: "";
    position: absolute;
    z-index: -1;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    width: 100%;
    pointer-events: none;
    background: linear-gradient(
      90deg,
      #ffffff 0%,
      #ffffff 75%,
      rgba(255, 255, 255, 0) 100%
    );
  }

  .dark .tk-hero-content::before {
    background: linear-gradient(
      90deg,
      var(--tk-surface, #0B0B0F) 0%,
      var(--tk-surface, #0B0B0F) 75%,
      rgba(11, 11, 15, 0) 100%
    );
  }

  .tk-hero-headline {
    font-size: 74px;
    letter-spacing: -3.7px;
    line-height: 0.95;
    margin: 0;
    max-width: none;
  }

  .tk-hero-body {
    margin: 0;
    max-width: none;
  }

  .tk-search-bar {
    max-width: 437px;
  }

  .tk-hero-illustration {
    position: absolute;
    top: 2.3125rem;
    right: 0;
    display: block;
    flex: none;
    width: 657px;
    min-width: 657px;
    min-height: 328px;
    margin: 0;
    overflow: visible;
    z-index: 1;
  }

  .tk-hero-img {
    display: block;
    width: 657px;
    max-width: none;
    height: auto;
  }
}

@media (max-width: 1190px) and (min-width: 869px) {
  .tk-hero-illustration {
    right: -100px;
  }
}

@media (max-width: 1000px) and (min-width: 869px) {
  .tk-hero {
    padding-left: 40px;
  }
}

/* Tablet — Figma Responsive frame (1000px) */
@media (max-width: 1000px) {
  .tk-whitepaper {
    padding: 20px 40px 0;
  }
}


/* Mobile — Figma Responsive 169:4327 (≤868px) */
@media (max-width: 868px) {
  .tk-hero {
    flex-direction: column;
    gap: 0;
    padding: 2.25rem 0 2.5rem;
    min-height: auto;
    overflow: visible;
  }

  .tk-hero-grid {
    display: none;
  }

  .tk-hero-illustration {
    display: none;
  }

  .tk-hero-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
    flex: 1 1 auto;
    max-width: 100%;
    width: 100%;
    isolation: auto;
  }

  .tk-hero-content::before {
    display: none;
  }

  .tk-hero-headline {
    font-size: 44px;
    letter-spacing: -2.2px;
    line-height: 0.95;
    margin: 0;
  }

  .tk-hero-body {
    margin: 0;
  }

  .tk-search-bar {
    max-width: none;
    width: 100%;
  }

  .tk-build,
  .tk-solutions-section,
  .tk-features-section,
  .tk-whitepaper-section {
    margin: 2.5rem 0;
  }
}

/* ============================================================
   Section headings
   ============================================================ */

.tk-section-heading {
  font-family: Inter, sans-serif;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.75px;
  line-height: 1.2;
  margin: 0 0 1.25rem;
  color: #1a191e;
}

.dark .tk-section-heading { color: #f9fafb; }

/* ============================================================
   Build with Turnkey
   ============================================================ */

/* Build with Turnkey — shared */
.tk-build {
  display: grid;
  grid-template-columns: 1fr;
  gap: 28px;
  align-items: start;
  margin: 52px 0;
}

.tk-build-intro {
  display: contents;
}

.tk-build-copy {
  display: flex;
  flex-direction: column;
  gap: 28px;
  grid-column: 1;
  grid-row: 1;
}

.tk-build-visual {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 40px; /* Figma: 40px between cube and selector panel */
  grid-column: 1;
  grid-row: 2;
  width: 100%;
  justify-self: start;
}

.tk-build-links {
  grid-column: 1;
  grid-row: 3;
}

.tk-build-selectors {
  flex: 0 0 auto;
  min-width: 0;
  max-width: none;
  justify-content: flex-start;
}

.tk-selector-list {
  width: 100%;
  max-width: 100%;
}

/* Tablet / small desktop — center cube + selectors row */
@media (min-width: 869px) and (max-width: 1379px) {
  .tk-build-visual {
    justify-content: center;
    justify-self: center;
    width: fit-content;
    max-width: 100%;
    margin-inline: auto;
  }
}

/* Desktop — Figma: intro | 40px | cube | selectors (no stretched gutter) */
@media (min-width: 1380px) {
  .tk-build {
    display: grid;
    grid-template-columns: minmax(0, 558px) auto;
    width: 100%;
    gap: 40px;
    align-items: center;
    justify-items: start;
  }

  .tk-build-intro {
    display: flex;
    flex-direction: column;
    gap: 28px;
    min-width: 0;
    max-width: 558px;
  }

  .tk-build-copy {
    grid-column: unset;
    grid-row: unset;
    max-width: none;
  }

  .tk-build-links {
    grid-column: unset;
    grid-row: unset;
  }

  .tk-build-visual {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    gap: 40px;
    width: auto;
    min-width: 0;
    grid-column: unset;
    grid-row: unset;
    justify-self: start;
  }

  .tk-build-selectors {
    flex: 0 0 auto;
    min-width: auto;
    max-width: none;
    justify-content: flex-start;
  }

  .tk-selector-list {
    width: max-content;
    max-width: 100%;
  }
}

.tk-build-heading {
  font-family: Inter, sans-serif;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.75px;
  line-height: normal;
  margin: 0;
  color: #1a191e;
}

.dark .tk-build-heading { color: #f9fafb; }

.tk-build-body {
  margin: 0;
}

.tk-build-links {
  display: flex;
  flex-direction: row;
  gap: 40px;
  flex-wrap: wrap;
}

.tk-build-link {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #4C48FF;
  font-weight: 500;
  text-decoration: none;
  font-size: 0.875rem;
  transition: opacity 0.15s;
}

.tk-build-link-icon {
  flex-shrink: 0;
}

.dark .tk-build-link { color: #5B68FF; }
.tk-build-link:hover { opacity: 0.75; }

/* Cube — Figma Boxes A/B/C (149×170) */
.tk-build-cube {
  width: 149px;
  flex-shrink: 0;
  margin: 0;
  padding: 0;
  align-self: center;
}

.tk-cube-wrapper {
  position: relative;
  width: 149px;
  height: 170px;
  margin: 0;
  padding: 0;
  pointer-events: none;
  user-select: none;
}

.tk-plane-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  opacity: 0;
  visibility: hidden;
  transition: none;
  pointer-events: none;
  cursor: default;
}

/* Selected row → swap plane (instant, no crossfade) */
.tk-build[data-selected="a"] .tk-plane-img[data-plane="a"],
.tk-build[data-selected="b"] .tk-plane-img[data-plane="b"],
.tk-build[data-selected="c"] .tk-plane-img[data-plane="c"] {
  opacity: 1;
  visibility: visible;
}

/* Selectors panel — Figma 249:1834 */
.tk-build-selectors {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 17px; /* Figma: list starts 33px, connectors end ~16px → ~17px */
  width: max-content;
  max-width: 100%;
  flex-shrink: 0;
  overflow: visible;
}

.tk-build-connectors {
  position: relative;
  flex-shrink: 0;
  width: 40px;
  height: 120px; /* Figma: 3 × 40px rows */
  margin-left: -24px; /* Figma: connectors overlap cube by ~24px */
  pointer-events: none;
}

.tk-connector-line {
  position: absolute;
  left: 0;
  width: 40px;
  height: 0;
}

.tk-connector-line--a { top: 20px; }
.tk-connector-line--b { top: 60px; }
.tk-connector-line--c { top: 100px; }

.tk-connector-line::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 1px;
  background: rgba(0, 0, 0, 0.2);
  transition: background 0.15s;
}

.dark .tk-connector-line::after {
  background: rgba(255, 255, 255, 0.2);
}

.tk-build[data-selected="a"] .tk-connector-line--a::after,
.tk-build[data-selected="b"] .tk-connector-line--b::after,
.tk-build[data-selected="c"] .tk-connector-line--c::after {
  background: #4C48FF;
}

.dark .tk-build[data-selected="a"] .tk-con