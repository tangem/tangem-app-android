package com.tangem.features.details.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.navigation.DummyRouter
import com.tangem.core.navigation.url.DummyUrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.ui.DetailsScreen
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.features.mcp.api.McpFeatureToggles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

internal class PreviewDetailsComponent : DetailsComponent {

    private val previewBlocks = runBlocking {
        ItemsBuilder(
            router = DummyRouter(),
            hotWalletRestrictionManager = object : HotWalletRestrictionManager {
                override fun isCreationEnabled(): StateFlow<Boolean> = MutableStateFlow(true)
                override fun isCreationEnabledSync(): Boolean = true
                override suspend fun toggleCreationEnabled() = Unit
            },
            mcpFeatureToggles = object : McpFeatureToggles {
                override val isMcpEnabled: Boolean = true
            },
        ).buildAll(
            isWalletConnectAvailable = true,
            hasAnyMobileWallet = true,
            userWalletId = UserWalletId(""),
            onSupportClick = {},
            onBuyClick = {},
        )
    }

    private val previewFooter = DetailsFooterUM(
        socials = SocialsBuilder(DummyUrlOpener()).buildAll(),
        appVersion = "1.0.0-preview",
    )

    val previewState = DetailsUM(
        items = previewBlocks,
        footer = previewFooter,
        selectFeedbackEmailTypeBSConfig = TangemBottomSheetConfig.Empty,
        selectContactSupportTypeBSConfig = TangemBottomSheetConfig.Empty,
        popBack = { /* no-op */ },
    )

    @Composable
    override fun Content(modifier: Modifier) {
        DetailsScreen(
            modifier = modifier,
            state = previewState,
            userWalletListBlockContent = PreviewUserWalletListComponent(),
        )
    }
}