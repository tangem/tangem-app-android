package com.tangem.features.onramp.swap.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference

/**
 * Exchange card UI model
 *
[REDACTED_AUTHOR]
 */
internal sealed interface ExchangeCardUM {

    /** Title reference */
    val titleUM: TitleUM

    /** Remove button UI model */
    val removeButtonUM: RemoveButtonUM?

    /**
     * Empty state
     *
     * @property titleUM    title reference
     * @property subtitleReference empty token subtitle reference
     */
    data class Empty(
        override val titleUM: TitleUM,
        val subtitleReference: TextReference,
    ) : ExchangeCardUM {

        override val removeButtonUM: RemoveButtonUM? = null
    }

    /**
     * Filled
     *
     * @property titleUM title reference
     * @property removeButtonUM remove button UI model
     * @property tokenItemState token item state
     */
    data class Filled(
        override val titleUM: TitleUM,
        override val removeButtonUM: RemoveButtonUM?,
        val tokenItemState: TokenItemState,
    ) : ExchangeCardUM

    data class RemoveButtonUM(val onClick: () -> Unit)

    @Immutable
    sealed interface TitleUM {

        data class Text(
            val title: TextReference,
        ) : TitleUM

        data class Account(
            val prefixText: TextReference,
            val name: TextReference,
            val icon: AccountIconUM.CryptoPortfolio,
        ) : TitleUM
    }
}