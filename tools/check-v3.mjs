import fs from "node:fs";

const source = fs.readFileSync(new URL("../index-pro.html", import.meta.url), "utf8");
const script = source.match(/<script>([\s\S]*)<\/script>/)?.[1];
if (!script) throw new Error("Inline script not found");
new Function(script);
const ids = Array.from(source.matchAll(/\sid="([^"]+)"/g), (match) => match[1]);
const duplicates = [...new Set(ids.filter((id, index) => ids.indexOf(id) !== index))];
if (duplicates.length) throw new Error(`Duplicate IDs: ${duplicates.join(", ")}`);
console.log(`JS syntax OK; ${ids.length} unique IDs; ${Buffer.byteLength(source)} bytes`);
