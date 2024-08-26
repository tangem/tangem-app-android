package com.tangem.features.markets.portfolio.impl.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.state.AddToPortfolioBSContentUM
import com.tangem.features.markets.portfolio.impl.ui.state.SelectNetworkUM
import kotlinx.collections.immutable.persistentListOf

internal class PreviewAddToPortfolioBSContentProvider : PreviewParameterProvider<AddToPortfolioBSContentUM> {

    private val blockchainRow = BlockchainRowUM(
        name = "Etherium 3",
        type = "TEST",
        iconResId = R.drawable.ic_eth_16,
        isMainNetwork = false,
        isSelected = false,
    )

    val userWallet = UserWalletItemUM(
        id = UserWalletId("1"),
        name = stringReference("Wallet 1"),
        information = stringReference("3 cards, 10,123$"),
        imageUrl = "",
        isEnabled = true,
        endIcon = UserWalletItemUM.EndIcon.Arrow,
        onClick = {},
    )

    override val values: Sequence<AddToPortfolioBSContentUM>
        get() = sequenceOf(
            AddToPortfolioBSContentUM(
                selectedWallet = userWallet,
                selectNetworkUM = SelectNetworkUM(
                    tokenId = "etherium",
                    tokenName = "Etherium",
                    tokenCurrencySymbol = "ETH",
                    networks = persistentListOf(
                        blockchainRow.copy(
                            type = "MAIN",
                            isMainNetwork = true,
                            isSelected = true,
                        ),
                        blockchainRow,
                        blockchainRow,
                    ),
                    onNetworkSwitchClick = { _, _ -> },
                    iconUrl = null,
                ),
                isScanCardNotificationVisible = true,
                continueButtonEnabled = true,
                onContinueButtonClick = {},
                walletSelectorConfig = TangemBottomSheetConfig(
                    isShow = false,
                    onDismissRequest = {},
                    content = TangemBottomSheetConfigContent.Empty,
                ),
                onWalletClick = {},
            ),
            AddToPortfolioBSContentUM(
                selectedWallet = userWallet,
                selectNetworkUM = SelectNetworkUM(
                    tokenId = "etherium",
                    tokenName = "Etherium",
                    tokenCurrencySymbol = "ETH",
                    networks = persistentListOf(
                        blockchainRow.copy(
                            type = "MAIN",
                            isMainNetwork = true,
                            isSelected = true,
                        ),
                        *Array(25) { blockchainRow },
                    ),

                    onNetworkSwitchClick = { _, _ -> },
                    iconUrl = null,
                ),
                isScanCardNotificationVisible = true,
                continueButtonEnabled = false,
                onContinueButtonClick = {},
                walletSelectorConfig = TangemBottomSheetConfig(
                    isShow = false,
                    onDismissRequest = {},
                    content = TangemBottomSheetConfigContent.Empty,
                ),
                onWalletClick = {},
            ),
        )
}
