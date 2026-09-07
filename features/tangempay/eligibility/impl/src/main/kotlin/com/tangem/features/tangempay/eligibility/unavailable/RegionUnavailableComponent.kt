package com.tangem.features.tangempay.eligibility.unavailable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.eligibility.TangemPayWaitlistComponent
import com.tangem.features.tangempay.eligibility.waitlist.DefaultTangemPayWaitlistComponent
import kotlinx.serialization.builtins.serializer

internal class RegionUnavailableComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val waitlistNavigation: SlotNavigation<Unit> = SlotNavigation()

    private val waitlistSlot = childSlot(
        source = waitlistNavigation,
        serializer = Unit.serializer(),
        handleBackButton = false,
        childFactory = { _, componentContext ->
            DefaultTangemPayWaitlistComponent(
                context = childByContext(componentContext),
                params = TangemPayWaitlistComponent.Params(onDismiss = waitlistNavigation::dismiss),
            )
        },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val waitlist by waitlistSlot.subscribeAsState()

        RegionUnavailableScreen(
            onSelectAnotherRegionClick = params.onSelectAnotherRegionClick,
            onJoinWaitlistClick = { waitlistNavigation.activate(Unit) },
            modifier = modifier,
        )

        waitlist.child?.instance?.BottomSheet()
    }

    data class Params(val onSelectAnotherRegionClick: () -> Unit)
}