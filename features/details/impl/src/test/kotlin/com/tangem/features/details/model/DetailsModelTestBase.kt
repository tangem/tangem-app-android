package com.tangem.features.details.model

import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayEligibilityManager
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class DetailsModelTestBase {

    protected val userWalletId = UserWalletId("011")
    protected val wallet1 = UserWalletId("01")
    protected val wallet2 = UserWalletId("02")
    protected val buyUrl = "https://tangem.com/buy"

    protected val socialsBuilder: SocialsBuilder = mockk()
    protected val itemsBuilder: ItemsBuilder = mockk()
    protected val appInfoProvider: AppInfoProvider = mockk()
    protected val checkIsWalletConnectAvailableUseCase: CheckIsWalletConnectAvailableUseCase = mockk()
    protected val router: Router = mockk(relaxUnitFun = true)
    protected val urlOpener: UrlOpener = mockk(relaxUnitFun = true)
    protected val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    protected val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()
    protected val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxUnitFun = true)
    protected val getWalletsUseCase: GetWalletsUseCase = mockk()
    protected val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase = mockk()
    protected val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    protected val tangemPayEligibilityManager: TangemPayEligibilityManager = mockk()

    // Captured from itemsBuilder.buildAll(...) so the feature buttons can be driven.
    protected val wcSlot = slot<Boolean>()
    protected val mobileSlot = slot<Boolean>()
    protected val walletIdSlot = slot<UserWalletId>()
    protected val onSupportSlot = slot<() -> Unit>()
    protected val onBuySlot = slot<() -> Unit>()
    protected val onTangemPaySlot = slot<() -> Unit>()

    @BeforeEach
    fun setUp() {
        // Defaults sufficient for model construction (init block).
        coEvery { checkIsWalletConnectAvailableUseCase(any()) } returns false.right()
        every { getWalletsUseCase.invokeSync() } returns emptyList()
        every { socialsBuilder.buildAll() } returns persistentListOf()
        every { appInfoProvider.appVersion } returns "1.2.3"
        every { appInfoProvider.appVersionCode } returns 456
        coEvery { tangemPayEligibilityManager.getCompatibleWalletsIds(any()) } returns emptyList()

        every {
            itemsBuilder.buildAll(
                isWalletConnectAvailable = capture(wcSlot),
                hasAnyMobileWallet = capture(mobileSlot),
                userWalletId = capture(walletIdSlot),
                onSupportClick = capture(onSupportSlot),
                onBuyClick = capture(onBuySlot),
            )
        } returns persistentListOf()
        every { itemsBuilder.addTangemPayItem(any(), capture(onTangemPaySlot)) } answers { firstArg() }
        every { itemsBuilder.removeTangemPayItem(any()) } answers { firstArg() }
        every { itemsBuilder.hasTangemPayItem(any()) } returns false
    }

    protected fun createModel(testScope: TestScope): DetailsModel = DetailsModel(
        socialsBuilder = socialsBuilder,
        paramsContainer = MutableParamsContainer(DetailsComponent.Params(userWalletId)),
        itemsBuilder = itemsBuilder,
        appInfoProvider = appInfoProvider,
        checkIsWalletConnectAvailableUseCase = checkIsWalletConnectAvailableUseCase,
        router = router,
        urlOpener = urlOpener,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        getWalletsUseCase = getWalletsUseCase,
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        generateBuyTangemCardLinkUseCase = generateBuyTangemCardLinkUseCase,
        analyticsEventHandler = analyticsEventHandler,
        tangemPayEligibilityManager = tangemPayEligibilityManager,
    ).also(DetailsModel::onResume)

    protected fun stubBuildAllReturns(list: ImmutableList<DetailsItemUM>) {
        every {
            itemsBuilder.buildAll(any(), any(), any(), any(), any())
        } returns list
    }

    protected fun metaInfo(id: UserWalletId = userWalletId): WalletMetaInfo = WalletMetaInfo(userWalletId = id)

    protected fun hotWallet(id: UserWalletId): UserWallet.Hot = mockk {
        every { walletId } returns id
    }

    protected fun coldWallet(id: UserWalletId): UserWallet.Cold = mockk {
        every { walletId } returns id
    }
}