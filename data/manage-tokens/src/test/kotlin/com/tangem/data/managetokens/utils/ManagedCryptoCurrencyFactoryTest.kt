package com.tangem.data.managetokens.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.jointaccount.derivation.toNetworkDerivationPath
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.account.OwnerKeyIndex
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The networks a token can be added on follow the account: a personal account puts its index into the card path,
 * a joint account puts every network on the owner key path.
 */
internal class ManagedCryptoCurrencyFactoryTest {

    private val excludedBlockchains = ExcludedBlockchains()
    private val factory = ManagedCryptoCurrencyFactory(
        networkFactory = NetworkFactory(excludedBlockchains),
        excludedBlockchains = excludedBlockchains,
    )
    private val userWallet = MockUserWalletFactory.create()
    private val ownerPath = OwnerKeyIndex(value = 2).getOrNull()!!.toNetworkDerivationPath()

    private val coinsResponse = CoinsResponse(
        imageHost = null,
        coins = listOf(
            CoinsResponse.Coin(
                id = "tether",
                name = "Tether",
                symbol = "USDT",
                active = true,
                networks = listOf(
                    CoinsResponse.Coin.Network(
                        networkId = "ethereum",
                        contractAddress = USDT_CONTRACT,
                        decimalCount = BigDecimal(6),
                    ),
                ),
            ),
        ),
        total = 1,
    )
    private val tokensResponse = UserTokensResponse(
        group = UserTokensResponse.GroupType.NONE,
        sort = UserTokensResponse.SortType.BALANCE,
        tokens = listOf(
            UserTokensResponse.Token(
                id = "tether",
                networkId = "ethereum",
                name = "Tether",
                symbol = "USDT",
                decimals = 6,
                contractAddress = USDT_CONTRACT,
            ),
        ),
    )

    @Test
    fun `GIVEN joint account WHEN create THEN the token is offered and marked added on the owner key path`() {
        // Act
        val items = factory.create(
            coinsResponse = coinsResponse,
            tokensResponse = tokensResponse,
            userWallet = userWallet,
            accountDerivation = AccountDerivation.Joint(ownerPath),
        )

        // Assert
        val token = items.single() as ManagedCryptoCurrency.Token
        assertThat(token.availableNetworks.map { it.network.derivationPath }).containsExactly(ownerPath)
        assertThat(token.addedIn.map { it.derivationPath }).containsExactly(ownerPath)
    }

    @Test
    fun `GIVEN personal account WHEN create THEN the token sits on the path of the account, not the owner key`() {
        // Act
        val items = factory.create(
            coinsResponse = coinsResponse,
            tokensResponse = tokensResponse,
            userWallet = userWallet,
            accountDerivation = AccountDerivation.Personal(DerivationIndex(value = 1).getOrNull()!!),
        )

        // Assert
        val token = items.single() as ManagedCryptoCurrency.Token
        val path = token.availableNetworks.single().network.derivationPath
        assertThat(path).isNotEqualTo(ownerPath)
        assertThat(token.addedIn.map { it.derivationPath }).containsExactly(path)
    }

    @Test
    fun `GIVEN joint account without a wallet WHEN create THEN nothing is offered`() {
        // Act
        val items = factory.create(
            coinsResponse = coinsResponse,
            tokensResponse = null,
            userWallet = null,
            accountDerivation = AccountDerivation.Joint(ownerPath),
        )

        // Assert
        assertThat(items).isEmpty()
    }

    private companion object {
        const val USDT_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
}