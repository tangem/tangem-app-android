package com.tangem.tangemtest._arch.structure.base

/**
[REDACTED_AUTHOR]
 */
interface DataHolder<D> {
    var viewModel: D?
}

interface Payload {
    val payload: MutableMap<String, Any?>
}

interface ItemListHolder<I> {
    fun setItems(list: MutableList<I>)
    fun getItems(): MutableList<I>
    fun addItem(item: I)
    fun removeItem(item: I)
    fun clear()
}