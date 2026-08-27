package com.tangem.data.account.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.*
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Routing of the `/accounts` rows by their `type`: a joint account of the wallet is a row of the very same
 * document as the crypto accounts, distinguished only by that field — inside [CryptoPortfolioConverter].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountRowRoutingTest {

    private val userWallet: UserWallet = mockk {
        every { walletId } returns WALLET_ID
    }
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk {
        every { createCurrencies(tokens = any(), userWallet = any(), accountIndex = any<DerivationIndex>()) } returns emptyList()
        every {
            createCurrencies(tokens = any(), userWallet = any(), derivationPath = any<Network.DerivationPath.Joint>())
        } returns emptyList()
    }
    private val rowConverter = CryptoPortfolioConverter(
        userWallet = userWallet,
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        userTokensResponseFactory = mockk(),
    )

    // Stubbed where it is declared: the converter resolves the factory in its constructor
    private val cryptoPortfolioConverterFactory: CryptoPortfolioConverter.Factory = mockk {
        every { create(userWallet) } returns rowConverter
    }

    private val listConverter = AccountListConverter(
        userWallet = userWallet,
        cryptoPortfolioConverterFactory = cryptoPortfolioConverterFactory,
    )

    @ParameterizedTest
    @ProvideTestModels
    fun routeByType(model: RoutingModel) {
        // Act
        // The main crypto account is always there — a joint account is an extra row of the same document
        val actual = listConverter.convert(value = response(secondRowType = model.type))

        // Assert
        val secondAccount = actual.accounts.first { it.accountId.value == SECOND_ROW_ID }
        assertThat(secondAccount).isInstanceOf(model.expected)
    }

    private fun provideTestModels() = listOf(
        RoutingModel(type = WalletAccountDTO.Type.JOINT, expected = Account.Joint::class.java),
        RoutingModel(type = WalletAccountDTO.Type.CRYPTO, expected = Account.CryptoPortfolio::class.java),
        // The field predates joint accounts, so its absence means crypto
        RoutingModel(type = null, expected = Account.CryptoPortfolio::class.java),
    )

    @Test
    fun `GIVEN account list with a joint account WHEN convert back THEN the joint row keeps its type`() {
        // Arrange
        val accountList = AccountList(
            userWalletId = WALLET_ID,
            accounts = listOf(cryptoAccount(), jointAccount()),
            totalAccounts = 2,
            totalArchivedAccounts = 0,
            totalJointAccounts = 1,
        ).getOrNull()!!

        // Act
        val actual = SaveWalletAccountsResponseConverter.convert(value = accountList)

        // Assert
        // A row missing from the document means "archived" to the backend, so the joint row must survive the
        // round trip — otherwise editing the portfolio would archive the joint account
        assertThat(actual.accounts.map { it.type })
            .containsExactly(WalletAccountDTO.Type.CRYPTO, WalletAccountDTO.Type.JOINT)
        assertThat(actual.accounts.map { it.id })
            .containsExactly(cryptoAccount().accountId.value, jointAccount().accountId.value)
    }

    private fun response(secondRowType: WalletAccountDTO.Type?): GetWalletAccountsResponse {
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = UserTokensResponse.GroupType.NONE,
                sort = UserTokensResponse.SortType.MANUAL,
                totalAccounts = 2,
                totalArchivedAccounts = 0,
            ),
            accounts = listOf(
                WalletAccountDTO(
                    id = cryptoAccount().accountId.value,
                    name = null,
                    derivationIndex = 0,
                    icon = CryptoPortfolioIcon.Icon.Star.name,
                    iconColor = CryptoPortfolioIcon.Color.Azure.name,
                    tokens = emptyList(),
                ),
                WalletAccountDTO(
                    id = SECOND_ROW_ID,
                    name = "Family",
                    derivationIndex = 1,
                    icon = CryptoPortfolioIcon.Icon.Star.name,
                    iconColor = CryptoPortfolioIcon.Color.Azure.name,
                    type = secondRowType,
                    tokens = emptyList(),
                ),
            ),
            unassignedTokens = emptyList(),
        )
    }

    private fun cryptoAccount(): Account.Personal {
        return Account.Personal.createMainAccount(userWalletId = WALLET_ID)
    }

    private fun jointAccount(): Account.Joint {
        return Account.Joint(
            accountId = AccountId.forJointAccount(userWalletId = WALLET_ID, value = SECOND_ROW_ID).getOrNull()!!,
            accountName = AccountName.Custom(value = "Family").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofCustomAccount(
                value = CryptoPortfolioIcon.Icon.Star,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            ownerKeyIndex = OwnerKeyIndex(value = 1).getOrNull()!!,
            cryptoCurrencies = emptyList(),
        )
    }

    internal data class RoutingModel(val type: WalletAccountDTO.Type?, val expected: Class<out Account>)

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val SECOND_ROW_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
    }
}