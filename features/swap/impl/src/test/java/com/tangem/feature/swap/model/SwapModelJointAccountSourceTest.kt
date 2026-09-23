package com.tangem.feature.swap.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.test.mock.MockAccounts
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * A joint-account token cannot fund a swap: picking one via the FROM chooser must alert and leave the FROM
 * slot untouched, while the same token via the TO chooser must proceed normally.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelJointAccountSourceTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns Either.Right(null)
        coEvery {
            swapInteractor.getPair(fromSwapCurrencyStatus = any(), toSwapCurrencyStatus = any(), filterProviderTypes = any())
        } returns Either.Left(ExpressError.UnknownError)
    }

    @Test
    fun `GIVEN joint account chosen as FROM WHEN onCurrencyChosen fires THEN alert is shown and FROM stays unset`() =
        runTest {
            // Arrange
            val model = createModel()
            val result = jointChooseTokenResult()

            // Act
            model.chooseFromTokenBridge.onCurrencyChosen.trySend(result)
            advanceUntilIdle()

            // Assert
            assertThat(model.dataState.fromSwapCurrencyStatus).isNull()
            verify(exactly = 1) { messageSender.send(match { it is DialogMessage }) }
            verify(exactly = 0) { router.pop() }
        }

    @Test
    fun `GIVEN joint account chosen as TO WHEN onCurrencyChosen fires THEN it is placed as TO normally`() = runTest {
        // Arrange
        val model = createModel()
        val result = jointChooseTokenResult()

        // Act
        model.chooseToTokenBridge.onCurrencyChosen.trySend(result)
        advanceUntilIdle()

        // Assert
        assertThat(model.dataState.toSwapCurrencyStatus?.account).isInstanceOf(Account.Joint::class.java)
        verify(exactly = 1) { router.pop() }
    }

    private fun jointChooseTokenResult(): ChooseTokenResult {
        val accountStatus: AccountStatus = AccountStatus.Joint(
            account = MockAccounts.createJointAccount(userWalletId = userWalletId),
            value = JointAccountStatusValue.Active(
                source = StatusSource.ACTUAL,
                totalMembers = 2,
                safeAddress = "0x5afe000000000000000000000000000000000002",
            ),
            tokenList = TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
                sortedBy = TokensSortType.NONE,
                currencies = emptyList(),
            ),
            priceChangeLce = Lce.Content(PriceChange(value = BigDecimal.ZERO, source = StatusSource.ACTUAL)),
        )
        return ChooseTokenResult(
            currency = mockk<CryptoCurrencyStatus>(relaxed = true),
            account = accountStatus,
            wallet = mockk<UserWallet>(relaxed = true),
        )
    }
}