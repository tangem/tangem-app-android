package com.tangem.data.account.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
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
 * document as the crypto accounts, distinguished only by that field.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountRowRoutingTest {

    private val userWallet: UserWallet = mockk {
        every { walletId } returns WALLET_ID
    }
    private val cryptoPortfolioConverter: CryptoPortfolioConverter = mockk()
    private val jointAccountConverter: JointAccountConverter = mockk()

    // Stubbed where they are declared: the converter resolves both factories in its constructor
    private val cryptoPortfolioConverterFactory: CryptoPortfolioConverter.Factory = mockk {
        every { create(userWallet) } returns cryptoPortfolioConverter
    }
    private val jointAccountConverterFactory: JointAccountConverter.Factory = mockk {
        every { create(userWallet) } returns jointAccountConverter
    }

    private val listConverter = AccountListConverter(
        userWallet = userWallet,
        cryptoPortfolioConverterFactory = cryptoPortfolioConverterFactory,
        jointAccountConverterFactory = jointAccountConverterFactory,
    )

    init {
        every { jointAccountConverter.convert(any()) } returns jointAccount()
        every { cryptoPortfolioConverter.convert(any()) } answers {
            val dto = firstArg<WalletAccountDTO>()

            if (dto.id == SECOND_ROW_ID) secondCryptoAccount() else cryptoAccount()
        }
    }

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
        RoutingModel(type = WalletAccountDTO.Type.JOINT.value, expected = Account.Joint::class.java),
        RoutingModel(type = WalletAccountDTO.Type.CRYPTO.value, expected = Account.CryptoPortfolio::class.java),
        // The field predates joint accounts, so its absence means crypto
        RoutingModel(type = null, expected = Account.CryptoPortfolio::class.java),
        // A type this build does not know must render as crypto rather than fail the whole account list
        RoutingModel(type = "shared_v2", expected = Account.CryptoPortfolio::class.java),
    )

    @Test
    fun `GIVEN a row of an unknown type without tokens WHEN convert THEN the account list still renders`() {
        // Arrange
        val response = response(secondRowType = "shared_v2").let { value ->
            value.copy(accounts = value.accounts.map { it.copy(tokens = null) })
        }

        // Act
        val actual = listConverter.convert(value = response)

        // Assert
        // The crypto converter refuses a row without tokens, and the producer turns that refusal into an endless
        // retry — a value the backend adds later must not cost the user the whole account list
        assertThat(actual.accounts.map { it.accountId.value })
            .containsExactly(cryptoAccount().accountId.value, SECOND_ROW_ID)
    }

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
            .containsExactly(WalletAccountDTO.Type.CRYPTO.value, WalletAccountDTO.Type.JOINT.value)
        assertThat(actual.accounts.map { it.id })
            .containsExactly(cryptoAccount().accountId.value, jointAccount().accountId.value)
    }

    private fun response(secondRowType: String?): GetWalletAccountsResponse {
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

    private fun secondCryptoAccount(): Account.Personal {
        return Account.Personal(
            accountId = AccountId.forCryptoPortfolio(userWalletId = WALLET_ID, value = SECOND_ROW_ID).getOrNull()!!,
            accountName = AccountName.Custom(value = "Second").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofCustomAccount(
                value = CryptoPortfolioIcon.Icon.Star,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            derivationIndex = DerivationIndex(value = 1).getOrNull()!!,
        )
    }

    private fun jointAccount(): Account.Joint {
        return Account.Joint(
            accountId = AccountId.forJointAccount(userWalletId = WALLET_ID, value = SECOND_ROW_ID).getOrNull()!!,
            accountName = AccountName.Custom(value = "Family").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofCustomAccount(
                value = CryptoPortfolioIcon.Icon.Star,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            derivationIndex = DerivationIndex(value = 1).getOrNull()!!,
            cryptoCurrencies = emptyList(),
        )
    }

    internal data class RoutingModel(val type: String?, val expected: Class<out Account>)

    private companion object {
        val WALLET_ID = UserWalletId("011")
        const val SECOND_ROW_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
    }
}