package com.tangem.datasource.local.datastore.core

@Deprecated(
    message = "Use RuntimeSharedStore instead",
    replaceWith = ReplaceWith("RuntimeSharedStore"),
)
internal interface StringKeyDataStore<Value : Any> : DataStore<String, Value>