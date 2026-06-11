package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.GetDynamicReceiveAddressUseCase
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetViewedTokenReceiveWarningUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class ReceiveAddressesFactoryTest {

    private val getEnsNameUseCase: GetEnsNameUseCase = mockk()
    private val getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase = mockk()
    private val getDynamicReceiveAddressUseCase: GetDynamicReceiveAddressUseCase = mockk()
    private val dynamicAddressesRepository: DynamicAddressesRepository = mockk()
    private val dynamicAddressesFeatureToggles: DynamicAddressesFeatureToggles = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val factory = ReceiveAddressesFactory(
        getEnsNameUseCase = getEnsNameUseCase,
        getViewedTokenReceiveWarningUseCase = getViewedTokenReceiveWarningUseCase,
        getDynamicReceiveAddressUseCase = getDynamicReceiveAddressUseCase,
        dynamicAddressesRepository = dynamicAddressesRepository,
        dynamicAddressesFeatureToggles = dynamicAddressesFeatureToggles,
        userWalletsListRepository = userWalletsListRepository,
    )

    @Test
    fun `GIVEN single-currency wallet WHEN create THEN standard addresses returned without status check`() = runTest(
        timeout = 3.seconds,
    ) {
        // GIVEN
        val userWallet = MockUserWalletFactory.createSingleWalletWithToken() // isMultiCurrency = false
        val coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
        val status = mockk<CryptoCurrencyStatus> {
            every { currency } returns coin
            every { value.networkAddress } returns NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = ADDRESS, type = NetworkAddress.Address.Type.Primary),
            )
        }

        every { dynamicAddressesFeatureToggles.isDynamicAddressesEnabled } returns true
        every { userWalletsListRepository.userWallets } returns MutableStateFlow<List<UserWallet>?>(listOf(userWallet))
        // Single-currency wallets never populate the accounts store, so the status flow never emits ([REDACTED_TASK_KEY])
        every { dynamicAddressesRepository.getStatus(any(), any()) } returns flow { awaitCancellation() }
        coEvery { getEnsNameUseCase.invoke(any(), any(), any()) } returns null
        coEvery { getViewedTokenReceiveWarningUseCase() } returns emptySet()

        // WHEN
        val config = factory.create(status = status, userWalletId = userWallet.walletId)

        // THEN
        assertThat(config).isNotNull()
        assertThat(config!!.receiveAddress.map { it.value }).containsExactly(ADDRESS)
    }

    private companion object {
        const val ADDRESS = "0x1234"
    }
}