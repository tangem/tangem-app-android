package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.promobanners.impl.campaigns.model.PredefinedTokenResolver
import com.tangem.features.promobanners.impl.campaigns.v2.PROMO_TNC_URL
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignRoute
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PromoCampaignModelTest {

    private val chooseTokenBridgeFactory: ChooseTokenBridge.Factory = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val onCurrencyChosen = Channel<ChooseTokenResult>(capacity = Channel.UNLIMITED)
    private val onClose = Channel<Unit>(capacity = Channel.UNLIMITED)

    @BeforeEach
    fun setup() {
        clearMocks(chooseTokenBridgeFactory, getSelectedAppCurrencyUseCase, multiAccountListSupplier, urlOpener, router)
        val bridge = mockk<ChooseTokenBridge>(relaxed = true) {
            every { onCurrencyChosen } returns this@PromoCampaignModelTest.onCurrencyChosen
            every { onClose } returns this@PromoCampaignModelTest.onClose
        }
        every { chooseTokenBridgeFactory.create(any(), any(), any()) } returns bridge
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { multiAccountListSupplier.invoke() } returns flowOf(emptyList<AccountList>())
    }

    @Test
    fun `GIVEN payout needed and no token WHEN action clicked THEN chooser opened`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = true)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()

        // Assert
        verify(exactly = 1) { router.push(PromoCampaignRoute.ChooseToken, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN payout not needed WHEN action clicked THEN chooser not opened`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()

        // Assert
        assertThat(model.uiState.value.tokenSelector).isNull()
        verify(exactly = 0) { router.push(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN chooser open WHEN token chosen THEN selector shows it and chooser closes`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = true)
        advanceUntilIdle()
        model.uiState.value.tokenSelector?.onClick?.invoke()

        // Act
        onCurrencyChosen.send(chooseTokenResult())
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.tokenSelector?.selectedToken).isNotNull()
        assertThat(state.tokenSelector?.selectedAccount).isNull()
        assertThat(state.footer.terms).isNotNull()
        assertThat(state.footer.buttonText).isEqualTo(stringReference("Enroll"))
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN token chosen WHEN action clicked THEN chooser not reopened`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = true)
        advanceUntilIdle()
        onCurrencyChosen.send(chooseTokenResult())
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN chooser open WHEN chooser dismissed THEN it closes`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = true)
        advanceUntilIdle()
        model.uiState.value.tokenSelector?.onClick?.invoke()

        // Act
        onClose.send(Unit)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN terms link WHEN clicked THEN tnc url opened`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.terms?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl(PROMO_TNC_URL) }
        model.onDestroy()
    }

    private fun chooseTokenResult(): ChooseTokenResult {
        val status = CryptoCurrencyStatus(
            currency = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum, contractAddress = "0xToken"),
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = null,
                fiatRate = null,
                networkAddress = NetworkAddress.Single(
                    NetworkAddress.Address(value = "0xUser", type = NetworkAddress.Address.Type.Primary),
                ),
            ),
        )
        return ChooseTokenResult(currency = status, account = mockk(relaxed = true), wallet = mockk<UserWallet>())
    }

    private fun TestScope.createModel(isPayoutNeeded: Boolean) = PromoCampaignModel(
        paramsContainer = MutableParamsContainer(
            PromoCampaignBottomSheetComponent.Params(
                campaign = createPromoCampaign(isPayoutNeeded = isPayoutNeeded),
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        chooseTokenBridgeFactory = chooseTokenBridgeFactory,
        predefinedTokenResolver = PredefinedTokenResolver(),
        router = router,
        multiAccountListSupplier = multiAccountListSupplier,
        urlOpener = urlOpener,
    )
}