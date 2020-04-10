package com.tangem.blockchain.common

import com.google.common.truth.Truth
import com.tangem.SessionEnvironment
import com.tangem.blockchain.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.cardano.CardanoWalletManager
import com.tangem.blockchain.ethereum.EthereumWalletManager
import com.tangem.blockchain.stellar.StellarWalletManager
import com.tangem.blockchain.xrp.XrpWalletManager
import com.tangem.commands.ReadCommand
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.hexToBytes
import org.junit.Test

internal class WalletManagerFactoryTest {

    @Test
    fun createBitcoinWalletManager() {
        val data = "0108bb00000000000304200754414e47454d00020102800a322e3432642053444b000341040876bdec26b89bd2159a668b9af3d9fe86370f318717c92b8d6c1186fb3648c32a5f9321998cc2d042901c91d40601e79a641e1cbcebe7a2358be6054e1b6e5d0a04041e76310c618102ffff8a0101820407e30b0d830b54414e47454d2053444b0084034254438640e17ceec48c5be36240c98019f95ad8b6e56acfebe60d11979c6279f715d607d76a860a137da8d109e805753f3f56b0130709f4bbf4cb9974b4c57b8469bf4b873041045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd26050a736563703235366b310008040000006407010009020bb8604104752a727e14bba5bd73b6714d72500f61ffd11026ad1196d2e1c54577cbeeac3d11fc68a64700f8d533f4e311964ea8fb3aa26c588295f2133868d69c3e62869362040000005c6304000000090f01009000"
        val responseApdu = ResponseApdu(data.hexToBytes())
        val card = ReadCommand().deserialize(SessionEnvironment(), responseApdu)
        val walletManager = WalletManagerFactory.makeWalletManager(card!!)

        Truth.assertThat(walletManager)
                .isInstanceOf(BitcoinWalletManager::class.java)
    }

    @Test
    fun createEthereumWalletManager() {
        val data = "0108bb00000000000536200754414e47454d00020102800a322e3432642053444b000341046c8aea0d5a850b0a608acf9a0c453c39ea86131e88bfa78800de3cfb5bf1007aeaa7b9ffc184212255758605c2461be343c0a661d73cabafa4c9c175b3f0e59a0a04041e76310c618102ffff8a0101820407e30b0d830b54414e47454d2053444b0084034554488640431b6244acfeac479becdff201a7f720a7d70a97edc4e019fb678596baf52dfe9d0e8faf08ceb4443b82d4e66815541f2dc8ec6dd3ff83eb42f06e5eab07f25f3041045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd26050a736563703235366b3100080400000064070100090205dc60410464dddc3f356744aaecfa07427f9eb996ff537d65f20fb5be3abccf0354352a6b5f8a1942e0f8ddeea3a170eda78d060be8162ad60e94e4e91fbbdf0a7054785562040000005b6304000000090f01009000"
        val responseApdu = ResponseApdu(data.hexToBytes())
        val card = ReadCommand().deserialize(SessionEnvironment(), responseApdu)
        val walletManager = WalletManagerFactory.makeWalletManager(card!!)

        Truth.assertThat(walletManager)
                .isInstanceOf(EthereumWalletManager::class.java)
    }

    @Test
    fun createStellarWalletManager() {
        val data = "0108bb00000000000379200754414e47454d00020102800a322e3432642053444b0003410487d7bb51b189213e3cedc3fcfa3fc047b3b71b7805b5b215e14639b3a8ebb1952c9dd5ea4354441b6ada4e8b8327674bb102ddae69df55be69643a2c916edf650a04041e76310c618102ffff8a0101820407e30b0d830b54414e47454d2053444b008403584c4d86409a4bc2baf0e5836887da21167cf33458d5249d1a610bced0e31dc053f23729ed24d715912bf89e6804669430dfe396ed83274e0031f6803e2bdb8c041fa993413041045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd2605086564323535313900080400000064070100090205dc6020e078212d58b2b9d0edc9c936830d10081cd38b90c31778c56dfb1171027e294e62040000003863040000002c0f01009000"
        val responseApdu = ResponseApdu(data.hexToBytes())
        val card = ReadCommand().deserialize(SessionEnvironment(), responseApdu)
        val walletManager = WalletManagerFactory.makeWalletManager(card!!)

        Truth.assertThat(walletManager)
                .isInstanceOf(StellarWalletManager::class.java)
    }

    @Test
    fun createCardanoWalletManager() {
        val data = "0108bb00000000000502200754414e47454d00020102800a322e3432642053444b0003410402c1e39257d60583489da2d67d35d1cc2a1c005cc05c1021f44838edcaf25d5615cad7c9d11c2e23f5efa93e50904d33c88808d0e169060508df840992e31f4d0a04041e76310c658102ffff8a0101820407e30b0d830b54414e47454d2053444b00840743415244414e4f8640f24ef5c8c6eba0ff97560d5b013edb4a452594270db9647bd0a3543df8104dec75731d4db3ebe0fc493f2afee00195e560b51e3c41189b7c61ba7895d6434b9d3041045f16bd1d2eafe463e62a335a09e6b2bbcbd04452526885cb679fc4d27af1bd22f553c7deefb54fd3d4f361d14e6dc3f11b7d4ea183250a60720ebdf9e110cd2605086564323535313900080400000064070100090205dc60208a71161cfdf1e0a85d8e7ff372aa4a01136046292aceb5f9ad7ebdb98d3f60a86204000000646304000000000f01009000"
        val responseApdu = ResponseApdu(data.hexToBytes())
        val card = ReadCommand().deserialize(SessionEnvironment(), responseApdu)
        val walletManager = WalletManagerFactory.makeWalletManager(card!!)

        Truth.assertThat(walletManager)
                .isInstanceOf(CardanoWalletManager::class.java)
    }

    @Test
    fun createXrpWalletManager() {
        val data = "0108cb21000000002154200b534d4152542043415348000201028006322e31317200034104bdad63848f97c535da53cf8fd300d24fa33f0516d194aa78ec164a06994d00204bae243a424e316c6ec845e02d9b15eafae8c19018a926b0b7435e6e941cdadb0a0400007e210c5a81020028820407e30502830754414e47454d00840358525086400ed8734b877869722c7d0b37ffb154b9fef21c54bf2c6496feb1fb5c1fc28a2ac28e201dde84f27495fa7f08b3ca2be2fb4954bf0fe78af027d6cdc16c3eee923041048196aa4b410ac44a3b9cce18e7be226aea070acc83a9cf67540fac49af25129f6a538a28ad6341358e3c4f9963064f7e365372a651d374e5c23cdd37fd099bf2050a736563703235366b31000804000f4240070100090205dc604104d2b9fb288540d54e5b32ecaf0381cd571f97f6f1ecd036b66bb11aa52ffe9981110d883080e2e255c6b1640586f7765e6faa325d1340f49b56b83d9de56bc7ed6204000f42406304000000000f01009000"
        val responseApdu = ResponseApdu(data.hexToBytes())
        val card = ReadCommand().deserialize(SessionEnvironment(), responseApdu)
        val walletManager = WalletManagerFactory.makeWalletManager(card!!)

        Truth.assertThat(walletManager)
                .isInstanceOf(XrpWalletManager::class.java)
    }
}