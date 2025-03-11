package com.tangem.core.decompose.utils

/**
 * Interface for owning tags.
 */
interface TagsOwner {

    /**
     * Provides access to the tags map instance.
     */
    val tags: HashMap<String, Any>
}