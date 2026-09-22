package com.tangem.data.account.producer

import arrow.core.getOrElse
import com.google.common.truth.Truth
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultSingleAccountListProducerTest {

    private val walletAccountListFlowFactory: WalletAccountListFlowFactory = mockk()

    private val userWalletId = UserWalletId("011")
    private val flowProducerTools: FlowProducerTools = mockk()
    private val userWallet = mockk<UserWallet.Hot> {
        every { walletId } returns userWalletId
        every { hotWalletId } returns mockk {
            every { authType } returns HotWalletId.AuthType.NoPassword
        }
    }
    private val userWalletsListRepository = mockk<UserWalletsListRepository> {
        every { userWallets } returns MutableStateFlow<List<UserWallet>?>(value = listOf(userWallet))
    }
    private val moneyAccountsFeatureToggles = mockk<MoneyAccountFeatureToggles> {
        every { isMoneyAccountsEnabled } returns false
    }
    private val polymarketFeatureToggles = mockk<PolymarketFeatureToggles> {
        every { isPolymarketEnabled } returns false
    }
    private val polymarketOnboardedStore = mockk<PolymarketOnboardedStore> {
        every { isOnboarded(any()) } returns flowOf(false)
    }
    private val collectiblesFeatureToggles = mockk<CollectiblesFeatureToggles> {
        every { isCollectiblesEnabled } returns false
    }
    private val collectiblesAccountRepository = mockk<CollectiblesAccountRepository> {
        every { getStatus(any()) } returns flowOf(null)
    }
    private val onboardedStatus = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)

    private val producer = DefaultSingleAccountListProducer(
        params = SingleAccountListProducer.Params(userWalletId = userWalletId),
        walletAccountListFlowFactory = walletAccountListFlowFactory,
        flowProducerTools = flowProducerTools,
        userWalletsListRepository = userWalletsListRepository,
        moneyAccountsFeatureToggles = moneyAccountsFeatureToggles,
        polymarketFeatureToggles = polymarketFeatureToggles,
        polymarketOnboardedStore = polymarketOnboardedStore,
        collectiblesFeatureToggles = collectiblesFeatureToggles,
        collectiblesAccountRepository = collectiblesAccountRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @AfterEach
    fun tearDownEach() {
        clearMocks(walletAccountListFlowFactory)
    }

    @Test
    fun produce() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = accountList.withPaymentAccount()
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            walletAccountListFlowFactory.create(userWalletId)
        }
    }

    @Test
    fun `GIVEN money accounts enabled WHEN produce THEN account list contains money account`() = runTest {
        // Arrange
        val supportedWallet = mockk<UserWallet.Hot> {
            every { walletId } returns userWalletId
            every { hotWalletId } returns mockk {
                every { authType } returns HotWalletId.AuthType.Password
            }
        }
        val userWalletsListRepository = mockk<UserWalletsListRepository> {
            every { userWallets } returns MutableStateFlow<List<UserWallet>?>(value = listOf(supportedWallet))
        }
        val moneyAccountsFeatureToggles = mockk<MoneyAccountFeatureToggles> {
            every { isMoneyAccountsEnabled } returns true
        }
        val producer = DefaultSingleAccountListProducer(
            params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            walletAccountListFlowFactory = walletAccountListFlowFactory,
            flowProducerTools = flowProducerTools,
            userWalletsListRepository = userWalletsListRepository,
            moneyAccountsFeatureToggles = moneyAccountsFeatureToggles,
            polymarketFeatureToggles = polymarketFeatureToggles,
            polymarketOnboardedStore = polymarketOnboardedStore,
            collectiblesFeatureToggles = collectiblesFeatureToggles,
            collectiblesAccountRepository = collectiblesAccountRepository,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = accountList
            .plus(Account.Payment(userWalletId))
            .getOrElse { error("Unable to add payment account: $it") }
            .plus(Account.Money(userWalletId))
            .getOrElse { error("Unable to add money account: $it") }
        Truth.assertThat(actual).containsExactly(expected)
    }

    @Test
    fun `GIVEN polymarket enabled WHEN produce THEN account list contains prediction account`() = runTest {
        // Arrange
        val producer = createProducer(isPolymarketEnabled = true, onboardedFlow = flowOf(true))

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).containsExactly(accountList.withPaymentAccount().withPredictionAccount())
    }

    @Test
    fun `GIVEN polymarket disabled WHEN produce THEN account list has no prediction account`() = runTest {
        // Arrange
        val producer = createProducer(isPolymarketEnabled = false)

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).containsExactly(accountList.withPaymentAccount())
    }

    @Test
    fun `GIVEN single-currency wallet WHEN produce THEN account list has no prediction account`() = runTest {
        // Arrange
        val singleCurrencyWallet = MockUserWalletFactory.createSingleWalletWithToken()
        val producer = createProducer(
            isPolymarketEnabled = true,
            onboardedFlow = flowOf(true),
            walletsRepository = mockk {
                every { userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(singleCurrencyWallet))
            },
        )

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual.single().accounts.filterIsInstance<Account.Prediction>()).isEmpty()
    }

    @Test
    fun `flow will updated if factoryFlow is updated`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = AccountList.empty(userWalletId = userWalletId, sortType = TokensSortType.NONE)
        val factoryFlow = MutableStateFlow<AccountList?>(null)

        every { walletAccountListFlowFactory.create(userWalletId) } returns factoryFlow.filterNotNull()

        // Act (first emission)
        factoryFlow.value = accountList
        val firstEmission = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        Truth.assertThat(firstEmission).containsExactly(accountList.withPaymentAccount())

        // Act (second emission)
        factoryFlow.value = updatedAccountList
        val secondEmission = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(secondEmission).containsExactly(updatedAccountList.withPaymentAccount())

        coVerifyOrder {
            walletAccountListFlowFactory.create(userWalletId)
            walletAccountListFlowFactory.create(userWalletId)
        }
    }

    @Test
    fun `flow is filtered the same response`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        val factoryFlow = MutableStateFlow<AccountList?>(null)

        every { walletAccountListFlowFactory.create(userWalletId) } returns factoryFlow.filterNotNull()

        // Act (first emission)
        factoryFlow.value = accountList
        val firstEmission = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        Truth.assertThat(firstEmission).containsExactly(accountList.withPaymentAccount())

        // Act (second emission) - the same status
        factoryFlow.value = accountList
        val secondEmission = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(secondEmission).containsExactly(accountList.withPaymentAccount())

        coVerify(ordering = Ordering.SEQUENCE) {
            walletAccountListFlowFactory.create(userWalletId)
            walletAccountListFlowFactory.create(userWalletId)
        }
    }

    @Test
    fun `GIVEN nothing recorded WHEN produce THEN account list has no prediction account`() = runTest {
        // Arrange
        val producer = createProducer(isPolymarketEnabled = true, onboardedFlow = flowOf(false))

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).containsExactly(accountList.withPaymentAccount())
    }

    @Test
    fun `GIVEN nothing recorded WHEN the record appears THEN account list re-emits with the account`() = runTest {
        // Arrange
        val record = MutableStateFlow(value = false)
        val producer = createProducer(isPolymarketEnabled = true, onboardedFlow = record)

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act & Assert
        producer.produce().test {
            Truth.assertThat(awaitItem()).isEqualTo(accountList.withPaymentAccount())

            record.value = true

            Truth.assertThat(awaitItem()).isEqualTo(accountList.withPaymentAccount().withPredictionAccount())
        }
    }

    @Test
    fun `GIVEN polymarket disabled WHEN produce THEN the onboarding record is never read`() = runTest {
        // Arrange
        val onboardedStore = mockk<PolymarketOnboardedStore>()
        val producer = createProducer(isPolymarketEnabled = false, onboardedStore = onboardedStore)

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        producer.produce().let(::getEmittedValues)

        // Assert
        verify(exactly = 0) { onboardedStore.isOnboarded(any()) }
    }

    @Test
    fun `GIVEN collectibles disabled WHEN produce THEN the account status is never read`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)
        val accountRepository = mockk<CollectiblesAccountRepository> {
            every { getStatus(any()) } returns flowOf(onboardedStatus)
        }
        val producer = createCollectiblesProducer(
            isCollectiblesEnabled = false,
            statusFlow = flowOf(onboardedStatus),
            accountRepository = accountRepository,
        )

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).containsExactly(accountList.withPaymentAccount())
        verify(exactly = 0) { accountRepository.getStatus(any()) }
    }

    @Test
    fun `GIVEN collectibles enabled but not onboarded WHEN produce THEN there is no collectibles account`() =
        runTest {
            // Arrange
            val accountList = AccountList.empty(userWalletId)
            every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)
            val producer = createCollectiblesProducer(isCollectiblesEnabled = true, statusFlow = flowOf(null))

            // Act
            val actual = producer.produce().let(::getEmittedValues)

            // Assert
            Truth.assertThat(actual).containsExactly(accountList.withPaymentAccount())
        }

    @Test
    fun `GIVEN an onboarded wallet WHEN produce THEN account list has exactly one collectibles account`() = runTest {
        // Arrange
        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)
        val producer = createCollectiblesProducer(isCollectiblesEnabled = true, statusFlow = flowOf(onboardedStatus))

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).containsExactly(accountList.withPaymentAccount().withCollectiblesAccount())
    }

    @Test
    fun `GIVEN onboarding happens WHEN produce THEN the collectibles account appears without resubscribing`() =
        runTest {
            // Arrange
            val accountList = AccountList.empty(userWalletId)
            every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)
            val onboarded = MutableStateFlow<CollectiblesAccountStatusValue?>(value = null)
            val producer = createCollectiblesProducer(isCollectiblesEnabled = true, statusFlow = onboarded)

            // Act & Assert
            producer.produce().test {
                Truth.assertThat(awaitItem()).isEqualTo(accountList.withPaymentAccount())

                onboarded.value = onboardedStatus

                Truth.assertThat(awaitItem())
                    .isEqualTo(accountList.withPaymentAccount().withCollectiblesAccount())
            }
        }

    private fun AccountList.withPaymentAccount(): AccountList = plus(Account.Payment(userWalletId))
        .getOrElse { error("Unable to add payment account: $it") }

    private fun AccountList.withPredictionAccount(): AccountList = plus(Account.Prediction(userWalletId))
        .getOrElse { error("Unable to add prediction account: $it") }

    private fun AccountList.withCollectiblesAccount(): AccountList = plus(Account.Collectibles(userWalletId))
        .getOrElse { error("Unable to add collectibles account: $it") }

    private fun createCollectiblesProducer(
        isCollectiblesEnabled: Boolean,
        statusFlow: Flow<CollectiblesAccountStatusValue?>,
        accountRepository: CollectiblesAccountRepository = mockk {
            every { getStatus(any()) } returns statusFlow
        },
    ): DefaultSingleAccountListProducer {
        return DefaultSingleAccountListProducer(
            params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            walletAccountListFlowFactory = walletAccountListFlowFactory,
            flowProducerTools = flowProducerTools,
            userWalletsListRepository = userWalletsListRepository,
            moneyAccountsFeatureToggles = moneyAccountsFeatureToggles,
            polymarketFeatureToggles = polymarketFeatureToggles,
            polymarketOnboardedStore = polymarketOnboardedStore,
            collectiblesFeatureToggles = mockk {
                every { this@mockk.isCollectiblesEnabled } returns isCollectiblesEnabled
            },
            collectiblesAccountRepository = accountRepository,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    private fun createProducer(
        isPolymarketEnabled: Boolean,
        onboardedFlow: Flow<Boolean> = flowOf(false),
        onboardedStore: PolymarketOnboardedStore = mockk {
            every { isOnboarded(any()) } returns onboardedFlow
        },
        walletsRepository: UserWalletsListRepository = userWalletsListRepository,
    ): DefaultSingleAccountListProducer {
        return DefaultSingleAccountListProducer(
            params = SingleAccountListProducer.Params(userWalletId = userWalletId),
            walletAccountListFlowFactory = walletAccountListFlowFactory,
            flowProducerTools = flowProducerTools,
            userWalletsListRepository = walletsRepository,
            moneyAccountsFeatureToggles = moneyAccountsFeatureToggles,
            polymarketFeatureToggles = mockk {
                every { this@mockk.isPolymarketEnabled } returns isPolymarketEnabled
            },
            polymarketOnboardedStore = onboardedStore,
            collectiblesFeatureToggles = collectiblesFeatureToggles,
            collectiblesAccountRepository = collectiblesAccountRepository,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }
}