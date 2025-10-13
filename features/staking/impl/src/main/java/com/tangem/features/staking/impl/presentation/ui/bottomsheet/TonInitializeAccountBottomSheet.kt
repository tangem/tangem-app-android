package com.tangem.features.staking.impl.presentation.ui.bottomsheet

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.features.staking.impl.presentation.ui.block.StakingFeeBlock
import java.math.BigDecimal

@Composable
internal fun TonInitializeAccountBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<TonInitializeAccountBottomSheetConfig>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content ->
        Column(
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing16,
                )
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpacerH(height = 16.dp)
            Image(
                painter = painterResource(id = R.drawable.img_ton_22),
                modifier = Modifier.size(56.dp),
                contentDescription = null,
            )

            SpacerH(height = 24.dp)

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = content.title.resolveReference(),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )

            SpacerH(height = 16.dp)

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = content.message.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
                textAlign = TextAlign.Center,
            )

            SpacerH(height = 24.dp)

            StakingFeeBlock(content.feeState)

            SpacerH(height = 12.dp)

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = content.footer.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )

            SpacerH(height = 20.dp)

            TangemButton(
                text = stringResourceSafe(R.string.common_activate),
                icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_tangem_24),
                onClick = content.onButtonClick,
                colors = TangemButtonsDefaults.primaryButtonColors,
                showProgress = content.feeState is FeeState.Loading || content.isButtonLoading,
                enabled = content.isButtonEnabled,
                size = TangemButtonSize.WideAction,
                textStyle = TangemTheme.typography.subtitle1,
                modifier = Modifier.fillMaxWidth(),
            )

            SpacerH(height = 8.dp)
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TonInitializeAccountBottomSheetContent() {
    TangemThemePreview {
        TonInitializeAccountBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TonInitializeAccountBottomSheetConfig(
                    title = resourceReference(R.string.staking_account_initialization_title),
                    message = resourceReference(R.string.staking_account_initialization_message),
                    footer = resourceReference(R.string.staking_account_initialization_footer),
                    onButtonClick = {},
                    isButtonEnabled = true,
                    isButtonLoading = false,
                    feeState = FeeState.Content(
                        fee = Fee.Common(
                            amount = Amount(
                                currencySymbol = "MATIC",
                                value = BigDecimal(0.159806),
                                decimals = 18,
                                type = AmountType.Coin,
                            ),
                        ),
                        rate = BigDecimal.ONE,
                        appCurrency = AppCurrency.Default,
                        isFeeApproximate = false,
                        isFeeConvertibleToFiat = true,
                    ),
                ),

            ),
        )
    }
}
// endregion Preview