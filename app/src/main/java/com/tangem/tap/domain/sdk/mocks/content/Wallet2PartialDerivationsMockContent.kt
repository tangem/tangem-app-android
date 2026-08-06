package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.domain.sdk.mocks.MockContent

/**
 * Wallet 2.0 (derivation style V3) multicurrency card that derives ONLY Bitcoin when addresses are
 * generated — every other token stays `MissedDerivation`, so the main screen shows a MIXED state (a derived
 * token next to underived ones) and the "Some addresses are missing" banner remains after tapping
 * "Synchronize addresses".
 *
 * Based on [Wallet2WithDerivationsMockContent] (NOT [WalletMockContent]) on purpose: with the V3 style the
 * card's default Bitcoin path is segwit `m/84'/0'/0'/0/0`, which matches what the WireMock `/user-tokens`
 * stub sends. A V2 card (default legacy `m/44'/0'/0'/0/0`) would flag Bitcoin as
 * `Network.DerivationPath.Custom` → `CryptoCurrency.isCustom` → the grey "custom derivation" dot and some
 * disabled actions. See [Wallet2WithDerivationsMockContent] for details.
 *
 * On-screen derivations come from `ScanResponse.derivedKeys`, which starts empty (at scan everything is
 * `MissedDerivation` and the banner shows) and is filled by merging [derivationTaskResponse] on
 * "Synchronize addresses". That response is restricted here to Bitcoin's derivations only — coin type 0,
 * keeping both legacy `m/44'/0'/…` and segwit `m/84'/0'/…`. The Bitcoin extended public key is inherited
 * (re-keyed) from [WalletMockContent] via [Wallet2WithDerivationsMockContent], so the resolved address is
 * byte-identical and picks up the same positive WireMock balance.
 */
object Wallet2PartialDerivationsMockContent : MockContent by Wallet2WithDerivationsMockContent {

    /** Bitcoin (coin type 0) derivation-path prefixes — legacy, wrapped-segwit, native-segwit. */
    private val bitcoinPathPrefixes = listOf("m/44'/0'/", "m/49'/0'/", "m/84'/0'/")

    override val derivationTaskResponse = DerivationTaskResponse(
        entries = Wallet2WithDerivationsMockContent.derivationTaskResponse.entries
            .mapValues { (_, keys) ->
                ExtendedPublicKeysMap(
                    keys.filterKeys { path -> bitcoinPathPrefixes.any { path.rawPath.startsWith(it) } },
                )
            }
            .filterValues { it.isNotEmpty() },
    )
}