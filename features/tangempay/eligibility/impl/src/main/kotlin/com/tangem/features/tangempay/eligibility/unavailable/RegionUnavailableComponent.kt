package com.tangem.features.tangempay.eligibility.unavailable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.eligibility.TangemPayWaitlistComponent
import kotlinx.serialization.builtins.serializer

internal class RegionUnavailableComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val waitlistComponentFactory: TangemPayWaitlistComponent.Factory,
    private val analytics: AnalyticsEventHandler,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val waitlistNavigation: SlotNavigation<Unit> = SlotNavigation()

    private val waitlistSlot = childSlot(
        source = waitlistNavigation,
        serializer = Unit.serializer(),
        handleBackButton = false,
        childFactory = { _, componentContext ->
            waitlistComponentFactory.create(
                context = childByContext(componentContext),
                params = TangemPayWaitlistComponent.Params(
                    userWalletId = params.userWalletId,
                    product = params.product,
                    countryCode = params.countryCode,
                    regionCode = params.regionCode,
                    onDismiss = waitlistNavigation::dismiss,
                ),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val waitlist by waitlistSlot.subscribeAsState()

        RegionUnavailableScreen(
            onSelectAnotherRegionClick = ::onSelectAnotherRegionClick,
            onJoinWaitlistClick = ::onJoinWaitlistClick,
            modifier = modifier,
        )

        waitlist.child?.instance?.BottomSheet()
    }

    private fun onSelectAnotherRegionClick() {
        analytics.send(TangemPayAnalyticsEvents.Eligibility.RegionNotAvailableSelectAnotherTapped())
        params.onSelectAnotherRegionClick()
    }

    private fun onJoinWaitlistClick() {
        analytics.send(TangemPayAnalyticsEvents.Eligibility.RegionNotAvailableJoinWaitlistTapped())
        waitlistNavigation.activate(Unit)
    }

    data class Params(
        val userWalletId: UserWalletId?,
        val product: TangemPayProduct,
        val countryCode: String,
        val regionCode: String?,
        val onSelectAnotherRegionClick: () -> Unit,
    )
}