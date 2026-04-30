package com.tangem.tap.domain.sdk.mocks.content

import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.tap.domain.sdk.mocks.MockContent

/**
 * Mock content for UI tests that need the Wallet 2 derivation style (V3) AND the full set of derivations
 * provided by [WalletMockContent].
 *
 * Why it exists:
 * - [WalletMockContent] is Wallet 1 (derivation style V2), so for Bitcoin the card's default path resolves to
 *   `m/44'/0'/0'/0/0`. WireMock `/user-tokens` stubs send `m/84'/0'/0'/0/0` → the path does not match the card default
 *   → `Network.DerivationPath.Custom` → `CryptoCurrency.isCustom == true`. That hides the swap-to-another-token button
 *   introduced in the Send flow and causes the Swap receive card to stay in Empty/Loading state.
 * - [Wallet2MockContent] is V3 (matches WireMock) but its `derivationTaskResponse` is keyed by the wrong wallet
 *   public key (inherited from [WalletMockContent]) and only contains a handful of derivation paths. As a result
 *   address synchronisation fails in tests that need more than BTC/ETH/BCH/DOGE.
 *
 * This mock combines both: Wallet 2 card DTO from [Wallet2MockContent] (V3 derivation style) + full derivation
 * entries from [WalletMockContent] re-keyed to [Wallet2MockContent]'s own wallet public keys.
 */
object Wallet2WithDerivationsMockContent : MockContent by Wallet2MockContent {

    private val secp256k1Pubkey: ByteArray =
        Wallet2MockContent.cardDto.wallets.first { it.curve == EllipticCurve.Secp256k1 }.publicKey

    private val ed25519Pubkey: ByteArray =
        Wallet2MockContent.cardDto.wallets.first { it.curve == EllipticCurve.Ed25519 }.publicKey

    override val derivationTaskResponse: DerivationTaskResponse = DerivationTaskResponse(
        entries = rekey(WalletMockContent.derivationTaskResponse.entries),
    )

    override val createProductWalletTaskResponse: CreateProductWalletTaskResponse =
        CreateProductWalletTaskResponse(
            card = Wallet2MockContent.cardDto,
            derivedKeys = rekey(WalletMockContent.createProductWalletTaskResponse.derivedKeys),
            primaryCard = Wallet2MockContent.createProductWalletTaskResponse.primaryCard,
        )

    /**
     * Takes an entry map keyed by [WalletMockContent]'s wallet public keys (Secp256k1 first, Ed25519 second in
     * insertion order) and re-keys it to [Wallet2MockContent]'s own wallet public keys so that
     * [com.tangem.data.wallets.derivations.DerivationsSource] lookups by card wallet pubkey succeed.
     */
    private fun rekey(
        sourceEntries: Map<ByteArrayKey, ExtendedPublicKeysMap>,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap> {
        val values = sourceEntries.values.toList()
        return buildMap {
            values.getOrNull(index = 0)?.let { put(ByteArrayKey(secp256k1Pubkey), it) }
            values.getOrNull(index = 1)?.let { put(ByteArrayKey(ed25519Pubkey), it) }
        }
    }
}