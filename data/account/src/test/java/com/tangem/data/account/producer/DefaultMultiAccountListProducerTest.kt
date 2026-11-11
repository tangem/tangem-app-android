package com.tangem.data.account.producer

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import app.cash.turbine.test
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMultiAccountListProducerTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxUnitFun = true)
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val flowProducerTools: FlowProducerTools = mockk()

    private val producer = DefaultMultiAccountListProducer(
        params = Unit,
        flowProducerTools = flowProducerTools,
        userWalletsListRepository = userWalletsListRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private fun TestScope.createProducer(): DefaultMultiAccountListProducer {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        return DefaultMultiAccountListProducer(
            params = Unit,
            flowProducerTools = TestFlowProducerTools(scope = backgroundScope, dispatcher = testDispatcher),
            userWalletsListRepository = userWalletsListRepository,
            singleAccountListSupplier = singleAccountListSupplier,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
        )
    }

    private val userWalletId = UserWalletId("011")
    private val userWallet = createUserWallet(id = userWalletId)

    private fun createUserWallet(id: UserWalletId, isLocked: Boolean = false): UserWallet = mockk<UserWallet.Cold> {
        every { this@mockk.walletId } returns id
        every { this@mockk.isLocked } returns isLocked
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(userWalletsListRepository, singleAccountListSupplier)
    }

    @Test
    fun produce() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWalletId)
        every { singleAccountListSupplier.invoke(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = listOf(accountList)
        Truth.assertThat(actual).containsExactly(expected)

        coVerifySequence {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
        }
    }

    @Test
    fun `flow will updated if factoryFlow is updated`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWalletId)
        val updatedAccountList = AccountList.empty(userWalletId = userWalletId, sortType = TokensSortType.NONE)
        val factoryFlow = MutableStateFlow<AccountList?>(null)

        every { singleAccountListSupplier.invoke(userWalletId) } returns factoryFlow.filterNotNull()

        // Act (first emission)
        factoryFlow.value = accountList
        val firstEmission = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        Truth.assertThat(firstEmission).containsExactly(listOf(accountList))

        // Act (second emission)
        factoryFlow.value = updatedAccountList
        val secondEmission = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(secondEmission).containsExactly(listOf(updatedAccountList))

        coVerifySequence {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
        }
    }

    @Test
    fun `flow is filtered the same response`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWalletId)
        val factoryFlow = MutableStateFlow<AccountList?>(null)

        every { singleAccountListSupplier.invoke(userWalletId) } returns factoryFlow.filterNotNull()

        // Act (first emission)
        factoryFlow.value = accountList
        val firstEmission = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        Truth.assertThat(firstEmission).containsExactly(listOf(accountList))

        // Act (second emission) - the same status
        factoryFlow.value = accountList
        val secondEmission = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(secondEmission).containsExactly(listOf(accountList))

        coVerifySequence {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
        }
    }

    @Test
    fun `flow returns empty list if factory throws exception`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val exception = RuntimeException("Converter error")
        every { singleAccountListSupplier.invoke(userWalletId) } throws exception

        // Act / Assert: the factory throws -> retryWhen emits the empty fallback.
        // Stop before the 2s retry fires so the upstream is collected exactly once.
        createProducer().produceWithFallback().test {
            Truth.assertThat(awaitItem()).isEqualTo(emptyList<AccountList>())
            cancelAndIgnoreRemainingEvents()
        }

        coVerifySequence {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
        }
    }

    @Test
    fun `flow is empty if userWalletsFlow returns empty flow`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow<List<UserWallet>>(emptyList())
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty() // no emissions

        coVerify(exactly = 1) {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
        }
        coVerify(inverse = true) { singleAccountListSupplier.invoke(any<UserWalletId>()) }
    }

    @Test
    fun `flow is empty if factory returns empty flow`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        every { singleAccountListSupplier.invoke(userWalletId) } returns emptyFlow()

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty() // no emissions

        coVerifySequence {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
        }
    }

    @Test
    fun `flow is empty if one of factoryFlow is empty`() = runTest {
        // Arrange
        val userWalletId2 = UserWalletId("012")
        val userWallet2 = createUserWallet(id = userWalletId2)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet, userWallet2))
        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWalletId)
        every { singleAccountListSupplier.invoke(userWalletId) } returns flowOf(accountList)
        every { singleAccountListSupplier.invoke(userWalletId2) } returns emptyFlow()

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty() // no emissions

        coVerifySequence {
            userWalletsListRepository.load()
            userWalletsListRepository.userWallets
            singleAccountListSupplier.invoke(userWalletId)
            singleAccountListSupplier.invoke(userWalletId2)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LockedWallet {

        private val lockedWalletId = UserWalletId("012")
        private val accountList = AccountList.empty(userWalletId)
        private val lockedWalletAccountList = AccountList.empty(lockedWalletId)

        @Test
        fun `GIVEN locked wallet without account list WHEN produce THEN other wallets are emitted`() = runTest {
            // Arrange
            val lockedWallet = createUserWallet(id = lockedWalletId, isLocked = true)
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet, lockedWallet))
            every { singleAccountListSupplier.invoke(userWalletId) } returns flowOf(accountList)
            every { singleAccountListSupplier.invoke(lockedWalletId) } returns
                MutableStateFlow<AccountList?>(null).filterNotNull()

            // Act
            val actual = producer.produce().let(::getEmittedValues)

            // Assert
            Truth.assertThat(actual).containsExactly(listOf(accountList))
        }

        @Test
        fun `GIVEN locked wallet with account list WHEN produce THEN it is included`() = runTest {
            // Arrange
            val lockedWallet = createUserWallet(id = lockedWalletId, isLocked = true)
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet, lockedWallet))
            every { singleAccountListSupplier.invoke(userWalletId) } returns flowOf(accountList)
            every { singleAccountListSupplier.invoke(lockedWalletId) } returns flowOf(lockedWalletAccountList)

            // Act
            val actual = producer.produce().let(::getEmittedValues)

            // Assert
            Truth.assertThat(actual.last()).containsExactly(accountList, lockedWalletAccountList).inOrder()
        }

        @Test
        fun `GIVEN wallet without account list WHEN it is unlocked THEN its account list is awaited`() = runTest {
            // Arrange
            val lockedWallet = createUserWallet(id = lockedWalletId, isLocked = true)
            val unlockedWallet = createUserWallet(id = lockedWalletId, isLocked = false)
            val userWalletsFlow = MutableStateFlow(listOf(userWallet, lockedWallet))
            val unlockedAccountListFlow = MutableStateFlow<AccountList?>(null)
            every { userWalletsListRepository.userWallets } returns userWalletsFlow
            every { singleAccountListSupplier.invoke(userWalletId) } returns flowOf(accountList)
            every { singleAccountListSupplier.invoke(lockedWalletId) } returns unlockedAccountListFlow.filterNotNull()

            producer.produce().test {
                Truth.assertThat(awaitItem()).containsExactly(accountList)

                // Act
                userWalletsFlow.value = listOf(userWallet, unlockedWallet)

                // Assert
                expectNoEvents()

                // Act
                unlockedAccountListFlow.value = lockedWalletAccountList

                // Assert
                Truth.assertThat(awaitItem()).containsExactly(accountList, lockedWalletAccountList).inOrder()
            }
        }
    }
}