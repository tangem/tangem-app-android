package com.tangem.common.ui.expressStatus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.tangem.common.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.inputrow.InputRowApprox
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Suppress("LongParameterList")
@Composable
fun ExpressEstimate(
    timestamp: TextReference,
    fromTokenIconState: CurrencyIconState,
    toTokenIconState: CurrencyIconState,
    fromCryptoAmount: TextReference,
    fromCryptoSymbol: String,
    toCryptoAmount: TextReference,
    toCryptoSymbol: String,
    fromFiatAmount: TextReference?,
    toFiatAmount: TextReference?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                    top = TangemTheme.dimens.spacing14,
                    bottom = TangemTheme.dimens.spacing2,
                ),
        ) {
            Text(
                text = stringResource(id = R.string.express_estimated_amount),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            Text(
                text = timestamp.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        InputRowApprox(
            leftIcon = fromTokenIconState,
            leftTitle = fromCryptoAmount,
            leftSubtitle = fromFiatAmount,
            leftTitleEllipsisOffset = fromCryptoSymbol.length,
            rightIcon = toTokenIconState,
            rightTitle = toCryptoAmount,
            rightSubtitle = toFiatAmount,
            rightTitleEllipsisOffset = toCryptoSymbol.length,
        )
    }
}