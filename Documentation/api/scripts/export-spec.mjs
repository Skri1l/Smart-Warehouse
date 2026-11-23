// Saves the spec that springdoc generates from the running backend into
// source/openapi.generated.json. Run after changing a controller or DTO:
//   (backend running)  npm run export && npm run build
import { writeFile } from 'node:fs/promises';

const url = process.env.BACKEND_URL ?? 'http://localhost:8080';
const response = await fetch(`${url}/v3/api-docs`);
if (!response.ok) {
  console.error(`GET ${url}/v3/api-docs -> ${response.status}. Is the backend running?`);
  process.exit(1);
}
const spec = await response.json();
delete spec.servers; // springdoc puts the local URL here; build-spec.mjs sets the real servers
await writeFile(
  new URL('../source/openapi.generated.json', import.meta.url),
  `${JSON.stringify(spec, null, 2)}\n`,
);
console.log(`Saved ${Object.keys(spec.paths).length} paths from ${url}`);
