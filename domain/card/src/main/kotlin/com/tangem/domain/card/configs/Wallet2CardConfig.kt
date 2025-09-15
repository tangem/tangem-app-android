package com.tangem.domain.card.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import timber.log.Timber

data object Wallet2CardConfig : CardConfig {
    override val mandatoryCurves: List<EllipticCurve>
        get() = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Bip0340,
            EllipticCurve.Ed25519Slip0010,
        )

    /**
     * Logic to determine primary curve for blockchain in TangemWallet 2.0
     * Order is important here
     */
    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        // order is important, new curve is preferred for wallet 2
        // TODO Comment old logic without direct mapping until tests and release
        // return when {
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519Slip0010) -> {
        //         EllipticCurve.Ed25519Slip0010
        //     }
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
        //         EllipticCurve.Secp256k1
        //     }
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Bls12381G2Aug) -> {
        //         EllipticCurve.Bls12381G2Aug
        //     }
        //     // only for support cardano on Wallet2
        //     blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
        //         EllipticCurve.Ed25519
        //     }
        //     else -> {
        //         Timber.e("Unsupported blockchain, curve not found")
        //         null
        //     }
        // }
        val curve = getPrimaryCurveForBlockchain(blockchain)
        // check curve supports
        if (!blockchain.getSupportedCurves().contains(curve)) {
            Timber.e("Unsupported curve $curve for blockchain $blockchain")
            return null
        }
        return curve
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun getPrimaryCurveForBlockchain(blockchain: Blockchain): EllipticCurve? {
        return when (blockchain) {
            Blockchain.Unknown -> null
            Blockchain.Arbitrum -> EllipticCurve.Secp256k1
            Blockchain.ArbitrumTestnet -> EllipticCurve.Secp256k1
            Blockchain.Avalanche -> EllipticCurve.Secp256k1
            Blockchain.AvalancheTestnet -> EllipticCurve.Secp256k1
            Blockchain.Binance -> EllipticCurve.Secp256k1
            Blockchain.BinanceTestnet -> EllipticCurve.Secp256k1
            Blockchain.BSC -> EllipticCurve.Secp256k1
            Blockchain.BSCTestnet -> EllipticCurve.Secp256k1
            Blockchain.Bitcoin -> EllipticCurve.Secp256k1
            Blockchain.BitcoinTestnet -> EllipticCurve.Secp256k1
            Blockchain.BitcoinCash -> EllipticCurve.Secp256k1
            Blockchain.BitcoinCashTestnet -> EllipticCurve.Secp256k1
            Blockchain.Cardano -> EllipticCurve.Ed25519
            Blockchain.Cosmos -> EllipticCurve.Secp256k1
            Blockchain.CosmosTestnet -> EllipticCurve.Secp256k1
            Blockchain.Dogecoin -> EllipticCurve.Secp256k1
            Blockchain.Ducatus -> EllipticCurve.Secp256k1
            Blockchain.Ethereum -> EllipticCurve.Secp256k1
            Blockchain.EthereumTestnet -> EllipticCurve.Secp256k1
            Blockchain.EthereumClassic -> EllipticCurve.Secp256k1
            Blockchain.EthereumClassicTestnet -> EllipticCurve.Secp256k1
            Blockchain.Fantom -> EllipticCurve.Secp256k1
            Blockchain.FantomTestnet -> EllipticCurve.Secp256k1
            Blockchain.Litecoin -> EllipticCurve.Secp256k1
            Blockchain.Near -> EllipticCurve.Ed25519Slip0010
            Blockchain.NearTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Polkadot -> EllipticCurve.Ed25519Slip0010
            Blockchain.PolkadotTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Kava -> EllipticCurve.Secp256k1
            Blockchain.KavaTestnet -> EllipticCurve.Secp256k1
            Blockchain.Kusama -> EllipticCurve.Ed25519Slip0010
            Blockchain.Polygon -> EllipticCurve.Secp256k1
            Blockchain.PolygonTestnet -> EllipticCurve.Secp256k1
            Blockchain.RSK -> EllipticCurve.Secp256k1
            Blockchain.Sei -> EllipticCurve.Secp256k1
            Blockchain.SeiTestnet -> EllipticCurve.Secp256k1
            Blockchain.Stellar -> EllipticCurve.Ed25519Slip0010
            Blockchain.StellarTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Solana -> EllipticCurve.Ed25519Slip0010
            Blockchain.SolanaTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Tezos -> EllipticCurve.Ed25519Slip0010
            Blockchain.Tron -> EllipticCurve.Secp256k1
            Blockchain.TronTestnet -> EllipticCurve.Secp256k1
            Blockchain.XRP -> EllipticCurve.Secp256k1
            Blockchain.Gnosis -> EllipticCurve.Secp256k1
            Blockchain.Dash -> EllipticCurve.Secp256k1
            Blockchain.Optimism -> EllipticCurve.Secp256k1
            Blockchain.OptimismTestnet -> EllipticCurve.Secp256k1
            Blockchain.Dischain -> EllipticCurve.Secp256k1
            Blockchain.EthereumPow -> EllipticCurve.Secp256k1
            Blockchain.EthereumPowTestnet -> EllipticCurve.Secp256k1
            Blockchain.Kaspa -> EllipticCurve.Secp256k1
            Blockchain.Telos -> EllipticCurve.Secp256k1
            Blockchain.TelosTestnet -> EllipticCurve.Secp256k1
            Blockchain.TON -> EllipticCurve.Ed25519Slip0010
            Blockchain.TONTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Ravencoin -> EllipticCurve.Secp256k1
            Blockchain.RavencoinTestnet -> EllipticCurve.Secp256k1
            Blockchain.TerraV1 -> EllipticCurve.Secp256k1
            Blockchain.TerraV2 -> EllipticCurve.Secp256k1
            Blockchain.Cronos -> EllipticCurve.Secp256k1
            Blockchain.AlephZero -> EllipticCurve.Ed25519Slip0010
            Blockchain.AlephZeroTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.OctaSpace -> EllipticCurve.Secp256k1
            Blockchain.OctaSpaceTestnet -> EllipticCurve.Secp256k1
            Blockchain.Chia -> EllipticCurve.Bls12381G2Aug
            Blockchain.ChiaTestnet -> EllipticCurve.Bls12381G2Aug
            Blockchain.Decimal -> EllipticCurve.Secp256k1
            Blockchain.DecimalTestnet -> EllipticCurve.Secp256k1
            Blockchain.XDC -> EllipticCurve.Secp256k1
            Blockchain.XDCTestnet -> EllipticCurve.Secp256k1
            Blockchain.VeChain -> EllipticCurve.Secp256k1
            Blockchain.VeChainTestnet -> EllipticCurve.Secp256k1
            Blockchain.Aptos -> EllipticCurve.Ed25519Slip0010
            Blockchain.AptosTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Playa3ull -> EllipticCurve.Secp256k1
            Blockchain.Shibarium -> EllipticCurve.Secp256k1
            Blockchain.ShibariumTestnet -> EllipticCurve.Secp256k1
            Blockchain.Algorand -> EllipticCurve.Ed25519Slip0010
            Blockchain.AlgorandTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Hedera -> EllipticCurve.Ed25519Slip0010
            Blockchain.HederaTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Aurora -> EllipticCurve.Secp256k1
            Blockchain.AuroraTestnet -> EllipticCurve.Secp256k1
            Blockchain.Areon -> EllipticCurve.Secp256k1
            Blockchain.AreonTestnet -> EllipticCurve.Secp256k1
            Blockchain.PulseChain -> EllipticCurve.Secp256k1
            Blockchain.PulseChainTestnet -> EllipticCurve.Secp256k1
            Blockchain.ZkSyncEra -> EllipticCurve.Secp256k1
            Blockchain.ZkSyncEraTestnet -> EllipticCurve.Secp256k1
            Blockchain.Nexa -> EllipticCurve.Secp256k1
            Blockchain.NexaTestnet -> EllipticCurve.Secp256k1
            Blockchain.Moonbeam -> EllipticCurve.Secp256k1
            Blockchain.MoonbeamTestnet -> EllipticCurve.Secp256k1
            Blockchain.Manta -> EllipticCurve.Secp256k1
            Blockchain.MantaTestnet -> EllipticCurve.Secp256k1
            Blockchain.PolygonZkEVM -> EllipticCurve.Secp256k1
            Blockchain.PolygonZkEVMTestnet -> EllipticCurve.Secp256k1
            Blockchain.Radiant -> EllipticCurve.Secp256k1
            Blockchain.Base -> EllipticCurve.Secp256k1
            Blockchain.BaseTestnet -> EllipticCurve.Secp256k1
            Blockchain.Moonriver -> EllipticCurve.Secp256k1
            Blockchain.MoonriverTestnet -> EllipticCurve.Secp256k1
            Blockchain.Mantle -> EllipticCurve.Secp256k1
            Blockchain.MantleTestnet -> EllipticCurve.Secp256k1
            Blockchain.Fact0rn -> EllipticCurve.Secp256k1
            Blockchain.Flare -> EllipticCurve.Secp256k1
            Blockchain.FlareTestnet -> EllipticCurve.Secp256k1
            Blockchain.Taraxa -> EllipticCurve.Secp256k1
            Blockchain.TaraxaTestnet -> EllipticCurve.Secp256k1
            Blockchain.Koinos -> EllipticCurve.Secp256k1
            Blockchain.KoinosTestnet -> EllipticCurve.Secp256k1
            Blockchain.Joystream -> EllipticCurve.Ed25519Slip0010
            Blockchain.Bittensor -> EllipticCurve.Ed25519Slip0010
            Blockchain.Filecoin -> EllipticCurve.Secp256k1
            Blockchain.Blast -> EllipticCurve.Secp256k1
            Blockchain.BlastTestnet -> EllipticCurve.Secp256k1
            Blockchain.Cyber -> EllipticCurve.Secp256k1
            Blockchain.CyberTestnet -> EllipticCurve.Secp256k1
            Blockchain.InternetComputer -> EllipticCurve.Secp256k1
            Blockchain.Sui -> EllipticCurve.Ed25519Slip0010
            Blockchain.SuiTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.EnergyWebChain -> EllipticCurve.Secp256k1
            Blockchain.EnergyWebChainTestnet -> EllipticCurve.Secp256k1
            Blockchain.EnergyWebX -> EllipticCurve.Ed25519Slip0010
            Blockchain.EnergyWebXTestnet -> EllipticCurve.Ed25519Slip0010
            Blockchain.Casper -> EllipticCurve.Secp256k1
            Blockchain.CasperTestnet -> EllipticCurve.Secp256k1
            Blockchain.Core -> EllipticCurve.Secp256k1
            Blockchain.CoreTestnet -> EllipticCurve.Secp256k1
            Blockchain.Xodex -> EllipticCurve.Secp256k1
            Blockchain.Canxium -> EllipticCurve.Secp256k1
            Blockchain.Chiliz -> EllipticCurve.Secp256k1
            Blockchain.ChilizTestnet -> EllipticCurve.Secp256k1
            Blockchain.Clore -> EllipticCurve.Secp256k1
            Blockchain.VanarChain -> EllipticCurve.Secp256k1
            Blockchain.VanarChainTestnet -> EllipticCurve.Secp256k1
            Blockchain.OdysseyChain -> EllipticCurve.Secp256k1
            Blockchain.OdysseyChainTestnet -> EllipticCurve.Secp256k1
            Blockchain.Bitrock -> EllipticCurve.Secp256k1
            Blockchain.BitrockTestnet -> EllipticCurve.Secp256k1
            Blockchain.Sonic -> EllipticCurve.Secp256k1
            Blockchain.SonicTestnet -> EllipticCurve.Secp256k1
            Blockchain.ApeChain -> EllipticCurve.Secp256k1
            Blockchain.ApeChainTestnet -> EllipticCurve.Secp256k1
            Blockchain.KaspaTestnet -> EllipticCurve.Secp256k1
            Blockchain.Alephium -> EllipticCurve.Secp256k1
            Blockchain.AlephiumTestnet -> EllipticCurve.Secp256k1
            Blockchain.Scroll -> EllipticCurve.Secp256k1
            Blockchain.ScrollTestnet -> EllipticCurve.Secp256k1
            Blockchain.ZkLinkNova -> EllipticCurve.Secp256k1
            Blockchain.ZkLinkNovaTestnet -> EllipticCurve.Secp256k1
            Blockchain.Pepecoin -> EllipticCurve.Secp256k1
            Blockchain.PepecoinTestnet -> EllipticCurve.Secp256k1
            Blockchain.Hyperliquid -> EllipticCurve.Secp256k1
            Blockchain.HyperliquidTestnet -> EllipticCurve.Secp256k1
        }
    }
}