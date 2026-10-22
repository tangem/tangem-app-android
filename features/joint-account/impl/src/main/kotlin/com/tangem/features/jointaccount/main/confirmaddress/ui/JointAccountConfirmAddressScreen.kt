package com.tangem.features.jointaccount.main.confirmaddress.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.jointaccount.main.confirmaddress.ui.state.JointAccountConfirmAddressUM
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountConfirmAddressScreen(state: JointAccountConfirmAddressUM, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary),
    ) {
        var topBarHeightPx by remember { mutableIntStateOf(0) }
        var footerHeightPx by remember { mutableIntStateOf(0) }
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val footerHeight = with(density) { footerHeightPx.toDp() }
        val bottomNavHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .background(color = TangemTheme.colors3.bg.primary)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = topBarHeight + 12.dp, bottom = footerHeight + 12.dp + bottomNavHeight),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResourceSafe(CoreUiR.string.joint_account_confirm_shared_address),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResourceSafe(CoreUiR.string.joint_account_confirm_account_subtitle),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            Spacer(Modifier.height(24.dp))
            AddressCard(address = state.address, onShareClick = state.onShareClick)
            Spacer(Modifier.height(24.dp))
            InfoItem(
                icon = Icons.ic_users_20,
                titleRes = CoreUiR.string.joint_account_confirm_address_compare_title,
                subtitleRes = CoreUiR.string.joint_account_confirm_address_compare_subtitle,
            )
            Spacer(Modifier.height(20.dp))
            InfoItem(
                icon = Icons.ic_checkmark_20,
                titleRes = CoreUiR.string.joint_account_confirm_address_everyone_title,
                subtitleRes = CoreUiR.string.joint_account_confirm_address_everyone_subtitle,
            )
        }

        Footer(
            state = state,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .onSizeChanged { footerHeightPx = it.height },
        )

        TangemTopNavigation(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { topBarHeightPx = it.height },
            startButton = { TangemButton.Back(onClick = state.onBackClick) },
        )
    }
}

@Composable
private fun AddressCard(address: String?, onShareClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickableSingle(onClick = onShareClick)
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResourceSafe(CoreUiR.string.joint_account_confirm_address_label),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = address.orEmpty(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.tertiary)
                .clickable(enabled = address != null, onClick = onShareClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_share_android_20,
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = stringResourceSafe(CoreUiR.string.common_share),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun InfoItem(icon: ImageVector, titleRes: Int, subtitleRes: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(horizontal = 8.dp)) {
        Icon(
            imageVector = icon,
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResourceSafe(titleRes),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResourceSafe(subtitleRes),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun Footer(state: JointAccountConfirmAddressUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            modifier = Modifier.matchParentSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.joint_account_confirm_address_mismatch_button),
                onClick = state.onMismatchClick,
            )
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.joint_account_confirm_address_confirm_button),
                iconEnd = state.buttonIcon,
                isEnabled = state.address != null,
                isLoading = state.isConfirming,
                onClick = state.onConfirmClick,
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 874)
@Preview(name = "Dark", showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JointAccountConfirmAddressScreen_Preview() {
    TangemThemePreviewRedesign {
        JointAccountConfirmAddressScreen(
            state = JointAccountConfirmAddressUM(
                address = "0xBef7B368aac4e6752A9cE0xBef7B36A9cEVef7B2336",
                buttonIcon = TangemIconUM.Icon(iconRes = CoreUiR.drawable.ic_tangem_24),
                isConfirming = false,
                onShareClick = {},
                onMismatchClick = {},
                onConfirmClick = {},
                onBackClick = {},
            ),
        )
    }
}