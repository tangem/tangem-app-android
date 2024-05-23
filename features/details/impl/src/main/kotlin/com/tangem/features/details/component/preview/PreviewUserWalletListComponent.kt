package com.tangem.features.details.component.preview

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.wallets.models.UserWalletId
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

internal class PreviewUserWalletListComponent : UserWalletListComponent {

    private val previewState = UserWalletListComponent.State(
        userWallets = persistentListOf(
            UserWalletListComponent.State.UserWallet(
                id = UserWalletId("user_wallet_1".encodeToByteArray()),
                name = "My Wallet",
                information = getInformation(3, "4 496,75 $"),
                imageResId = R.drawable.ill_card_wallet_2_211_343,
                onClick = {},
            ),
            UserWalletListComponent.State.UserWallet(
                id = UserWalletId("user_wallet_2".encodeToByteArray()),
                name = "Old wallet",
                information = getInformation(3, "4 496,75 $"),
                imageResId = R.drawable.ill_card_note_eth_211_343,
                onClick = {},
            ),
            UserWalletListComponent.State.UserWallet(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = "Multi Card",
                information = getInformation(3, "4 496,75 $"),
                imageResId = R.drawable.ill_card_note_bnb_211_343,
                onClick = {},
            ),
        ),
        addNewWalletText = resourceReference(R.string.user_wallet_list_add_button),
        onAddNewWalletClick = {},
    )

    private val state = MutableStateFlow(value = previewState)

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    override fun View(modifier: Modifier) {
        TODO("Will be implemented in AND-7107")
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
