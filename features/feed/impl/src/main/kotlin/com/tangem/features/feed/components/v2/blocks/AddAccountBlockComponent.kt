package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_16
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.walletsettings.component.AddAccountTypeComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.analytics.FeedBlocksAnalytics
import com.tangem.features.feed.components.v2.blocks.model.AddAccountBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedShortcutCard
import kotlinx.coroutines.flow.StateFlow

/**
 * Shortcut into account creation and the sheets it walks through: the wallet chooser when more than
 * one wallet can take a new account, then the kind of account to create — see [AddAccountBlockModel]
 * for which wallets are offered and when each sheet is skipped.
 */
internal class AddAccountBlockComponent(
    context: AppComponentContext,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    private val addAccountTypeComponentFactory: AddAccountTypeComponent.Factory,
    private val blocksAnalytics: FeedBlocksAnalytics,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: AddAccountBlockModel = getOrCreateModel()

    override val size: FeedBlock.Size = FeedBlock.Size.Small

    override val isVisible: StateFlow<Boolean> = model.isVisible

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
    ) { config, componentContext ->
        when (config) {
            BottomSheetConfig.WalletSelector -> walletSelectorChild(componentContext)
            is BottomSheetConfig.AccountType -> accountTypeChild(config, componentContext)
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        FeedShortcutCard(
            title = stringResourceSafe(R.string.account_form_title_create),
            onClick = ::onClick,
            modifier = modifier,
            icon = { AddAccountIcon() },
        )
    }

    @Composable
    override fun Overlay() {
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun onClick() {
        blocksAnalytics.onAddAccountClicked()
        model.onClick()
    }

    private fun walletSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true, isDs3Modal = true),
            ),
        )

    private fun accountTypeChild(
        config: BottomSheetConfig.AccountType,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = addAccountTypeComponentFactory.create(
        context = childByContext(componentContext),
        params = AddAccountTypeComponent.Params(
            onCryptoAccountClick = { model.onCryptoAccountClick(config) },
            onJointAccountClick = { model.onJointAccountClick(config) },
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    /** The sheets the block raises, in the order a click walks through them. */
    sealed interface BottomSheetConfig {

        /** Chooser of the wallet the new account goes to; skipped when only one wallet qualifies. */
        data object WalletSelector : BottomSheetConfig

        /**
         * Which kind of account to create in [userWalletId]. The two flags are that wallet's cap per
         * kind — a kind already at its cap is still listed and explains the limit when tapped.
         */
        data class AccountType(
            val userWalletId: UserWalletId,
            val isCryptoAccountLimitReached: Boolean,
            val isJointAccountLimitReached: Boolean,
        ) : BottomSheetConfig
    }

    companion object {
        const val KEY = "add_account"
    }
}

@Composable
private fun AddAccountIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size = 24.dp)
            .clip(shape = CircleShape)
            .background(color = TangemTheme.colors3.bg.accent.blue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.ic_sign_plus_16,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.size(size = 20.dp),
        )
    }
}