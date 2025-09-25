import js from "@eslint/js";
import globals from "globals";
import security from "eslint-plugin-security";

export default [
  {
    ignores: [
      "**/factgraph-3.1.0.js",
      "**/uswds-3.13.0/**",
      "**/pdf-lib-1.17.1.min.js"
    ],
  },
  security.configs.recommended,
  {
    languageOptions: {
      globals: { ...globals.browser },
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: "module",
      },
    },
    rules: {
      "no-eval": "error",
      "no-new-func": "error",
      "no-implied-eval": "error",
      "no-implicit-globals": "error",
      "eqeqeq": "error",
    },
  },
];
