package com.tangem.datasource.api.common.config

typealias ApiConfigs = Map<String, @JvmSuppressWildcards ApiConfig>

/**
 * Api config
 *
 * @see <a href="https://www.notion.so/tangem/API-eacb264e7daf420a88b419a8a26f5b26?pvs=4">API configuration</a>
 *
[REDACTED_AUTHOR]
 */
// Base type for configs declared in other modules; keep it a class, not an interface.
@Suppress("UnnecessaryAbstractClass")
abstract class ApiConfig {

    /** Default environment */
    abstract val defaultEnvironment: ApiEnvironment

    /** Available environments */
    abstract val environmentConfigs: List<ApiEnvironmentConfig>

    /** Unique id */
    abstract val id: ID

    /**
     * Type-safe config identifier, backed by a stable string [name] (used as the persistence key
     * for the selected environment). Constants for the built-in configs live in the companion.
     */
    @JvmInline
    value class ID(val name: String) {

        companion object {

            /**
             * Raw string keys. Use these where a compile-time constant is required (e.g. as an
             * annotation argument); prefer the type-safe [ID] instances everywhere else.
             */
            const val EXPRESS = "Express"
            const val TANGEM_TECH = "TangemTech"
            const val STAKE_KIT = "StakeKit"
            const val P2P_ETH_POOL = "P2PEthPool"
            const val TANGEM_PAY = "TangemPay"
            const val TANGEM_PAY_AUTH = "TangemPayAuth"
            const val BLOCK_AID = "BlockAid"
            const val YIELD_SUPPLY = "YieldSupply"
            const val MOON_PAY = "MoonPay"
            const val NEWS = "News"
            const val GASLESS_TX_SERVICE = "GaslessTxService"
            const val SURVEY_SPARROW = "SurveySparrow"
            const val AUTH = "Auth"

            val Express = ID(EXPRESS)
            val TangemTech = ID(TANGEM_TECH)
            val StakeKit = ID(STAKE_KIT)
            val P2PEthPool = ID(P2P_ETH_POOL)
            val TangemPay = ID(TANGEM_PAY)
            val TangemPayAuth = ID(TANGEM_PAY_AUTH)
            val BlockAid = ID(BLOCK_AID)
            val YieldSupply = ID(YIELD_SUPPLY)
            val MoonPay = ID(MOON_PAY)
            val News = ID(NEWS)
            val GaslessTxService = ID(GASLESS_TX_SERVICE)
            val SurveySparrow = ID(SURVEY_SPARROW)
            val Auth = ID(AUTH)
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