package com.tangem.data.tokens.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.staking.*
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.MemoValidatorFacade
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.model.RentData
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Tests `getRentInfoWarning` — whether the user is warned that their account sits below the rent-exemption
 * threshold.
 *
 * Two reasons this is worth pinning. It reads `getTotalStakingBalance`, the staking scalar that **excludes
 * rewards** (the with-rewards twin backs portfolio totals instead); switching it would silently clear the warning
 * for users who are only above the threshold on paper. And it reads the staking balance through
 * `stakingBalanceData`, so it must reach the same verdict whether the balance arrives as a contribution or
 * through the legacy typed field.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCurrencyChecksRepositoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val memoValidatorFacade: MemoValidatorFacade = mockk()

    private val repository = DefaultCurrencyChecksRepository(
        walletManagersFacade = walletManagersFacade,
        memoValidatorFacade = memoValidatorFacade,
        coroutineDispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val solana by lazy { cryptoCurrencyFactory.createCoin(Blockchain.Solana) }

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, memoValidatorFacade)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun getRentInfoWarning(model: RentTestModel) = runTest {
        // Arrange
        coEvery { walletManagersFacade.getRentInfo(USER_WALLET_ID, any()) } returns model.rentData

        // Act
        val actual = repository.getRentInfoWarning(userWalletId = USER_WALLET_ID, currencyStatus = model.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN the same staked balance WHEN reached either way THEN the verdict is identical`() = runTest {
        // Arrange — a balance that lifts the user over the threshold, delivered through each architecture
        coEvery { walletManagersFacade.getRentInfo(USER_WALLET_ID, any()) } returns RENT
        val staked = stakeKit(BalanceType.STAKED to 5)

        // Act
        val viaContributions = repository.getRentInfoWarning(
            userWalletId = USER_WALLET_ID,
            currencyStatus = status(amount = 1, contributions = listOf(staked)),
        )
        val viaLegacyField = repository.getRentInfoWarning(
            userWalletId = USER_WALLET_ID,
            currencyStatus = status(amount = 1, stakingBalance = staked),
        )

        // Assert
        assertThat(viaContributions).isEqualTo(viaLegacyField)
        assertThat(viaContributions).isNull()
    }

    private fun provideTestModels() = listOf(
        RentTestModel(
            description = "below the exemption with nothing staked -> warns",
            status = status(amount = 1),
            expected = rentWarning(),
        ),
        RentTestModel(
            description = "below the exemption but staked above it -> the stake clears the threshold",
            status = status(amount = 1, contributions = listOf(stakeKit(BalanceType.STAKED to 5))),
            expected = null,
        ),
        RentTestModel(
            description = "rewards do NOT count toward the threshold -> still warns",
            // the scalar here excludes rewards on purpose; with the with-rewards scalar this would return null
            status = status(amount = 1, contributions = listOf(stakeKit(BalanceType.REWARDS to 5))),
            expected = rentWarning(),
        ),
        RentTestModel(
            description = "nothing at all -> no warning to give",
            status = status(amount = 0),
            expected = null,
        ),
        RentTestModel(
            description = "zero balance but a live stake -> no warning",
            status = status(amount = 0, contributions = listOf(stakeKit(BalanceType.STAKED to 5))),
            expected = null,
        ),
        RentTestModel(
            description = "exactly at the exemption -> no warning, the threshold is strict",
            // pins `<` against `<=`: at the boundary the account is already exempt
            status = status(amount = 2),
            expected = null,
        ),
        RentTestModel(
            description = "above the exemption -> no warning",
            status = status(amount = 100),
            expected = null,
        ),
        RentTestModel(
            description = "a stake mixing rewards and principal -> only the principal is weighed",
            // 1 + 5 staked clears the threshold; the 5 in rewards is not what got it there
            status = status(
                amount = 1,
                contributions = listOf(stakeKit(BalanceType.STAKED to 5, BalanceType.REWARDS to 5)),
            ),
            expected = null,
        ),
        RentTestModel(
            description = "the network charges no rent -> nothing to warn about",
            status = status(amount = 1),
            rentData = null,
            expected = null,
        ),
        RentTestModel(
            description = "a token, not a coin -> rent is a coin concern",
            status = CryptoCurrencyStatus(
                currency = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Solana),
                value = loadedValue(amount = 1),
            ),
            expected = null,
        ),
        RentTestModel(
            description = "the balance is not Loaded -> nothing to measure",
            status = CryptoCurrencyStatus(currency = solana, value = CryptoCurrencyStatus.Loading),
            expected = null,
        ),
    )

    internal data class RentTestModel(
        val description: String,
        val status: CryptoCurrencyStatus,
        val expected: CryptoCurrencyWarning.Rent?,
        val rentData: RentData? = RENT,
    ) {

        override fun toString(): String = description
    }

    // region Fixtures
    private fun status(
        amount: Int,
        contributions: List<BalanceContribution> = emptyList(),
        stakingBalance: StakingBalance? = null,
    ) = CryptoCurrencyStatus(
        currency = solana,
        value = loadedValue(amount = amount, contributions = contributions, stakingBalance = stakingBalance),
    )

    private fun loadedValue(
        amount: Int,
        contributions: List<BalanceContribution> = emptyList(),
        stakingBalance: StakingBalance? = null,
    ) = CryptoCurrencyStatus.Loaded(
        amount = BigDecimal(amount),
        fiatAmount = BigDecimal(amount),
        fiatRate = BigDecimal.ONE,
        priceChange = BigDecimal.ZERO,
        stakingBalance = stakingBalance,
        yieldSupplyStatus = null,
        hasCurrentNetworkTransactions = false,
        pendingTransactions = emptySet(),
        networkAddress = mockk(relaxed = true),
        sources = CryptoCurrencyStatus.Sources(),
        contributions = contributions,
    )

    private fun rentWarning() = CryptoCurrencyWarning.Rent(
        rent = RENT.rent,
        exemptionAmount = RENT.exemptionAmount,
        cryptoCurrency = solana,
    )

    /**
     * A real [StakingBalance.Data], never a mock: `totalDeltaCryptoAmount()` is a member function MockK
     * intercepts, so a relaxed mock would return a mocked `BigDecimal` and an assertion could pass while testing
     * nothing. [BalanceItem] is a plain carrier and is safe to mock.
     */
    private fun stakeKit(vararg items: Pair<BalanceType, Int>) = StakingBalance.Data.StakeKit(
        stakingId = StakingID(integrationId = INTEGRATION_ID, address = "0x1"),
        source = StatusSource.ACTUAL,
        balance = YieldBalanceItem(
            items = items.map { (type, amount) ->
                mockk<BalanceItem>(relaxed = true) {
                    every { this@mockk.amount } returns BigDecimal(amount)
                    every { this@mockk.type } returns type
                    every { token.coinGeckoId } returns solanaRawCurrencyId
                }
            },
            integrationId = INTEGRATION_ID,
        ),
    )

    private val solanaRawCurrencyId: String? get() = solana.id.rawCurrencyId?.value
    // endregion

    private companion object {

        val USER_WALLET_ID = UserWalletId(stringValue = "011")
        val RENT = RentData(rent = BigDecimal("0.002"), exemptionAmount = BigDecimal(2))
        const val INTEGRATION_ID = "solana-sol-native-multivalidator-staking"
    }
}