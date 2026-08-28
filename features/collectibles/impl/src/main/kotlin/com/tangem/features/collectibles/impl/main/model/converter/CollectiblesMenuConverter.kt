package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_globe_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

/** Builds the top bar menu closed; opening and dismissing it is a state update. */
internal class CollectiblesMenuConverter(
    private val onMenuClick: () -> Unit,
    private val onDismiss: () -> Unit,
    private val onActivityLogClick: () -> Unit,
    private val onDeliveriesClick: () -> Unit,
    private val onHowItWorksClick: () -> Unit,
    private val onGetAssistanceClick: () -> Unit,
) : Converter<Unit, CollectiblesMainUM.MenuUM> {

    override fun convert(value: Unit): CollectiblesMainUM.MenuUM = CollectiblesMainUM.MenuUM(
        items = persistentListOf(
            CollectiblesMainUM.MenuItemUM(
                id = MENU_ACTIVITY_LOG,
                icon = Icons.ic_clock_20,
                title = stringReference("Activity log"),
                hasDividerAbove = false,
                onClick = onActivityLogClick,
            ),
            CollectiblesMainUM.MenuItemUM(
                id = MENU_DELIVERIES,
                icon = Icons.ic_globe_20,
                title = stringReference("Deliveries"),
                hasDividerAbove = false,
                onClick = onDeliveriesClick,
            ),
            CollectiblesMainUM.MenuItemUM(
                id = MENU_HOW_IT_WORKS,
                icon = Icons.ic_info_20,
                title = stringReference("How it works"),
                hasDividerAbove = true,
                onClick = onHowItWorksClick,
            ),
            CollectiblesMainUM.MenuItemUM(
                id = MENU_GET_ASSISTANCE,
                icon = Icons.ic_mail_20,
                title = stringReference("Get assistance"),
                hasDividerAbove = false,
                onClick = onGetAssistanceClick,
            ),
        ),
        isShown = false,
        onMenuClick = onMenuClick,
        onDismiss = onDismiss,
    )

    private companion object {

        const val MENU_ACTIVITY_LOG = "activity_log"
        const val MENU_DELIVERIES = "deliveries"
        const val MENU_HOW_IT_WORKS = "how_it_works"
        const val MENU_GET_ASSISTANCE = "get_assistance"
    }
}