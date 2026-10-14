package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.cosign.CosignRequestBanner
import com.tangem.feature.wallet.presentation.wallet.state.model.JointOperationBannerUM
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.jointOperationBanners(
    banners: ImmutableList<JointOperationBannerUM>,
    modifier: Modifier = Modifier,
) {
    itemsIndexed(
        items = banners,
        key = { _, item -> item.operationId },
        contentType = { _, _ -> JointOperationBannerUM::class.java },
        itemContent = { index, item ->
            CosignRequestBanner(
                state = item.banner,
                modifier = modifier.padding(top = if (index == 0) 12.dp else 8.dp),
            )
        },
    )
}