package com.tangem.features.walletconnect.connections.components

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.walletconnect.connections.model.WcSelectWalletModel
import com.tangem.features.walletconnect.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class WcSelectWalletComponent(
    appComponentContext: AppComponentContext,
    private val params: WcSelectWalletParams,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val model: WcSelectWalletModel = getOrCreateModel(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        WcSelectWalletModalBS(
            wallets = state.wallets,
            selectedWalletId = state.selectedUserWalletId,
            onBack = router::pop,
            onDismiss = ::dismiss,
        )
    }

    interface ModelCallback {
        fun onWalletSelected(userWalletId: UserWalletId)
    }

    data class WcSelectWalletParams(
        val selectedWalletId: UserWalletId,
        val callback: ModelCallback,
        val onDismiss: () -> Unit,
    )
}

@Composable
private fun WcSelectWalletModalBS(
    wallets: ImmutableList<UserWalletItemUM>,
    selectedWalletId: UserWalletId,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (wallets.isEmpty()) return

    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onBack,
        containerColor = TangemTheme.colors.background.primary,
        title = {
            TangemModalBottomSheetTitle(
                title = stringReference("Choose networks"),
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onBack,
            )
        },
        content = {
            WcSelectWalletContent(
                modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                wallets = wallets,
                selectedWalletId = selectedWalletId,
            )
        },
    )
}

@Composable
private fun WcSelectWalletContent(
    wallets: ImmutableList<UserWalletItemUM>,
    selectedWalletId: UserWalletId,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        wallets.fastForEach { state ->
            key(state.id) {
                val baseModifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = state.onClick)
                val itemModifier = if (state.id == selectedWalletId) {
                    baseModifier.border(
                        width = 1.dp,
                        color = TangemTheme.colors.text.accent,
                        shape = RoundedCornerShape(14.dp),
                    )
                } else {
                    baseModifier
                }
                UserWalletItem(
                    modifier = itemModifier,
                    state = state,
                    blockColors = TangemBlockCardColors.copy(
                        containerColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                    ),
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSelectWalletContent_Preview() {
    TangemThemePreview {
        TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
            containerColor = TangemTheme.colors.background.primary,
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TangemBottomSheetConfigContent.Empty,
            ),
            title = {
                TangemModalBottomSheetTitle(
                    title = stringReference("Choose wallet"),
                    onEndClick = {},
                    endIconRes = R.drawable.ic_close_24,
                )
            },
            content = {
                WcSelectWalletContent(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    selectedWalletId = UserWalletId("user_wallet_1".encodeToByteArray()),
                    wallets = persistentListOf(
                        UserWalletItemUM(
                            id = UserWalletId("user_wallet_1".encodeToByteArray()),
                            name = stringReference("Tangem 2.0"),
                            information = getInformation(42),
                            balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                            isEnabled = true,
                            onClick = {},
                        ),
                        UserWalletItemUM(
                            id = UserWalletId("user_wallet_2".encodeToByteArray()),
                            name = stringReference("Tangem White"),
                            information = getInformation(24),
                            balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                            isEnabled = true,
                            onClick = {},
                        ),
                        UserWalletItemUM(
                            id = UserWalletId("user_wallet_3".encodeToByteArray()),
                            name = stringReference("Bitcoin"),
                            information = getInformation(1),
                            balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                            isEnabled = true,
                            onClick = {},
                        ),
                        UserWalletItemUM(
                            id = UserWalletId("user_wallet_4".encodeToByteArray()),
                            name = stringReference("Tangem 1.0"),
                            information = getInformation(21),
                            balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                            isEnabled = true,
                            onClick = {},
                        ),
                        UserWalletItemUM(
                            id = UserWalletId("user_wallet_4".encodeToByteArray()),
                            name = stringReference("Tangem 1.0"),
                            information = UserWalletItemUM.Information.Loading,
                            balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                            isEnabled = true,
                            onClick = {},
                        ),
                        UserWalletItemUM(
                            id = UserWalletId("user_wallet_4".encodeToByteArray()),
                            name = stringReference("Tangem 1.0"),
                            information = UserWalletItemUM.Information.Failed,
                            balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                            isEnabled = true,
                            onClick = {},
                        ),
                    ),
                )
            },
        )
    }
}

private fun getInformation(tokenCount: Int): UserWalletItemUM.Information.Loaded {
    val text = TextReference.PluralRes(
        id = R.plurals.card_label_token_count,
        count = tokenCount,
        formatArgs = wrappedList(tokenCount),
    )
    return UserWalletItemUM.Information.Loaded(text)
}