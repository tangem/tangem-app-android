package com.tangem.tap.features.tokens.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.loadCurrenciesIcon
import com.tangem.tap.common.extensions.show
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.domain.tokens.getTokensName
import com.tangem.tap.features.tokens.redux.CurrencyListItem
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemCurrencySubtitleBinding
import com.tangem.wallet.databinding.ItemPopularTokenBinding
import java.util.*

class CurrenciesAdapter : ListAdapter<CurrencyListItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    var addedCurrencies: CardCurrencies? = null

    private var unfilteredList = listOf<CurrencyListItem>()

    private val currentlyAddedItems = mutableListOf<CurrencyListItem>()

    private var vhOnItemAddListener: ((CurrencyListItem) -> Unit) = {
        currentlyAddedItems.add(it)
        onItemAddListener?.invoke()
    }
    private var onItemAddListener: VoidCallback? = null

    fun submitUnfilteredList(list: List<CurrencyListItem>) {
        unfilteredList = list
        submitList(list)
    }

    fun setOnItemAddListener(itemAddListener: VoidCallback) {
        onItemAddListener = itemAddListener
        if (currentlyAddedItems.isNotEmpty()) itemAddListener()
    }

    fun getAddedItems(): List<CurrencyListItem> {
        return currentlyAddedItems.toList()
    }

    override fun getItemViewType(position: Int): Int {
        return when (currentList[position]) {
            is CurrencyListItem.TitleListItem -> 0
            is CurrencyListItem.BlockchainListItem, is CurrencyListItem.TokenListItem -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> TitleViewHolder(
                binding = ItemCurrencySubtitleBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )
            1 -> CurrenciesViewHolder(
                binding = ItemPopularTokenBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false),
                onItemAddListener = vhOnItemAddListener
            )
            else -> CurrenciesViewHolder(
                binding = ItemPopularTokenBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false),
                onItemAddListener = vhOnItemAddListener
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = currentList[position]
        if (holder is TitleViewHolder && listItem is CurrencyListItem.TitleListItem) {
            holder.bind(listItem)
        } else if (holder is CurrenciesViewHolder) {
            holder.bind(listItem, addedCurrencies, currentlyAddedItems)
        }
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<CurrencyListItem>() {
        override fun areContentsTheSame(
            oldItem: CurrencyListItem, newItem: CurrencyListItem,
        ) = oldItem == newItem

        override fun areItemsTheSame(
            oldItem: CurrencyListItem, newItem: CurrencyListItem,
        ) = oldItem == newItem
    }

    fun filter(query: CharSequence?) {
        val list = mutableListOf<CurrencyListItem>()

        if (!query.isNullOrEmpty()) {
            store.dispatch(TokensAction.OpenAllTokens)
            val queryNormalized = query.toString().toLowerCase(Locale.US)
            list.addAll(
                unfilteredList.filter { element ->
                    when (element) {
                        is CurrencyListItem.BlockchainListItem -> {
                            element.blockchain.currency.toLowerCase(Locale.US)
                                .contains(queryNormalized) ||
                                    element.blockchain.fullName.toLowerCase(Locale.US)
                                        .contains(queryNormalized)

                        }
                        is CurrencyListItem.TokenListItem -> {
                            element.token.name.toLowerCase(Locale.US)
                                .contains(queryNormalized) ||
                                    element.token.symbol.toLowerCase(Locale.US)
                                        .contains(queryNormalized)
                        }
                        is CurrencyListItem.TitleListItem -> true
                    }
                })
        } else {
            list.addAll(unfilteredList)
        }
        submitList(list)
    }

    class CurrenciesViewHolder(
        private val binding: ItemPopularTokenBinding,
        private val onItemAddListener: ((CurrencyListItem) -> Unit),
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            currency: CurrencyListItem,
            addedCurrencies: CardCurrencies?,
            currentlyAddedItems: MutableList<CurrencyListItem>
        ) {
            when (currency) {
                is CurrencyListItem.BlockchainListItem -> {
                    val addedBlockchains = addedCurrencies?.blockchains ?: listOf()
                    val currentlyAdded =
                        currentlyAddedItems.filterIsInstance<CurrencyListItem.BlockchainListItem>()
                    bindBlockchain(currency, addedBlockchains, currentlyAdded)
                }
                is CurrencyListItem.TokenListItem -> {
                    val addedTokens = addedCurrencies?.tokens ?: listOf()
                    val currentlyAdded =
                        currentlyAddedItems.filterIsInstance<CurrencyListItem.TokenListItem>()
                    bindToken(currency, addedTokens, currentlyAdded)
                }
            }
        }

        private fun bindBlockchain(
            currency: CurrencyListItem.BlockchainListItem,
            addedBlockchains: List<Blockchain>,
            currentlyAdded: List<CurrencyListItem.BlockchainListItem>
        ) = with(binding) {
            val blockchain = currency.blockchain
            Picasso.get().loadCurrenciesIcon(ivCurrency, tvTokenLetter, null, blockchain)

            tvCurrencyName.text = blockchain.fullName
            tvCurrencySymbol.text = blockchain.currency
            btnAddToken.setOnClickListener {
                onItemAddListener.invoke(currency)
                currency.isAdded = true
                modifyAddTokenButton(currency)
            }

            val isAddedBefore = addedBlockchains.contains(blockchain)
            val isCurrentlyAdded = currentlyAdded.any { it.blockchain == blockchain }
            currency.isAdded = isAddedBefore || isCurrentlyAdded
            modifyAddTokenButton(currency)
        }

        private fun bindToken(
            currency: CurrencyListItem.TokenListItem,
            addedTokens: List<Token>,
            currentlyAdded: List<CurrencyListItem.TokenListItem>
        ) = with(binding) {
            val token = currency.token
            Picasso.get()
                .loadCurrenciesIcon(ivCurrency, tvTokenLetter, token, token.blockchain)

            tvCurrencyName.text = token.name
            tvCurrencySymbol.text = tvCurrencySymbol.getString(
                R.string.token_symbol_address_format,
                token.symbol, token.contractAddress
            )
            btnAddToken.setOnClickListener {
                onItemAddListener.invoke(currency)
                currency.isAdded = true
                modifyAddTokenButton(currency)
            }

            val isAddedBefore = addedTokens.any { it == token }
            val isCurrentlyAdded = currentlyAdded.any { it.token == token }
            currency.isAdded = isAddedBefore || isCurrentlyAdded

            modifyAddTokenButton(currency)
        }

        private fun modifyAddTokenButton(currency: CurrencyListItem) = with(binding) {
            if (currency.isLocked) {
                btnAddToken.setText(R.string.common_add)
                btnAddToken.isEnabled = false
            } else {
                val text = if (currency.isAdded) R.string.add_token_added else R.string.common_add
                btnAddToken.setText(text)
                btnAddToken.isEnabled = !currency.isAdded
            }
        }
    }

    class TitleViewHolder(val binding: ItemCurrencySubtitleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(title: CurrencyListItem.TitleListItem) = with(binding) {
            if (title.blockchain != null) {
                val subtitle = root.getString(title.titleResId, title.blockchain.getTokensName())
                tvSubtitle.text = subtitle.uppercase()
                clSubtitleContainer.setOnClickListener {
                    val rotation = if (title.isContentShown) -90f else 90f
                    store.dispatch(
                        TokensAction.ToggleShowTokensForBlockchain(
                            title.isContentShown, title.blockchain
                        )
                    )
                    ivToggleSublistVisibility.animate().rotation(rotation).withEndAction {
                        showArrow(title.isContentShown)
                    }
                }
                showArrow(title.isContentShown)
            } else {
                tvSubtitle.text = root.getString(title.titleResId).uppercase()
                ivToggleSublistVisibility.hide()
            }
        }

        private fun showArrow(isContentShown: Boolean) = with(binding) {
            ivToggleSublistVisibility.show()
            val drawable = if (isContentShown) {
                R.drawable.ic_arrow_angle_down
            } else {
                R.drawable.ic_arrow_angle_right
            }
            ivToggleSublistVisibility.setImageResource(drawable)
            ivToggleSublistVisibility.rotation = 0f
        }
    }
}

