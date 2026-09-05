/** @type {import('@commitlint/types').UserConfig} */
export default {
  extends: ["@commitlint/config-conventional"],
  // DRY: keep rules minimal, SIMPLE overrides only if needed
  // Uncomment to allow monorepo scopes:
  // rules: {
  //   "scope-enum": [2, "always", ["common", "api", "worker", "build-logic", "deps"]],
  // }
  rules: {
    // hedge-fund: allow longer header if needed, but warn
    "header-max-length": [1, "always", 100],
    "body-max-line-length": [1, "always", 150]
  }
};
