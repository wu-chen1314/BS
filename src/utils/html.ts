const ALLOWED_TAGS = new Set([
  "a",
  "blockquote",
  "br",
  "code",
  "div",
  "em",
  "h1",
  "h2",
  "h3",
  "h4",
  "h5",
  "h6",
  "hr",
  "img",
  "li",
  "ol",
  "p",
  "pre",
  "span",
  "strong",
  "u",
  "ul",
]);

const ALLOWED_ATTRS = new Set(["alt", "href", "rel", "src", "target", "title"]);
const BLOCKED_TAGS = new Set(["button", "embed", "form", "iframe", "input", "math", "object", "script", "select", "style", "svg", "textarea"]);
const SAFE_PROTOCOLS = ["http:", "https:", "mailto:", "tel:"];

export const escapeHtml = (value: string) =>
  value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const sanitizeUrl = (value: string) => {
  const normalized = value.trim();
  if (!normalized) {
    return "";
  }

  if (normalized.startsWith("/") || normalized.startsWith("./") || normalized.startsWith("../")) {
    return normalized;
  }

  if (normalized.startsWith("#")) {
    return normalized;
  }

  try {
    const parsed = new URL(normalized, "https://heritage.local");
    return SAFE_PROTOCOLS.includes(parsed.protocol) ? normalized : "";
  } catch (_error) {
    return "";
  }
};

const sanitizeNode = (node: Node) => {
  if (node.nodeType === Node.TEXT_NODE) {
    return;
  }

  if (node.nodeType !== Node.ELEMENT_NODE) {
    node.parentNode?.removeChild(node);
    return;
  }

  const element = node as HTMLElement;
  const tagName = element.tagName.toLowerCase();

  if (!ALLOWED_TAGS.has(tagName)) {
    if (BLOCKED_TAGS.has(tagName)) {
      element.parentNode?.removeChild(element);
      return;
    }

    const parent = element.parentNode;
    if (!parent) {
      return;
    }

    for (const child of [...element.childNodes]) {
      parent.insertBefore(child, element);
      sanitizeNode(child);
    }
    parent.removeChild(element);
    return;
  }

  for (const attribute of [...element.attributes]) {
    const attrName = attribute.name.toLowerCase();
    if (attrName.startsWith("on") || !ALLOWED_ATTRS.has(attrName)) {
      element.removeAttribute(attribute.name);
      continue;
    }

    if (attrName === "href" || attrName === "src") {
      const nextValue = sanitizeUrl(attribute.value);
      if (!nextValue) {
        element.removeAttribute(attribute.name);
      } else {
        element.setAttribute(attribute.name, nextValue);
      }
    }
  }

  if (tagName === "a") {
    const href = element.getAttribute("href");
    if (!href) {
      element.removeAttribute("target");
      element.removeAttribute("rel");
    } else if (href.startsWith("http")) {
      element.setAttribute("target", "_blank");
      element.setAttribute("rel", "noopener noreferrer");
    }
  }

  for (const child of [...element.childNodes]) {
    sanitizeNode(child);
  }
};

export const sanitizeRichHtml = (value?: string | null, fallback = "") => {
  if (!value) {
    return fallback;
  }

  if (typeof DOMParser === "undefined") {
    return escapeHtml(value);
  }

  const parser = new DOMParser();
  const documentFragment = parser.parseFromString(value, "text/html");

  for (const child of [...documentFragment.body.childNodes]) {
    sanitizeNode(child);
  }

  const sanitized = documentFragment.body.innerHTML.trim();
  return sanitized || fallback;
};
