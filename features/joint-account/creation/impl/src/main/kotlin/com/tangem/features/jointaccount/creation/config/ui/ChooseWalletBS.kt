package com.tangem.features.jointaccount.creation.config.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.userwallet.CardImage
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.jointaccount.creation.config.ui.state.JointAccountConfigUM
import com.tangem.features.jointaccount.creation.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun ChooseWalletBS(state: JointAccountConfigUM.ChooseWalletUM, modifier: Modifier = Modifier) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.common_choose_wallet),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = state.onDismiss,
            )
        },
        content = {
            WalletList(
                wallets = state.wallets,
                modifier = modifier,
            )
        },
    )
}

@Composable
private fun WalletList(wallets: ImmutableList<JointAccountConfigUM.WalletItemUM>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TangemTheme.colors3.bg.tertiary),
    ) {
        wallets.fastForEach { wallet ->
            WalletRow(wallet = wallet)
        }
    }
}

@Composable
private fun WalletRow(wallet: JointAccountConfigUM.WalletItemUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        onClick = wallet.onClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            CardImage(imageState = wallet.image)
        },
        titleSlot = {
            TangemRowText(text = wallet.name, role = TangemRowTextRole.Title)
        },
        subtitleSlot = {
            TangemRowText(text = wallet.info, role = TangemRowTextRole.Subtitle)
        },
        endSlot = {
            TangemCheckmark(
                checked = wallet.isSelected,
                onCheckedChange = { wallet.onClick() },
            )
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ChooseWalletContent() {
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier.background(TangemTheme.colors3.bg.secondary),
            contentAlignment = Alignment.Center,
        ) {
            WalletList(
                wallets = persistentListOf(
                    JointAccountConfigUM.WalletItemUM(
                        id = "1",
                        name = stringReference(value = "My wallet"),
                        image = UserWalletItemUM.ImageState.MobileWallet,
                        info = stringReference(value = "3 cards • $4,496.75"),
                        isSelected = true,
                        onClick = {},
                    ),
                    JointAccountConfigUM.WalletItemUM(
                        id = "2",
                        name = stringReference(value = "My wallet 2"),
                        image = UserWalletItemUM.ImageState.MobileWallet,
                        info = stringReference(value = "1 card • $128.40"),
                        isSelected = false,
                        onClick = {},
                    ),
                ),
            )
        }
    }
}