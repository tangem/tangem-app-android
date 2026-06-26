package com.tangem.features.details.model

import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.repository.FeedbackFeatureToggles
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.virtualaccount.model.VirtualAccountEligibility
import com.tangem.domain.virtualaccount.usecase.GetVirtualAccountEligibilityUseCase
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.AddressBookFeatureToggles
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class DetailsModelTestBase {

    protected val userWalletId = UserWalletId("011")
    protected val wallet1 = UserWalletId("01")
    protected val wallet2 = UserWalletId("02")
    protected val customerId = "customer-1"
    protected val buyUrl = "https://tangem.com/buy"

    protected val socialsBuilder: SocialsBuilder = mockk()
    protected val feedbackFeatureToggles: FeedbackFeatureToggles = mockk()
    protected val addressBookFeatureToggles: AddressBookFeatureToggles = mockk()
    protected val itemsBuilder: ItemsBuilder = mockk()
    protected val appInfoProvider: AppInfoProvider = mockk()
    protected val checkIsWalletConnectAvailableUseCase: CheckIsWalletConnectAvailableUseCase = mockk()
    protected val router: Router = mockk(relaxUnitFun = true)
    protected val urlOpener: UrlOpener = mockk(relaxUnitFun = true)
    protected val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    protected val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    protected val getTangemPayCustomerIdUseCase: GetTangemPayCustomerIdUseCase = mockk()
    protected val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxUnitFun = true)
    protected val getWalletsUseCase: GetWalletsUseCase = mockk()
    protected val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase = mockk()
    protected val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    protected val tangemPayEligibilityManager: TangemPayEligibilityManager = mockk()
    protected val getVirtualAccountEligibilityUseCase: GetVirtualAccountEligibilityUseCase = mockk()

    // Captured from itemsBuilder.buildAll(...) so the feature buttons can be driven.
    protected val wcSlot = slot<Boolean>()
    protected val abSlot = slot<Boolean>()
    protected val mobileSlot = slot<Boolean>()
    protected val walletIdSlot = slot<UserWalletId>()
    protected val onSupportSlot = slot<() -> Unit>()
    protected val onBuySlot = slot<() -> Unit>()
    protected val onTangemPaySlot = slot<() -> Unit>()

    @BeforeEach
    fun setUp() {
        mockkObject(VisaUtilities)

        // Defaults sufficient for model construction (init block).
        coEvery { checkIsWalletConnectAvailableUseCase(any()) } returns false.right()
        every { addressBookFeatureToggles.isAddressBookEnabled } returns false
        every { feedbackFeatureToggles.isUsedeskEnabled } returns false
        every { getWalletsUseCase.invokeSync() } returns emptyList()
        every { socialsBuilder.buildAll() } returns persistentListOf()
        every { appInfoProvider.appVersion } returns "1.2.3"
        every { appInfoProvider.appVersionCode } returns 456
        coEvery { tangemPayEligibilityManager.getEligibleWallets(any(), any()) } returns emptyList()
        coEvery { getVirtualAccountEligibilityUseCase(any()) } returns VirtualAccountEligibility.NotAvailable

        every {
            itemsBuilder.buildAll(
                isWalletConnectAvailable = capture(wcSlot),
                isAddressBookAvailable = capture(abSlot),
                hasAnyMobileWallet = capture(mobileSlot),
                userWalletId = capture(walletIdSlot),
                onSupportClick = capture(onSupportSlot),
                onBuyClick = capture(onBuySlot),
            )
        } returns persistentListOf()
        every { itemsBuilder.addTangemPayItem(any(), capture(onTangemPaySlot)) } answers { firstArg() }
        every { itemsBuilder.removeTangemPayItem(any()) } answers { firstArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(VisaUtilities)
    }

    protected fun createModel(testScope: TestScope): DetailsModel = DetailsModel(
        socialsBuilder = socialsBuilder,
        paramsContainer = MutableParamsContainer(DetailsComponent.Params(userWalletId)),
        feedbackFeatureToggles = feedbackFeatureToggles,
        addressBookFeatureToggles = addressBookFeatureToggles,
        itemsBuilder = itemsBuilder,
        appInfoProvider = appInfoProvider,
        checkIsWalletConnectAvailableUseCase = checkIsWalletConnectAvailableUseCase,
        router = router,
        urlOpener = urlOpener,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        getTangemPayCustomerIdUseCase = getTangemPayCustomerIdUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        getWalletsUseCase = getWalletsUseCase,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        generateBuyTangemCardLinkUseCase = generateBuyTangemCardLinkUseCase,
        analyticsEventHandler = analyticsEventHandler,
        tangemPayEligibilityManager = tangemPayEligibilityManager,
        getVirtualAccountEligibilityUseCase = getVirtualAccountEligibilityUseCase,
    )

    protected fun stubBuildAllReturns(list: ImmutableList<DetailsItemUM>) {
        every {
            itemsBuilder.buildAll(any(), any(), any(), any(), any(), any())
        } returns list
    }

    protected fun metaInfo(id: UserWalletId = userWalletId, isVisa: Boolean? = null): WalletMetaInfo =
        WalletMetaInfo(userWalletId = id, isVisa = isVisa)

    protected fun hotWallet(id: UserWalletId): UserWallet.Hot = mockk {
        every { walletId } returns id
    }

    protected fun coldWallet(id: UserWalletId, isVisa: Boolean): UserWallet.Cold {
        val cardMock = mockk<CardDTO>()
        every { VisaUtilities.isVisaCard(cardMock) } returns isVisa
        val scan = mockk<ScanResponse> { every { card } returns cardMock }
        return mockk {
            every { walletId } returns id
            every { scanResponse } returns scan
        }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}