package com.tangem.features.collectibles.impl.main.model.converter

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_copy_20
import com.tangem.core.ui.res.generated.icons.ic_globe_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.core.ui.res.generated.icons.ic_trash_bin_20
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal enum class CollectiblesMenuItem(val id: String) {
    ACTIVITY_LOG("activity_log"),
    DELIVERIES("deliveries"),
    HOW_IT_WORKS("how_it_works"),
    GET_ASSISTANCE("get_assistance"),
    COPY_TOP_UP_ADDRESS("copy_top_up_address"),
    CLOSE_ACCOUNT("close_account"),
}

/** Builds the top bar menu closed; opening and dismissing it is a state update. */
internal class CollectiblesMenuConverter(
    private val onMenuClick: () -> Unit,
    private val onDismiss: () -> Unit,
    private val onItemClick: (CollectiblesMenuItem) -> Unit,
) : Converter<Unit, CollectiblesMainUM.MenuUM> {

    override fun convert(value: Unit): CollectiblesMainUM.MenuUM = CollectiblesMainUM.MenuUM(
        items = persistentListOf(
            item(item = CollectiblesMenuItem.ACTIVITY_LOG, icon = Icons.ic_clock_20, title = "Activity log"),
            item(item = CollectiblesMenuItem.DELIVERIES, icon = Icons.ic_globe_20, title = "Deliveries"),
            item(
                item = CollectiblesMenuItem.HOW_IT_WORKS,
                icon = Icons.ic_info_20,
                title = "How it works",
                hasDividerAbove = true,
            ),
            item(item = CollectiblesMenuItem.GET_ASSISTANCE, icon = Icons.ic_mail_20, title = "Get assistance"),

            // TODO move both to test menu
            item(
                item = CollectiblesMenuItem.COPY_TOP_UP_ADDRESS,
                icon = Icons.ic_copy_20,
                title = "Copy USDC address",
                hasDividerAbove = true,
            ),
            item(
                item = CollectiblesMenuItem.CLOSE_ACCOUNT,
                icon = Icons.ic_trash_bin_20,
                title = "Close account",
            ),
        ),
        isShown = false,
        onMenuClick = onMenuClick,
        onDismiss = onDismiss,
    )

    private fun item(item: CollectiblesMenuItem, icon: ImageVector, title: String, hasDividerAbove: Boolean = false) =
        CollectiblesMainUM.MenuItemUM(
            id = item.id,
            icon = icon,
            title = stringReference(title),
            hasDividerAbove = hasDividerAbove,
            onClick = { onItemClick(item) },
        )
}