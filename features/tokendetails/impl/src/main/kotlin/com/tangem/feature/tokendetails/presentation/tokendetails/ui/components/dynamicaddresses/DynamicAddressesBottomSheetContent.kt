package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.HoldToConfirmButton
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.audits.AuditLabel
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.res.R
import com.tangem.core.ui.R as CoreR
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses.DynamicAddressesBottomSheetConfig.DisableFeeState

@Composable
internal fun DynamicAddressesEnableContent(content: DynamicAddressesBottomSheetConfig.Enable) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = CoreR.drawable.ic_dynamic_addresses_bottomsheet_enable_top),
            contentDescription = null,
            modifier = Modifier.size(TangemTheme.dimens.size44),
            tint = TangemTheme.colors.icon.accent,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing12))

        Text(
            text = stringResourceSafe(id = R.string.dynamic_addresses),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))

        Text(
            text = stringResourceSafe(id = R.string.dynamic_addresses_enter_subtitle),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing24))

        FeatureItem(
            iconRes = CoreR.drawable.ic_dynamic_addresses_bottomsheet_flash_24,
            title = stringResourceSafe(id = R.string.dynamic_addresses_enter_features_receving_title),
            description = stringResourceSafe(id = R.string.dynamic_addresses_enter_features_receving_description),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))

        FeatureItem(
            iconRes = CoreR.drawable.ic_dynamic_addresses_bottomsheet_check_24,
            title = stringResourceSafe(id = R.string.dynamic_addresses_enter_features_privacy_title),
            description = stringResourceSafe(id = R.string.dynamic_addresses_enter_features_privacy_description),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing24))

        PrimaryButtonIconEnd(
            text = stringResourceSafe(id = R.string.dynamic_addresses_enter_main_button_title),
            iconResId = content.iconRes,
            onClick = content.onEnableClick,
            modifier = Modifier.fillMaxWidth(),
            showProgress = content.isLoading,
            enabled = !content.isLoading,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))
    }
}

@Composable
internal fun DynamicAddressesDisableWithoutConsolidationContent(
    content: DynamicAddressesBottomSheetConfig.DisableWithoutConsolidation,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DisableHeader()

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing24))

        PrimaryButtonIconEnd(
            text = stringResourceSafe(id = R.string.dynamic_addresses_disable_main_button_title),
            iconResId = null,
            onClick = content.onDisableClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))
    }
}

@Composable
internal fun DynamicAddressesDisableWithConsolidationContent(
    content: DynamicAddressesBottomSheetConfig.DisableWithConsolidation,
) {
    val isConfirmEnabled = content.feeState is DisableFeeState.Content && !content.isSending

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DisableHeader()

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))

        DisableFeeBlock(
            feeState = content.feeState,
            onReadMoreClick = content.onReadMoreClick,
        )

        if (content.feeState is DisableFeeState.Error) {
            Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing12))

            Notification(
                config = NotificationConfig(
                    title = resourceReference(R.string.send_fee_unreachable_error_title),
                    subtitle = resourceReference(R.string.send_fee_unreachable_error_text),
                    iconResId = CoreR.drawable.ic_alert_24,
                    iconTint = NotificationConfig.IconTint.Warning,
                    buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                        text = resourceReference(R.string.warning_button_refresh),
                        onClick = content.onRefreshFee,
                    ),
                ),
                containerColor = TangemTheme.colors.background.action,
            )
        }

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing24))

        if (content.isHoldToConfirm) {
            HoldToConfirmButton(
                text = stringResourceSafe(id = R.string.dynamic_addresses_disable_main_button_title),
                onConfirm = content.onDisableClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = isConfirmEnabled,
                isLoading = content.isSending,
            )
        } else {
            PrimaryButtonIconEnd(
                text = stringResourceSafe(id = R.string.dynamic_addresses_disable_main_button_title),
                iconResId = content.iconRes,
                onClick = content.onDisableClick,
                modifier = Modifier.fillMaxWidth(),
                showProgress = content.isSending,
                enabled = isConfirmEnabled,
            )
        }

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))
    }
}

@Composable
private fun DisableHeader() {
    Icon(
        painter = painterResource(id = CoreR.drawable.ic_dynamic_addresses_bottomsheet_enable_unavailable),
        contentDescription = null,
        modifier = Modifier.size(TangemTheme.dimens.size44),
        tint = TangemTheme.colors.icon.attention,
    )

    Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing12))

    Text(
        text = stringResourceSafe(id = R.string.dynamic_addresses_disable_title),
        style = TangemTheme.typography.h3,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))

    Text(
        text = stringResourceSafe(id = R.string.dynamic_addresses_disable_description),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.secondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DisableFeeBlock(feeState: DisableFeeState, onReadMoreClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .padding(TangemTheme.dimens.spacing12),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResourceSafe(id = R.string.common_network_fee_title),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            when (feeState) {
                is DisableFeeState.Loading -> TextShimmer(
                    style = TangemTheme.typography.body2,
                    modifier = Modifier.size(
                        width = TangemTheme.dimens.size80,
                        height = TangemTheme.dimens.spacing16,
                    ),
                )
                is DisableFeeState.Content -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AuditLabel(
                        state = AuditLabelUM(
                            text = stringReference(feeState.feeSymbol),
                            type = AuditLabelUM.Type.General,
                        ),
                    )
                    Text(
                        text = feeState.fiatFormatted,
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                    )
                }
                is DisableFeeState.Error -> Text(
                    text = "\u2014",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))
        DisableFeeDescription(onReadMoreClick = onReadMoreClick)
    }
}

@Composable
private fun DisableFeeDescription(onReadMoreClick: () -> Unit) {
    val readMoreText = stringResourceSafe(id = R.string.common_read_more)
    val fullText = stringResourceSafe(id = R.string.dynamic_addresses_disable_fee_description)

    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
            append(fullText)
            append(" ")
        }
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "read_more",
                linkInteractionListener = { onReadMoreClick() },
            ),
        ) {
            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                append(readMoreText)
            }
        }
    }

    Text(
        text = annotatedString,
        style = TangemTheme.typography.caption2,
    )
}

@Composable
internal fun DynamicAddressesConflictingCustomTokensContent(
    content: DynamicAddressesBottomSheetConfig.ConflictingCustomTokens,
) {
    ErrorContent(
        titleRes = R.string.dynamic_addresses_error_has_custom_token_title,
        descriptionRes = R.string.dynamic_addresses_error_has_custom_token_description,
        buttonTextRes = R.string.common_got_it,
        onButtonClick = content.onDismissClick,
    )
}

@Composable
internal fun DynamicAddressesServiceUnavailableContent(content: DynamicAddressesBottomSheetConfig.ServiceUnavailable) {
    ErrorContent(
        titleRes = R.string.dynamic_addresses_error_service_unavailable_title,
        descriptionRes = R.string.dynamic_addresses_error_service_unavailable_description,
        buttonTextRes = R.string.common_got_it,
        onButtonClick = content.onDismissClick,
    )
}

@Composable
private fun ErrorContent(titleRes: Int, descriptionRes: Int, buttonTextRes: Int, onButtonClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = CoreR.drawable.ic_dynamic_addresses_bottomsheet_enable_unavailable),
            contentDescription = null,
            modifier = Modifier.size(TangemTheme.dimens.size44),
            tint = TangemTheme.colors.icon.attention,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing12))

        Text(
            text = stringResourceSafe(id = titleRes),
            style = TangemTheme.typography.h3,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing8))

        Text(
            text = stringResourceSafe(id = descriptionRes),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing24))

        PrimaryButton(
            text = stringResourceSafe(id = buttonTextRes),
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))
    }
}

@Composable
private fun FeatureItem(iconRes: Int, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(TangemTheme.dimens.size24),
            tint = TangemTheme.colors.icon.accent,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing4))
            Text(
                text = description,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

// region Previews

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Enable() {
    TangemThemePreview {
        DynamicAddressesEnableContent(
            content = DynamicAddressesBottomSheetConfig.Enable(
                iconRes = null,
                onEnableClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_EnableWithCardScan() {
    TangemThemePreview {
        DynamicAddressesEnableContent(
            content = DynamicAddressesBottomSheetConfig.Enable(
                iconRes = CoreR.drawable.ic_tangem_24,
                onEnableClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DisableWithoutConsolidation() {
    TangemThemePreview {
        DynamicAddressesDisableWithoutConsolidationContent(
            content = DynamicAddressesBottomSheetConfig.DisableWithoutConsolidation(
                onDisableClick = {},
                onReadMoreClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DisableFeeLoading() {
    TangemThemePreview {
        DynamicAddressesDisableWithConsolidationContent(
            content = DynamicAddressesBottomSheetConfig.DisableWithConsolidation(
                feeState = DisableFeeState.Loading,
                onDisableClick = {},
                onRefreshFee = {},
                onReadMoreClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DisableFeeLoaded() {
    TangemThemePreview {
        DynamicAddressesDisableWithConsolidationContent(
            content = DynamicAddressesBottomSheetConfig.DisableWithConsolidation(
                feeState = DisableFeeState.Content(
                    feeSymbol = "BTC",
                    fiatFormatted = "~$0.12",
                ),
                onDisableClick = {},
                onRefreshFee = {},
                onReadMoreClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DisableFeeError() {
    TangemThemePreview {
        DynamicAddressesDisableWithConsolidationContent(
            content = DynamicAddressesBottomSheetConfig.DisableWithConsolidation(
                feeState = DisableFeeState.Error,
                onDisableClick = {},
                onRefreshFee = {},
                onReadMoreClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DisableSending() {
    TangemThemePreview {
        DynamicAddressesDisableWithConsolidationContent(
            content = DynamicAddressesBottomSheetConfig.DisableWithConsolidation(
                feeState = DisableFeeState.Content(
                    feeSymbol = "BTC",
                    fiatFormatted = "~$0.12",
                ),
                isSending = true,
                onDisableClick = {},
                onRefreshFee = {},
                onReadMoreClick = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ConflictingCustomTokens() {
    TangemThemePreview {
        DynamicAddressesConflictingCustomTokensContent(
            content = DynamicAddressesBottomSheetConfig.ConflictingCustomTokens(onDismissClick = {}),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ServiceUnavailable() {
    TangemThemePreview {
        DynamicAddressesServiceUnavailableContent(
            content = DynamicAddressesBottomSheetConfig.ServiceUnavailable(onDismissClick = {}),
        )
    }
}

// endregion