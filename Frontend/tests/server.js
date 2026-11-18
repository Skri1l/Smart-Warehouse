import { setupServer } from 'msw/node';
import { handlers } from '../src/mocks/handlers.js';

/** MSW server for Vitest: the same handlers the browser mock uses. */
export const server = setupServer(...handlers);
