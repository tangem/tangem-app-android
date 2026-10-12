package com.tangem.data.walletmanager

import com.tangem.blockchain.blockchains.ethereum.SafeEthereumWalletManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.address.Address
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.hot.sdk.model.HotWalletId
import io.mockk.every
import io.mockk.mockk

/** A hot wallet that owns a joint account: its owner key at [OWNER_PATH] is derived, the Safe sits at [SAFE_ADDRESS] */
internal object SafeTestFixtures {

    const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    const val OWNER_PATH = "m/44'/60'/888888'/0/2"

    val userWalletId = UserWalletId("011")

    private val ownerKey = (
        "040876BDEC26B89BD2159A668B9AF3D9FE86370F318717C92B8D6C1186FB3648C32A5F9321998CC2D042901C91D40601E79A641E1CB" +
            "CEBE7A2358BE6054E1B6E5D"
        ).hexToBytes()

    val jointNetwork: Network = MockCryptoCurrencyFactory().ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = OWNER_PATH)

        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }

    fun hotWallet(derivedPaths: Set<String> = setOf(OWNER_PATH)): UserWallet.Hot = UserWallet.Hot(
        name = "Hot",
        walletId = userWalletId,
        hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
        wallets = listOf(
            MobileWallet(
                publicKey = ownerKey,
                chainCode = ByteArray(size = 32),
                curve = EllipticCurve.Secp256k1,
                derivedKeys = derivedPaths.associate { path ->
                    DerivationPath(path) to ExtendedPublicKey(publicKey = ownerKey, chainCode = ByteArray(size = 32))
                },
            ),
        ),
        backedUp = true,
    )

    /** The Safe's wallet as the SDK manager holds it: the owner key, the Safe address */
    fun safeWallet(): Wallet = Wallet(
        blockchain = Blockchain.Ethereum,
        addresses = setOf(Address(SAFE_ADDRESS)),
        publicKey = Wallet.PublicKey(seedKey = ownerKey, derivationType = null),
        tokens = emptySet(),
    )

    fun safeManager(wallet: Wallet = safeWallet()): SafeEthereumWalletManager = mockk(relaxed = true) {
        every { this@mockk.wallet } returns wallet
        every { cardTokens } returns mutableSetOf()
    }
}