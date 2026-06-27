(function () {
  const EXIT_PATTERN = /choose your exit|leave player|exit player|back to browse|leave watch/i;
  const PIP_HASH = "#qstarem-pip";

  function matchesExitControl(node) {
    if (!node || node.nodeType !== 1) return false;
    const text = (node.textContent || "").trim();
    const aria = (node.getAttribute("aria-label") || "").trim();
    const title = (node.getAttribute("title") || "").trim();
    const combined = `${text} ${aria} ${title}`;
    if (!combined) return false;
    if (EXIT_PATTERN.test(combined)) return true;
    if (/^exit$/i.test(text) && text.length < 24) return true;
    return false;
  }

  function requestPip() {
    const video = document.querySelector("video");
    if (!video || video.paused) return;
    if (location.hash !== PIP_HASH) {
      location.hash = PIP_HASH;
    }
    window.dispatchEvent(new CustomEvent("qstarem-enter-pip"));
  }

  function enhanceExitControls(root) {
    root.querySelectorAll("button, a, [role='button'], [role='link']").forEach((node) => {
      if (!matchesExitControl(node)) return;
      if (node.dataset.qstaremBound === "1") return;
      node.dataset.qstaremBound = "1";
      node.classList.add("qstarem-embedded-exit");
      node.addEventListener(
        "click",
        (event) => {
          const video = document.querySelector("video");
          if (!video || video.paused) return;
          event.preventDefault();
          event.stopPropagation();
          requestPip();
        },
        true,
      );
    });
  }

  function boot() {
    enhanceExitControls(document);
    const observer = new MutationObserver(() => enhanceExitControls(document));
    observer.observe(document.documentElement, { childList: true, subtree: true });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
