package com.tangem.common.ui.amountScreen.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.extensions.shareText
import com.tangem.core.ui.res.TangemTheme
import com.tangem.common.ui.R
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun SendDoneButtons(
    txUrl: String,
    onExploreClick: () -> Unit,
    onShareClick: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible && txUrl.isNotBlank(),
        modifier = modifier,
        enter = slideInVertically().plus(fadeIn()),
        exit = slideOutVertically().plus(fadeOut()),
        label = "Animate show sent state buttons",
    ) {
        Row(modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12)) {
            SecondaryButtonIconStart(
                text = stringResource(id = R.string.common_explore),
                iconResId = R.drawable.ic_web_24,
                onClick = onExploreClick,
                modifier = Modifier.weight(1f),
            )
            SpacerW12()
            SecondaryButtonIconStart(
                text = stringResource(id = R.string.common_share),
                iconResId = R.drawable.ic_share_24,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.shareText(txUrl)
                    onShareClick()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 328)
@Preview(showBackground = true, widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SendDoneButtons_Preview() {
    TangemThemePreview {
        SendDoneButtons(
            txUrl = "txUrl",
            onShareClick = {},
            onExploreClick = {},
            isVisible = true,
        )
    }
}
