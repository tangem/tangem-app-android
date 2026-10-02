@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.accountrow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds2.expandable.ExpandableCaptionText
import com.tangem.core.ui.ds2.expandable.ExpandableDot
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cloud_exclamation_20
import com.tangem.utils.StringsSigns

/** Collapsed subtitle: `2 tokens · 2 members`. The separating dot is dropped with the members count. */
@Composable
internal fun AccountRowSubtitle(
    tokensAmount: TextReference?,
    membersAmount: TextReference?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tokensAmount != null) {
            ExpandableCaptionText(text = tokensAmount)
        }
        if (tokensAmount != null && membersAmount != null) {
            ExpandableDot()
        }
        if (membersAmount != null) {
            ExpandableCaptionText(text = membersAmount)
        }
    }
}

/** Collapsed fiat balance, with the optional update-error cloud in front of it. */
@Composable
internal fun AccountRowLargeBalance(
    balance: TextReference,
    modifier: Modifier = Modifier,
    showUpdateWarning: Boolean = false,
    isBalanceHidden: Boolean = false,
    isFlickering: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showUpdateWarning) {
            Icon(
                imageVector = Icons.ic_cloud_exclamation_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            style = TangemTheme.typography3.body.medium.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = TangemTheme.colors3.text.primary,
            ),
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
        )
    }
}

/** Expanded heading balance — the same number one step down the type scale, in secondary. */
@Composable
internal fun AccountRowSmallBalance(
    balance: TextReference,
    modifier: Modifier = Modifier,
    isBalanceHidden: Boolean = false,
    isFlickering: Boolean = false,
) {
    Text(
        text = balance.orMaskWithStars(isBalanceHidden).resolveReference(),
        style = TangemTheme.typography3.caption.medium.applyBladeBrush(
            isEnabled = isFlickering,
            textColor = TangemTheme.colors3.text.secondary,
        ),
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        modifier = modifier,
    )
}

/** The `—` shown instead of a balance when it could not be calculated. */
@Composable
internal fun AccountRowBalanceError(modifier: Modifier = Modifier) {
    Text(
        text = StringsSigns.DASH_SIGN,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        maxLines = 1,
        modifier = modifier,
    )
}