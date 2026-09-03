package com.tangem.domain.models.scan.serialization

import com.squareup.moshi.Moshi

/**
 * Moshi adapters that serialize [com.tangem.domain.models.scan.ScanResponse] and its card-crypto parts
 * (derived keys, extended public keys, backup status, derivation paths).
 *
 * Owned by `domain:models` and exposed here so every [Moshi] that needs to (de)serialize card data
 * reuses the exact same set instead of re-listing the individual adapters.
 */
fun scanResponseMoshiAdapters(): List<Any> = listOf(
    WalletDerivedKeysMapAdapter(),
    ScanResponseDerivedKeysMapAdapter(),
    ByteArrayKeyAdapter(),
    ExtendedPublicKeysMapAdapter(),
    CardBackupStatusAdapter(),
    DerivationPathAdapterWithMigration(),
)

/** Adds [scanResponseMoshiAdapters] to this [Moshi.Builder]. */
fun Moshi.Builder.addScanResponseAdapters(): Moshi.Builder = apply {
    scanResponseMoshiAdapters().forEach(::add)
}