package com.tangem.features.onramp.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.TangemTopAppBarHeight
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onramp.component.ConfirmResidencyComponent
import com.tangem.features.onramp.component.preview.PreviewConfirmResidencyComponent
import com.tangem.features.onramp.entity.ConfirmResidencyUM
import com.tangem.features.onramp.impl.R

@Composable
internal fun ConfirmResidencyBottomSheet(config: TangemBottomSheetConfig, content: @Composable (Modifier) -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = config,
        addBottomInsets = true,
        containerColor = TangemTheme.colors.background.tertiary,
        title = { _ -> Title() },
        content = {
            val contentModifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing16)
                .padding(bottom = TangemTheme.dimens.spacing16)
                .fillMaxWidth()
                .wrapContentHeight()

            content(contentModifier)
        },
    )
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    TangemTopAppBar(
        modifier = modifier,
        title = resourceReference(R.string.onramp_residency_bottomsheet_title),
        titleAlignment = Alignment.CenterHorizontally,
        height = TangemTopAppBarHeight.BOTTOM_SHEET,
    )
}

@Composable
internal fun ConfirmResidencyBottomSheetContent(model: ConfirmResidencyUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CountryContent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens.spacing24),
            name = model.country,
            flagUrl = model.countryFlagUrl,
            isCountrySupported = model.isCountrySupported,
        )
        SecondaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = model.secondaryButtonConfig.text.resolveReference(),
            onClick = model.secondaryButtonConfig.onClick,
        )
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TangemTheme.dimens.spacing12),
            text = model.primaryButtonConfig.text.resolveReference(),
            onClick = model.primaryButtonConfig.onClick,
        )
    }
}

@Composable
private fun CountryContent(name: String, flagUrl: String, isCountrySupported: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val flagSize = with(LocalDensity.current) { TangemTheme.dimens.size36.roundToPx() }
        SubcomposeAsyncImage(
            modifier = Modifier.size(TangemTheme.dimens.size36),
            model = ImageRequest.Builder(LocalContext.current)
                .size(size = flagSize)
                .data(flagUrl)
                .memoryCacheKey(flagUrl + flagSize)
                .crossfade(true)
                .allowHardware(false)
                .build(),
            loading = { CircleShimmer() },
            contentDescription = null,
        )

        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
            text = name,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
            maxLines = 1,
        )

        Text(
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing6),
            text = stringResource(
                id = if (isCountrySupported) {
                    R.string.onramp_residency_bottomsheet_country_subtitle
                } else {
                    R.string.onramp_residency_bottomsheet_country_not_supported
                },
            ),
            color = if (isCountrySupported) TangemTheme.colors.text.tertiary else TangemTheme.colors.text.warning,
            style = TangemTheme.typography.body2,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_ConfirmResidencyBottomSheet(
    @PreviewParameter(ConfirmResidencyComponentPreviewProvider::class) component: ConfirmResidencyComponent,
) {
    TangemThemePreview {
        component.BottomSheet()
    }
}

private class ConfirmResidencyComponentPreviewProvider : PreviewParameterProvider<ConfirmResidencyComponent> {
    override val values: Sequence<ConfirmResidencyComponent>
        get() = sequenceOf(
            PreviewConfirmResidencyComponent(),
            PreviewConfirmResidencyComponent(
                initialState = ConfirmResidencyUM(
                    country = "Russia",
                    countryFlagUrl = "https://hatscripts.github.io/circle-flags/flags/ru.svg",
                    isCountrySupported = false,
                    primaryButtonConfig = ConfirmResidencyUM.ActionButtonConfig(
                        onClick = {},
                        text = stringReference("Close"),
                    ),
                    secondaryButtonConfig = ConfirmResidencyUM.ActionButtonConfig(
                        onClick = {},
                        text = stringReference("Change"),
                    ),
                ),
            ),
        )
}