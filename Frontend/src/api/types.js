/**
 * Response shapes of the backend REST API, copied from the Java DTO records in
 * `Backend/src/main/java/com/skr1l/backend/<resource>/dto/*ResponseDto.java`.
 * Dates are ISO strings (`LocalDate` = `YYYY-MM-DD`, `LocalDateTime` = `YYYY-MM-DDTHH:mm:ss`).
 *
 * @typedef {'ADMIN' | 'MANAGER' | 'WORKER'} UserRole
 * @typedef {{ id: number, username: string, email: string, role: UserRole }} CurrentUser
 * @typedef {{ id: number, sku: string, name: string, description: string | null }} Product
 * @typedef {{ id: number, name: string, address: string }} Warehouse
 * @typedef {{ id: number, label: string, warehouseId: number }} Rack
 * @typedef {{ id: number, label: string, capacity: number, status: 'ACTIVE' | 'INACTIVE', rackId: number }} Shelf
 * @typedef {{ id: number, shelfId: number, productId: number, quantity: number, expirationDate: string, status: 'ACTIVE' | 'EXPIRED' }} ShelfStock
 * @typedef {'RECEIPT' | 'REMOVAL' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'WRITE_OFF' | 'ADJUSTMENT'} StockMovementType
 * @typedef {'MANUAL' | 'CAMERA' | 'SYSTEM'} StockMovementSource
 * @typedef {{ id: number, productId: number, shelfId: number, quantityDelta: number, type: StockMovementType, source: StockMovementSource, createdAt: string }} StockMovement
 * @typedef {{ id: number, deviceKey: string, status: 'ACTIVE' | 'INACTIVE', lastSeenAt: string | null, shelfId: number }} Device
 * @typedef {{ id: number, deviceId: number, shelfId: number, imagePath: string, capturedAt: string, receivedAt: string, processingStatus: 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'FAILED' }} ImageEvent
 * @typedef {{ id: number, imageEventId: number, productId: number, detectedQuantity: number, quantityDelta: number, confidence: number, status: 'DETECTED' | 'CONFIRMED' | 'REJECTED' | 'FAILED' }} RecognitionResult
 * @typedef {'NEW' | 'APPROVED' | 'REJECTED' | 'ORDERED' | 'COMPLETED'} RecommendationStatus
 * @typedef {{ id: number, productId: number, recommendedQuantity: number, requiredBy: string, reason: string, status: RecommendationStatus, createdAt: string }} Recommendation
 */
export {};
