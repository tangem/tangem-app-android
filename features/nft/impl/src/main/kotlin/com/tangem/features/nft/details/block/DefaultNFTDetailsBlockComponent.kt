package com.tangem.features.nft.details.block

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.nft.details.block.ui.NFTDetailsBlock
import com.tangem.features.nft.impl.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultNFTDetailsBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTDetailsBlockComponent.Params,
) : NFTDetailsBlockComponent, AppComponentContext by context {

    private val account = params.account

    @Composable
    override fun Content(modifier: Modifier) {
        NFTDetailsBlock(
            assetName = stringReference(params.nftAsset.name.orEmpty()),
            collectionName = stringReference(params.nftCollectionName),
            assetImage = params.nftAsset.media?.imageUrl,
            accountTitleUM = if (account != null && params.isAccountsMode) {
                AccountTitleUM.Account(
                    prefixText = resourceReference(R.string.common_from),
                    name = account.accountName.toUM().value,
                    icon = account.icon.toUM(),
                )
            } else {
                AccountTitleUM.Text(params.walletTitle)
            },
            isSuccessScreen = params.isSuccessScreen,
            networkIconRes = getActiveIconRes(params.nftAsset.network.rawId),
        )
    }

    @AssistedFactory
    interface Factory : NFTDetailsBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTDetailsBlockComponent.Params,
        ): DefaultNFTDetailsBlockComponent
    }
}