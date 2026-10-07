package com.tangem.features.promobanners.impl.campaigns.v2.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoMatchMode
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult
import com.tangem.domain.promo.models.PromoRegistrationResult.PromoRegistrationStatus
import com.tangem.domain.promo.models.PromoWalletType
import com.tangem.domain.promo.usecase.GetPromoCampaignsUseCase
import com.tangem.domain.promo.usecase.RegisterPromoCampaignUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.promobanners.impl.campaigns.model.PredefinedTokenResolver
import com.tangem.features.promobanners.impl.campaigns.v2.PROMO_TNC_URL
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignRoute
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignStatusUM
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
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
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val registerPromoCampaignUseCase: RegisterPromoCampaignUseCase = mockk()
    private val getPromoCampaignsUseCase: GetPromoCampaignsUseCase = mockk()
    private val userWalletsFetcherFactory: UserWalletsFetcher.Factory = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)

    private val mainWallet = MockUserWalletFactory.create()
    private val linkedWallet = MockUserWalletFactory.create().copy(walletId = UserWalletId("0b"), name = "Linked")

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val onCurrencyChosen = Channel<ChooseTokenResult>(capacity = Channel.UNLIMITED)
    private val onClose = Channel<Unit>(capacity = Channel.UNLIMITED)

    @BeforeEach
    fun setup() {
        clearMocks(
            chooseTokenBridgeFactory,
            getSelectedAppCurrencyUseCase,
            multiAccountListSupplier,
            urlOpener,
            router,
            getWalletsUseCase,
            registerPromoCampaignUseCase,
            getPromoCampaignsUseCase,
            userWalletsFetcherFactory,
            getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase,
            messageSender,
        )
        val bridge = mockk<ChooseTokenBridge>(relaxed = true) {
            every { onCurrencyChosen } returns this@PromoCampaignModelTest.onCurrencyChosen
            every { onClose } returns this@PromoCampaignModelTest.onClose
        }
        every { chooseTokenBridgeFactory.create(any(), any(), any()) } returns bridge
        every { getSelectedAppCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { multiAccountListSupplier.invoke() } returns flowOf(emptyList<AccountList>())
        every { getWalletsUseCase.invokeSync() } returns listOf(mainWallet, linkedWallet)
        coEvery { registerPromoCampaignUseCase(any(), any()) } returns
            PromoRegistrationError.Generic(IllegalStateException()).left()
        coEvery { getPromoCampaignsUseCase(any(), any()) } returns emptyList<PromoCampaign>().right()
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
    fun `GIVEN chosen token has no address WHEN token chosen THEN selector stays empty and chooser stays open`() =
        runTest {
            // Arrange
            val model = createModel(isPayoutNeeded = true)
            advanceUntilIdle()
            model.uiState.value.tokenSelector?.onClick?.invoke()

            // Act
            onCurrencyChosen.send(chooseTokenResult(address = null))
            advanceUntilIdle()
            model.uiState.value.footer.onButtonClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.tokenSelector?.selectedToken).isNull()
            verify(exactly = 0) { router.pop(any()) }
            coVerify(exactly = 0) { registerPromoCampaignUseCase(any(), any()) }
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

    @Test
    fun `GIVEN token chosen WHEN enroll clicked THEN main wallet registered with chosen payout`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = true)
        advanceUntilIdle()
        val chosen = chooseTokenResult()
        onCurrencyChosen.send(chosen)
        advanceUntilIdle()
        val token = chosen.currency.currency as CryptoCurrency.Token
        val expected = listOf(
            PromoEnrollment(
                userWalletId = mainWallet.walletId,
                walletType = PromoWalletType.Card,
                batchId = mainWallet.scanResponse.card.batchId,
                payout = PromoEnrollment.PromoPayoutTarget(
                    userAddress = "0xUser",
                    tokenAddress = "0xToken",
                    networkId = token.network.rawId,
                    tokenId = token.id.rawCurrencyId?.value,
                ),
                isMain = true,
            ),
        )

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { registerPromoCampaignUseCase(promotionId = 1, enrollments = expected) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN registration enrolled WHEN enroll clicked THEN success status replaces the campaign`() = runTest {
        // Arrange
        coEvery { registerPromoCampaignUseCase(any(), any()) } returns
            listOf(registered(mainWallet.walletId, PromoRegistrationStatus.Enrolled)).right()
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        val status = model.statusState.value
        assertThat(status?.variant).isEqualTo(TangemStatusScreen.Variant.Success)
        assertThat(status?.description).isNull()
        assertThat(model.uiState.value.footer.isLoading).isFalse()
        verify(exactly = 1) { router.replaceAll(PromoCampaignRoute.Status, onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN already enrolled to token chosen from another wallet WHEN enroll clicked THEN recipient labelled with that wallet`() =
        runTest {
            // Arrange
            val chosen = chooseTokenResult(wallet = linkedWallet)
            val token = chosen.currency.currency as CryptoCurrency.Token
            val payout = PromoRegistrationResult.PromoRegisteredPayout(
                userAddress = "0xUser",
                tokenAddress = "0xToken",
                networkId = token.network.rawId,
            )
            coEvery { registerPromoCampaignUseCase(any(), any()) } returns listOf(
                PromoRegistrationResult(
                    userWalletId = mainWallet.walletId,
                    status = PromoRegistrationStatus.AlreadyEnrolled,
                    payout = payout,
                ),
            ).right()
            val model = createModel(isPayoutNeeded = true)
            advanceUntilIdle()
            onCurrencyChosen.send(chosen)
            advanceUntilIdle()

            // Act
            model.uiState.value.footer.onButtonClick()
            advanceUntilIdle()

            // Assert
            val details = model.statusState.value?.details as? PromoCampaignStatusUM.Details.Recipient
            assertThat(details?.walletName).isEqualTo(stringReference(linkedWallet.name))
            model.onDestroy()
        }

    @Test
    fun `GIVEN linked wallet ineligible WHEN enroll clicked THEN only enrolled wallets listed`() = runTest {
        // Arrange
        coEvery { registerPromoCampaignUseCase(any(), any()) } returns listOf(
            registered(mainWallet.walletId, PromoRegistrationStatus.Enrolled),
            registered(linkedWallet.walletId, PromoRegistrationStatus.Ineligible),
        ).right()
        val mainItem = walletItem(mainWallet.walletId)
        every { userWalletsFetcherFactory.create(any(), any(), any(), any(), any()) } returns
            mockk { every { userWallets } returns flowOf(persistentListOf(mainItem, walletItem(linkedWallet.walletId))) }
        val model = createModel(isPayoutNeeded = false, matchMode = PromoMatchMode.LinkedWallet)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.statusState.value?.details)
            .isEqualTo(PromoCampaignStatusUM.Details.EnrolledWallets(persistentListOf(mainItem)))
        model.onDestroy()
    }

    @Test
    fun `GIVEN generic error WHEN enroll clicked THEN dialog shown and campaign stays`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.statusState.value).isNull()
        verify(exactly = 1) { messageSender.send(any<DialogMessage>()) }
        verify(exactly = 0) { router.replaceAll(any(), onComplete = any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN registration in flight WHEN enroll clicked again THEN registered once`() = runTest {
        // Arrange
        val response = CompletableDeferred<List<PromoRegistrationResult>>()
        coEvery { registerPromoCampaignUseCase(any(), any()) } coAnswers { response.await().right() }
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.footer.isLoading).isTrue()
        coVerify(exactly = 1) { registerPromoCampaignUseCase(any(), any()) }
        response.complete(emptyList())
        advanceUntilIdle()
        model.onDestroy()
    }

    @Test
    fun `GIVEN linked campaign WHEN registration succeeds THEN campaigns force-refreshed for every enrolled wallet`() =
        runTest {
            // Arrange
            coEvery { registerPromoCampaignUseCase(any(), any()) } returns listOf(
                registered(mainWallet.walletId, PromoRegistrationStatus.Enrolled),
                registered(linkedWallet.walletId, PromoRegistrationStatus.Enrolled),
            ).right()
            every { userWalletsFetcherFactory.create(any(), any(), any(), any(), any()) } returns
                mockk { every { userWallets } returns flowOf(persistentListOf()) }
            val model = createModel(isPayoutNeeded = false, matchMode = PromoMatchMode.LinkedWallet)
            advanceUntilIdle()

            // Act
            model.uiState.value.footer.onButtonClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { getPromoCampaignsUseCase(mainWallet, forceRefresh = true) }
            coVerify(exactly = 1) { getPromoCampaignsUseCase(linkedWallet, forceRefresh = true) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN registration fails WHEN enroll clicked THEN campaigns not refreshed`() = runTest {
        // Arrange
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { getPromoCampaignsUseCase(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN campaigns refresh fails WHEN registration succeeds THEN status still shown`() = runTest {
        // Arrange
        coEvery { registerPromoCampaignUseCase(any(), any()) } returns
            listOf(registered(mainWallet.walletId, PromoRegistrationStatus.Enrolled)).right()
        coEvery { getPromoCampaignsUseCase(any(), any()) } returns RuntimeException("network").left()
        val model = createModel(isPayoutNeeded = false)
        advanceUntilIdle()

        // Act
        model.uiState.value.footer.onButtonClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.statusState.value?.variant).isEqualTo(TangemStatusScreen.Variant.Success)
        verify(exactly = 1) { router.replaceAll(PromoCampaignRoute.Status, onComplete = any()) }
        model.onDestroy()
    }

    private fun registered(walletId: UserWalletId, status: PromoRegistrationStatus) =
        PromoRegistrationResult(userWalletId = walletId, status = status, payout = null)

    private fun walletItem(walletId: UserWalletId) = UserWalletItemUM(
        id = walletId.stringValue,
        name = stringReference(walletId.stringValue),
        information = UserWalletItemUM.Information.Loading,
        balance = UserWalletItemUM.Balance.Loading,
        isEnabled = true,
        onClick = {},
    )

    private fun chooseTokenResult(
        wallet: UserWallet = mainWallet,
        address: String? = "0xUser",
    ): ChooseTokenResult {
        val status = CryptoCurrencyStatus(
            currency = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum, contractAddress = "0xToken"),
            value = CryptoCurrencyStatus.Unreachable(
                priceChange = null,
                fiatRate = null,
                networkAddress = address?.let {
                    NetworkAddress.Single(NetworkAddress.Address(value = it, type = NetworkAddress.Address.Type.Primary))
                },
            ),
        )
        return ChooseTokenResult(currency = status, account = mockk(relaxed = true), wallet = wallet)
    }

    private fun TestScope.createModel(
        isPayoutNeeded: Boolean,
        matchMode: PromoMatchMode = PromoMatchMode.Strict,
    ) = PromoCampaignModel(
        paramsContainer = MutableParamsContainer(
            PromoCampaignBottomSheetComponent.Params(
                campaign = createPromoCampaign(isPayoutNeeded = isPayoutNeeded, matchMode = matchMode),
                userWalletId = mainWallet.walletId,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        chooseTokenBridgeFactory = chooseTokenBridgeFactory,
        predefinedTokenResolver = PredefinedTokenResolver(),
        router = router,
        multiAccountListSupplier = multiAccountListSupplier,
        urlOpener = urlOpener,
        getWalletsUseCase = getWalletsUseCase,
        registerPromoCampaignUseCase = registerPromoCampaignUseCase,
        getPromoCampaignsUseCase = getPromoCampaignsUseCase,
        userWalletsFetcherFactory = userWalletsFetcherFactory,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        messageSender = messageSender,
    )
}