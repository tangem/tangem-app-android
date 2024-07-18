package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.rows.model.ChainRowUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class CurrencyItemUM {

    abstract val id: String
    abstract val model: ChainRowUM

    data class Basic(
        override val id: String,
        override val model: ChainRowUM,
        val networks: NetworksUM,
        val onExpandClick: () -> Unit,
    ) : CurrencyItemUM() {

        @Immutable
        sealed class NetworksUM {

            data object Collapsed : NetworksUM()

            data class Expanded(
                val networks: ImmutableList<CurrencyNetworkUM>,
            ) : NetworksUM()
        }
    }

    data class Custom(
        override val id: String,
        override val model: ChainRowUM,
        val onRemoveClick: () -> Unit,
    ) : CurrencyItemUM()
}
