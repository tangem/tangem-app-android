package com.tangem.feature.walletsettings.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_user_20
import com.tangem.core.ui.res.generated.icons.ic_users_20
import com.tangem.feature.walletsettings.impl.R

@Composable
internal fun AddAccountTypeBS(
    onCryptoAccountClick: () -> Unit,
    onJointAccountClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                title = resourceReference(R.string.account_form_title_create),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = onDismiss,
            )
        },
        content = {
            AddAccountTypeContent(
                onCryptoAccountClick = onCryptoAccountClick,
                onJointAccountClick = onJointAccountClick,
            )
        },
    )
}

@Composable
private fun AddAccountTypeContent(
    onCryptoAccountClick: () -> Unit,
    onJointAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AccountTypeItem(
            icon = Icons.ic_user_20,
            title = resourceReference(R.string.common_crypto_account),
            subtitle = resourceReference(R.string.add_crypto_account_subtitle),
            onClick = onCryptoAccountClick,
        )

        AccountTypeItem(
            icon = Icons.ic_users_20,
            title = resourceReference(R.string.common_joint_account),
            subtitle = resourceReference(R.string.add_joint_account_subtitle),
            onClick = onJointAccountClick,
        )
    }
}

@Composable
private fun AccountTypeItem(
    icon: ImageVector,
    title: TextReference,
    subtitle: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(color = TangemTheme.colors3.bg.tertiary),
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            TangemIcon(
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = icon,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = TangemTheme.colors3.bg.opaque.secondary)
                    .padding(10.dp),
            )
        },
        titleSlot = {
            TangemRowText(text = title, role = TangemRowTextRole.Title)
        },
        subtitleSlot = {
            TangemRowText(text = subtitle, role = TangemRowTextRole.Subtitle, maxLines = 2)
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AddAccountTypeContent() {
    TangemThemePreviewRedesign {
        AddAccountTypeContent(
            onCryptoAccountClick = {},
            onJointAccountClick = {},
        )
    }
}