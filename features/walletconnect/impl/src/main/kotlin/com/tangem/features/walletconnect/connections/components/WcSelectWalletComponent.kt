package com.tangem.features.walletconnect.connections.components

import android.content.res.Configuration
import androidx.compose.foundation.background
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
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.walletconnect.connections.model.WcAppInfoModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class WcSelectWalletComponent(
    appComponentContext: AppComponentContext,
    private val model: WcAppInfoModel,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        val walletsState by model.walletsUiState.collectAsStateWithLifecycle()
        WcSelectWalletContent(
            modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            wallets = walletsState.wallets,
            selectedWalletId = walletsState.selectedUserWallet.walletId,
        )
    }
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

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WcSelectWalletContent_Preview() {
    TangemThemePreview {
        WcSelectWalletContent(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            selectedWalletId = UserWalletId("user_wallet_1".encodeToByteArray()),
            wallets = persistentListOf(
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_1".encodeToByteArray()),
                    name = stringReference("Tangem 2.0"),
                    information = stringReference("42 tokens"),
                    balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                    isEnabled = true,
                    onClick = {},
                ),
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_2".encodeToByteArray()),
                    name = stringReference("Tangem White"),
                    information = stringReference("24 tokens"),
                    balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                    isEnabled = true,
                    onClick = {},
                ),
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_3".encodeToByteArray()),
                    name = stringReference("Bitcoin"),
                    information = stringReference("1 token"),
                    balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                    isEnabled = true,
                    onClick = {},
                ),
                UserWalletItemUM(
                    id = UserWalletId("user_wallet_4".encodeToByteArray()),
                    name = stringReference("Tangem 1.0"),
                    information = stringReference("21 tokens"),
                    balance = UserWalletItemUM.Balance.Loaded("1 496,34 \$", isFlickering = false),
                    isEnabled = true,
                    onClick = {},
                ),
            ),
        )
    }
}