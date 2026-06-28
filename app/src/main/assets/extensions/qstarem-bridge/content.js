(function () {
  if (window.top !== window.self) return;

  const EXIT_PATTERN = /choose your exit|leave player|exit player|back to browse|leave watch/i;
  const PIP_HASH = "#qstarem-pip";
  const EXIT_HASH = "#qstarem-exit";
  const SAVE_HASH = "#qstarem-save";
  const CLEAR_HASH = "#qstarem-clear";
  const UPDATE_CHECK_HASH = "#qstarem-update-check";
  const UPDATE_INSTALL_HASH = "#qstarem-update-install";
  const RETURN_ITEM_ID = "qstarem-return-item";
  const SETTINGS_ITEM_ID = "qstarem-settings-item";
  const SETTINGS_PANEL_ID = "qstarem-settings-panel";
  const STORAGE_KEY = "qstarem-app-settings";
  const ICON_OPTIONS = [
    { id: 1, label: "Q Play" },
    { id: 2, label: "Film Reel" },
    { id: 3, label: "Z Waves" },
    { id: 4, label: "Viewfinder" },
    { id: 5, label: "Orbital" },
    { id: 6, label: "Clapper" },
  ];

  const SITE_MENU_LABELS = [
    /^account preferences$/i,
    /^appearance$/i,
    /^subtitles$/i,
  ];

  let enhanceScheduled = false;

  const CATEGORY_SCROLL_SELECTOR =
    ".overflow-x-auto.no-scrollbar.mask-linear-right, .overflow-x-auto.scrollbar-hide.carousel-container, .carousel-container.overflow-x-auto";

  function isHorizontalScrollRow(node) {
    if (!node || node.nodeType !== 1) return false;
    const style = window.getComputedStyle(node);
    if (!/(auto|scroll)/.test(style.overflowX)) return false;
    const rect = node.getBoundingClientRect();
    if (rect.width < 120) return false;
    return node.scrollWidth > rect.width + 12;
  }

  function allowParentHorizontalScroll(node) {
    let parent = node.parentElement;
    while (parent && parent !== document.body) {
      const style = window.getComputedStyle(parent);
      if (style.overflowX === "hidden" && parent.scrollWidth <= parent.clientWidth + 1) {
        parent.classList.add("qstarem-allow-horizontal-scroll");
      }
      parent = parent.parentElement;
    }
  }

  function ensureCategoryScrollStart(node) {
    if (!node || !isHorizontalScrollRow(node)) return;
    if (node.scrollLeft > 0) return;

    const firstChip = node.querySelector("button, a, [role='button']");
    if (!firstChip) return;

    const rowRect = node.getBoundingClientRect();
    const chipRect = firstChip.getBoundingClientRect();
    if (chipRect.left >= rowRect.left - 2) return;

    const hiddenLeft = rowRect.left - chipRect.left;
    if (hiddenLeft > 0) {
      node.scrollLeft = Math.max(0, node.scrollLeft - hiddenLeft - 8);
    }
  }

  function fixCategoryScrollRow(node) {
    if (!node || node.dataset.qstaremScrollFixed === "true") return;
    if (!isHorizontalScrollRow(node)) return;

    node.dataset.qstaremScrollFixed = "true";
    node.classList.add("qstarem-category-scroll");
    allowParentHorizontalScroll(node);

    if (!node.querySelector(":scope > .qstarem-category-scroll-lead")) {
      const lead = document.createElement("span");
      lead.className = "qstarem-category-scroll-lead";
      lead.setAttribute("aria-hidden", "true");
      node.insertBefore(lead, node.firstChild);
    }

    const syncStart = () => ensureCategoryScrollStart(node);
    node.addEventListener("scroll", syncStart, { passive: true });
    syncStart();
    requestAnimationFrame(syncStart);
  }

  function fixCategoryScrollRows(root) {
    root.querySelectorAll(CATEGORY_SCROLL_SELECTOR).forEach((node) => {
      fixCategoryScrollRow(node);
    });

    root.querySelectorAll(".overflow-x-auto, .overflow-x-scroll").forEach((node) => {
      if (node.dataset.qstaremScrollFixed === "true") return;
      const chips = node.querySelectorAll(":scope > button, :scope > a, :scope > [role='button']");
      if (chips.length >= 3) {
        fixCategoryScrollRow(node);
      }
    });
  }

  function readSettings() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_error) {
      /* use defaults */
    }
    return {
      homeUrl: "https://zstream.mov",
      adBlocker: "UBLOCK",
      pStreamEnabled: true,
      appIconId: 1,
    };
  }

  function writeSettings(settings) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  }

  function normalizeText(value) {
    return (value || "").replace(/\s+/g, " ").trim();
  }

  function matchesSiteMenuLabel(text) {
    return SITE_MENU_LABELS.some((pattern) => pattern.test(normalizeText(text)));
  }

  function matchesExitControl(node) {
    if (!node || node.nodeType !== 1) return false;
    if (node.closest(`#${RETURN_ITEM_ID}, #${SETTINGS_ITEM_ID}, #${SETTINGS_PANEL_ID}`)) {
      return false;
    }
    const text = normalizeText(node.textContent);
    const aria = normalizeText(node.getAttribute("aria-label"));
    const title = normalizeText(node.getAttribute("title"));
    const combined = `${text} ${aria} ${title}`;
    if (!combined) return false;
    if (EXIT_PATTERN.test(combined)) return true;
    if (/^exit$/i.test(text) && text.length < 24) return true;
    return false;
  }

  function findExitControls(root) {
    const matches = [];
    root.querySelectorAll("button, a, [role='button'], [role='link']").forEach((node) => {
      if (matchesExitControl(node)) matches.push(node);
    });
    return matches;
  }

  function clickOriginalExit() {
    const hidden = document.querySelector(".qstarem-hidden-exit");
    if (hidden) {
      hidden.click();
      return true;
    }
    const exit = findExitControls(document)[0];
    if (exit) {
      exit.click();
      return true;
    }
    return false;
  }

  function requestExitPlayer() {
    closeSettingsPanel();
    if (clickOriginalExit()) return;
    if (window.history.length > 1) {
      window.history.back();
    }
  }

  function isVisible(node) {
    if (!node || !node.isConnected) return false;
    const style = window.getComputedStyle(node);
    if (style.display === "none" || style.visibility === "hidden" || Number(style.opacity) === 0) {
      return false;
    }
    const rect = node.getBoundingClientRect();
    return rect.width > 8 && rect.height > 8;
  }

  function findSiteMenuRow(labelPattern) {
    const candidates = document.querySelectorAll(
      "a, button, [role='button'], [role='menuitem'], li, div, span, p",
    );

    for (const node of candidates) {
      if (!isVisible(node)) continue;
      const text = normalizeText(node.textContent);
      if (!labelPattern.test(text)) continue;
      if (text.length > 48) continue;
      return node.closest("a, button, [role='button'], li, div") || node;
    }

    return null;
  }

  function findSiteMenuContainer() {
    const anchors = SITE_MENU_LABELS.map((pattern) => findSiteMenuRow(pattern)).filter(Boolean);
    if (anchors.length === 0) return null;

    const first = anchors[0];
    let parent = first.parentElement;
    while (parent && parent !== document.body) {
      const matches = Array.from(parent.children).filter((child) => {
        const text = normalizeText(child.textContent);
        return SITE_MENU_LABELS.some((pattern) => pattern.test(text));
      });
      if (matches.length >= 2) {
        return { container: parent, template: matches[matches.length - 1] };
      }
      parent = parent.parentElement;
    }

    return { container: first.parentElement, template: first };
  }

  function cloneMenuRow(template, label, id, onClick) {
    const row = template.cloneNode(true);
    row.id = id;
    row.querySelectorAll("[id]").forEach((node) => node.removeAttribute("id"));

    const textNodes = [];
    const walker = document.createTreeWalker(row, NodeFilter.SHOW_TEXT);
    let current = walker.nextNode();
    while (current) {
      if (normalizeText(current.textContent)) textNodes.push(current);
      current = walker.nextNode();
    }

    if (textNodes.length > 0) {
      textNodes[0].textContent = label;
      for (let index = 1; index < textNodes.length; index += 1) {
        textNodes[index].textContent = "";
      }
    } else {
      const target = row.querySelector("span, p, a, button") || row;
      target.textContent = label;
    }

    const link = row.querySelector("a, button");
    if (link && link.tagName === "A") {
      link.setAttribute("href", "#");
    }

    row.addEventListener(
      "click",
      (event) => {
        event.preventDefault();
        event.stopPropagation();
        onClick();
      },
      true,
    );

    return row;
  }

  function injectMenuItems() {
    const menu = findSiteMenuContainer();
    if (!menu) return;

    const { container, template } = menu;

    if (!container.querySelector(`#${SETTINGS_ITEM_ID}`)) {
      container.appendChild(
        cloneMenuRow(template, "App settings", SETTINGS_ITEM_ID, () => {
          openSettingsPanel();
        }),
      );
    }

    if (document.querySelector("video") && !container.querySelector(`#${RETURN_ITEM_ID}`)) {
      container.insertBefore(
        cloneMenuRow(template, "Return to browse", RETURN_ITEM_ID, () => {
          requestExitPlayer();
        }),
        container.firstChild,
      );
    }
  }

  function extensionAsset(path) {
    if (typeof browser !== "undefined" && browser.runtime?.getURL) {
      return browser.runtime.getURL(path);
    }
    return path;
  }

  function renderIconChoices(selectedId) {
    ensureSettingsPanel();
    const grid = document.getElementById("qstarem-icon-choices");
    if (!grid) return;

    grid.innerHTML = "";
    ICON_OPTIONS.forEach((option) => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "qstarem-icon-choice";
      button.dataset.iconId = String(option.id);
      button.setAttribute("aria-pressed", option.id === selectedId ? "true" : "false");

      const image = document.createElement("img");
      image.src = extensionAsset(`icons/icon-${option.id}.png`);
      image.alt = option.label;
      image.width = 56;
      image.height = 56;

      const label = document.createElement("span");
      label.textContent = option.label;

      button.append(image, label);
      button.addEventListener("click", () => {
        grid.dataset.selectedIconId = String(option.id);
        renderIconChoices(option.id);
      });

      grid.appendChild(button);
    });
    grid.dataset.selectedIconId = String(selectedId);
  }

  function selectedIconIdFromPanel() {
    const grid = document.getElementById("qstarem-icon-choices");
    const value = Number(grid?.dataset.selectedIconId || readSettings().appIconId || 1);
    if (Number.isNaN(value)) return 1;
    return Math.min(6, Math.max(1, value));
  }

  function ensureSettingsPanel() {
    if (document.getElementById(SETTINGS_PANEL_ID)) return;

    const panel = document.createElement("div");
    panel.id = SETTINGS_PANEL_ID;
    panel.hidden = true;
    panel.innerHTML = `
      <div class="qstarem-settings-backdrop" data-qstarem-close></div>
      <section class="qstarem-settings-sheet" role="dialog" aria-label="App settings">
        <header class="qstarem-settings-header">
          <h2>App settings</h2>
          <button type="button" class="qstarem-settings-close" data-qstarem-close aria-label="Close">×</button>
        </header>
        <div class="qstarem-settings-body">
          <label class="qstarem-settings-field">
            <span>Home URL</span>
            <input id="qstarem-home-url" type="url" autocomplete="off" />
          </label>
          <button type="button" class="qstarem-settings-link" id="qstarem-reset-home">Reset to zstream.mov</button>
          <fieldset class="qstarem-settings-field">
            <legend>Ad blocker</legend>
            <label><input type="radio" name="qstarem-blocker" value="UBLOCK" /> uBlock Origin</label>
            <label><input type="radio" name="qstarem-blocker" value="ADGUARD" /> AdGuard</label>
            <label><input type="radio" name="qstarem-blocker" value="NONE" /> Off</label>
          </fieldset>
          <label class="qstarem-settings-toggle">
            <span>P-Stream extension</span>
            <input id="qstarem-pstream" type="checkbox" />
          </label>
          <div class="qstarem-settings-field">
            <span>App icon</span>
            <div id="qstarem-icon-choices" class="qstarem-icon-grid"></div>
          </div>
          <div class="qstarem-settings-field qstarem-update-section">
            <span>Updates</span>
            <div id="qstarem-update-badge" class="qstarem-update-badge qstarem-update-badge-idle">Tap to check</div>
            <div class="qstarem-update-version-grid">
              <div class="qstarem-update-version-card">
                <span class="qstarem-update-version-label">Installed</span>
                <strong id="qstarem-installed-version" class="qstarem-update-version-value">—</strong>
              </div>
              <div class="qstarem-update-version-card">
                <span class="qstarem-update-version-label">Latest</span>
                <strong id="qstarem-latest-version" class="qstarem-update-version-value">—</strong>
              </div>
            </div>
            <div id="qstarem-update-progress-wrap" class="qstarem-update-progress-wrap" hidden>
              <div class="qstarem-update-progress-track">
                <div id="qstarem-update-progress-bar" class="qstarem-update-progress-bar"></div>
              </div>
              <span id="qstarem-update-progress-label" class="qstarem-update-progress-label">0%</span>
            </div>
            <p id="qstarem-update-status" class="qstarem-update-status">Open App settings and tap Check for updates.</p>
            <button type="button" class="qstarem-settings-link" id="qstarem-check-updates">Check for updates</button>
            <button type="button" class="qstarem-settings-primary qstarem-update-install" id="qstarem-install-update" hidden>Install update</button>
          </div>
        </div>
        <footer class="qstarem-settings-footer">
          <button type="button" class="qstarem-settings-primary" id="qstarem-save-settings">Save</button>
          <button type="button" class="qstarem-settings-secondary" id="qstarem-clear-data">Clear browsing data</button>
        </footer>
      </section>
    `;
    document.body.appendChild(panel);

    panel.querySelectorAll("[data-qstarem-close]").forEach((node) => {
      node.addEventListener("click", closeSettingsPanel);
    });
    panel.querySelector("#qstarem-reset-home").addEventListener("click", () => {
      panel.querySelector("#qstarem-home-url").value = "https://zstream.mov";
    });
    panel.querySelector("#qstarem-save-settings").addEventListener("click", saveSettingsFromPanel);
    panel.querySelector("#qstarem-clear-data").addEventListener("click", () => {
      closeSettingsPanel();
      location.hash = CLEAR_HASH;
    });
    panel.querySelector("#qstarem-check-updates").addEventListener("click", () => {
      openSettingsPanel();
      updateStatusFromBridge({
        phase: "checking",
        message: "Checking for updates…",
        currentVersion: readSettings().appVersion,
      });
      location.hash = UPDATE_CHECK_HASH;
    });
    panel.querySelector("#qstarem-install-update").addEventListener("click", () => {
      closeSettingsPanel();
      location.hash = UPDATE_INSTALL_HASH;
    });
  }

  function updateStatusFromBridge(detail) {
    const panel = document.getElementById(SETTINGS_PANEL_ID);
    if (!panel) return;

    const statusEl = panel.querySelector("#qstarem-update-status");
    const installBtn = panel.querySelector("#qstarem-install-update");
    const badgeEl = panel.querySelector("#qstarem-update-badge");
    const installedEl = panel.querySelector("#qstarem-installed-version");
    const latestEl = panel.querySelector("#qstarem-latest-version");
    const progressWrap = panel.querySelector("#qstarem-update-progress-wrap");
    const progressBar = panel.querySelector("#qstarem-update-progress-bar");
    const progressLabel = panel.querySelector("#qstarem-update-progress-label");
    if (!statusEl || !installBtn) return;

    const phase = detail.phase || "idle";
    const progress = Number(detail.progress || 0);
    const installedVersion = detail.currentVersion || readSettings().appVersion || "unknown";
    const latestVersion = detail.availableVersion || installedVersion;

    if (installedEl) {
      installedEl.textContent = `v${installedVersion}`;
    }
    if (latestEl) {
      latestEl.textContent =
        phase === "checking" && !detail.availableVersion ? "Checking…" : `v${latestVersion}`;
    }

    let message = detail.message || "Up to date.";
    let badgeText = "Up to date";
    let badgeClass = "qstarem-update-badge-idle";

    if (phase === "downloading") {
      badgeText = "Downloading";
      badgeClass = "qstarem-update-badge-downloading";
      message = detail.message || `Downloading QStarem ${latestVersion}…`;
      if (progress > 0) {
        message = `${message} ${Math.round(progress * 100)}% complete.`;
      }
    } else if (phase === "checking") {
      badgeText = "Checking…";
      badgeClass = "qstarem-update-badge-checking";
      message = detail.message || "Checking for updates…";
    } else if (phase === "ready") {
      badgeText = "Update ready";
      badgeClass = "qstarem-update-badge-ready";
      message = detail.message || `QStarem ${latestVersion} is ready to install.`;
    } else if (phase === "error") {
      badgeText = "Update failed";
      badgeClass = "qstarem-update-badge-error";
      message = detail.message || "Update check failed.";
    } else if (detail.availableVersion && detail.availableVersion !== installedVersion) {
      badgeText = "Update available";
      badgeClass = "qstarem-update-badge-ready";
      message = detail.message || `QStarem ${detail.availableVersion} is available.`;
    } else {
      badgeText = "Up to date";
      badgeClass = "qstarem-update-badge-current";
      message =
        detail.message ||
        `You're on the latest build. Installed v${installedVersion}, latest v${latestVersion}.`;
    }

    statusEl.textContent = message;
    installBtn.hidden = phase !== "ready";

    if (badgeEl) {
      badgeEl.textContent = badgeText;
      badgeEl.className = `qstarem-update-badge ${badgeClass}`;
    }

    if (progressWrap && progressBar && progressLabel) {
      const showProgress = phase === "downloading" && progress > 0;
      progressWrap.hidden = !showProgress;
      if (showProgress) {
        const percent = Math.round(progress * 100);
        progressBar.style.width = `${percent}%`;
        progressLabel.textContent = `${percent}%`;
      }
    }
  }

  function populateSettingsPanel() {
    ensureSettingsPanel();
    const settings = readSettings();
    const panel = document.getElementById(SETTINGS_PANEL_ID);
    panel.querySelector("#qstarem-home-url").value = settings.homeUrl || "https://zstream.mov";
    panel.querySelector("#qstarem-pstream").checked = settings.pStreamEnabled !== false;
    const blocker = settings.adBlocker || "UBLOCK";
    panel.querySelectorAll('input[name="qstarem-blocker"]').forEach((input) => {
      input.checked = input.value === blocker;
    });
    renderIconChoices(settings.appIconId || 1);

    updateStatusFromBridge({
      phase: "idle",
      currentVersion: settings.appVersion || "unknown",
      message: "Tap Check for updates to verify your build.",
    });
  }

  function openSettingsPanel() {
    populateSettingsPanel();
    const panel = document.getElementById(SETTINGS_PANEL_ID);
    panel.hidden = false;
    document.body.classList.add("qstarem-settings-open");
  }

  function closeSettingsPanel() {
    const panel = document.getElementById(SETTINGS_PANEL_ID);
    if (!panel) return;
    panel.hidden = true;
    document.body.classList.remove("qstarem-settings-open");
  }

  function saveSettingsFromPanel() {
    const panel = document.getElementById(SETTINGS_PANEL_ID);
    const homeUrl = panel.querySelector("#qstarem-home-url").value.trim() || "https://zstream.mov";
    const adBlocker =
      panel.querySelector('input[name="qstarem-blocker"]:checked')?.value || "UBLOCK";
    const pStreamEnabled = panel.querySelector("#qstarem-pstream").checked;
    const appIconId = selectedIconIdFromPanel();

    writeSettings({ homeUrl, adBlocker, pStreamEnabled, appIconId });

    const params = new URLSearchParams({
      home: homeUrl,
      blocker: adBlocker.toLowerCase(),
      pstream: pStreamEnabled ? "1" : "0",
      icon: String(appIconId),
    });
    closeSettingsPanel();
    location.hash = `${SAVE_HASH}?${params.toString()}`;
  }

  function hideExitControls() {
    if (!document.querySelector("video")) return;
    findExitControls(document).forEach((node) => {
      if (node.classList.contains("qstarem-hidden-exit")) return;
      node.classList.add("qstarem-hidden-exit");
    });
  }

  function scheduleEnhance() {
    if (enhanceScheduled) return;
    enhanceScheduled = true;
    requestAnimationFrame(() => {
      enhanceScheduled = false;
      hideExitControls();
      injectMenuItems();
      fixCategoryScrollRows(document);
    });
  }

  function boot() {
    ensureSettingsPanel();
    hideExitControls();
    scheduleEnhance();

    const observer = new MutationObserver(() => {
      scheduleEnhance();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });
  }

  window.addEventListener("qstarem-settings-updated", populateSettingsPanel);
  window.addEventListener("qstarem-update-status", (event) => {
    updateStatusFromBridge(event.detail || {});
  });
  window.addEventListener("qstarem-exit-player", requestExitPlayer);
  window.addEventListener("hashchange", () => {
    if (location.hash === EXIT_HASH) {
      requestExitPlayer();
      history.replaceState(null, "", location.pathname + location.search);
    }
  });

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }

  window.addEventListener("pageshow", () => {
    hideExitControls();
    populateSettingsPanel();
    scheduleEnhance();
    fixCategoryScrollRows(document);
  });
})();
