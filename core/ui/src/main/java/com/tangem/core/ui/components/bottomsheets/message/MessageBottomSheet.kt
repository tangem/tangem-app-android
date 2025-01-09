package com.tangem.core.ui.components.bottomsheets.message

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.isNullOrEmpty
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun MessageBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { notification: MessageBottomSheetUM ->
        Content(model = notification)
    }
}

@Composable
internal fun Content(model: MessageBottomSheetUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier)

        if (model.iconResId != null) {
            Icon(
                modifier = Modifier.size(48.dp),
                painter = painterResource(model.iconResId),
                tint = TangemTheme.colors.icon.primary1,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!model.title.isNullOrEmpty()) {
                Text(
                    text = model.title.resolveReference(),
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = model.message.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (model.primaryAction != null) {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = model.primaryAction.text.resolveReference(),
                    enabled = model.primaryAction.enabled,
                    onClick = model.primaryAction.onClick,
                )
            }

            if (model.secondaryAction != null) {
                SecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = model.secondaryAction.text.resolveReference(),
                    enabled = model.secondaryAction.enabled,
                    onClick = model.secondaryAction.onClick,
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_NotificationBottomSheet(
    @PreviewParameter(MessageBottomSheetUMPreviewProvider::class) params: MessageBottomSheetUM,
) {
    TangemThemePreview {
        MessageBottomSheet(
            config = TangemBottomSheetConfig(
                content = params,
                isShown = true,
                onDismissRequest = {},
            ),
        )
    }
}

private class MessageBottomSheetUMPreviewProvider : PreviewParameterProvider<MessageBottomSheetUM> {
    val balancesHiddenMessage = MessageBottomSheetUM(
        iconResId = R.drawable.ic_eye_off_outline_24,
        title = resourceReference(R.string.balance_hidden_title),
        message = resourceReference(R.string.balance_hidden_description),
        primaryAction = MessageBottomSheetUM.ActionUM(
            text = resourceReference(R.string.balance_hidden_got_it_button),
            onClick = {},
        ),
        secondaryAction = MessageBottomSheetUM.ActionUM(
            text = resourceReference(R.string.balance_hidden_do_not_show_button),
            onClick = {},
        ),
    )

    override val values: Sequence<MessageBottomSheetUM>
        get() = sequenceOf(
            balancesHiddenMessage,
            balancesHiddenMessage.copy(title = null),
            balancesHiddenMessage.copy(iconResId = null),
            balancesHiddenMessage.copy(primaryAction = null),
            balancesHiddenMessage.copy(secondaryAction = null),
            balancesHiddenMessage.copy(
                primaryAction = null,
                secondaryAction = null,
            ),
            balancesHiddenMessage.copy(
                title = null,
                iconResId = null,
                primaryAction = null,
                secondaryAction = null,
            ),
        )
}
// endregion Preview