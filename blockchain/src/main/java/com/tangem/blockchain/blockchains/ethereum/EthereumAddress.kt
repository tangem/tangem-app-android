package com.tangem.blockchain.blockchains.ethereum

import com.tangem.blockchain.common.AddressService
import org.kethereum.crypto.toAddress
import org.kethereum.erc55.isValid
import org.kethereum.model.Address
import org.kethereum.model.PublicKey

class EthereumAddressService : AddressService {
    override fun makeAddress(walletPublicKey: ByteArray): String =
            PublicKey(walletPublicKey.sliceArray(1..64)).toAddress().hex

    override fun validate(address: String): Boolean = Address(address).isValid()
}

enum class Chain(val id: Int) {
    Mainnet(1),
    Morden(2),
    Ropsten(3),
    Rinkeby(4),
    RootstockMainnet(30),
    RootstockTestnet(31),
    Kovan(42),
    EthereumClassicMainnet(61),
    EthereumClassicTestnet(62),
    Geth_private_chains(1337),
    MaticTestnet(8995);
}