package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.dynamicaddresses.GetDynamicReceiveAddressUseCase
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.TokenReceiveNotification
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.NFTFeatureToggles
import com.tangem.domain.tokens.GetViewedTokenReceiveWarningUseCase
import com.tangem.domain.transaction.R
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration.Companion.seconds

internal class ReceiveAddressesFactoryTest {

    private val getEnsNameUseCase: GetEnsNameUseCase = mockk()
    private val getViewedTokenReceiveWarningUseCase: GetViewedTokenReceiveWarningUseCase = mockk()
    private val getDynamicReceiveAddressUseCase: GetDynamicReceiveAddressUseCase = mockk()
    private val dynamicAddressesRepository: DynamicAddressesRepository = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val nftFeatureToggles: NFTFeatureToggles = mockk()

    private val factory = ReceiveAddressesFactory(
        getEnsNameUseCase = getEnsNameUseCase,
        getViewedTokenReceiveWarningUseCase = getViewedTokenReceiveWarningUseCase,
        getDynamicReceiveAddressUseCase = getDynamicReceiveAddressUseCase,
        dynamicAddressesRepository = dynamicAddressesRepository,
        userWalletsListRepository = userWalletsListRepository,
        nftFeatureToggles = nftFeatureToggles,
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

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN Solana NFT receive WHEN createForNft THEN the unsupported types warning follows the toggle`(
        isToggleEnabled: Boolean,
    ) = runTest {
        // Arrange
        every { nftFeatureToggles.isSolanaPnftAndCoreNftEnabled } returns isToggleEnabled
        val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
        coEvery { getEnsNameUseCase.invoke(any(), any(), any()) } returns null
        coEvery { getViewedTokenReceiveWarningUseCase() } returns emptySet()

        // Act
        val config = factory.createForNft(
            userWalletId = MockUserWalletFactory.createSingleWalletWithToken().walletId,
            addresses = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = ADDRESS, type = NetworkAddress.Address.Type.Primary),
            ),
            network = solana.network,
            nft = solana,
        )

        // Assert
        val expectedSubtitle = if (isToggleEnabled) {
            R.string.nft_receive_unsupported_cnft_description
        } else {
            R.string.nft_receive_unsupported_types_description
        }
        assertThat(config.tokenReceiveNotification).containsExactly(
            TokenReceiveNotification(title = R.string.nft_receive_unsupported_types, subtitle = expectedSubtitle),
        )
    }

    private companion object {
        const val ADDRESS = "0x1234"
    }
}