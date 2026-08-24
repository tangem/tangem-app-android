package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_20
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberAvatarUM
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun MemberCardModal(
    avatar: MemberAvatarUM,
    name: TextReference,
    address: TextReference,
    onCopyClick: () -> Unit,
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
                blurBackground = false,
                endButton = { TangemButton.Close(onClick = onDismiss) },
            )
        },
        content = {
            MemberCardContent(
                avatar = avatar,
                name = name,
                address = address,
                onCopyClick = onCopyClick,
                onCloseClick = onDismiss,
            )
        },
    )
}

@Composable
private fun MemberCardContent(
    avatar: MemberAvatarUM,
    name: TextReference,
    address: TextReference,
    onCopyClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AccountIcon(
                name = stringReference(avatar.monogram),
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Letter,
                    color = avatar.color,
                ),
                size = AccountIconSize.ContactLarge,
            )

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MemberIdentity(name = name, address = address)

                Spacer(Modifier.height(24.dp))

                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X9,
                    text = resourceReference(CoreUiR.string.common_copy_address),
                    iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_copy_20),
                    onClick = onCopyClick,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TangemButton(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_close),
            onClick = onCloseClick,
        )
    }
}

@Composable
private fun MemberIdentity(name: TextReference, address: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = name.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = address.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// region Preview
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MemberCardContentPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            TangemTopNavigation(
                blurBackground = false,
                endButton = { TangemButton.Close(onClick = {}) },
            )
            MemberCardContent(
                avatar = MemberAvatarUM(monogram = "D", color = CryptoPortfolioIcon.Color.CandyGrapeFizz),
                name = stringReference("Danil Kolbasenko"),
                address = stringReference("0xBef7B368aac4e6752A9cE0xBef7B36A9cE"),
                onCopyClick = {},
                onCloseClick = {},
            )
        }
    }
}
// endregion