package com.tangem.data.account.producer

import com.google.common.truth.Truth
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
class DefaultMultiAccountListProducerTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val walletAccountListFlowFactory: WalletAccountListFlowFactory = mockk()

    private val producer = DefaultMultiAccountListProducer(
        params = Unit,
        userWalletsStore = userWalletsStore,
        walletAccountListFlowFactory = walletAccountListFlowFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet = mockk<UserWallet> {
        every { this@mockk.walletId } returns userWalletId
    }

    @AfterEach
    fun tearDownEach() {
        clearMocks(userWalletsStore, walletAccountListFlowFactory)
    }

    @Test
    fun produce() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsStore.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWallet)
        every { walletAccountListFlowFactory.create(userWallet) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = listOf(accountList)
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
        }
    }

    @Test
    fun `flow will updated if factoryFlow is updated`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsStore.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWallet)
        val updatedAccountList = AccountList.empty(userWallet = userWallet, sortType = TokensSortType.NONE)
        val factoryFlow = MutableStateFlow<AccountList?>(null)

        every { walletAccountListFlowFactory.create(userWallet) } returns factoryFlow.filterNotNull()

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

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
        }
    }

    @Test
    fun `flow is filtered the same response`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsStore.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWallet)
        val factoryFlow = MutableStateFlow<AccountList?>(null)

        every { walletAccountListFlowFactory.create(userWallet) } returns factoryFlow.filterNotNull()

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

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
        }
    }

    @Test
    fun `flow returns empty list if factory throws exception`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsStore.userWallets } returns userWalletsFlow

        val exception = RuntimeException("Converter error")
        every { walletAccountListFlowFactory.create(userWallet) } throws exception

        // Act
        val actual = producer.produceWithFallback().let(::getEmittedValues)

        // Assert
        val expected = emptyList<AccountList>()
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
        }
    }

    @Test
    fun `flow is empty if userWalletsFlow returns empty flow`() = runTest {
        // Arrange
        val userWalletsFlow = emptyFlow<List<UserWallet>>()
        every { userWalletsStore.userWallets } returns userWalletsFlow

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty() // no emissions

        coVerify(exactly = 1) { userWalletsStore.userWallets }
        coVerify(inverse = true) { walletAccountListFlowFactory.create(any()) }
    }

    @Test
    fun `flow is empty if factory returns empty flow`() = runTest {
        // Arrange
        val userWalletsFlow = MutableStateFlow(value = listOf(userWallet))
        every { userWalletsStore.userWallets } returns userWalletsFlow

        every { walletAccountListFlowFactory.create(userWallet) } returns emptyFlow()

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty() // no emissions

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
        }
    }

    @Test
    fun `flow is empty if one of factoryFlow is empty`() = runTest {
        // Arrange
        val userWalletId2 = UserWalletId("012")
        val userWallet2 = mockk<UserWallet> {
            every { this@mockk.walletId } returns userWalletId2
        }

        val userWalletsFlow = MutableStateFlow(listOf(userWallet, userWallet2))
        every { userWalletsStore.userWallets } returns userWalletsFlow

        val accountList = AccountList.empty(userWallet)
        every { walletAccountListFlowFactory.create(userWallet) } returns flowOf(accountList)
        every { walletAccountListFlowFactory.create(userWallet2) } returns emptyFlow()

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty() // no emissions

        coVerify(ordering = Ordering.SEQUENCE) {
            userWalletsStore.userWallets
            walletAccountListFlowFactory.create(userWallet)
            walletAccountListFlowFactory.create(userWallet2)
        }
    }
}