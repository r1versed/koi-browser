"use strict";

let port = null;
const seen = new Set();
const dirCounts = new Map();

const VIDEO_PATH = /\.(mp4|m4v|webm|mkv|mov|m3u8|mpd)$/i;
const STREAM_PATH = /\.(m3u8|mpd)$/i;
const SKIP_PATH = /\.(ts|m4s|aac|vtt|srt|jpg|jpeg|png|gif|webp|svg|js|css|json|woff2?)$/i;
const STREAM_MIME = /mpegurl|dash\+xml/i;
const SKIP_MIME = /iso\.segment|mp2t/i;
const MIN_FILE_BYTES = 150 * 1024;

function send(message) {
  if (!port) return;
  try { port.postMessage(message); } catch (_) {}
}

function header(details, name) {
  const found = (details.responseHeaders || []).find((h) => h.name.toLowerCase() === name);
  return found && found.value ? found.value : "";
}

function topUrlOf(details) {
  const ancestors = details.frameAncestors;
  if (ancestors && ancestors.length) return ancestors[ancestors.length - 1].url || "";
  return details.documentUrl || details.originUrl || "";
}

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    seen.clear();
    dirCounts.clear();
    send({ type: "reset", topUrl: details.url });
  },
  { urls: ["http://*/*", "https://*/*"], types: ["main_frame"] },
);

browser.webRequest.onHeadersReceived.addListener(
  (details) => {
    if (details.statusCode !== 200 && details.statusCode !== 206) return;
    let path;
    try { path = new URL(details.url).pathname; } catch (_) { return; }
    const mime = header(details, "content-type").split(";")[0].trim().toLowerCase();
    if (SKIP_MIME.test(mime) || SKIP_PATH.test(path)) return;
    const isStream = STREAM_PATH.test(path) || STREAM_MIME.test(mime);
    const isVideo = mime.startsWith("video/") ||
      (VIDEO_PATH.test(path) && (mime === "" || mime.includes("octet-stream")));
    if (!isStream && !isVideo) return;
    let size = -1;
    const range = /\/(\d+)\s*$/.exec(header(details, "content-range"));
    if (range) {
      size = parseInt(range[1], 10);
    } else {
      const length = parseInt(header(details, "content-length"), 10);
      if (!Number.isNaN(length)) size = length;
    }
    if (!isStream && size >= 0 && size < MIN_FILE_BYTES) return;
    const topUrl = topUrlOf(details);
    if (!topUrl || seen.has(details.url)) return;
    const dir = details.url.split("?")[0].replace(/[^/]*$/, "");
    const count = (dirCounts.get(dir) || 0) + 1;
    dirCounts.set(dir, count);
    if (!isStream && count > 4) return;
    seen.add(details.url);
    send({ type: "media", topUrl: topUrl, url: details.url, mime: mime, size: size, stream: isStream });
  },
  { urls: ["http://*/*", "https://*/*"], types: ["media", "xmlhttprequest", "other", "object"] },
  ["responseHeaders"],
);

function connect() {
  const connected = browser.runtime.connectNative("candyMediaSniffer");
  port = connected;
  connected.onDisconnect.addListener(() => {
    if (port !== connected) return;
    port = null;
    setTimeout(connect, 500);
  });
  connected.postMessage({ type: "ready", version: browser.runtime.getManifest().version });
}

connect();
