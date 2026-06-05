package com.tangem.features.tangempay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_success_24
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

private const val BG_YELLOW_COLOR = 0x52DFAF12

@Suppress("MagicNumber")
@Composable
internal fun TangemPayChangePinCodeSuccessScreenV2(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(192.dp)
                .drawBehind {
                    val w = size.width
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(BG_YELLOW_COLOR),
                                Color.Transparent,
                            ),
                            center = Offset(w / 2f, -w * .1f),
                            radius = w + w * .2f,
                            tileMode = TileMode.Clamp,
                        ),
                    )
                },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp),
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.ic_success_24,
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = null,
            )
            SpacerH(TangemTheme.dimens2.x4)
            Text(
                modifier = Modifier.testTag(TangemPayTestTags.PIN_SUCCESS_TITLE),
                text = stringResourceSafe(R.string.tangempay_card_details_change_pin_success_title),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                modifier = Modifier.testTag(TangemPayTestTags.PIN_SUCCESS_DESCRIPTION),
                text = stringResourceSafe(R.string.tangempay_card_details_change_pin_success_description),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            SpacerHMax()
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TangemTheme.dimens2.x3),
                onClick = onClose,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_close),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        TangemPayChangePinCodeSuccessScreenV2(onClose = {})
    }
}