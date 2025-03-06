package com.tangem.lib.visa

import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import com.tangem.lib.visa.model.VisaContractInfo
import com.tangem.lib.visa.utils.Constants
import com.tangem.lib.visa.utils.Constants.NETWORK_LOGS_TAG
import com.tangem.lib.visa.utils.toHexString
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import okhttp3.OkHttpClient
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.FastRawTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.gas.StaticEIP1559GasProvider
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

interface VisaContractInfoProvider {

    /**
     * Fetches Visa contract info for the given wallet address.
     *
     * @param walletAddress Wallet address to fetch contract info for.
     * @param paymentAccountAddress Payment account address to fetch data from. If null,
     * it will be fetched from the registry.
     */
    suspend fun getContractInfo(walletAddress: String, paymentAccountAddress: String?): VisaContractInfo

    class Builder(
        private val useTestnetRpc: Boolean,
        private val bridgeProcessorAddress: String,
        private val paymentAccountRegistryAddress: String,
        private val isNetworkLoggingEnabled: Boolean,
        private val dispatchers: CoroutineDispatcherProvider,
        private val chainId: Long = Constants.CHAIN_ID,
        private val decimals: Int = Constants.DECIMALS,
        private val gasLimit: Long = Constants.GAS_LIMIT,
        private val networkTimeoutSeconds: Long = Constants.NETWORK_TIMEOUT_SECONDS,
        private val privateKey: String = ByteArray(Constants.PRIVATE_KEY_LENGTH).toHexString(),
    ) {

        fun build(): VisaContractInfoProvider {
            val web3j = createWeb3J()
            val gasProvider = createGasProvider()
            val transactionManager = createTransactionManager(web3j)

            return DefaultVisaContractInfoProvider(
                web3j = web3j,
                transactionManager = transactionManager,
                gasProvider = gasProvider,
                bridgeProcessorAddress = bridgeProcessorAddress,
                paymentAccountRegistryAddress = paymentAccountRegistryAddress,
                dispatchers = dispatchers,
            )
        }

        private fun createWeb3J(): Web3j {
            val baseUrl: String = if (useTestnetRpc) Constants.TESTNET_RPC_URL else Constants.MAINNET_RPC_URL

            val httpClient = OkHttpClient.Builder().apply {
                connectTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)
                readTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)
                writeTimeout(networkTimeoutSeconds, TimeUnit.SECONDS)

                if (isNetworkLoggingEnabled) {
                    addInterceptor(
                        LoggingInterceptor.Builder()
                            .setLevel(Level.BODY)
                            .tag(NETWORK_LOGS_TAG)
                            .build(),
                    )
                }
            }.build()

            val web3jService = HttpService(
                /* url = */ baseUrl,
                /* httpClient = */ httpClient,
                /* includeRawResponses = */ false,
            )

            return Web3j.build(web3jService)
        }

        private fun createGasProvider(): ContractGasProvider = StaticEIP1559GasProvider(
            /* chainId = */ chainId,
            /* maxFeePerGas = */ BigDecimal.ONE.movePointLeft(decimals).toBigInteger(),
            /* maxPriorityFeePerGas = */ BigDecimal.ONE.movePointLeft(decimals).toBigInteger(),
            /* gasLimit = */ BigInteger.valueOf(gasLimit),
        )

        private fun createTransactionManager(web3j: Web3j): TransactionManager = FastRawTransactionManager(
            /* web3j = */ web3j,
            /* credentials = */ Credentials.create(privateKey),
            /* chainId = */ chainId,
        )
    }
}