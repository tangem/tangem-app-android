package com.tangem.tap.features.tokens.ui.adapters

import android.view.LayoutInflater
import android.view.View
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
import com.tangem.tap.features.tokens.redux.CurrencyListItem
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.item_currency_subtitle.view.*
import kotlinx.android.synthetic.main.item_popular_token.view.*
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
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_currency_subtitle, parent, false)
            )
            1 -> CurrenciesViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_popular_token, parent, false),
                vhOnItemAddListener
            )
            else -> CurrenciesViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_popular_token, parent, false),
                vhOnItemAddListener
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
        private val view: View,
        private val onItemAddListener: ((CurrencyListItem) -> Unit),
    ) : RecyclerView.ViewHolder(view) {

        fun bind(
            currency: CurrencyListItem,
            addedCurrencies: CardCurrencies?,
            currentlyAddedItems: MutableList<CurrencyListItem>
        ) {
            when (currency) {
                is CurrencyListItem.BlockchainListItem -> {
                    val addedBlockchains = addedCurrencies?.blockchains ?: listOf()
                    val currentlyAdded = currentlyAddedItems.filterIsInstance<CurrencyListItem.BlockchainListItem>()
                    bindBlockchain(currency, addedBlockchains, currentlyAdded)
                }
                is CurrencyListItem.TokenListItem -> {
                    val addedTokens = addedCurrencies?.tokens ?: listOf()
                    val currentlyAdded = currentlyAddedItems.filterIsInstance<CurrencyListItem.TokenListItem>()
                    bindToken(currency, addedTokens, currentlyAdded)
                }
            }
        }

        private fun bindBlockchain(
            currency: CurrencyListItem.BlockchainListItem,
            addedBlockchains: List<Blockchain>,
            currentlyAdded: List<CurrencyListItem.BlockchainListItem>
        ) {
            val blockchain = currency.blockchain
            Picasso.get().loadCurrenciesIcon(view.iv_currency, view.tv_token_letter, null, blockchain)

            view.tv_currency_name.text = blockchain.fullName
            view.tv_currency_symbol.text = blockchain.currency
            view.btn_add_token.setOnClickListener {
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
        ) {
            val token = currency.token
            Picasso.get().loadCurrenciesIcon(view.iv_currency, view.tv_token_letter, token, token.blockchain)

            view.tv_currency_name.text = token.name
            view.tv_currency_symbol.text = token.symbol
            view.btn_add_token.setOnClickListener {
                onItemAddListener.invoke(currency)
                currency.isAdded = true
                modifyAddTokenButton(currency)
            }

            val isAddedBefore = addedTokens.any { it == token }
            val isCurrentlyAdded = currentlyAdded.any { it.token == token }
            currency.isAdded = isAddedBefore || isCurrentlyAdded

            modifyAddTokenButton(currency)
        }

        private fun modifyAddTokenButton(currency: CurrencyListItem) {
            if (currency.isLock) {
                view.btn_add_token.setText(R.string.common_add)
                view.btn_add_token.isEnabled = false
            } else {
                val text = if (currency.isAdded) R.string.add_token_added else R.string.common_add
                view.btn_add_token.setText(text)
                view.btn_add_token.isEnabled = !currency.isAdded
            }
        }
    }

    class TitleViewHolder(val view: View) :
        RecyclerView.ViewHolder(view) {
        fun bind(title: CurrencyListItem.TitleListItem) {
            view.tv_subtitle.text = view.getString(title.titleResId).uppercase()

            if (title.blockchain != null) {
                view.cl_subtitle_container.setOnClickListener {
                    val rotation = if (title.isContentShown) -90f else 0f
                    store.dispatch(TokensAction.ToggleShowTokensForBlockchain(
                        title.isContentShown, title.blockchain
                    ))
                    view.iv_toggle_sublist_visibility.animate().rotation(rotation)
                }
                view.iv_toggle_sublist_visibility.show()
                view.iv_toggle_sublist_visibility.setImageResource(R.drawable.ic_arrow_angle_down)
            } else {
                view.iv_toggle_sublist_visibility.hide()
            }
        }
    }
}

