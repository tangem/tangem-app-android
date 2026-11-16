package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.res.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_wallet_plus_20
import com.tangem.core.ui.test.AddWalletSheetTestTags
import com.tangem.features.introduction.impl.ui.state.AddWalletUM
import com.tangem.features.introduction.impl.ui.state.HaveWalletUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.core.ui.R as CoreUiR

private val AddWalletOptionTestTags = WalletOptionTestTags(
    cardOrRing = AddWalletSheetTestTags.CARD_OR_RING_ROW,
    socialLogin = AddWalletSheetTestTags.SOCIAL_LOGIN_ROW,
    seedPhrase = AddWalletSheetTestTags.SEED_PHRASE_ROW,
    importWallet = AddWalletSheetTestTags.IMPORT_WALLET_ROW,
)

@Composable
internal fun AddWalletSheet(config: TangemBottomSheetConfig, onCollapseEnd: () -> Unit) {
    WalletOptionsBottomSheet(
        config = config,
        optionsTitle = resourceReference(R.string.user_wallet_add_wallet),
        forceDarkTheme = false,
        isMaterialEnabled = true,
        containerColor = null,
        optionTestTags = AddWalletOptionTestTags,
        onCollapseEnd = onCollapseEnd,
    )
}

@Composable
internal fun AddWalletContent(state: AddWalletUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        SocialLoginProgressHost(progress = state.socialLoginProgress)
        state.socialLoginConsent?.let { SocialLoginConsentScreen(state = it) }
    }
    state.sheetConfig?.let { AddWalletSheet(config = it, onCollapseEnd = state.onSheetCollapsed) }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddWalletOptionsLightPreview() {
    TangemThemePreviewRedesign(isDark = false) { AddWalletOptionsPreviewContent() }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AddWalletOptionsDarkPreview() {
    TangemThemePreviewRedesign(isDark = true) { AddWalletOptionsPreviewContent() }
}

@Composable
private fun AddWalletOptionsPreviewContent() {
    OptionsStep(
        step = HaveWalletUM.Step.Options(
            cardOrRing = previewOption(
                icon = WalletOptionUM.Icon.Tinted(Icons.ic_logo_tangem_20),
                titleRes = R.string.introduction_create_wallet_hardware_title,
                descriptionRes = R.string.introduction_create_wallet_hardware_description,
            ),
            socialLogin = previewOption(
                icon = WalletOptionUM.Icon.Image(CoreUiR.drawable.img_google_32),
                titleRes = R.string.introduction_i_have_wallet_sign_in_google_title,
                descriptionRes = R.string.introduction_create_wallet_social_description,
            ),
            seedPhrase = previewOption(
                icon = WalletOptionUM.Icon.Tinted(Icons.ic_wallet_plus_20),
                titleRes = R.string.introduction_create_wallet_seed_phrase_title,
                descriptionRes = R.string.introduction_create_wallet_seed_phrase_description,
            ),
            importWallet = previewOption(
                icon = WalletOptionUM.Icon.Tinted(Icons.ic_arrow_down_20),
                titleRes = R.string.hw_import_existing_wallet,
                descriptionRes = R.string.introduction_create_wallet_seed_phrase_description,
            ),
        ),
    )
}

private fun previewOption(icon: WalletOptionUM.Icon, titleRes: Int, descriptionRes: Int) = WalletOptionUM(
    icon = icon,
    title = resourceReference(titleRes),
    description = resourceReference(descriptionRes),
    onClick = {},
)
// endregion Preview