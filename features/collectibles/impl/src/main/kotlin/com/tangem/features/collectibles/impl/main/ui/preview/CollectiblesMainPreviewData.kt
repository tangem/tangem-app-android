package com.tangem.features.collectibles.impl.main.ui.preview

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_globe_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.core.ui.res.generated.icons.ic_trash_bin_20
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import kotlinx.collections.immutable.persistentListOf

internal object CollectiblesMainPreviewData {

    val default = CollectiblesMainUM(
        title = stringReference("Gacha"),
        balance = CollectiblesMainUM.BalanceUM(
            fiatStyled = annotatedReference(buildAnnotatedString { append("$ 2,700.52") }),
            fiatPlain = stringReference("$ 2,700.52"),
            crypto = stringReference("2,500.52 USDC"),
        ),
        actions = persistentListOf(
            actionButton(icon = Icons.ic_arrow_down_24, title = "Add funds", isEnabled = true),
            actionButton(icon = Icons.ic_arrow_up_24, title = "Withdrawal", isEnabled = false),
        ),
        collection = CollectiblesMainUM.CollectionUM(
            subtitle = stringReference("My collection"),
            title = stringReference("25 cards"),
            onClick = {},
        ),
        menu = menu(isShown = false),
        isBalanceRefreshing = false,
        onBackClick = {},
    )

    val zero = default.copy(
        balance = CollectiblesMainUM.BalanceUM(
            fiatStyled = annotatedReference(buildAnnotatedString { append("$ 0.00") }),
            fiatPlain = stringReference("$ 0.00"),
            crypto = stringReference("0.00 USDC"),
        ),
        collection = null,
    )

    val menuShown = default.copy(menu = menu(isShown = true))

    private fun menu(isShown: Boolean) = CollectiblesMainUM.MenuUM(
        items = persistentListOf(
            menuItem(id = "activity_log", icon = Icons.ic_clock_20, title = "Activity log"),
            menuItem(id = "deliveries", icon = Icons.ic_globe_20, title = "Deliveries"),
            menuItem(id = "how_it_works", icon = Icons.ic_info_20, title = "How it works", hasDividerAbove = true),
            menuItem(id = "get_assistance", icon = Icons.ic_mail_20, title = "Get assistance"),
            menuItem(
                id = "close_account",
                icon = Icons.ic_trash_bin_20,
                title = "Close account",
                hasDividerAbove = true,
            ),
        ),
        isShown = isShown,
        onMenuClick = {},
        onDismiss = {},
    )

    private fun actionButton(icon: ImageVector, title: String, isEnabled: Boolean) = TangemButtonUM(
        text = stringReference(title),
        tangemIconUM = TangemIconUM.Icon(
            imageVector = icon,
            tintReference = { TangemTheme.colors3.icon.primary },
        ),
        type = TangemButtonType.Secondary,
        isEnabled = isEnabled,
        onClick = {},
    )

    private fun menuItem(id: String, icon: ImageVector, title: String, hasDividerAbove: Boolean = false) =
        CollectiblesMainUM.MenuItemUM(
            id = id,
            icon = icon,
            title = stringReference(title),
            hasDividerAbove = hasDividerAbove,
            onClick = {},
        )
}