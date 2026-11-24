import { describe, expect, it } from 'vitest';
import {
  isValid,
  validateDevice,
  validateLogin,
  validateRecommendationChange,
} from './validation.js';

const isoDate = (offsetDays) => {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

describe('validateLogin', () => {
  it('requires both fields and ignores surrounding spaces in the username', () => {
    expect(Object.keys(validateLogin({ username: '  ', password: '' }))).toEqual([
      'username',
      'password',
    ]);
    expect(isValid(validateLogin({ username: 'admin', password: 'x' }))).toBe(true);
  });
});

describe('validateDevice', () => {
  it.each(['ab', '-cam', 'Shelf-A', 'shelf a', 'a'.repeat(65)])('rejects key "%s"', (key) => {
    expect(validateDevice({ deviceKey: key, shelfId: '1' })).toHaveProperty('deviceKey');
  });

  it('accepts a key in the shelf agent format and needs a shelf', () => {
    expect(validateDevice({ deviceKey: 'shelf-b3-cam', shelfId: '4' })).toEqual({});
    expect(validateDevice({ deviceKey: 'shelf-b3-cam', shelfId: '' })).toHaveProperty('shelfId');
  });
});

describe('validateRecommendationChange', () => {
  const valid = { quantity: '12', requiredBy: isoDate(3), reason: 'Promo week' };

  it('accepts a positive whole quantity, a future date and a reason', () => {
    expect(validateRecommendationChange(valid)).toEqual({});
  });

  it.each(['', '0', '-3', '2.5', 'abc'])('rejects quantity "%s"', (quantity) => {
    expect(validateRecommendationChange({ ...valid, quantity })).toHaveProperty('quantity');
  });

  it('rejects a past date and an empty reason', () => {
    const errors = validateRecommendationChange({ ...valid, requiredBy: isoDate(-1), reason: ' ' });
    expect(errors).toEqual({
      requiredBy: 'The date cannot be in the past.',
      reason: 'Explain the change.',
    });
  });
});
