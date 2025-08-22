package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.domain.blockaid.models.transaction.ValidationResult
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.walletconnect.impl.R

@Composable
internal fun WcTransactionRequestButtons(
    activeButtonText: TextReference,
    isLoading: Boolean,
    validationResult: ValidationResult?,
    onDismiss: () -> Unit,
    onClickActiveButton: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    WcCommonButtons(
        onDismiss = onDismiss,
        modifier = modifier,
        primaryButton = {
            // Before change, make sure you are align with WcSendTransactionModel::onSign
            when (validationResult) {
                ValidationResult.UNSAFE,
                ValidationResult.WARNING,
                -> PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = stringResourceSafe(R.string.common_continue),
                    onClick = onClickActiveButton,
                    showProgress = isLoading,
                    enabled = enabled,
                )
                ValidationResult.SAFE,
                ValidationResult.FAILED_TO_VALIDATE,
                null,
                -> PrimaryButtonIconEnd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = activeButtonText.resolveReference(),
                    onClick = onClickActiveButton,
                    iconResId = R.drawable.ic_tangem_24,
                    showProgress = isLoading,
                    enabled = enabled,
                )
            }
        },
    )
}

@Composable
internal fun WcSimpleConfirmButtons(
    activeButtonText: TextReference,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onClickActiveButton: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    WcCommonButtons(
        onDismiss = onDismiss,
        modifier = modifier,
        primaryButton = {
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = activeButtonText.resolveReference(),
                onClick = onClickActiveButton,
                showProgress = isLoading,
                enabled = enabled,
            )
        },
    )
}

@Composable
internal fun WcCommonButtons(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    primaryButton: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8)) {
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = stringResourceSafe(R.string.common_cancel),
            onClick = onDismiss,
        )
        primaryButton()
    }
}