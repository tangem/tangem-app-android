package com.tangem.features.feed.components.v2.blocks.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.features.feed.components.v2.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayBlockModelTest {

    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val walletAId = UserWalletId("aa")
    private val walletBId = UserWalletId("bb")

    private lateinit var accountLists: MutableStateFlow<List<AccountList>>

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiAccountListSupplier, paymentAccountStatusSupplier)
        accountLists = MutableStateFlow(emptyList())
        every { multiAccountListSupplier() } returns accountLists
        every { paymentAccountStatusSupplier(any<UserWalletId>()) } returns emptyFlow<AccountStatus.Payment>()
    }

    @Test
    fun `GIVEN no wallet carries a payment account WHEN the model is created THEN the pitch is hidden`() = runTest {
        // Arrange
        accountLists.value = listOf(accountList(walletAId, mockk<Account.Personal>()))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no wallet carries a payment account WHEN the model is created THEN no status is requested`() = runTest {
        // Arrange
        accountLists.value = listOf(accountList(walletAId, mockk<Account.Personal>()))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { paymentAccountStatusSupplier(any<UserWalletId>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a wallet with a created spending account WHEN the model is created THEN the pitch is hidden`() =
        runTest {
            // Arrange
            accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
            every { paymentAccountStatusSupplier(walletAId) } returns
                MutableStateFlow(paymentStatus(mockk<PaymentAccountStatusValue.Loaded>()))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a payment account still loading WHEN the model is created THEN the pitch stays hidden`() = runTest {
        // Arrange — a known account must never read as absent while its status refreshes
        accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
        every { paymentAccountStatusSupplier(walletAId) } returns
            MutableStateFlow(paymentStatus(PaymentAccountStatusValue.Loading))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN Tangem Pay is unavailable for the wallet WHEN the model is created THEN the pitch is hidden`() =
        runTest {
            // Arrange
            accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
            every { paymentAccountStatusSupplier(walletAId) } returns
                MutableStateFlow(paymentStatus(PaymentAccountStatusValue.Empty))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN no status has arrived yet WHEN the model is created THEN the pitch is hidden`() = runTest {
        // Arrange
        accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a wallet whose account was never created WHEN the model is created THEN the pitch is shown`() =
        runTest {
            // Arrange
            accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
            every { paymentAccountStatusSupplier(walletAId) } returns
                MutableStateFlow(paymentStatus(PaymentAccountStatusValue.NotCreated))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a wallet whose customer is still in KYC WHEN the model is created THEN the pitch is shown`() =
        runTest {
            // Arrange
            accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
            every { paymentAccountStatusSupplier(walletAId) } returns
                MutableStateFlow(paymentStatus(mockk<PaymentAccountStatusValue.UnderReview>()))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN one of several wallets has a spending account WHEN the model is created THEN the pitch is hidden`() =
        runTest {
            // Arrange
            accountLists.value = listOf(
                accountList(walletAId, mockk<Account.Payment>()),
                accountList(walletBId, mockk<Account.Payment>()),
            )
            every { paymentAccountStatusSupplier(walletAId) } returns
                MutableStateFlow(paymentStatus(PaymentAccountStatusValue.NotCreated))
            every { paymentAccountStatusSupplier(walletBId) } returns
                MutableStateFlow(paymentStatus(mockk<PaymentAccountStatusValue.Deactivated>()))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.isVisible.value).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a status arrives late WHEN it reports an account THEN the pitch disappears`() = runTest {
        // Arrange
        val status = MutableStateFlow(paymentStatus(PaymentAccountStatusValue.NotCreated))
        accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
        every { paymentAccountStatusSupplier(walletAId) } returns status
        val model = createModel(testScope = this)
        advanceUntilIdle()
        assertThat(model.isVisible.value).isTrue()

        // Act
        status.value = paymentStatus(mockk<PaymentAccountStatusValue.IssuingCard>())
        advanceUntilIdle()

        // Assert
        assertThat(model.isVisible.value).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no wallet carries a payment account WHEN the model is created THEN the pitch leads nowhere`() =
        runTest {
            // Arrange
            accountLists.value = listOf(accountList(walletAId, mockk<Account.Personal>()))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.onboardingMode.value).isNull()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a wallet whose account was never created WHEN the pitch is shown THEN it opens the activation`() =
        runTest {
            // Arrange
            accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
            every { paymentAccountStatusSupplier(walletAId) } returns
                MutableStateFlow(paymentStatus(PaymentAccountStatusValue.NotCreated))

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.onboardingMode.value)
                .isEqualTo(AppRoute.TangemPayOnboarding.Mode.FromBannerOnMain)
            model.onDestroy()
        }

    @Test
    fun `GIVEN a wallet already in KYC WHEN the pitch is shown THEN it resumes that wallet's onboarding`() = runTest {
        // Arrange
        accountLists.value = listOf(accountList(walletAId, mockk<Account.Payment>()))
        every { paymentAccountStatusSupplier(walletAId) } returns
            MutableStateFlow(paymentStatus(mockk<PaymentAccountStatusValue.UnderReview>()))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.onboardingMode.value)
            .isEqualTo(AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding(userWalletId = walletAId))
        model.onDestroy()
    }

    @Test
    fun `GIVEN one of two wallets is in KYC WHEN the pitch is shown THEN it resumes the one in KYC`() = runTest {
        // Arrange
        accountLists.value = listOf(
            accountList(walletAId, mockk<Account.Payment>()),
            accountList(walletBId, mockk<Account.Payment>()),
        )
        every { paymentAccountStatusSupplier(walletAId) } returns
            MutableStateFlow(paymentStatus(PaymentAccountStatusValue.NotCreated))
        every { paymentAccountStatusSupplier(walletBId) } returns
            MutableStateFlow(paymentStatus(mockk<PaymentAccountStatusValue.UnderReview>()))

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.onboardingMode.value)
            .isEqualTo(AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding(userWalletId = walletBId))
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): TangemPayBlockModel = TangemPayBlockModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        multiAccountListSupplier = multiAccountListSupplier,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )

    private fun accountList(userWalletId: UserWalletId, vararg accounts: Account): AccountList = mockk {
        every { this@mockk.userWalletId } returns userWalletId
        every { this@mockk.accounts } returns accounts.toList()
    }

    private fun paymentStatus(value: PaymentAccountStatusValue): AccountStatus.Payment = mockk {
        every { this@mockk.value } returns value
    }
}