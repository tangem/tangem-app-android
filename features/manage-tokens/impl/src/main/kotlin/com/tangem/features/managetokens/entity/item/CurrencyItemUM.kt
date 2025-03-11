package com.tangem.features.managetokens.entity.item

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class CurrencyItemUM {

    abstract val id: ManagedCryptoCurrency.ID
    abstract val name: String
    abstract val symbol: String
    abstract val icon: CurrencyIconState

    data class Basic(
        override val id: ManagedCryptoCurrency.ID,
        override val name: String,
        override val symbol: String,
        override val icon: CurrencyIconState,
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
        override val id: ManagedCryptoCurrency.ID,
        override val name: String,
        override val symbol: String,
        override val icon: CurrencyIconState,
        val onRemoveClick: () -> Unit,
    ) : CurrencyItemUM()

    class Loading(val index: Int) : CurrencyItemUM() {

        override val id: ManagedCryptoCurrency.ID = ManagedCryptoCurrency.ID(value = "loading_$index")
        override val name: String = "loading"
        override val symbol: String = "loading"
        override val icon: CurrencyIconState = CurrencyIconState.Loading
    }

    data object SearchNothingFound : CurrencyItemUM() {
        override val id: ManagedCryptoCurrency.ID = ManagedCryptoCurrency.ID(value = "not found text")
        override val name: String = "content not found"
        override val symbol: String = "content not found"
        override val icon: CurrencyIconState = CurrencyIconState.Loading
    }
}