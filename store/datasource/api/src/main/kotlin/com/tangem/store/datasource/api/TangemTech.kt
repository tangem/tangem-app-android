package com.tangem.store.datasource.api

import com.tangem.core.remote.config.ApiConfig

/**
 * Skeleton of the TangemTech (api.tangem.org) API config. Concrete configs extend it to inherit the
 * shared identity and override only what differs. For now it carries only the [ID] so that APIs keyed
 * on this config can reference it from outside `store:datasource`.
 */
abstract class TangemTech : ApiConfig() {

    override val id: ApiConfig.ID get() = ID

    companion object {
        const val KEY = "TangemTech"
        val ID = ApiConfig.ID(KEY)
    }
}