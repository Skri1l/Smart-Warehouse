// Highlights the download card that matches the visitor's operating system.
(function () {
  var ua = (navigator.userAgentData && navigator.userAgentData.platform) || navigator.userAgent || "";
  ua = ua.toLowerCase();
  var os = null;
  if (ua.indexOf("win") !== -1) os = "windows";
  else if (ua.indexOf("mac") !== -1 && ua.indexOf("iphone") === -1 && ua.indexOf("ipad") === -1) os = "macos";
  else if (ua.indexOf("linux") !== -1 && ua.indexOf("android") === -1) os = "linux";
  if (!os) return;
  var card = document.querySelector('[data-os="' + os + '"]');
  if (card) card.classList.add("recommended");
})();
