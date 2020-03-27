package com.tangem.tangemtest._arch.structure

/**
[REDACTED_AUTHOR]
 */
interface DataHolder<D> {
    var viewModel: D
}

typealias Payload = MutableMap<String, Any?>

interface PayloadHolder {
    val payload: Payload
}

interface ItemListHolder<I> {
    fun setItems(list: MutableList<I>)
    fun getItems(): MutableList<I>
    fun addItem(item: I)
    fun removeItem(item: I)
    fun clear()
}