package com.tangem.common.ui.notifications

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.notifications.CloseableIconButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.ForceDarkTheme
import com.tangem.core.ui.res.TangemTheme

private const val GRADIENT_START_COLOR = 0xFF252934
private const val GRADIENT_END_COLOR = 0xFF12141E
private const val GRADIENT_OFFSET_X = 164f
private const val GRADIENT_OFFSET_Y = 39f
private const val GRADIENT_RADIUS = 82f

@Composable
fun CreatePaymentAccountNotification(
    onClick: () -> Unit,
    onCloseClick: () -> Unit,
    @DrawableRes image: Int,
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(GRADIENT_START_COLOR), Color(GRADIENT_END_COLOR)),
                    center = Offset(GRADIENT_OFFSET_X, GRADIENT_OFFSET_Y),
                    radius = GRADIENT_RADIUS,
                ),
            )
            .clickable(onClick = onClick),
    ) {
        Image(
            modifier = Modifier.size(78.dp),
            painter = painterResource(id = image),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .padding(start = 78.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                .align(Alignment.CenterStart),
        ) {
            Text(
                modifier = Modifier.padding(end = TangemTheme.dimens.size32),
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.constantWhite,
            )
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        CloseableIconButton(
            onClick = onCloseClick,
            modifier = Modifier.align(alignment = Alignment.TopEnd),
            iconTint = TangemTheme.colors.icon.inactive,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun CreatePaymentAccountNotification_Preview() {
    ForceDarkTheme {
        CreatePaymentAccountNotification(
            onClick = {},
            onCloseClick = {},
            image = R.drawable.img_tangem_pay_visa_banner,
            title = resourceReference(R.string.tangempay_onboarding_banner_title),
            subtitle = resourceReference(R.string.tangempay_onboarding_banner_description),
        )
    }
}