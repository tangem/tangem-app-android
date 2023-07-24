package com.tangem.datasource.local.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class RuntimeDataStore<Key, Data>(private val keyProvider: (Data) -> Key) {

    private val store = MutableStateFlow<HashMap<Key, Data>>(hashMapOf())

    fun get(selector: (Data) -> Boolean = { true }): Flow<List<Data>> {
        return store.map { getInternal(it, selector) }
    }

    fun getSync(selector: (Data) -> Boolean = { true }): List<Data> {
        return getInternal(store.value, selector)
    }

    // TODO: Uncomment if needed
    // fun addOrReplace(items: Collection<Data>) {
    //     if (items.isEmpty()) return
    //
    //     val storeValue = store.value
    //
    //     items.forEach { item ->
    //         storeValue[keyProvider(item)] = item
    //     }
    //
    //     store.value = storeValue
    // }

    fun addOrReplace(item: Data) {
        val storeValue = store.value
        storeValue[keyProvider(item)] = item

        store.value = storeValue
    }

    fun remove(selector: (Data) -> Boolean) {
        val storeValue = store.value

        storeValue.forEach { (key, item) ->
            if (selector(item)) {
                storeValue.remove(key)
            }
        }

        store.value = storeValue
    }

    fun clear() {
        store.update { hashMapOf() }
    }

    private fun getInternal(store: HashMap<Key, Data>, selector: (Data) -> Boolean): List<Data> {
        return store.values.filter { selector(it) }
    }
}