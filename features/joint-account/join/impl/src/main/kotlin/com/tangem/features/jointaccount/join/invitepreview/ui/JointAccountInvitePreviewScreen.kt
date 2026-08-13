package com.tangem.features.jointaccount.join.invitepreview.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_16
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.join.impl.R
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM

@Composable
internal fun JointAccountInvitePreviewScreen(state: JointAccountInvitePreviewUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(endButton = { TangemButton.Close(onClick = state.onCloseClick) })
        },
    ) { contentPadding ->
        val accountColor = state.accountIcon.color.getUiColor()
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(440.dp)
                .background(
                    Brush.verticalGradient(colors = listOf(accountColor.copy(alpha = 0.2f), Color.Transparent)),
                ),
        )
        Column(
            modifier = Modifier
                .padding(top = contentPadding.calculateTopPadding())
                .fillMaxSize(),
        ) {
            AccountHeroIcon(
                icon = state.accountIcon,
                accountName = state.accountName,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f),
            )

            InviteInfo(state = state)

            Rows(state = state)

            Footer(
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                onContinueClick = state.onContinueClick,
            )
        }
    }
}

@Composable
private fun AccountHeroIcon(icon: AccountIconUM.CryptoPortfolio, accountName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(icon.color.getUiColor())
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.2f)),
                    ),
                )
                .border(
                    width = 2.dp,
                    color = TangemTheme.colors3.border.primary,
                    shape = RoundedCornerShape(40.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon.value == CryptoPortfolioIcon.Icon.Letter) {
                Text(
                    text = accountName.firstOrNull()?.uppercase().orEmpty(),
                    style = TangemTheme.typography3.display.medium,
                    color = TangemTheme.colors3.text.staticDark.primary,
                )
            } else {
                Icon(
                    modifier = Modifier.size(60.dp),
                    imageVector = ImageVector.vectorResource(id = icon.value.getResId()),
                    tint = TangemTheme.colors3.icon.staticDark,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun InviteInfo(state: JointAccountInvitePreviewUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = titleText(accountName = state.accountName),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.secondary,
        )

        Text(
            text = explanationText(state = state),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun titleText(accountName: String): AnnotatedString {
    val text = stringResourceSafe(R.string.joint_account_join_title, accountName)

    return buildAnnotatedString {
        append(text)

        val start = text.indexOf(accountName)
        if (start >= 0 && accountName.isNotEmpty()) {
            addStyle(
                style = SpanStyle(color = TangemTheme.colors3.text.primary),
                start = start,
                end = start + accountName.length,
            )
        }
    }
}

@Composable
private fun explanationText(state: JointAccountInvitePreviewUM): AnnotatedString {
    val highlightedPart = stringResourceSafe(
        R.string.joint_account_join_subtitle_value,
        state.requiredToSign.toString(),
        state.totalMembers.toString(),
    )
    val text = stringResourceSafe(R.string.joint_account_join_subtitle, highlightedPart)

    return buildAnnotatedString {
        append(text)

        val start = text.indexOf(highlightedPart)
        if (start >= 0 && highlightedPart.isNotEmpty()) {
            addStyle(
                style = SpanStyle(
                    color = TangemTheme.colors3.text.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
                start = start,
                end = start + highlightedPart.length,
            )
        }
    }
}

@Composable
private fun Rows(state: JointAccountInvitePreviewUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        CreatorRow(
            creatorName = state.creatorName,
            hasDivider = state.wallet != null,
            onInfoClick = state.onCreatorInfoClick,
        )

        state.wallet?.let { wallet -> WalletRow(wallet = wallet) }
    }
}

@Composable
private fun CreatorRow(creatorName: String, hasDivider: Boolean, onInfoClick: () -> Unit) {
    TangemRow(
        divider = hasDivider,
        onClick = onInfoClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.joint_account_invite_members_creator),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TangemRowText(text = stringReference(value = creatorName), role = TangemRowTextRole.Value)

                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.ic_info_20,
                    tint = TangemTheme.colors3.icon.secondary,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun WalletRow(wallet: JointAccountInvitePreviewUM.WalletUM) {
    TangemRow(
        onClick = wallet.onClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(text = resourceReference(R.string.common_wallet), role = TangemRowTextRole.Title)
        },
        valueSlot = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_key_card_20),
                    tint = TangemTheme.colors3.icon.secondary,
                    contentDescription = null,
                )

                TangemRowText(text = stringReference(value = wallet.name), role = TangemRowTextRole.Value)
            }
        },
        endSlot = {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.ic_chevron_down_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        },
    )
}

@Composable
private fun Footer(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        variant = TangemButton.Variant.Primary,
        size = TangemButton.Size.X12,
        text = resourceReference(R.string.common_continue),
        onClick = onContinueClick,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountInvitePreviewScreen(
    @PreviewParameter(JointAccountInvitePreviewStateProvider::class) state: JointAccountInvitePreviewUM,
) {
    TangemThemePreviewRedesign {
        JointAccountInvitePreviewScreen(state = state)
    }
}

private class JointAccountInvitePreviewStateProvider : CollectionPreviewParameterProvider<JointAccountInvitePreviewUM>(
    collection = listOf(
        createPreviewState(
            wallet = JointAccountInvitePreviewUM.WalletUM(name = "My wallet", onClick = {}),
        ),
        createPreviewState(wallet = null),
    ),
)

private fun createPreviewState(wallet: JointAccountInvitePreviewUM.WalletUM?): JointAccountInvitePreviewUM {
    return JointAccountInvitePreviewUM(
        accountName = "Family savings",
        accountIcon = AccountIconUM.CryptoPortfolio(
            value = CryptoPortfolioIcon.Icon.Safe,
            color = CryptoPortfolioIcon.Color.VitalGreen,
        ),
        requiredToSign = 2,
        totalMembers = 5,
        creatorName = "Igor Sinyak",
        wallet = wallet,
        chooseWallet = null,
        onCreatorInfoClick = {},
        onContinueClick = {},
        onCloseClick = {},
    )
}