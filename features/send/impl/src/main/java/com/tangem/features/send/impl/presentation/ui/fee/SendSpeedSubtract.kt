package com.tangem.features.send.impl.presentation.ui.fee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun SendSpeedSubtract(
    receivingAmount: StateFlow<String>,
    isSubtract: StateFlow<Boolean>,
    onSelectClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = isSubtract.collectAsStateWithLifecycle()
    val footer = receivingAmount.collectAsStateWithLifecycle()

    val footerText = if (isSelected.value) {
        stringResource(R.string.send_amount_substract_footer, footer.value)
    } else {
        null
    }

    FooterContainer(
        footer = footerText,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action)
                .padding(
                    vertical = TangemTheme.dimens.spacing16,
                    horizontal = TangemTheme.dimens.spacing20,
                ),
        ) {
            Text(
                text = stringResource(R.string.send_amount_substract),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(end = TangemTheme.dimens.spacing12),
            )
            TangemSwitch(
                checked = isSelected.value,
                onCheckedChange = onSelectClick,
            )
        }
    }
}
