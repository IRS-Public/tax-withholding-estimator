import globals from 'globals'
import { defineConfig, globalIgnores } from 'eslint/config'
import neostandard from 'neostandard'
import security from 'eslint-plugin-security'

export default defineConfig([
  globalIgnores(['./website-static/vendor/*']),
  ...neostandard(),
  security.configs.recommended,
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        factGraph: 'writable',
        PDFLib: 'readonly'
      },
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
      },
    },
    rules: {
      'no-eval': 'error',
      'no-new-func': 'error',
      'no-implied-eval': 'error',
      'no-implicit-globals': 'error',
      eqeqeq: 'error',
    },
  },
])
