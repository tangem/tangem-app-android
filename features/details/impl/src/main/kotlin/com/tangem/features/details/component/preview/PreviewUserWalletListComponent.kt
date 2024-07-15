package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.impl.R
import com.tangem.features.details.ui.UserWalletListBlock
import kotlinx.collections.immutable.persistentListOf

internal class PreviewUserWalletListComponent : UserWalletListComponent {

    private val previewState = UserWalletListUM(
        userWallets = persistentListOf(
            UserWalletListUM.UserWalletUM(
                id = UserWalletId("user_wallet_1".encodeToByteArray()),
                name = stringReference("My Wallet"),
                information = getInformation(3, "4 496,75 $"),
                imageUrl = "",
                isEnabled = true,
                onClick = {},
            ),
            UserWalletListUM.UserWalletUM(
                id = UserWalletId("user_wallet_2".encodeToByteArray()),
                name = stringReference("Old wallet"),
                information = getInformation(3, "4 496,75 $"),
                imageUrl = "",
                isEnabled = true,
                onClick = {},
            ),
            UserWalletListUM.UserWalletUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(3, "4 496,75 $"),
                imageUrl = "",
                isEnabled = false,
                onClick = {},
            ),
        ),
        addNewWalletText = resourceReference(R.string.user_wallet_list_add_button),
        isWalletSavingInProgress = false,
        onAddNewWalletClick = {},
    )

    @Composable
    override fun Content(modifier: Modifier) {
        UserWalletListBlock(state = previewState, modifier = modifier)
    }

    private fun getInformation(cardCount: Int, totalBalance: String): TextReference {
        val t1 = TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = cardCount,
            formatArgs = wrappedList(cardCount),
        )
        val divider = stringReference(value = " • ")
        val t2 = stringReference(totalBalance)

        return TextReference.Combined(wrappedList(t1, divider, t2))
    }
}
