package com.tangem.features.account.details

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import com.tangem.domain.account.status.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.details.entity.AccountDetailsUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import com.tangem.domain.core.utils.lceLoading

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountDetailsModelTest {

    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val archiveCryptoPortfolioUseCase: ArchiveCryptoPortfolioUseCase = mockk()
    private val singleAccountSupplier: SingleAccountSupplier = mockk()
    private val singleAccountStatusSupplier: SingleAccountStatusSupplier = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            messageSender,
            router,
            archiveCryptoPortfolioUseCase,
            singleAccountSupplier,
            singleAccountStatusSupplier,
            analyticsEventHandler,
            getUserWalletUseCase,
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN joint account WHEN status is not active THEN manage tokens locked and activation pending`(
        testModel: InactiveJointModel,
    ) = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        val status = AccountStatus.Joint(
            account = account,
            value = testModel.statusValue,
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        )
        every { singleAccountStatusSupplier(any()) } returns flowOf(status)
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        val expected = ActivationUM(isManageTokensAvailable = false, isPendingActivation = true)
        assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

        model.onDestroy()
    }

    @Test
    fun `GIVEN joint account WHEN status is active and wallet is multi-currency THEN manage tokens available`() =
        runTest {
            // Arrange
            val account = MockAccounts.createJointAccount()
            every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
            givenWallet(isMultiCurrency = true)

            // Act
            val model = createModel(account = account, testScope = this)
            advanceUntilIdle()

            // Assert
            val expected = ActivationUM(isManageTokensAvailable = true, isPendingActivation = false)
            assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

            model.onDestroy()
        }

    @Test
    fun `GIVEN joint account WHEN status is active and wallet is single-currency THEN manage tokens locked`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
        givenWallet(isMultiCurrency = false)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        val expected = ActivationUM(isManageTokensAvailable = false, isPendingActivation = false)
        assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

        model.onDestroy()
    }

    @Test
    fun `GIVEN joint account WHEN status has not been emitted yet THEN activation is pending`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns emptyFlow()
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        val expected = ActivationUM(isManageTokensAvailable = false, isPendingActivation = true)
        assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

        model.onDestroy()
    }

    @Test
    fun `GIVEN personal account WHEN model loads THEN activation is not pending`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val status = AccountStatus.CryptoPortfolio(
            account = account,
            tokenList = TokenList.Empty,
            priceChangeLce = PriceChange(value = BigDecimal.ZERO, source = StatusSource.ACTUAL).lceContent(),
        )
        every { singleAccountSupplier.filterPersonalAccount(any()) } returns flowOf(account)
        every { singleAccountStatusSupplier(any()) } returns flowOf(status)
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        val expected = ActivationUM(isManageTokensAvailable = true, isPendingActivation = false)
        assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

        model.onDestroy()
    }

    private fun activeStatus(account: Account.Joint): AccountStatus.Joint {
        return AccountStatus.Joint(
            account = account,
            value = JointAccountStatusValue.Active(source = StatusSource.ACTUAL, totalMembers = 2, safeAddress = "0x5afe"),
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        )
    }

    private fun givenWallet(isMultiCurrency: Boolean) {
        val userWallet: UserWallet = MockUserWalletFactory.create().copy(isMultiCurrency = isMultiCurrency)

        every { getUserWalletUseCase(any()) } returns userWallet.right()
    }

    private fun createModel(account: Account.CryptoPortfolio, testScope: TestScope): AccountDetailsModel {
        return AccountDetailsModel(
            paramsContainer = MutableParamsContainer(value = AccountDetailsComponent.Params(account = account)),
            messageSender = messageSender,
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            archiveCryptoPortfolioUseCase = archiveCryptoPortfolioUseCase,
            singleAccountSupplier = singleAccountSupplier,
            singleAccountStatusSupplier = singleAccountStatusSupplier,
            analyticsEventHandler = analyticsEventHandler,
            getUserWalletUseCase = getUserWalletUseCase,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun AccountDetailsUM.toActivationUM(): ActivationUM {
        return ActivationUM(
            isManageTokensAvailable = isManageTokensAvailable,
            isPendingActivation = isPendingActivation,
        )
    }

    internal data class ActivationUM(val isManageTokensAvailable: Boolean, val isPendingActivation: Boolean)

    internal data class InactiveJointModel(val statusValue: JointAccountStatusValue)

    private fun provideTestModels() = listOf(
        InactiveJointModel(statusValue = JointAccountStatusValue.Loading),
        InactiveJointModel(statusValue = JointAccountStatusValue.Pending(source = StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 2)),
        InactiveJointModel(statusValue = JointAccountStatusValue.Confirming(source = StatusSource.ACTUAL, totalMembers = 2)),
        InactiveJointModel(statusValue = JointAccountStatusValue.Error.Unavailable),
    )
}