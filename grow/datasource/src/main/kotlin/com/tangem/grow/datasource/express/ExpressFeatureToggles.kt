package com.tangem.grow.datasource.express

interface ExpressFeatureToggles {

    /** Express is called on `/v2` with session (JWT) auth instead of `/v1` */
    val isExpressV2Enabled: Boolean
}