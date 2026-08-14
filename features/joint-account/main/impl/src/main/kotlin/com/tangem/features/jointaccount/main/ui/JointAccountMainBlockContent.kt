package com.tangem.features.jointaccount.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountRow
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.entity.JointAccountMainUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointAccountMainBlockContent(
    state: JointAccountMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is JointAccountMainUM.Empty -> Unit
        is JointAccountMainUM.Loading -> JointAccountLoading(modifier)
        is JointAccountMainUM.WaitingMembers -> JointAccountWaitingMembers(state, isBalanceHidden, modifier)
        is JointAccountMainUM.Blocked -> JointAccountBlocked(state, isBalanceHidden, modifier)
        is JointAccountMainUM.TemporaryUnavailable -> JointAccountUnavailable(modifier)
    }
}

@Composable
private fun JointAccountWaitingMembers(
    state: JointAccountMainUM.WaitingMembers,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountRow(
        modifier = modifier.jointAccountCard(),
        title = state.title,
        subtitle = state.subtitle,
        icon = state.icon,
        balance = state.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
        priceChange = state.priceChange,
        onClick = state.onClick,
        extraBottom = if (state.canInvite) {
            {
                JointAccountBanner(
                    text = resourceReference(CoreUiR.string.joint_account_invite_members_to_join),
                    onClick = state.onInviteClick,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun JointAccountBlocked(
    state: JointAccountMainUM.Blocked,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountRow(
        modifier = modifier.jointAccountCard(),
        title = state.title,
        subtitle = state.subtitle,
        icon = state.icon,
        balance = state.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
        priceChange = state.priceChange,
        onClick = state.onClick,
        extraBottom = { JointAccountBanner(text = state.warning) },
    )
}

@Composable
private fun JointAccountUnavailable(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.jointAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            AccountIcon(
                name = resourceReference(CoreUiR.string.common_joint_account),
                icon = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Family,
                    color = CryptoPortfolioIcon.Color.DullLavender,
                ),
                size = AccountIconSize.Default,
            )
        },
        titleSlot = {
            Text(
                text = resourceReference(CoreUiR.string.common_joint_account).resolveReference(),
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.body.medium,
            )
        },
        subtitleSlot = {
            Text(
                text = DASH_SIGN,
                color = TangemTheme.colors3.text.tertiary,
                style = TangemTheme.typography3.caption.medium,
            )
        },
    )
}

@Composable
private fun JointAccountLoading(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.jointAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { CircleShimmer(modifier = Modifier.size(40.dp)) },
        titleSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(100.dp),
            )
        },
        subtitleSlot = {
            TextShimmer(
                style = TangemTheme.typography3.caption.medium,
                radius = 10.dp,
                modifier = Modifier.width(44.dp),
            )
        },
        valueSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(80.dp),
            )
        },
    )
}

@Composable
private fun JointAccountBanner(text: TextReference, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TangemTheme.colors3.bg.status.warningSubtle)
            .conditional(onClick != null) { clickableSingle(onClick = requireNotNull(onClick)) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(CoreUiR.drawable.ic_group_24),
            tint = TangemTheme.colors3.icon.status.warning,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text.resolveReference(),
            color = TangemTheme.colors3.text.primary,
            style = TangemTheme.typography3.caption.medium,
        )
        if (onClick != null) {
            Icon(
                modifier = Modifier.size(12.dp),
                painter = painterResource(CoreUiR.drawable.ic_chevron_right_24),
                tint = TangemTheme.colors3.icon.tertiary,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun Modifier.jointAccountCard(): Modifier = this
    .clip(RoundedCornerShape(24.dp))
    .background(TangemTheme.colors3.bg.secondary)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JointAccountMainBlockContent_Preview(
    @PreviewParameter(JointAccountMainPreviewProvider::class) state: JointAccountMainUM,
) {
    TangemThemePreviewRedesign {
        JointAccountMainBlockContent(
            state = state,
            isBalanceHidden = false,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}

private class JointAccountMainPreviewProvider : CollectionPreviewParameterProvider<JointAccountMainUM>(
    collection = listOf(
        JointAccountMainUM.Loading,
        JointAccountMainUM.WaitingMembers(
            title = stringReference("Joint account"),
            subtitle = stringReference("1 of 5 members"),
            icon = previewIcon,
            balance = stringReference("$0.00"),
            priceChange = previewNeutralPriceChange,
            canInvite = true,
            onClick = {},
            onInviteClick = {},
        ),
        JointAccountMainUM.WaitingMembers(
            title = stringReference("Joint account"),
            subtitle = stringReference("3 of 5 members"),
            icon = previewIcon,
            balance = stringReference("$0.00"),
            priceChange = previewNeutralPriceChange,
            canInvite = false,
            onClick = {},
            onInviteClick = {},
        ),
        JointAccountMainUM.Blocked(
            title = stringReference("Family savings"),
            subtitle = stringReference("4 tokens • 5 members"),
            icon = previewIcon,
            balance = stringReference("$0.00"),
            priceChange = previewNeutralPriceChange,
            warning = stringReference("This account is temporarily unavailable"),
            onClick = {},
        ),
        JointAccountMainUM.TemporaryUnavailable,
    ),
)

private val previewIcon = AccountIconUM.CryptoPortfolio(
    value = CryptoPortfolioIcon.Icon.Family,
    color = CryptoPortfolioIcon.Color.CandyGrapeFizz,
)

private val previewNeutralPriceChange = TangemPriceChange.State(
    value = stringReference("0.00%"),
    direction = TangemPriceChange.Direction.Neutral,
)
// endregion