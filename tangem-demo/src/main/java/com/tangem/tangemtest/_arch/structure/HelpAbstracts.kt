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

    fun get(key: String): Any? = payload[key]
    fun remove(key: String): Any? = payload.remove(key)
    fun set(key: String, value: Any?) {
        payload[key] = value
    }
}

interface ItemListHolder<I> {
    fun setItems(list: MutableList<I>)
    fun getItems(): MutableList<I>
    fun addItem(item: I)
    fun removeItem(item: I)
    fun clear()
}