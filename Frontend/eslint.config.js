import js from '@eslint/js';
import globals from 'globals';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import prettier from 'eslint-config-prettier';

export default [
  { ignores: ['dist', 'coverage', 'public/mockServiceWorker.js'] },
  js.configs.recommended,
  react.configs.flat.recommended,
  react.configs.flat['jsx-runtime'],
  jsxA11y.flatConfigs.recommended,
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.browser },
    },
    settings: { react: { version: '18.3' } },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react/prop-types': 'off',
      'react/no-danger': 'error',
      'no-console': 'error',
      'no-var': 'error',
      'prefer-const': 'error',
    },
  },
  {
    files: ['tests/**', '**/*.test.{js,jsx}', 'vite.config.js', 'eslint.config.js'],
    languageOptions: { globals: { ...globals.node, ...globals.vitest } },
  },
  prettier,
];
