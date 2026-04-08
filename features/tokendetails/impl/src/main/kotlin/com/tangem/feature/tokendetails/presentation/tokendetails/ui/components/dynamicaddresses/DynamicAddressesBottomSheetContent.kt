package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses

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
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.res.R
import com.tangem.core.ui.R as CoreR

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

        PrimaryButton(
            text = stringResourceSafe(id = R.string.dynamic_addresses_enter_main_button_title),
            onClick = content.onEnableClick,
            modifier = Modifier.fillMaxWidth(),
            showProgress = content.isLoading,
            enabled = !content.isLoading,
            // TODO add card icon when isCardScanRequired
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))
    }
}

@Composable
internal fun DynamicAddressesUnavailableContent(content: DynamicAddressesBottomSheetConfig.Unavailable) {
    ErrorContent(
        titleRes = R.string.dynamic_addresses_error_has_custom_token_title,
        descriptionRes = R.string.dynamic_addresses_error_has_custom_token_description,
        buttonTextRes = R.string.common_got_it,
        onButtonClick = content.onGotItClick,
    )
}

@Composable
internal fun DynamicAddressesServiceUnavailableContent(content: DynamicAddressesBottomSheetConfig.ServiceUnavailable) {
    ErrorContent(
        titleRes = R.string.dynamic_addresses_error_service_unavailable_title,
        descriptionRes = R.string.dynamic_addresses_error_service_unavailable_description,
        buttonTextRes = R.string.common_got_it,
        onButtonClick = content.onGotItClick,
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
            tint = TangemTheme.colors.icon.warning,
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