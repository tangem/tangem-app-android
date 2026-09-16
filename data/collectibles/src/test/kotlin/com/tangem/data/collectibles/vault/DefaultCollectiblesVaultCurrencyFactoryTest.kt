package com.tangem.data.collectibles.vault

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import com.tangem.domain.models.network.Network
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

/** Describing the network by hand is only safe while it agrees with what the shared factory builds. */
internal class DefaultCollectiblesVaultCurrencyFactoryTest {

    private val excludedBlockchains: ExcludedBlockchains = mockk {
        every { contains(any<Blockchain>()) } returns false
    }

    private val factory = DefaultCollectiblesVaultCurrencyFactory()

    private val expectedNetwork: Network by lazy {
        requireNotNull(
            NetworkFactory(excludedBlockchains).create(
                blockchain = Blockchain.Solana,
                extraDerivationPath = null,
                derivationStyleProvider = null,
                canHandleTokens = true,
            ),
        )
    }

    @Test
    fun `GIVEN a mint WHEN the token is created THEN it matches the token the shared factory would build`() {
        // Arrange
        val expected = CryptoCurrencyFactory(excludedBlockchains).createToken(
            network = expectedNetwork,
            rawId = CollectiblesVaultCurrencies.USDC_RAW_ID,
            name = CollectiblesVaultCurrencies.TOKEN_NAME,
            symbol = CollectiblesVaultCurrencies.TOKEN_SYMBOL,
            decimals = CollectiblesVaultCurrencies.DECIMALS,
            contractAddress = MINT,
        )

        // Act
        val actual = factory.create(mint = MINT)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    /** The mint comes from the program's config, so two configs must not collapse into one currency. */
    @Test
    fun `GIVEN two mints WHEN tokens are created THEN their ids differ`() {
        // Act
        val first = factory.create(mint = MINT)
        val second = factory.create(mint = OTHER_MINT)

        // Assert
        assertThat(first.id).isNotEqualTo(second.id)
    }

    private companion object {
        const val MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
        const val OTHER_MINT = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
    }
}