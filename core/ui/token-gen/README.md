# token-gen

Generates Kotlin (Jetpack Compose) source files from design tokens defined in the `ds-tokens` git submodule.

## Updating tokens

1. Update the `ds-tokens` submodule to the latest commit:
   ```bash
   git submodule update --remote core/ui/ds-tokens
   ```
2. Re-run the build:
   ```bash
   cd core/ui/token-gen && npm run build
   ```
3. Commit both the submodule pointer and generated files.

## How it works

The script uses [Style Dictionary v5](https://styledictionary.com/) with [@tokens-studio/sd-transforms](https://github.com/tokens-studio/sd-transforms) to read JSON token files from `core/ui/ds-tokens/tokens/` and generate Kotlin files into `core/ui/src/main/java/com/tangem/core/ui/res/generated/`.

All generated files are written to `com.tangem.core.ui.res.generated` and should not be edited manually.
