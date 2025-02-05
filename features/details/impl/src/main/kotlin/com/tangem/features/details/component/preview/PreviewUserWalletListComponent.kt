package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay

internal class PreviewUserWalletListComponent : UserWalletListComponent {

    private val previewState = mutableStateOf(
        UserWalletListUM(
            userWallets = persistentListOf(
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_1".encodeToByteArray()),
                    name = stringReference("My Wallet"),
                    information = getInformation(cardCount = 1),
                    balance = UserWalletItemUM.Balance.Loading,
                    imageUrl = "",
                    isEnabled = true,
                    onClick = {},
                ),
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_2".encodeToByteArray()),
                    name = stringReference("Old wallet"),
                    information = getInformation(cardCount = 2),
                    balance = UserWalletItemUM.Balance.Loading,
                    imageUrl = "",
                    isEnabled = true,
                    onClick = {},
                ),
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_3".encodeToByteArray()),
                    name = stringReference("Multi Card"),
                    information = getInformation(cardCount = 3),
                    balance = UserWalletItemUM.Balance.Loading,
                    imageUrl = "",
                    isEnabled = false,
                    onClick = {},
                ),
            ),
            addNewWalletText = resourceReference(R.string.user_wallet_list_add_button),
            isWalletSavingInProgress = false,
            onAddNewWalletClick = {},
        ),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by previewState

        UserWalletListBlock(state = state, modifier = modifier)

        LaunchedEffect(key1 = this) {
            delay(timeMillis = 2_000)

            previewState.value = state.copy(
                userWallets = state.userWallets.map {
                    it.copy(
                        balance = UserWalletItemUM.Balance.Loaded(
                            value = "1.000 BTC",
                            isFlickering = true,
                        ),
                    )
                }.toImmutableList(),
            )
        }
    }

    private fun getInformation(cardCount: Int): TextReference {
        return TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = cardCount,
            formatArgs = wrappedList(cardCount),
        )
    }
}