package com.tangem.features.onramp.hottokens.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.tokenlist.TokenListItem
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds2.accountrow.Section
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onramp.hottokens.entity.HotCryptoUM
import com.tangem.features.onramp.impl.R
import kotlinx.collections.immutable.ImmutableList

/**
 * Hot crypto
 *
 * @param state    state
 * @param modifier modifier
 */
@Composable
internal fun HotCrypto(state: State<HotCryptoUM>, modifier: Modifier = Modifier) {
    val rows = state.value.rows
    if (rows.isNotEmpty()) {
        HotCryptoRows(rows = rows, modifier = modifier)
        return
    }
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
                        backgroundColor = TangemTheme.colors3.bg.secondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HotCryptoRows(rows: ImmutableList<TangemTokenRow.State>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemAccountRow.Section(
            title = resourceReference(R.string.tokens_list_hot_crypto_header),
            modifier = Modifier.roundedShapeItemDecoration(
                currentIndex = 0,
                lastIndex = rows.size,
                addDefaultPadding = false,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            ),
        )
        rows.fastForEachIndexed { index, row ->
            key(row.id) {
                TangemTokenRow(
                    state = row,
                    modifier = Modifier.roundedShapeItemDecoration(
                        currentIndex = index + 1,
                        lastIndex = rows.size,
                        addDefaultPadding = false,
                        backgroundColor = TangemTheme.colors3.bg.secondary,
                    ),
                )
            }
        }
    }
}