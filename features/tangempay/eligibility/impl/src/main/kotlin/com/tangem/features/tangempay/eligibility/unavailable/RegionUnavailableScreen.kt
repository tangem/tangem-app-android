package com.tangem.features.tangempay.eligibility.unavailable

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.common.TangemPayResultScreen

@Composable
internal fun RegionUnavailableScreen(
    onSelectAnotherRegionClick: () -> Unit,
    onJoinWaitlistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO v_rodionov: #[REDACTED_TASK_KEY]
    TangemPayResultScreen(
        modifier = modifier,
        variant = TangemPayResultScreen.Variant.Unavailable,
        title = TextReference.Str("Not available in your region"),
        subtitle = subtitleReference(),
        secondaryButtonText = TextReference.Str("Select another region"),
        onSecondaryButtonClick = onSelectAnotherRegionClick,
        buttonText = TextReference.Str("Join the waitlist"),
        onButtonClick = onJoinWaitlistClick,
    )
}

@Composable
private fun subtitleReference(): TextReference {
    val accentStyle = SpanStyle(color = TangemTheme.colors3.text.primary)

    // TODO v_rodionov: #[REDACTED_TASK_KEY]
    return TextReference.Annotated(
        value = buildAnnotatedString {
            append("We're working on expanding to more regions. ")
            withStyle(accentStyle) { append("Join the waitlist") }
            append(" to be the first to know when it's available")
        },
    )
}

@Preview(widthDp = 402, heightDp = 874, showBackground = true)
@Preview(widthDp = 402, heightDp = 874, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_RegionUnavailableScreen() {
    TangemThemePreviewRedesign {
        RegionUnavailableScreen(onSelectAnotherRegionClick = {}, onJoinWaitlistClick = {})
    }
}