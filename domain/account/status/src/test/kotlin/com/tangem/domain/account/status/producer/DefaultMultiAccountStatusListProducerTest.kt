package com.tangem.domain.account.status.producer

import com.google.common.truth.Truth
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMultiAccountStatusListProducerTest {

    private val accountsCRUDRepository: AccountsCRUDRepository = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val userWalletId1 = UserWalletId("001")
    private val userWallet1 = mockk<UserWallet> {
        every { walletId } returns userWalletId1
    }

    private val userWalletId2 = UserWalletId("002")
    private val userWallet2 = mockk<UserWallet> {
        every { walletId } returns userWalletId2
    }

    private val producer = DefaultMultiAccountStatusListProducer(
        params = Unit,
        accountsCRUDRepository = accountsCRUDRepository,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        dispatchers = dispatchers,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(accountsCRUDRepository, singleAccountStatusListSupplier)
    }

    @Test
    fun `produce returns status lists for all user wallets`() = runTest {
        // Arrange
        val wallets = listOf(userWallet1, userWallet2)
        val walletsFlow = MutableStateFlow(wallets)

        every { accountsCRUDRepository.getUserWallets() } returns walletsFlow
        val accountStatusList1 = mockk<AccountStatusList>()
        val accountStatusList2 = mockk<AccountStatusList>()

        every {
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId1),
            )
        } returns flowOf(accountStatusList1)

        every {
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId2),
            )
        } returns flowOf(accountStatusList2)

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        val expected = listOf(accountStatusList1, accountStatusList2)
        Truth.assertThat(actual).containsExactly(expected)

        coVerify(ordering = Ordering.SEQUENCE) {
            accountsCRUDRepository.getUserWallets()
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId1),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId2),
            )
        }
    }

    @Test
    fun `produce returns empty flow if userWallets is empty list`() = runTest {
        // Arrange
        val walletsFlow = MutableStateFlow<List<UserWallet>>(emptyList())
        every { accountsCRUDRepository.getUserWallets() } returns walletsFlow

        // Act
        val actual = producer.produce().let(::getEmittedValues)

        // Assert
        Truth.assertThat(actual).isEmpty()

        coVerify(ordering = Ordering.SEQUENCE) {
            accountsCRUDRepository.getUserWallets()
        }
    }

    // TODO: uncomment after migration on UserWalletsListRepository
    // @Test
    // fun `produce returns empty flow if userWallets is null`() = runTest {
    //     // Arrange
    //     val walletsFlow = MutableStateFlow<List<UserWallet>?>(null)
    //     every { accountsCRUDRepository.getUserWallets() } returns walletsFlow
    //
    //     // Act
    //     val actual = producer.produce().let(::getEmittedValues)
    //
    //     // Assert
    //     Truth.assertThat(actual).isEmpty()
    //
    //     coVerify(ordering = Ordering.SEQUENCE) {
    //         accountsCRUDRepository.getUserWallets()
    //     }
    // }

    @Test
    fun `flow will updated if userWallets are updated`() = runTest {
        // Arrange
        val userWalletId3 = UserWalletId("003")
        val userWallet3 = mockk<UserWallet> { every { walletId } returns userWalletId3 }

        val walletsFlow = MutableStateFlow(listOf(userWallet1, userWallet2))
        every { accountsCRUDRepository.getUserWallets() } returns walletsFlow

        val accountStatusList1 = mockk<AccountStatusList>()
        val accountStatusList2 = mockk<AccountStatusList>()
        val accountStatusList3 = mockk<AccountStatusList>()

        every {
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId1),
            )
        } returns flowOf(accountStatusList1)

        every {
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId2),
            )
        } returns flowOf(accountStatusList2)

        every {
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId3),
            )
        } returns flowOf(accountStatusList3)

        // Act (first emission)
        val actual1 = producer.produce().let(::getEmittedValues)

        // Assert (first emission)
        val expected1 = listOf(accountStatusList1, accountStatusList2)
        Truth.assertThat(actual1).containsExactly(expected1)

        // Act (second emission)
        walletsFlow.value = listOf(userWallet1, userWallet2, userWallet3)
        val actual2 = producer.produce().let(::getEmittedValues)

        // Assert (second emission)
        val expected2 = listOf(accountStatusList1, accountStatusList2, accountStatusList3)
        Truth.assertThat(actual2).containsExactly(expected2)

        coVerify(ordering = Ordering.SEQUENCE) {
            accountsCRUDRepository.getUserWallets()
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId1),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId2),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId1),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId2),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId3),
            )
            accountsCRUDRepository.getUserWallets()
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId1),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId2),
            )
            singleAccountStatusListSupplier(
                params = SingleAccountStatusListProducer.Params(userWalletId3),
            )
        }
    }
}