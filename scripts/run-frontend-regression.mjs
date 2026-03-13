import { spawn } from "node:child_process";

const isWindows = process.platform === "win32";
const npmCommand = isWindows ? "npm.cmd" : "npm";

const run = (label, args) =>
  new Promise((resolve, reject) => {
    const command = isWindows ? "cmd.exe" : npmCommand;
    const commandArgs = isWindows ? ["/d", "/s", "/c", npmCommand, ...args] : args;
    const child = spawn(command, commandArgs, {
      cwd: process.cwd(),
      stdio: "inherit",
      shell: false,
      env: process.env,
    });

    child.on("exit", (code) => {
      if (code === 0) {
        resolve(undefined);
        return;
      }
      reject(new Error(`${label} failed with exit code ${code}`));
    });
  });

const startedAt = Date.now();

try {
  console.log("[regression] running unit tests");
  await run("unit tests", ["run", "test:unit"]);

  console.log("[regression] running production build");
  await run("build", ["run", "build"]);

  const duration = ((Date.now() - startedAt) / 1000).toFixed(1);
  console.log(`[regression] completed successfully in ${duration}s`);
} catch (error) {
  console.error("[regression] failed");
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
}
