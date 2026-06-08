# token-gen

Generates Kotlin (Jetpack Compose) source files from design tokens defined in the `ds-tokens` git submodule.

## Updating tokens

> **Note:** You only need `git submodule update --remote` when you want to pull **new** design tokens
> from the remote `ds-tokens` repository. If you're just regenerating Kotlin from the tokens already
> checked out (e.g. changing the generation script), **skip step 1** — don't run it without the need,
> as it moves the submodule pointer to the latest remote commit and pulls in unrelated token changes.
>
> For all other cases (a fresh checkout, or making sure the submodule is at the pinned commit), use:
> ```bash
> git submodule update --init --recursive
> ```
> This checks out the submodule at the commit already recorded in the repo, without pulling anything new.

1. *(Only if you need newer tokens)* Update the `ds-tokens` submodule to the latest commit:
   ```bash
   git submodule update --remote core/ui/ds-tokens
   ```
2. Re-run the build:
   ```bash
   cd core/ui/token-gen && npm run build
   ```
3. Commit the generated files (and the submodule pointer too, only if you ran step 1).

## How it works

The script uses [Style Dictionary v5](https://styledictionary.com/) with [@tokens-studio/sd-transforms](https://github.com/tokens-studio/sd-transforms) to read JSON token files from `core/ui/ds-tokens/tokens/` and generate Kotlin files into `core/ui/src/main/java/com/tangem/core/ui/res/generated/`.

All generated files are written to `com.tangem.core.ui.res.generated` and should not be edited manually.
