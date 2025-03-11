package com.tangem.features.onramp.hottokens.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.hottokens.entity.HotCryptoUM

/**
 * Hot crypto
 *
 * @param state    state
 * @param modifier modifier
 */
@Composable
internal fun HotCrypto(state: State<HotCryptoUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        state.value.items.fastForEachIndexed { index, item ->
            key(item.id) {
                TokenListItem(
                    state = item,
                    isBalanceHidden = false, // because token doesn't contain balance info
                    modifier = Modifier.roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = state.value.items.lastIndex,
                        addDefaultPadding = false,
                        backgroundColor = TangemTheme.colors.background.primary,
                    ),
                )
            }
        }
    }
}