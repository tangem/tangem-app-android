package com.tangem.datasource.api.oneinch

import com.tangem.datasource.api.oneinch.models.AllowanceResponse
import com.tangem.datasource.api.oneinch.models.ApproveCalldataResponse
import com.tangem.datasource.api.oneinch.models.ApproveSpenderResponse
import com.tangem.datasource.api.oneinch.models.ProtocolsResponse
import com.tangem.datasource.api.oneinch.models.QuoteResponse
import com.tangem.datasource.api.oneinch.models.StatusResponse
import com.tangem.datasource.api.oneinch.models.SwapResponse
import com.tangem.datasource.api.oneinch.models.TokensResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OneInchApi {

    /**
     * Healthcheck return 200 if service is available
     *
     * @return [StatusResponse]
     */
    @GET("healthcheck")
    suspend fun healthcheck(): StatusResponse

    //region Approve
    /**
     * Address of the 1inch router that must be trusted to spend funds for the exchange
     *
     * @return [ApproveSpenderResponse]
     */
    @GET("approve/spender")
    suspend fun approveSpender(): ApproveSpenderResponse

    /**
     * Generate data for calling the contract in order to allow the 1inch router to spend funds
     *
     * @param tokenAddress Token address you want to exchange
     * @param amount The number of tokens that the 1inch router is allowed to spend.
     * If not specified, it will be allowed to spend an infinite amount of tokens.
     *
     * @return [ApproveCalldataResponse] Transaction body to allow the exchange with the 1inch router
     */
    @GET("approve/transaction")
    suspend fun approveTransaction(
        @Query("tokenAddress") tokenAddress: String,
        @Query("amount") amount: String? = null,
    ): ApproveCalldataResponse

    /**
     * Get the number of tokens that the 1inch router is allowed to spend
     *
     * @param tokenAddress Token address you want to exchange
     * @param walletAddress Wallet address for which you want to check
     *
     * @return [AllowanceResponse]
     */
    @GET("approve/allowance")
    suspend fun approveAllowance(
        @Query("tokenAddress") tokenAddress: String,
        @Query("walletAddress") walletAddress: String,
    ): Response<AllowanceResponse>
    //endregion Approve

    //region Info
    /**
     *  List of tokens that are available for swap in the 1inch Aggregation protocol
     *
     * @return [TokensResponse]
     */
    @GET("tokens")
    suspend fun tokensAvailable(): TokensResponse

    /**
     * List of liquidity sources that are available for routing in the 1inch Aggregation protocol
     *
     * @return
     */
    @GET("liquidity-sources")
    suspend fun liquiditySources(): ProtocolsResponse
    //endregion Info

    //region Swap
    /**
     * Find the best quote to exchange via 1inch router
     *
     * @param fromTokenAddress Example : 0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE
     * @param toTokenAddress Example : 0x111111111117dc0aa78b770fa6a738034120c302
     * @param amount amount of a token to sell, set in minimal divisible units e.g.:
     * 1.00 DAI set as 1000000000000000000
     * 51.03 USDC set as 51030000
     *
     * @param protocols default: all
     * @param fee this percentage of fromTokenAddress token amount will be sent to referrerAddress,
     * the rest will be used as input for a swap
     * Min: 0; max: 3; Max: 0; max: 3; default: 0; !should be the same for quote and swap!
     *
     * @param gasLimit maximum amount of gas for a swap;
     * @param connectorTokens token-connectors can be specified via this parameter.
     * The more is set — the longer route estimation will take.
     * If not set, default token-connectors will be usedmax: 5; !should be the same for quote and swap!
     *
     * @param complexityLevel maximum number of token-connectors to be used in a transaction.
     * The more is used — the longer route estimation will take
     * min: 0; max: 3; default: 2; !should be the same for quote and swap!
     *
     * @param mainRouteParts default: 10; max: 50 !should be the same for quote and swap!
     * @param parts limit maximum number of parts each main route parts can be split into;
     * should be the same for a quote and swap
     * default: 20; max: 100
     *
     * @param gasPrice 1inch takes in account gas expenses to determine exchange route.
     * It is important to use the same gas price on the quote and swap methods.
     * Gas price set in wei: 12.5 GWEI set as 12500000000
     * default: fast from network
     *
     * @return [QuoteResponse]
     */
    @GET("quote")
    suspend fun quote(
        @Query("fromTokenAddress") fromTokenAddress: String,
        @Query("toTokenAddress") toTokenAddress: String,
        @Query("amount") amount: String,
        @Query("protocols") protocols: String? = null,
        @Query("fee") fee: String? = null,
        @Query("gasLimit") gasLimit: String? = null,
        @Query("connectorTokens") connectorTokens: String? = null,
        @Query("complexityLevel") complexityLevel: String? = null,
        @Query("mainRouteParts") mainRouteParts: String? = null,
        @Query("parts") parts: String? = null,
        @Query("gasPrice") gasPrice: String? = null,
    ): Response<QuoteResponse>

    /**
     * Generate data for calling the 1inch router for exchange
     *
     * @param fromTokenAddress Example : 0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE
     * @param toTokenAddress Example : 0x111111111117dc0aa78b770fa6a738034120c302
     * @param amount amount of a token to sell, set in minimal divisible units e.g.:
     * 1.00 DAI set as 1000000000000000000
     * 51.03 USDC set as 51030000
     *
     * @param fromAddress The address that calls the 1inch contract
     * @param slippage limit of price slippage you are willing to accept in percentage, may be set with decimals.
     * &slippage=0.5 means 0.5% slippage is acceptable. Low values increase chances that transaction will fail,
     * high values increase chances of front running. min: 0; max: 50;
     *
     * @param protocols default: all
     * @param destinationAddress Receiver of destination currency. default: fromAddress
     * @param fee this percentage of fromTokenAddress token amount will be sent to referrerAddress,
     * the rest will be used as input for a swap
     * Min: 0; max: 3; Max: 0; max: 3; default: 0; !should be the same for quote and swap!
     *
     * @param permit https://eips.ethereum.org/EIPS/eip-2612
     * @param compatibilityMode Allows to build calldata without optimized routers
     * @param burnChi If true, CHI will be burned from fromAddress to compensate gas.
     * Check CHI balance and allowance before turning that on. CHI should be approved for the spender address
     *
     * @param connectorTokens token-connectors can be specified via this parameter.
     * The more is set — the longer route estimation will take.
     * If not set, default token-connectors will be usedmax: 5; !should be the same for quote and swap!
     *
     * @param complexityLevel maximum number of token-connectors to be used in a transaction.
     * The more is used — the longer route estimation will take
     * min: 0; max: 3; default: 2; !should be the same for quote and swap!
     *
     * @param mainRouteParts default: 10; max: 50 !should be the same for quote and swap!
     * @param parts limit maximum number of parts each main route parts can be split into;
     * should be the same for a quote and swap
     * default: 20; max: 100
     *
     * @param gasLimit maximum amount of gas for a swap;
     * @param gasPrice 1inch takes in account gas expenses to determine exchange route.
     * It is important to use the same gas price on the quote and swap methods.
     * Gas price set in wei: 12.5 GWEI set as 12500000000
     * default: fast from network
     *
     * @return [SwapResponse]
     */
    @GET("swap")
    suspend fun swap(
        @Query("fromTokenAddress") fromTokenAddress: String,
        @Query("toTokenAddress") toTokenAddress: String,
        @Query("amount") amount: String,
        @Query("fromAddress") fromAddress: String,
        @Query("slippage") slippage: Int,
        @Query("protocols") protocols: String? = null,
        @Query("destReceiver") destinationAddress: String? = null,
        @Query("referrerAddress") referrerAddress: String? = null,
        @Query("fee") fee: String? = null,
        @Query("disableEstimate") disableEstimate: Boolean? = null,
        @Query("permit") permit: String? = null,
        @Query("compatibilityMode") compatibilityMode: Boolean? = null,
        @Query("burnChi") burnChi: Boolean? = null,
        @Query("allowPartialFill") allowPartialFill: Boolean? = null,
        @Query("parts") parts: String? = null,
        @Query("mainRouteParts") mainRouteParts: String? = null,
        @Query("connectorTokens") connectorTokens: String? = null,
        @Query("complexityLevel") complexityLevel: String? = null,
        @Query("gasLimit") gasLimit: String? = null,
        @Query("gasPrice") gasPrice: String? = null,
    ): Response<SwapResponse>
    //endregion Swap
}
