# token-gen

Generates Kotlin (Jetpack Compose) source files from design tokens and icons defined in the `ds-tokens` git submodule.

## Making sure submodule is at the pinned commit

***For the most cases*** (a fresh checkout, or making sure the submodule is at the pinned commit), use:
```bash
git submodule update --init --recursive
```

## Updating tokens

> **Note:** You only need `git submodule update --remote` when you want to pull **new** design tokens
> from the remote `ds-tokens` repository. If you're just regenerating Kotlin from the tokens already
> checked out (e.g. changing the generation script), **skip step 1** — don't run it without the need,
> as it moves the submodule pointer to the latest remote commit and pulls in unrelated token changes.
> This checks out the submodule at the commit already recorded in the repo, without pulling anything new.

1. *(Only if you need newer tokens)* Update the `ds-tokens` submodule to the latest commit:
   ```bash
   git submodule update --remote core/ui/ds-tokens
   ```
2. Re-run the build:
   ```bash
   cd core/ui/token-gen && npm run build
   ```
   `npm run build` generates tokens, icons and assets. To regenerate only one of them:
   ```bash
   npm run build:tokens   # node build-tokens.mjs (also runs icons + assets, folds their hashes into .tokens-hash)
   npm run build:icons    # node build-icons.mjs  (generates both icons and assets)
   ```
3. Commit the generated files (and the submodule pointer too, only if you ran step 1).

## How it works

The build runs two scripts:

- **`build-tokens.mjs`** uses [Style Dictionary v5](https://styledictionary.com/) with [@tokens-studio/sd-transforms](https://github.com/tokens-studio/sd-transforms) to read JSON token files from `core/ui/ds-tokens/tokens/` and generate Kotlin files into `core/ui/src/main/java/com/tangem/core/ui/res/generated/`.
- **`build-icons.mjs`** generates Compose `ImageVector` Kotlin sources from two SVG folders:
  - `core/ui/ds-tokens/icons/` → `…/res/generated/icons/` as extension properties on the `Icons` object.
    Icons are single-color and tintable — the `#0F0F0F` placeholder is rewritten to `Color.Black` so
    `Icon(tint = …)` can re-color them.
  - `core/ui/ds-tokens/assets/` → `…/res/generated/assets/` as extension properties on the **same `Icons`
    object** (imported from the icons package). Assets are illustrations that keep their own colors
    verbatim (no tint placeholder), reachable at call sites as `Icons.il_token_custom` etc.

All generated files are written to `com.tangem.core.ui.res.generated` and should not be edited manually.
