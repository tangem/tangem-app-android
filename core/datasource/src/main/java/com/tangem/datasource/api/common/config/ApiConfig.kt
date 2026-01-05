package com.tangem.datasource.api.common.config

typealias ApiConfigs = Set<@JvmSuppressWildcards ApiConfig>

/**
 * Api config
 *
 * @see <a href="https://www.notion.so/tangem/API-eacb264e7daf420a88b419a8a26f5b26?pvs=4">API configuration</a>
 *
[REDACTED_AUTHOR]
 */
sealed class ApiConfig {

    /** Default environment */
    abstract val defaultEnvironment: ApiEnvironment

    /** Available environments */
    abstract val environmentConfigs: List<ApiEnvironmentConfig>

    /** Unique id */
    val id: ID = initializeId()

    enum class ID {
        Express,
        TangemTech,
        StakeKit,
        P2PEthPool,
        TangemPay,
        TangemPayAuth,
        BlockAid,
        YieldSupply,
        MoonPay,
        News,
        GaslessTxService,
    }

    private fun initializeId(): ID {
        return when (this) {
            is Express -> ID.Express
            is TangemTech -> ID.TangemTech
            is StakeKit -> ID.StakeKit
            is P2PEthPool -> ID.P2PEthPool
            is TangemPay -> ID.TangemPay
            is TangemPayAuth -> ID.TangemPayAuth
            is BlockAid -> ID.BlockAid
            is YieldSupply -> ID.YieldSupply
            is MoonPay -> ID.MoonPay
            is News -> ID.News
            is GaslessTxService -> ID.GaslessTxService
        }
    }

    companion object {
        internal const val DEBUG_BUILD_TYPE = "debug"
        internal const val INTERNAL_BUILD_TYPE = "internal"
        internal const val MOCKED_BUILD_TYPE = "mocked"
        internal const val EXTERNAL_BUILD_TYPE = "external"
        internal const val RELEASE_BUILD_TYPE = "release"
    }
}