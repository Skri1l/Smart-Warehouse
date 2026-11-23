// Builds openapi.yaml from the springdoc export (source/openapi.generated.json) plus the
// documentation layer below: overview, examples for every schema, real error bodies for
// every status code, tag groups. Run `npm run build`; never edit openapi.yaml by hand.
//
// Every example and error message here was captured from the running backend.
import { readFile, writeFile } from 'node:fs/promises';
import YAML from 'yaml';

const REPO = 'https://github.com/Skri1l/Smart-Warehouse/blob/main';
const raw = JSON.parse(
  await readFile(new URL('../source/openapi.generated.json', import.meta.url), 'utf8'),
);
const spec = structuredClone(raw);

// ---------------------------------------------------------------- info, servers, tags

spec.info = {
  title: 'Smart Warehouse API',
  version: raw.info?.version ?? '0.0.1',
  summary: 'Warehouse layout, stock, shelf cameras, recognition and purchasing.',
  description: `
REST API of the Smart Warehouse backend (Spring Boot). It manages the warehouse layout
(warehouses → racks → shelves), the product catalogue, stock on shelves, shelf cameras
and their photos, recognition results, and purchasing (demand plans → replenishment
recommendations → purchase orders).

**New here?** Start with the [Getting Started guide](${REPO}/Documentation/api/GETTING_STARTED.md),
then follow the tutorial [Connect a shelf camera](${REPO}/Documentation/api/tutorials/connect-shelf-camera.md).

## Conventions

| Topic | Rule |
|---|---|
| Base URL | \`http://localhost:8080\` when run locally. All paths start at the root (no \`/api/v1\` prefix yet). |
| Format | JSON (\`application/json\`) in and out; camera photos are \`multipart/form-data\`. |
| Authentication | None yet — every endpoint is open in development. Do not expose this build publicly. |
| Create | \`POST\` returns **201** and the new resource **ID as a bare number** (e.g. \`7\`), not the object. Fetch it with \`GET /{resource}/{id}\`. |
| Update / delete | \`PUT\` replaces the whole resource and returns it (**200**); \`DELETE\` and status actions (\`PATCH …/activate\`) return **204** with no body. |
| Lists | Plain JSON arrays, not paginated yet. |
| Dates | \`date\` = \`YYYY-MM-DD\`; \`date-time\` = \`YYYY-MM-DDThh:mm:ss\` in server local time, without a timezone. |

## Errors

Business errors (not found, conflict, Vision failure) use \`ErrorResponse\`:

\`\`\`json
{ "status": 404, "message": "Product is not found" }
\`\`\`

Request errors detected by Spring before the controller runs (invalid body, wrong
parameter type, missing parameter, wrong content type) use the Spring Boot error body
\`SpringErrorResponse\`:

\`\`\`json
{ "timestamp": "2026-11-23T20:38:10.000Z", "status": 400, "error": "Bad Request",
  "message": "Validation failed for object='productRequestDto'. Error count: 2", "path": "/products" }
\`\`\`

| Status | Meaning | Body |
|---|---|---|
| 400 | Invalid JSON, failed validation (\`@NotBlank\`, \`@Positive\`, \`@FutureOrPresent\`…), wrong or missing parameter | \`SpringErrorResponse\` |
| 404 | The resource or a referenced resource (e.g. \`productId\`) does not exist | \`ErrorResponse\` |
| 409 | Duplicate (SKU, device key, user e-mail/username, warehouse name + address) or a state conflict | \`ErrorResponse\` |
| 415 | Wrong \`Content-Type\` (e.g. JSON sent to the photo upload) | \`SpringErrorResponse\` |
| 500 | Unexpected server error | \`SpringErrorResponse\` |
| 502 | The Vision service is unreachable or returned an invalid count | \`ErrorResponse\` |
`.trim(),
  license: { name: 'Proprietary — see LICENSE', url: `${REPO}/LICENSE` },
};

spec.servers = [
  { url: 'http://localhost:8080', description: 'Local backend (docker compose or ./mvnw spring-boot:run)' },
];

const TAG_RENAMES = {
  'device-image-controller': 'Camera Images',
  'stock-query-controller': 'Stock',
};

const TAGS = {
  Warehouses: 'Warehouses (sites). Unique by name + address.',
  Racks: 'Racks inside a warehouse. A rack holds shelves.',
  Shelves: 'Shelves on a rack. One shelf holds one product and has at most one camera.',
  Products: 'Product catalogue. `sku` is unique.',
  'Shelf Stock':
    'How many units of a product lie on a shelf, with expiry date. Camera processing needs exactly one stock record per shelf.',
  Stock: 'Aggregated stock per SKU across all shelves (used by the Planning service).',
  'Stock Movements':
    'Audit log of every stock change: receipts, removals, transfers, write-offs and camera adjustments.',
  Devices:
    'Shelf cameras (Raspberry Pi agents). A device is created `INACTIVE`; it becomes `ACTIVE` with `activate` or its first `heartbeat`.',
  'Camera Images':
    'Photo upload from a shelf camera. The backend saves the photo, asks the Vision service to count items and updates stock in one step.',
  'Image Events': 'Every photo received from a camera, with its processing status.',
  'Recognition Results': 'What the Vision service saw on a photo: detected quantity, change and status.',
  'Demand Plans': 'Expected demand: how many units of a product are needed by a date.',
  'Replenishment Recommendations':
    'Suggested purchases calculated from stock and demand. A manager approves, rejects or edits them.',
  'Purchase Orders': 'Orders placed from an approved recommendation (one order per recommendation).',
  Users: 'Users of the web UI. Roles: `WORKER`, `MANAGER`, `ADMIN`. New users are `WORKER`/`ACTIVE`.',
};

spec.tags = Object.entries(TAGS).map(([name, description]) => ({ name, description }));

// No authentication yet: say so explicitly instead of leaving security undefined.
spec.security = [];

// RackController names the same path parameter `rackId` in PUT/DELETE and `id` in GET.
// One URL, one path item: merge them under `{id}` (the wire format is identical).
if (spec.paths['/racks/{rackId}']) {
  const renamed = spec.paths['/racks/{rackId}'];
  for (const op of Object.values(renamed)) {
    for (const param of op.parameters ?? []) if (param.name === 'rackId') param.name = 'id';
  }
  spec.paths['/racks/{id}'] = { ...spec.paths['/racks/{id}'], ...renamed };
  delete spec.paths['/racks/{rackId}'];
}
spec['x-tagGroups'] = [
  { name: 'Warehouse layout', tags: ['Warehouses', 'Racks', 'Shelves'] },
  { name: 'Catalogue & stock', tags: ['Products', 'Shelf Stock', 'Stock', 'Stock Movements'] },
  {
    name: 'Cameras & recognition',
    tags: ['Devices', 'Camera Images', 'Image Events', 'Recognition Results'],
  },
  {
    name: 'Purchasing',
    tags: ['Demand Plans', 'Replenishment Recommendations', 'Purchase Orders'],
  },
  { name: 'Administration', tags: ['Users'] },
];

// ---------------------------------------------------------------- schemas

const schemas = spec.components.schemas;

schemas.ErrorResponse = {
  type: 'object',
  description: 'Business error produced by `GlobalExceptionHandler`.',
  required: ['status', 'message'],
  properties: {
    status: { type: 'integer', format: 'int32', description: 'HTTP status code, repeated in the body.' },
    message: { type: 'string', description: 'Human-readable reason. Do not parse it.' },
  },
  example: { status: 404, message: 'Product is not found' },
};

schemas.SpringErrorResponse = {
  type: 'object',
  description:
    'Default Spring Boot error body for request errors (validation, parameter conversion, content type) and unexpected 500s. In a development run with spring-boot-devtools it also contains `errors` (field details) and `trace`.',
  required: ['timestamp', 'status', 'error', 'path'],
  properties: {
    timestamp: { type: 'string', format: 'date-time' },
    status: { type: 'integer', format: 'int32' },
    error: { type: 'string', description: 'HTTP reason phrase.' },
    message: { type: 'string', description: 'Reason (enabled by `spring.web.error.include-message=always`).' },
    path: { type: 'string', description: 'Request path.' },
  },
  example: {
    timestamp: '2026-11-23T20:38:10.000Z',
    status: 400,
    error: 'Bad Request',
    message: "Validation failed for object='productRequestDto'. Error count: 2",
    path: '/products',
  },
};

/** Example + optional field descriptions for every DTO. IDs match the tutorial. */
const SCHEMA_DOCS = {
  WarehouseRequestDTO: { example: { name: 'Central warehouse', address: 'Gedimino pr. 1, Vilnius' } },
  WarehouseResponseDTO: { example: { id: 1, name: 'Central warehouse', address: 'Gedimino pr. 1, Vilnius' } },
  RackRequestDto: {
    example: { label: 'A', warehouseId: 1 },
    fields: { warehouseId: 'Ignored on create: the warehouse comes from the path.' },
  },
  RackResponseDto: { example: { id: 1, label: 'A', warehouseId: 1 } },
  ShelfRequestDto: {
    example: { label: 'A-1', capacity: 120, rackId: 1 },
    fields: { capacity: 'Maximum number of units the shelf holds. Must be > 0.' },
  },
  ShelfResponseDto: {
    example: { id: 1, label: 'A-1', capacity: 120, status: 'ACTIVE', rackId: 1 },
    fields: { status: '`INACTIVE` shelves are hidden from operations until activated.' },
  },
  ProductRequestDto: {
    example: { sku: 'SKU-1001', name: 'Milk 1L', description: 'UHT milk 3.2%, 1 litre carton' },
    fields: { sku: 'Stock keeping unit. Unique across the catalogue.' },
  },
  ProductResponseDto: {
    example: { id: 1, sku: 'SKU-1001', name: 'Milk 1L', description: 'UHT milk 3.2%, 1 litre carton' },
  },
  ShelfStockRequestDto: {
    example: { shelfId: 1, productId: 1, quantity: 24, expirationDate: '2026-12-20' },
    fields: { quantity: 'Units on the shelf. Must be > 0.', expirationDate: 'Today or later.' },
  },
  ShelfStockResponseDto: {
    example: { id: 1, shelfId: 1, productId: 1, quantity: 24, expirationDate: '2026-12-20', status: 'ACTIVE' },
    fields: { status: '`EXPIRED` once `expirationDate` has passed.' },
  },
  ProductStockResponseDto: {
    example: { productSKU: 'SKU-1001', productRemains: 20 },
    fields: { productRemains: 'Sum of `quantity` over all shelf stock records of this SKU.' },
  },
  StockMovementRequestDto: {
    example: { productId: 1, shelfId: 1, quantityDelta: -6, type: 'REMOVAL', source: 'MANUAL' },
    fields: { quantityDelta: 'Signed change in units: positive adds stock, negative removes it.' },
  },
  StockMovementResponseDto: {
    example: {
      id: 1,
      productId: 1,
      shelfId: 1,
      quantityDelta: -4,
      type: 'ADJUSTMENT',
      source: 'CAMERA',
      createdAt: '2026-11-23T10:15:02',
    },
  },
  DeviceRequestDto: {
    example: { deviceKey: 'shelf-a1-cam', shelfId: 1 },
    fields: {
      deviceKey: 'Unique key of the camera; the same value goes into the shelf agent config (`device.toml`).',
      shelfId: 'Shelf the camera photographs. A shelf can have only one camera.',
    },
  },
  DeviceResponseDto: {
    example: { id: 1, deviceKey: 'shelf-a1-cam', status: 'ACTIVE', lastSeenAt: '2026-11-23T10:15:02', shelfId: 1 },
    fields: { lastSeenAt: 'Last heartbeat or photo; `null` until the camera first reports.' },
  },
  CameraImageResponseDto: {
    example: {
      imageEventId: 1,
      recognitionResultId: 1,
      shelfId: 1,
      previousQuantity: 24,
      quantity: 20,
      quantityDelta: -4,
    },
    fields: {
      previousQuantity: 'Shelf stock before this photo.',
      quantity: 'Units counted by the Vision service; the shelf stock is set to this value.',
      quantityDelta: '`quantity - previousQuantity`. A non-zero delta creates a `CAMERA` stock movement.',
    },
  },
  ImageEventRequestDto: {
    example: { deviceId: 1, imagePath: 'uploads/camera/2f6c0a8e.image', capturedAt: '2026-11-23T10:15:00' },
  },
  ImageEventResponseDto: {
    example: {
      id: 1,
      deviceId: 1,
      shelfId: 1,
      imagePath: 'uploads/camera/2f6c0a8e.image',
      capturedAt: '2026-11-23T10:15:00',
      receivedAt: '2026-11-23T10:15:02',
      processingStatus: 'PROCESSED',
    },
    fields: { imagePath: 'Where the backend stored the photo (server-side path).' },
  },
  RecognitionResultRequestDto: {
    example: { imageEventId: 1, productId: 1, detectedQuantity: 20, quantityDelta: -4, confidence: 0.47 },
    fields: { confidence: 'Model confidence from 0 to 1.' },
  },
  RecognitionResultResponseDto: {
    example: {
      id: 1,
      imageEventId: 1,
      productId: 1,
      detectedQuantity: 20,
      quantityDelta: -4,
      confidence: null,
      status: 'CONFIRMED',
    },
    fields: {
      confidence:
        'Model confidence from 0 to 1; `null` for results created by the camera upload, which are confirmed automatically.',
    },
  },
  DemandPlanRequestDto: {
    example: { productId: 1, requiredDate: '2026-12-05', requiredQuantity: 120 },
    fields: { requiredDate: 'Today or later.', requiredQuantity: 'Must be > 0.' },
  },
  DemandPlanResponseDto: { example: { id: 1, productId: 1, requiredDate: '2026-12-05', requiredQuantity: 120 } },
  ReplenishmentRecommendationRequestDto: {
    example: { productId: 1, recommendedQuantity: 40, requiredBy: '2026-12-01', reason: 'Below minimum stock' },
    fields: { requiredBy: 'Today or later.', reason: 'Why this quantity is suggested; shown to the manager.' },
  },
  ReplenishmentRecommendationResponseDto: {
    example: {
      id: 1,
      productId: 1,
      recommendedQuantity: 40,
      requiredBy: '2026-12-01',
      reason: 'Below minimum stock',
      status: 'NEW',
      createdAt: '2026-11-23T10:20:00',
    },
  },
  PurchaseOrderRequestDto: {
    example: { recommendationId: 1, quantity: 40 },
    fields: { recommendationId: 'Only one purchase order can exist per recommendation.' },
  },
  PurchaseOrderResponseDto: {
    example: { id: 1, recommendationId: 1, productId: 1, quantity: 40, status: 'CREATED', createdAt: '2026-11-23T10:25:00' },
  },
  UserRequestDTO: {
    example: { email: 'anna@example.com', username: 'anna', password: 'S3cure-passw0rd' },
    fields: { password: 'Plain text in the request; stored as a BCrypt hash, never returned.' },
  },
  UserResponseDTO: {
    example: { id: 1, email: 'anna@example.com', username: 'anna', role: 'WORKER', status: 'ACTIVE' },
  },
};

for (const [name, docs] of Object.entries(SCHEMA_DOCS)) {
  const schema = schemas[name];
  if (!schema) throw new Error(`Schema ${name} is not in the generated spec any more`);
  schema.example = docs.example;
  for (const [field, description] of Object.entries(docs.fields ?? {})) {
    if (!schema.properties?.[field]) throw new Error(`${name}.${field} does not exist`);
    schema.properties[field].description = description;
  }
}
const undocumented = Object.keys(schemas).filter((name) => !schemas[name].example);
if (undocumented.length) throw new Error(`Schemas without an example: ${undocumented.join(', ')}`);

// ---------------------------------------------------------------- errors per operation

const NOT_FOUND = {
  Warehouses: 'Warehouse is not found',
  Racks: 'Rack is not found',
  Shelves: 'Shelf with id 42 is not found',
  Products: 'Product is not found',
  'Shelf Stock': 'Shelf stock is not found',
  'Stock Movements': 'Stock movement with id 42 is not found',
  Devices: 'Device with id 42 is not found',
  'Camera Images': 'Device not found: 42',
  'Image Events': 'Image event with id 42 is not found',
  'Recognition Results': 'Recognition result with id 42 is not found',
  'Demand Plans': 'Demand plan with id 42 is not found',
  'Replenishment Recommendations': 'Replenishment recommendation with id 42 is not found',
  'Purchase Orders': 'Purchase order with id 42 is not found',
  Users: 'User is not found',
};
/** On create/update a 404 usually means a referenced ID (productId, shelfId…) is wrong. */
const NOT_FOUND_REFERENCE = {
  createRack: 'Warehouse with id 42 is not found',
  createShelf: 'Rack with id 42 is not found',
  createShelfStock: 'Product is not found',
  createStockMovement: 'Product with id 42 is not found',
  createDevice: 'Shelf with id 42 is not found',
  createImageEvent: 'Device with id 42 is not found',
  createRecognitionResult: 'Image event with id 42 is not found',
  createDemandPlan: 'Product with id 42 is not found',
  createRecommendation: 'Product with id 42 is not found',
  createPurchaseOrder: 'Replenishment recommendation with id 42 is not found',
};
const CONFLICT = {
  createWarehouse: 'Warehouse with name Central warehouse and address Gedimino pr. 1, Vilnius already exists',
  updateWarehouse: 'Warehouse with name Central warehouse and address Gedimino pr. 1, Vilnius already exists',
  createProduct: 'Product with this SKU already exists',
  updateProduct: 'Product with this SKU already exists',
  createDevice: 'Device with key shelf-a1-cam already exists',
  updateDevice: 'Device with key shelf-a1-cam already exists',
  createUser: 'User with that email already exists',
  updateUser: 'User with that username already exists',
  createPurchaseOrder: 'Purchase order for recommendation with id 1 already exists',
};

const errorBody = (status, message) => ({
  'application/json': {
    schema: { $ref: '#/components/schemas/ErrorResponse' },
    example: { status, message },
  },
});
const springError = (status, error, message, path) => ({
  'application/json': {
    schema: { $ref: '#/components/schemas/SpringErrorResponse' },
    example: { timestamp: '2026-11-23T20:38:10.000Z', status, error, message, path },
  },
});
const examplePath = (path) => path.replace(/\{[^}]+\}/g, '42');
const decapitalize = (name) => name[0].toLowerCase() + name.slice(1);

const OP_DOCS = {
  uploadImage: {
    summary: 'Upload a shelf photo from a camera',
    description: `
Called by the shelf agent after it takes a photo. In one transaction the backend:

1. checks the device exists and is \`ACTIVE\`, and that its shelf has **exactly one** stock record;
2. saves the photo and sends it to the Vision service (\`POST /recognize\`), which returns the item count;
3. creates an image event (\`PROCESSED\`) and a \`CONFIRMED\` recognition result;
4. sets the shelf stock to the counted quantity and, if it changed, writes an \`ADJUSTMENT\` stock movement with source \`CAMERA\`;
5. updates the device's \`lastSeenAt\`.

Photos must arrive in capture order: a photo older than (or as old as) one already processed for the shelf is rejected with 409.

\`\`\`bash
curl -X POST "http://localhost:8080/devices/1/images?capturedAt=2026-11-23T10:15:00" \\
  -F "file=@shelf-a1.jpg;type=image/jpeg"
\`\`\``.trim(),
    params: { deviceId: 'ID of the camera (from `POST /devices`).', capturedAt: 'When the photo was taken, camera local time.' },
    responses: {
      200: 'Photo processed; stock updated to the counted quantity.',
      400: springError(400, 'Bad Request', "Required parameter 'capturedAt' is not present.", '/devices/1/images'),
      404: errorBody(404, 'Device not found: 42'),
      409: errorBody(409, 'An image with the same or a later capture time was already processed'),
      415: springError(415, 'Unsupported Media Type', "Content-Type 'application/json' is not supported.", '/devices/1/images'),
      502: errorBody(502, 'Vision service request failed'),
    },
    responseDescriptions: {
      400: 'Missing or invalid `capturedAt`, or an empty file (`Image is empty`).',
      404: 'Device does not exist.',
      409: 'Device is inactive (`Device is inactive`), the shelf does not have exactly one stock record (`Camera processing requires exactly one stock record on the shelf`), or the photo is older than one already processed.',
      415: 'The request is not `multipart/form-data`.',
      502: 'Vision service is down or returned an invalid count.',
    },
    fileDescription: 'JPEG or PNG photo of the shelf, up to 10 MB.',
  },
  getStocks: {
    summary: 'Get total stock for one or more SKUs',
    description:
      'Sums `quantity` over all shelf stock records of each SKU. Repeat the parameter for several SKUs: `GET /stocks?sku=SKU-1001&sku=SKU-2001`. Unknown SKUs are omitted from the result.',
    params: { sku: 'Product SKU; repeat for several.' },
    responses: {
      400: springError(400, 'Bad Request', "Required parameter 'sku' is not present.", '/stocks'),
    },
    responseDescriptions: { 400: '`sku` is missing.' },
  },
  getProductBySku: {
    responses: { 404: errorBody(404, 'Product with SKU SKU-9999 is not found') },
  },
  heartbeat: {
    description: 'Called by the shelf agent periodically. Sets `lastSeenAt` to now and the device status to `ACTIVE`.',
  },
  createDevice: {
    responses: {
      500: springError(500, 'Internal Server Error', null, '/devices'),
    },
    responseDescriptions: {
      500: '**Known issue:** the shelf already has a camera (`ShelfAlreadyHasDeviceException` is not mapped to 409 yet).',
    },
  },
};

const PARAM_DOCS = {
  id: 'Resource ID.',
  from: 'Start of the range (inclusive).',
  to: 'End of the range (inclusive).',
  min: 'Minimum confidence, from 0 to 1.',
  name: 'Part of the product name, case-insensitive.',
  date: 'Expiry date to match.',
};

for (const [path, item] of Object.entries(spec.paths)) {
  for (const [method, op] of Object.entries(item)) {
    op.tags = op.tags.map((tag) => TAG_RENAMES[tag] ?? tag);
    const tag = op.tags[0];
    const docs = OP_DOCS[op.operationId] ?? {};
    if (docs.summary) op.summary = docs.summary;
    if (docs.description) op.description = docs.description;
    if (!op.summary) throw new Error(`${method.toUpperCase()} ${path} has no summary`);

    for (const param of op.parameters ?? []) {
      param.description ??= docs.params?.[param.name] ?? PARAM_DOCS[param.name];
      if (param.schema?.format === 'date-time') param.example ??= '2026-11-23T10:15:00';
      else if (param.schema?.format === 'date') param.example ??= '2026-12-01';
      else if (param.schema?.type === 'integer') param.example ??= 1;
    }
    if (docs.fileDescription) {
      op.requestBody.content['multipart/form-data'].schema.properties.file.description = docs.fileDescription;
      op.requestBody.content['multipart/form-data'].encoding = { file: { contentType: 'image/jpeg, image/png' } };
    }

    const bodyRef = op.requestBody?.content?.['application/json']?.schema?.$ref;
    const hasPathOrQuery = (op.parameters ?? []).length > 0;
    if ((bodyRef || hasPathOrQuery) && !op.responses['400']) {
      op.responses['400'] = { description: 'Invalid parameter.' };
    }

    for (const [code, response] of Object.entries(op.responses)) {
      const status = Number(code);
      if (docs.responseDescriptions?.[code]) response.description = docs.responseDescriptions[code];
      if (docs.responses?.[code] && typeof docs.responses[code] === 'string') {
        response.description = docs.responses[code];
      }
      if (status === 204) {
        delete response.content;
        continue;
      }
      if (status < 300) {
        const content = response.content?.['*/*'] ?? response.content?.['application/json'];
        if (content) {
          response.content = { 'application/json': content };
          if (content.schema?.type === 'integer' && method === 'post') {
            response.description = `${response.description}. The body is the ID of the new resource.`;
            content.example = 1;
          }
        }
        continue;
      }
      if (docs.responses?.[code] && typeof docs.responses[code] === 'object') {
        response.content = docs.responses[code];
        if (response.content['application/json'].example.message === null) {
          delete response.content['application/json'].example.message;
        }
        continue;
      }
      if (status === 400) {
        response.content = bodyRef
          ? springError(
              400,
              'Bad Request',
              `Validation failed for object='${decapitalize(bodyRef.split('/').pop())}'. Error count: 1`,
              examplePath(path),
            )
          : springError(
              400,
              'Bad Request',
              hasPathOrQuery && (op.parameters ?? []).some((p) => p.in === 'path')
                ? "Method parameter 'id': Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'; For input string: \"abc\""
                : `Required parameter '${op.parameters[0].name}' is not present.`,
              examplePath(path),
            );
        if (response.description === 'Invalid parameter.' && bodyRef) {
          response.description = 'Invalid JSON or a field fails validation.';
        }
        continue;
      }
      if (status === 404) {
        const message = (method === 'post' && NOT_FOUND_REFERENCE[op.operationId]) || NOT_FOUND[tag];
        response.content = errorBody(404, message);
        continue;
      }
      if (status === 409) {
        response.content = errorBody(409, CONFLICT[op.operationId] ?? 'Conflict');
        continue;
      }
      throw new Error(`No error example for ${code} on ${op.operationId}`);
    }
    // Responses the code can return but the controller does not declare.
    for (const [code, content] of Object.entries(docs.responses ?? {})) {
      if (op.responses[code] || typeof content === 'string') continue;
      if (content['application/json'].example.message === null) delete content['application/json'].example.message;
      op.responses[code] = { description: docs.responseDescriptions?.[code] ?? 'Error.', content };
    }
    op.responses = Object.fromEntries(
      Object.entries(op.responses).sort(([a], [b]) => Number(a) - Number(b)),
    );
  }
}

// ---------------------------------------------------------------- write

const header = `# GENERATED by scripts/build-spec.mjs from source/openapi.generated.json — do not edit by hand.
# Rebuild: npm run build (in Documentation/api). Validate: npm run lint.
`;
await writeFile(
  new URL('../openapi.yaml', import.meta.url),
  header + YAML.stringify(spec, { lineWidth: 0, aliasDuplicateObjects: false }),
);
const operations = Object.values(spec.paths).flatMap((item) => Object.keys(item)).length;
console.log(`openapi.yaml: ${Object.keys(spec.paths).length} paths, ${operations} operations`);
