package com.tangem.data.jointaccount.safe

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.ADDRESS_BYTES
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.WORD_BYTES
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.domain.jointaccount.safe.SafeAddressCalculator
import com.tangem.domain.jointaccount.safe.SafeSingleton
import java.math.BigInteger
import javax.inject.Inject

/**
 * The Safe v1.5.0 address of a composition, identical on every supported network (Ethereum, BNB Smart Chain,
 * Polygon PoS, Arbitrum One, Base, Optimism, Avalanche; zkSync Era is out — Safe is deployed at non-canonical
 * addresses there).
 *
 * ```
 * initializer = Safe.setup(ownersSortedLowercase, threshold, 0, "", fallbackHandler, 0, 0, 0)
 * salt        = keccak256(keccak256(initializer) ++ uint256(saltNonce))
 * deployData  = proxyCreationCode ++ uint256(uint160(singleton))
 * address     = keccak256(0xff ++ proxyFactory ++ salt ++ keccak256(deployData))[12..31]
 * ```
 *
 * Owners are sorted AFTER lowercasing: `'B'` (0x42) sorts before `'a'` (0x61), so sorting the checksummed strings
 * gives a different order in the initializer and a different address.
 */
internal class DefaultSafeAddressCalculator @Inject constructor() : SafeAddressCalculator {

    override fun compute(
        owners: List<String>,
        threshold: Int,
        singleton: SafeSingleton,
    ): Either<SafeAddressCalculator.Error, String> = either {
        validate(owners = owners, threshold = threshold)

        val salt = (initializer(owners, threshold).toKeccak() + word(SALT_NONCE)).toKeccak()
        val deploymentData = PROXY_CREATION_CODE.hexToBytes() + singleton.address.addressWord()

        val proxy = (
            byteArrayOf(CREATE2_PREFIX.toByte()) + PROXY_FACTORY.hexToBytes() + salt + deploymentData.toKeccak()
            ).toKeccak()

        eip55Checksum(proxy.copyOfRange(WORD_BYTES - ADDRESS_BYTES, WORD_BYTES).toHexString().lowercase())
    }

    private fun Raise<SafeAddressCalculator.Error>.validate(owners: List<String>, threshold: Int) {
        ensure(owners.isNotEmpty()) { SafeAddressCalculator.Error.NoOwners }

        owners.forEach { owner ->
            ensure(ADDRESS_FORMAT.matches(owner)) {
                SafeAddressCalculator.Error.MalformedOwnerAddress(address = owner)
            }
        }

        owners.map { it.lowercase() }.firstDuplicateOrNull()?.let { duplicate ->
            raise(SafeAddressCalculator.Error.DuplicateOwners(address = duplicate))
        }

        ensure(threshold in MIN_THRESHOLD..owners.size) {
            SafeAddressCalculator.Error.ThresholdOutOfRange(threshold = threshold, ownersCount = owners.size)
        }
    }

    private fun List<String>.firstDuplicateOrNull(): String? {
        val seen = mutableSetOf<String>()

        return firstOrNull { !seen.add(it) }
    }

    /** ABI encoding of `setup`: eight head words, then the owners array, then the empty `data`. */
    internal fun initializer(owners: List<String>, threshold: Int): ByteArray {
        val sorted = owners.map { it.lowercase() }.sorted()
        val ownersOffset = SETUP_HEAD_WORDS * WORD_BYTES
        val dataOffset = ownersOffset + WORD_BYTES * (1 + sorted.size)

        return SETUP_SELECTOR.hexToBytes() +
            word(ownersOffset) + // _owners, offset to the tail
            word(threshold) + // _threshold
            word(0) + // to
            word(dataOffset) + // data, offset to the tail
            FALLBACK_HANDLER.addressWord() +
            word(0) + // paymentToken
            word(0) + // payment
            word(0) + // paymentReceiver
            word(sorted.size) + // _owners.length
            sorted.fold(ByteArray(size = 0)) { acc, owner -> acc + owner.addressWord() } +
            word(0) // data.length
    }

    private fun word(value: Int): ByteArray = BigInteger.valueOf(value.toLong()).toFixedSizeBytes()

    private fun String.addressWord(): ByteArray = hexToFixedSizeBytes()

    private fun eip55Checksum(addressHexLowercase: String): String {
        val checksumDigest = addressHexLowercase.toByteArray(Charsets.US_ASCII).toKeccak().toHexString().lowercase()

        val checksummed = addressHexLowercase
            .mapIndexed { i, char ->
                if (checksumDigest[i].digitToInt(radix = HEX_RADIX) >= EIP55_UPPERCASE_THRESHOLD) {
                    char.uppercaseChar()
                } else {
                    char
                }
            }
            .joinToString(separator = "")

        return "0x$checksummed"
    }

    internal companion object {

        const val PROXY_FACTORY = "0x14F2982D601c9458F93bd70B218933A6f8165e7b"
        const val FALLBACK_HANDLER = "0x3EfCBb83A4A7AfcB4F68D501E2c2203a38be77f4"

        /**
         * `type(SafeProxy).creationCode`, read from `proxyCreationCode()` on the v1.5.0 factory.
         * Do NOT recompile SafeProxy.sol: the trailing metadata hash of a local build differs from the
         * deployed one. keccak256 of this blob: `0x941b3e88811b2f33b8e26783c7407d2e978404581a21c9f07abf2cad9cb87e12`.
         */
        const val PROXY_CREATION_CODE =
            "608060405234801561001057600080fd5b506040516101b63803806101b68339818101604052602081101561003357600080" +
                "fd5b8101908080519060200190929190505050600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffff" +
                "ffffffffffffffffffffffffffffff1614156100ca576040517f08c379a00000000000000000000000000000000000000000" +
                "0000000000000000815260040180806020018281038252602281526020018061019460229139604001915050604051809103" +
                "90fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffff" +
                "ffffffffffffffffffffff16021790555050607b806101196000396000f3fe608060405260005463a619486e60003560e01c" +
                "14156024578060601b606c5260206060f35b3660008037600080366000845af43d6000803e806040573d6000fd5b3d6000f3" +
                "fea2646970667358221220e61834ebd2d8cd909d362bf67c47ef58fd665df38e6dd036ce65611101d072e964736f6c634300" +
                "07060033496e76616c69642073696e676c65746f6e20616464726573732070726f7669646564"

        // keccak256("setup(address[],uint256,address,bytes,address,address,uint256,address)")[0..3]
        const val SETUP_SELECTOR = "0xb63e800d"
        const val SALT_NONCE = 0
        const val SETUP_HEAD_WORDS = 8
        const val CREATE2_PREFIX = 0xff
        const val EIP55_UPPERCASE_THRESHOLD = 8
        const val MIN_THRESHOLD = 1
        const val HEX_RADIX = 16
        val ADDRESS_FORMAT = Regex(pattern = "^0x[0-9a-fA-F]{40}$")
    }
}