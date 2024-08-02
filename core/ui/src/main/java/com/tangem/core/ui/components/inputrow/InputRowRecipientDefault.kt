package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * Read only version of [InputRowRecipient].
 * [Input Row Recipient](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-826&mode
 * =design&t=IQ5lBJEkFGU4WSvi-4)
 *
 * @param title title reference
 * @param value recipient address
 * @param modifier composable modifier
 * @param titleColor title color
 * @param showDivider show divider
 * @see InputRowRecipient for editable version
 */
@Composable
fun InputRowRecipientDefault(
    title: TextReference,
    value: String,
    modifier: Modifier = Modifier,
    titleColor: Color = TangemTheme.colors.text.secondary,
    showDivider: Boolean = false,
) {
    DividerContainer(
        modifier = modifier,
        showDivider = showDivider,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = titleColor,
            )
            Row(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing8),
            ) {
                IdentIcon(
                    address = value,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clip(RoundedCornerShape(TangemTheme.dimens.radius18))
                        .size(TangemTheme.dimens.size36)
                        .background(TangemTheme.colors.background.tertiary),
                )
                Text(
                    text = value,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens.spacing12)
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                )
            }
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowRecipientPreview() {
    TangemThemePreview {
        InputRowRecipientDefault(
            value = "0x391316d97a07027a0702c8A002c8A0C25d8470",
            title = TextReference.Res(R.string.send_recipient),
            showDivider = true,
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        )
    }
}
//endregion