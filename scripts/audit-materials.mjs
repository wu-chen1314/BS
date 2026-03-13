import fs from "node:fs";
import path from "node:path";

const rootDir = process.cwd();
const scanTargets = [
  { path: "src", extensions: new Set([".vue", ".ts", ".js"]) },
  { path: "demo/src/main/java", extensions: new Set([".java"]) },
];

const externalUrlPattern = /https?:\/\/[^\s"'`)<]+/g;
const materialFieldPattern = /\b(coverUrl|videoUrl|avatarUrl|audioUrl|posterUrl)\b/g;
const uploadEndpointPattern = /\/api\/file\/upload(?:\/avatar)?/g;

const args = new Map(
  process.argv.slice(2).map((value, index, all) => {
    if (!value.startsWith("--")) {
      return [String(index), value];
    }

    const nextValue = all[index + 1];
    return [value, nextValue && !nextValue.startsWith("--") ? nextValue : "true"];
  })
);

const outputFile = args.get("--out");

const walkFiles = (directory, extensions, collector) => {
  if (!fs.existsSync(directory)) {
    return;
  }

  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      walkFiles(absolutePath, extensions, collector);
      continue;
    }

    if (extensions.has(path.extname(entry.name))) {
      collector.push(absolutePath);
    }
  }
};

const scanFiles = [];
for (const target of scanTargets) {
  walkFiles(path.join(rootDir, target.path), target.extensions, scanFiles);
}

const externalUrls = [];
const materialFieldUsage = new Map();
const uploadEndpoints = [];

for (const file of scanFiles) {
  const content = fs.readFileSync(file, "utf8");
  const lines = content.split(/\r?\n/);

  lines.forEach((line, index) => {
    const lineNumber = index + 1;

    for (const match of line.matchAll(externalUrlPattern)) {
      externalUrls.push({
        file: path.relative(rootDir, file).replace(/\\/g, "/"),
        line: lineNumber,
        url: match[0],
      });
    }

    for (const match of line.matchAll(materialFieldPattern)) {
      const key = match[0];
      materialFieldUsage.set(key, (materialFieldUsage.get(key) || 0) + 1);
    }

    for (const match of line.matchAll(uploadEndpointPattern)) {
      uploadEndpoints.push({
        file: path.relative(rootDir, file).replace(/\\/g, "/"),
        line: lineNumber,
        endpoint: match[0],
      });
    }
  });
}

const materialLibraryDir = path.join(rootDir, "public", "materials");
const libraryFiles = [];
walkFiles(materialLibraryDir, new Set([".svg", ".png", ".jpg", ".jpeg", ".webp", ".md", ".json"]), libraryFiles);

const groupedDomains = externalUrls.reduce((acc, item) => {
  const hostMatch = item.url.match(/^https?:\/\/([^/]+)/i);
  const domain = hostMatch?.[1] || "invalid-url";
  acc[domain] = (acc[domain] || 0) + 1;
  return acc;
}, {});

const report = {
  generatedAt: new Date().toISOString(),
  scannedFileCount: scanFiles.length,
  localMaterialLibrary: {
    fileCount: libraryFiles.length,
    files: libraryFiles
      .map((file) => path.relative(rootDir, file).replace(/\\/g, "/"))
      .sort(),
  },
  externalUrlSummary: {
    count: externalUrls.length,
    domains: groupedDomains,
    entries: externalUrls,
  },
  materialFieldUsage: Object.fromEntries([...materialFieldUsage.entries()].sort()),
  uploadEndpoints,
};

const serialized = `${JSON.stringify(report, null, 2)}\n`;

if (outputFile) {
  const destination = path.resolve(rootDir, outputFile);
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.writeFileSync(destination, serialized, "utf8");
  console.log(`material audit written to ${path.relative(rootDir, destination).replace(/\\/g, "/")}`);
} else {
  process.stdout.write(serialized);
}
