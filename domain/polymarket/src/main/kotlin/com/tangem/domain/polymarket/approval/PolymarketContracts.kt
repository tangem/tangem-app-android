package com.tangem.domain.polymarket.approval

object PolymarketContracts {

    const val CHAIN_ID: Long = 137

    /**
     * The exchange's own settlement token (pUSD), which the wallet approves and which every order spends.
     * It is not what a user tops the account up with — see [USDC_TOKEN_ADDRESS].
     */
    const val COLLATERAL: String = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"

    /**
     * Native USDC on Polygon, the asset the bridge is asked to accept for a deposit. Lower-cased because
     * Express answers with lower-cased addresses and matches pairs by string equality.
     */
    const val USDC_TOKEN_ADDRESS: String = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
    const val CONDITIONAL_TOKENS: String = "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
    const val EXCHANGE_DOMAIN_NAME: String = "Polymarket CTF Exchange"
    const val EXCHANGE_DOMAIN_VERSION: String = "2"

    const val CTF_EXCHANGE: String = "0xE111180000d2663C0091e4f400237545B87B996B"
    const val NEG_RISK_CTF_EXCHANGE: String = "0xe2222d279d744050d28e00520010520000310F59"
    const val NEG_RISK_ADAPTER: String = "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296"
    const val CTF_COLLATERAL_ADAPTER: String = "0xAdA100Db00Ca00073811820692005400218FcE1f"
    const val NEG_RISK_CTF_COLLATERAL_ADAPTER: String = "0xadA2005600Dec949baf300f4C6120000bDB6eAab"

    const val EXCHANGE_V3: String = "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa"
    const val ROUTER_V3: String = "0x12121212006e4CD160D18e3f00711DA5c3372600"

    const val COLLATERAL_DECIMALS: Int = 6

    const val SIGNATURE_TYPE_DEPOSIT_WALLET: Int = 3
    const val SIGNATURE_RSV_HEX_LENGTH: Int = 130

    const val HEX_PREFIX: String = "0x"

    const val DW_FACTORY: String = "0x00000000000Fb5C9ADea0298D729A0CB3823Cc07"
    const val DW_BEACON: String = "0x7A18EDfe055488A3128f01F563e5B479D92ffc3a"

    const val DW_BEACON_INIT_PREFIX: String = "0x6100923d8160233d3973"

    const val DW_BEACON_INIT_SUFFIX: String = "0x60195155f3" +
        "363d3d373d3d363d602036600436635c60da1b60e01b36527f" +
        "a3f0ad74e5423aebfd80d3ef4346578335a9a72aeaee59ff6cb3582b35133d50" +
        "545afa5036515af43d6000803e604d573d6000fd5b3d6000f3"
}