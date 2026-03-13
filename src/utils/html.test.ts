import { describe, expect, it } from "vitest";
import { escapeHtml, sanitizeRichHtml } from "./html";

describe("html utils", () => {
  it("removes blocked tags and dangerous attributes from nested markup", () => {
    const html = sanitizeRichHtml(
      '<section><img src="/cover.png" onerror="alert(1)"><script>alert(2)</script><strong>safe</strong></section>'
    );

    expect(html).toContain('<img src="/cover.png">');
    expect(html).toContain("<strong>safe</strong>");
    expect(html).not.toContain("script");
    expect(html).not.toContain("onerror");
  });

  it("strips unsafe urls while keeping safe links", () => {
    const html = sanitizeRichHtml(
      '<p><a href="javascript:alert(1)" onclick="alert(2)">bad</a><a href="https://example.com/page">good</a></p>'
    );

    expect(html).toContain(">bad</a>");
    expect(html).not.toContain("javascript:");
    expect(html).not.toContain("onclick");
    expect(html).toContain('href="https://example.com/page"');
    expect(html).toContain('target="_blank"');
    expect(html).toContain('rel="noopener noreferrer"');
  });

  it("escapes plain text when DOMParser is unavailable", () => {
    const originalDomParser = globalThis.DOMParser;
    try {
      // Cover the non-browser fallback path explicitly.
      // @ts-expect-error test override
      delete globalThis.DOMParser;

      expect(sanitizeRichHtml('<img src=x onerror="alert(1)">')).toBe(
        "&lt;img src=x onerror=&quot;alert(1)&quot;&gt;"
      );
      expect(escapeHtml("<b>text</b>")).toBe("&lt;b&gt;text&lt;/b&gt;");
    } finally {
      globalThis.DOMParser = originalDomParser;
    }
  });
});
