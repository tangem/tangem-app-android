package com.tangem.data.account.producer

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
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
    private val userWallet = mockk<UserWallet> {
        every { this@mockk.walletId } returns userWalletId
    }

    private val producer = DefaultSingleAccountListProducer(
        params = SingleAccountListProducer.Params(userWalletId = userWalletId),
        walletAccountListFlowFactory = walletAccountListFlowFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @AfterEach
    fun tearDownEach() {
        clearMocks(walletAccountListFlowFactory)
    }

    @Test
    fun produce() = runTest {
        // Arrange
        MutableStateFlow(listOf(userWallet))

        val accountList = AccountList.empty(userWalletId)
        every { walletAccountListFlowFactory.create(userWalletId) } returns flowOf(accountList)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = accountList
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            walletAccountListFlowFactory.create(userWalletId)
        }
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
        Truth.assertThat(firstEmission).containsExactly(accountList)

        // Act (second emission)
        factoryFlow.value = updatedAccountList
        val secondEmission = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(secondEmission).containsExactly(updatedAccountList)

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
        Truth.assertThat(firstEmission).containsExactly(accountList)

        // Act (second emission) - the same status
        factoryFlow.value = accountList
        val secondEmission = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        Truth.assertThat(secondEmission).containsExactly(accountList)

        coVerify(ordering = Ordering.SEQUENCE) {
            walletAccountListFlowFactory.create(userWalletId)
            walletAccountListFlowFactory.create(userWalletId)
        }
    }
}