package com.tangem.features.txhistory.model

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Unit tests for [resolveOwner]: resolving a counterparty address to a [ResolvedOwner] with the precedence
 * account (accounts mode on) → wallet (accounts mode off) → external. Also pins the two non-obvious rules the KDoc
 * relies on: the `networkRawId == null` cross-network lookup (accepted only on exactly one account) and the
 * case-insensitive fallback that tolerates EIP-55 vs. lowercase address drift.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResolveOwnerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun resolveOwner(model: Model) {
        // Act
        val result = model.lookup.resolveOwner(address = model.address, networkRawId = model.networkRawId)

        // Assert
        assertThat(result).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels(): List<Model> = listOf(
        // Accounts mode: a crypto-portfolio own address names the owning account.
        Model(
            name = "accounts mode, own crypto address -> OwnAccount",
            lookup = lookup(ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT)),
            address = OWN_ADDRESS,
            networkRawId = ETH,
            expected = ResolvedOwner.OwnAccount(CRYPTO_ACCOUNT),
        ),
        // Accounts mode: a Tangem Pay deposit address resolves as own — not external.
        Model(
            name = "accounts mode, own payment address -> OwnPaymentAccount",
            lookup = lookup(ETH to mapOf(OWN_ADDRESS to PAYMENT_ACCOUNT)),
            address = OWN_ADDRESS,
            networkRawId = ETH,
            expected = ResolvedOwner.OwnPaymentAccount(PAYMENT_ACCOUNT),
        ),
        // Accounts mode: an address that is none of the user's is external.
        Model(
            name = "accounts mode, unknown address -> External",
            lookup = lookup(ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT)),
            address = EXTERNAL_ADDRESS,
            networkRawId = ETH,
            expected = ResolvedOwner.External(EXTERNAL_ADDRESS),
        ),
        // Wallet mode with wallet info present: an own address names the owning wallet.
        Model(
            name = "wallet mode, own address with wallet info -> OwnWallet",
            lookup = lookup(
                ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT),
                isAccountsModeEnabled = false,
                walletInfoById = mapOf(USER_WALLET_ID to WALLET_INFO),
            ),
            address = OWN_ADDRESS,
            networkRawId = ETH,
            expected = ResolvedOwner.OwnWallet(USER_WALLET_ID, WALLET_INFO),
        ),
        // Wallet mode but the resolved wallet has no display info: falls back to external.
        Model(
            name = "wallet mode, own address but wallet info missing -> External",
            lookup = lookup(
                ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT),
                isAccountsModeEnabled = false,
                walletInfoById = emptyMap(),
            ),
            address = OWN_ADDRESS,
            networkRawId = ETH,
            expected = ResolvedOwner.External(OWN_ADDRESS),
        ),
        // Case drift (EIP-55 checksummed stored, lowercase queried) still resolves as own.
        Model(
            name = "case-insensitive fallback (EIP-55 drift) -> OwnAccount",
            lookup = lookup(ETH to mapOf(OWN_ADDRESS_CHECKSUM to CRYPTO_ACCOUNT)),
            address = OWN_ADDRESS_LOWER,
            networkRawId = ETH,
            expected = ResolvedOwner.OwnAccount(CRYPTO_ACCOUNT),
        ),
        // Unresolved leg (networkRawId null): the address maps to a single account across chains -> accepted.
        Model(
            name = "network null, same account across networks -> OwnAccount",
            lookup = lookup(
                ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT),
                POLYGON to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT),
            ),
            address = OWN_ADDRESS,
            networkRawId = null,
            expected = ResolvedOwner.OwnAccount(CRYPTO_ACCOUNT),
        ),
        // Unresolved leg: the address maps to two distinct accounts -> ambiguous -> external.
        Model(
            name = "network null, tie across distinct accounts -> External",
            lookup = lookup(
                ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT),
                POLYGON to mapOf(OWN_ADDRESS to SECONDARY_ACCOUNT),
            ),
            address = OWN_ADDRESS,
            networkRawId = null,
            expected = ResolvedOwner.External(OWN_ADDRESS),
        ),
        // Unresolved leg: address is none of the user's -> external.
        Model(
            name = "network null, unknown address -> External",
            lookup = lookup(ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT)),
            address = EXTERNAL_ADDRESS,
            networkRawId = null,
            expected = ResolvedOwner.External(EXTERNAL_ADDRESS),
        ),
        // The requested network has no own addresses -> external.
        Model(
            name = "network absent from map -> External",
            lookup = lookup(ETH to mapOf(OWN_ADDRESS to CRYPTO_ACCOUNT)),
            address = OWN_ADDRESS,
            networkRawId = POLYGON,
            expected = ResolvedOwner.External(OWN_ADDRESS),
        ),
    )

    private fun lookup(
        vararg networks: Pair<Network.RawID, Map<String, Account>>,
        isAccountsModeEnabled: Boolean = true,
        walletInfoById: Map<UserWalletId, WalletInfo> = mapOf(USER_WALLET_ID to WALLET_INFO),
    ): TxHistoryLookupContext = TxHistoryLookupContext(
        ownAccountByNetwork = networks.toMap(),
        isAccountsModeEnabled = isAccountsModeEnabled,
        walletInfoById = walletInfoById,
    )

    internal data class Model(
        val name: String,
        val lookup: TxHistoryLookupContext,
        val address: String,
        val networkRawId: Network.RawID?,
        val expected: ResolvedOwner,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val ETH = Network.RawID(value = "ethereum")
        val POLYGON = Network.RawID(value = "polygon")

        val USER_WALLET_ID = MockAccounts.userWalletId
        val CRYPTO_ACCOUNT = Account.Personal.createMainAccount(USER_WALLET_ID)
        val SECONDARY_ACCOUNT = MockAccounts.createAccount(derivationIndex = 1, userWalletId = USER_WALLET_ID)
        val PAYMENT_ACCOUNT = Account.Payment(USER_WALLET_ID)
        val WALLET_INFO = WalletInfo(
            name = "My Wallet",
            deviceIconUM = DeviceIconUM.Card(mainColor = Color(color = 0xFF1E1E1E), secondColor = null),
        )

        const val OWN_ADDRESS = "0xOwnAddress1234"
        const val OWN_ADDRESS_CHECKSUM = "0xABCDEF1234567890"
        const val OWN_ADDRESS_LOWER = "0xabcdef1234567890"
        const val EXTERNAL_ADDRESS = "0xExternalAddress5678"
    }
}