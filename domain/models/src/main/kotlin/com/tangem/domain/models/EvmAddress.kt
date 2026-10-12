package com.tangem.domain.models

/**
 * EVM addresses reach the app in two spellings: the token list carries them checksummed, while the backend
 * lowercases what it stores. They are the same address either way, so they are never keyed or compared as raw text.
 */
fun String.normalizeEvmAddress(): String = lowercase()

fun String.equalsEvmAddress(other: String): Boolean = equals(other, ignoreCase = true)