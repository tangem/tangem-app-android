package com.tangem.features.account.details

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
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
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
        val expected = ActivationUM(
            isManageTokensAvailable = false,
            isPendingActivation = true,
            membersCount = testModel.expectedMembersCount,
        )
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
            val expected = ActivationUM(
                isManageTokensAvailable = true,
                isPendingActivation = false,
                membersCount = activeMembersCount,
            )
            assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

            model.onDestroy()
        }

    @Test
    fun `GIVEN joint account WHEN status is active and wallet is single-currency THEN manage tokens locked`() =
        runTest {
            // Arrange
            val account = MockAccounts.createJointAccount()
            every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
            givenWallet(isMultiCurrency = false)

            // Act
            val model = createModel(account = account, testScope = this)
            advanceUntilIdle()

            // Assert
            val expected = ActivationUM(
                isManageTokensAvailable = false,
                isPendingActivation = false,
                membersCount = activeMembersCount,
            )
            assertThat(model.uiState.value.toActivationUM()).isEqualTo(expected)

            model.onDestroy()
        }

    @Test
    fun `GIVEN joint account WHEN members row clicked THEN router pushes JointAccountMembers`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        val status = AccountStatus.Joint(
            account = account,
            value = JointAccountStatusValue.Pending(source = StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 2),
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        )
        every { singleAccountStatusSupplier(any()) } returns flowOf(status)
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()
        model.uiState.value.members?.onClick?.invoke()

        // Assert
        verify {
            router.push(
                AppRoute.JointAccountMembers(
                    userWalletId = account.accountId.userWalletId,
                    cryptoAccountId = account.accountId.value,
                ),
            )
        }

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
    fun `GIVEN joint account WHEN model loads THEN archive mode is available`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.archiveMode).isInstanceOf(AccountDetailsUM.ArchiveMode.Available::class.java)

        model.onDestroy()
    }

    @Test
    fun `GIVEN joint account WHEN model loads THEN title is joint account`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.title).isEqualTo(resourceReference(R.string.common_joint_account))

        model.onDestroy()
    }

    @Test
    fun `GIVEN joint account WHEN status is active THEN networks info is shown`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
        givenWallet(isMultiCurrency = true)

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.networksInfo).isNotNull()

        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN joint account WHEN status is not active THEN networks info is hidden`(
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
        assertThat(model.uiState.value.networksInfo).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN joint account WHEN archive confirmed THEN account archived and screen closed`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
        givenWallet(isMultiCurrency = true)
        coEvery { archiveCryptoPortfolioUseCase(account.accountId) } returns Unit.right()
        val dialogSlot = slot<DialogMessage>()
        every { messageSender.send(capture(dialogSlot)) } returns Unit

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()
        val archiveMode = model.uiState.value.archiveMode as AccountDetailsUM.ArchiveMode.Available
        archiveMode.onArchiveAccountClick()

        // Assert
        assertThat(dialogSlot.captured.message)
            .isEqualTo(resourceReference(R.string.account_details_archive_joint_description))

        // Act
        dialogSlot.captured.firstAction.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { archiveCryptoPortfolioUseCase(account.accountId) }
        verify { router.pop() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN joint account WHEN archive fails THEN generic error dialog shown`() = runTest {
        // Arrange
        val account = MockAccounts.createJointAccount()
        every { singleAccountStatusSupplier(any()) } returns flowOf(activeStatus(account))
        givenWallet(isMultiCurrency = true)
        coEvery { archiveCryptoPortfolioUseCase(account.accountId) } returns
            ArchiveCryptoPortfolioUseCase.Error.DataOperationFailed(cause = IllegalStateException()).left()
        val dialogSlot = slot<DialogMessage>()
        every { messageSender.send(capture(dialogSlot)) } returns Unit

        // Act
        val model = createModel(account = account, testScope = this)
        advanceUntilIdle()
        val archiveMode = model.uiState.value.archiveMode as AccountDetailsUM.ArchiveMode.Available
        archiveMode.onArchiveAccountClick()
        dialogSlot.captured.firstAction.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(dialogSlot.captured.title).isEqualTo(resourceReference(R.string.common_something_went_wrong))
        assertThat((model.uiState.value.archiveMode as AccountDetailsUM.ArchiveMode.Available).isLoading).isFalse()
        verify(exactly = 0) { router.pop() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN personal account WHEN model loads THEN activation is not pending`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val status = AccountStatus.Personal(
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

    @Test
    fun `GIVEN personal account WHEN model loads THEN title is account details title`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1)
        val status = AccountStatus.Personal(
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
        assertThat(model.uiState.value.title).isEqualTo(resourceReference(R.string.account_details_title))

        model.onDestroy()
    }

    private fun activeStatus(account: Account.Joint): AccountStatus.Joint {
        return AccountStatus.Joint(
            account = account,
            value = JointAccountStatusValue.Active(
                source = StatusSource.ACTUAL,
                totalMembers = 2,
                safeAddress = "0x5afe"
            ),
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

    private fun AccountDetailsUM.toActivationUM(): ActivationUM {
        return ActivationUM(
            isManageTokensAvailable = isManageTokensAvailable,
            isPendingActivation = isPendingActivation,
            membersCount = members?.membersCount,
        )
    }

    private val activeMembersCount: TextReference = pluralReference(
        id = R.plurals.account_details_joint_members_count,
        count = 2,
        formatArgs = wrappedList(2),
    )

    internal data class ActivationUM(
        val isManageTokensAvailable: Boolean,
        val isPendingActivation: Boolean,
        val membersCount: TextReference? = null,
    )

    internal data class InactiveJointModel(
        val statusValue: JointAccountStatusValue,
        val expectedMembersCount: TextReference?,
    )

    private fun provideTestModels() = listOf(
        InactiveJointModel(statusValue = JointAccountStatusValue.Loading, expectedMembersCount = null),
        InactiveJointModel(
            statusValue = JointAccountStatusValue.Pending(
                source = StatusSource.ACTUAL,
                joinedMembers = 1,
                totalMembers = 2
            ),
            expectedMembersCount = resourceReference(
                id = R.string.account_details_joint_members_progress,
                formatArgs = wrappedList(1, 2),
            ),
        ),
        InactiveJointModel(
            statusValue = JointAccountStatusValue.Confirming(source = StatusSource.ACTUAL, totalMembers = 2),
            expectedMembersCount = pluralReference(
                id = R.plurals.account_details_joint_members_count,
                count = 2,
                formatArgs = wrappedList(2),
            ),
        ),
        InactiveJointModel(statusValue = JointAccountStatusValue.Error.Unavailable, expectedMembersCount = null),
    )
}