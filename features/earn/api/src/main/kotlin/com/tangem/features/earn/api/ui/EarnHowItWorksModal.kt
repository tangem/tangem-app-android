@file:Suppress("MagicNumber")

package com.tangem.features.earn.api.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.persistentListOf

/** The "How it works" sheet, shown while it is in composition. "Got it", close, back and the scrim call [onDismiss]. */
@Composable
fun EarnHowItWorksModal(state: EarnHowItWorksUM, onDismiss: () -> Unit) {
    TangemModal<EarnHowItWorksUM>(
        config = TangemBottomSheetConfig(isShown = true, onDismissRequest = onDismiss, content = state),
        title = { content -> TangemModal.TopNavigation(title = content.title, onClose = onDismiss) },
    ) { content ->
        EarnHowItWorksContent(state = content, onDismiss = onDismiss)
    }
}

@Composable
fun EarnHowItWorksContent(state: EarnHowItWorksUM, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            state.steps.forEachIndexed { index, step ->
                Step(number = index + 1, step = step)
                HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
            }
        }
        TangemFooter(
            modifier = Modifier.padding(top = 24.dp),
            windowInsets = TangemFooter.ModalWindowInsets,
            fadeEnabled = false,
            extraTop = state.footnote?.let { footnote ->
                {
                    Text(
                        text = footnote.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        ) {
            TangemButtonGroup(
                orientation = TangemButtonGroup.Orientation.Horizontal,
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.onLearnMoreClick?.let { onLearnMoreClick ->
                    TangemButton(
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X12,
                        text = resourceReference(R.string.common_learn_more),
                        onClick = onLearnMoreClick,
                    )
                }
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_got_it),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun Step(number: Int, step: EarnHowItWorksUM.Step) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(color = TangemTheme.colors3.bg.inverse, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.inverse.primary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = step.title.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = step.description.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EarnHowItWorksContentPreview() {
    TangemThemePreviewRedesign {
        EarnHowItWorksContent(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            state = EarnHowItWorksUM(
                title = stringReference("How it works"),
                steps = persistentListOf(
                    EarnHowItWorksUM.Step(
                        title = stringReference("You choose the amount"),
                        description = stringReference("Pick how much ETH to lend. The rest stays in your wallet."),
                    ),
                    EarnHowItWorksUM.Step(
                        title = stringReference("Your money starts working"),
                        description = stringReference("Any interest you receive is compounded. Nothing to manage"),
                    ),
                    EarnHowItWorksUM.Step(
                        title = stringReference("Take it back anytime"),
                        description = stringReference("Withdrawals are usually instant, subject to Aave liquidity."),
                    ),
                ),
                footnote = stringReference("Contract audited by Pessimistic"),
                onLearnMoreClick = {},
            ),
            onDismiss = {},
        )
    }
}
// endregion