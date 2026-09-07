package com.tangem.features.jointaccount.join.invitepreview.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.jointaccount.impl.R
import com.tangem.features.jointaccount.join.invitepreview.ui.state.JointAccountInvitePreviewUM
import com.tangem.core.ui.R as CoreUiR

/**
 * A dead end: the invite cannot be joined (missing, redeemed, or this wallet already holds a slot). There is
 * nothing to retry, so unlike the rest of the invite-preview flow this screen carries no top bar / close button.
 */
@Composable
internal fun JointAccountInviteInvalidLinkScreen(
    state: JointAccountInvitePreviewUM.InvalidLink,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Warning,
        title = resourceReference(R.string.joint_account_join_invalid_link_title),
        description = resourceReference(R.string.joint_account_join_invalid_link_subtitle),
        asset = { TangemStatusIcon(kind = TangemStatusIcon.Kind.Warning, modifier = Modifier.size(40.dp)) },
        footer = {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.common_got_it),
                onClick = state.onGotItClick,
            )
        },
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountInviteInvalidLinkScreen() {
    TangemThemePreviewRedesign {
        JointAccountInviteInvalidLinkScreen(state = JointAccountInvitePreviewUM.InvalidLink(onGotItClick = {}))
    }
}