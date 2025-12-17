package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.R
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.inputrow.InputRowImageBase
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme

private const val DISABLED_ALPHA = 0.6F

@Composable
internal fun TangemPayExposedDeviceState(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier
            .clip(RoundedCornerShape(size = TangemTheme.dimens.radius14))
            .background(TangemTheme.colors.background.primary)
            .alpha(DISABLED_ALPHA),
        enabled = false,
        onClick = {},
    ) {
        InputRowImageBase(
            modifier = Modifier
                .padding(
                    all = TangemTheme.dimens.spacing12,
                ),
            subtitle = resourceReference(R.string.tangempay_payment_account),
            caption = resourceReference(R.string.tangem_pay_rooted_device_subtitle),
            subtitleColor = TangemTheme.colors.text.primary1,
            captionColor = TangemTheme.colors.text.tertiary,
            iconResWebp = R.drawable.img_visa_36,
            endIconTint = TangemTheme.colors.icon.warning,
        )
    }
}