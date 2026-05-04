package com.tangem.domain.wallets.usecase

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.common.util.getCardsCount
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletIcon
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.wallets.repository.WalletsRepository
import kotlinx.coroutines.runBlocking

class GetWalletIconUseCase(
    private val walletsRepository: WalletsRepository,
) {

    @Suppress("CyclomaticComplexMethod", "UnsafeCallOnNullableType")
    operator fun invoke(userWallet: UserWallet): UserWalletIcon {
        if (userWallet.isHotWallet) {
            return UserWalletIcon.Hot
        }

        userWallet.requireColdWallet()

        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        val cardsCount = userWallet.getCardsCount() ?: 1

        val cobrandColor by lazy {
            colorByBatchId(userWallet.scanResponse.card.batchId)
        }

        return when {
            cardTypesResolver.isDevKit() -> otherColor(OtherCardType.Devkit)
            userWallet.isRing() -> UserWalletIcon.Default(isRing = true, cardsCount = cardsCount)
            cobrandColor != null -> cobrandColor!!.withCount(cardsCount)
            cardTypesResolver.isWallet2() -> UserWalletIcon.Default(isRing = false, cardsCount = cardsCount)
            cardTypesResolver.isShibaWallet() -> otherColor(OtherCardType.Shiba, cardsCount)
            cardTypesResolver.isTangemWallet() -> otherColor(OtherCardType.Wallet1, cardsCount)
            cardTypesResolver.isWhiteWallet() -> otherColor(OtherCardType.WhiteWallet, cardsCount)
            cardTypesResolver.isTangemTwins() -> otherColor(OtherCardType.Twins, cardsCount)
            cardTypesResolver.isStart2Coin() -> otherColor(OtherCardType.Starts2com, cardsCount)
            cardTypesResolver.isTangemNote() ->
                resolveNoteColor(userWallet) ?: UserWalletIcon.Stub(cardsCount = cardsCount)
            DemoConfig.isDemoCardId(cardId = userWallet.cardId) ->
                UserWalletIcon.Default(isRing = false, cardsCount = cardsCount)
            else -> UserWalletIcon.Stub(cardsCount = cardsCount)
        }
    }

    private fun resolveNoteColor(userWallet: UserWallet.Cold): UserWalletIcon? {
        val noteBlockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()

        val otherCardType = when (noteBlockchain) {
            Blockchain.Bitcoin -> OtherCardType.NoteBitcoin
            Blockchain.Ethereum -> OtherCardType.NoteEthereum
            Blockchain.XRP -> OtherCardType.NoteXRP
            Blockchain.Binance -> OtherCardType.NoteBinance
            Blockchain.Cardano -> OtherCardType.NoteCardano
            Blockchain.Dogecoin -> OtherCardType.NoteDoge
            else -> return null
        }

        return otherColor(otherCardType)
    }

    private fun otherColor(otherCardType: OtherCardType, cardsCount: Int = 1): UserWalletIcon.Colored {
        return UserWalletIcon.Colored(
            isRing = false,
            mainColor = otherCardType.mainColor,
            secondColor = if (cardsCount > 1) otherCardType.mainColor else null,
            thirdColor = if (cardsCount > 2) otherCardType.mainColor else null,
        )
    }

    private fun UserWalletIcon.Colored.withCount(count: Int): UserWalletIcon.Colored {
        return this.copy(
            mainColor = mainColor,
            secondColor = if (count > 1) secondColor else null,
            thirdColor = if (count > 2) thirdColor else null,
        )
    }

    private fun UserWallet.Cold.isRing(): Boolean {
        return scanResponse.cardTypesResolver.isRing() ||
            runBlocking { walletsRepository.isWalletWithRing(userWalletId = this@isRing.walletId) }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun colorByBatchId(batchId: String): UserWalletIcon.Colored? {
        fun color(main: String, second: String = main, third: String = main) =
            UserWalletIcon.Colored(isRing = false, mainColor = main, secondColor = second, thirdColor = third)

        val cobrandType = CobrandType.entries.firstOrNull { it.batchIds.contains(batchId) }

        return when (cobrandType) {
            CobrandType.Avrora -> color("#1E1E1C")
            CobrandType.BabyDoge -> color("#E7D34C")
            CobrandType.Bad -> color("#395467")
            CobrandType.BitcoinGold -> color("#F08A1F")
            CobrandType.BitcoinPizzaDay -> color("#AF4D37")
            CobrandType.BitcoinPizza2 -> color("#F3C63A")
            CobrandType.BTC365 -> color("#191E3E")
            CobrandType.CashClubGold -> color("#D1BF78")
            CobrandType.Changenow -> color("#191B2C")
            CobrandType.Chilliz -> color("#49324A")
            CobrandType.CoinMetrica -> color("#A140C7")
            CobrandType.COQ -> color("#ED1F3A")
            CobrandType.CryptoCasey -> color("#301E45")
            CobrandType.CryptoOrg -> color("#27B39A")
            CobrandType.CryptoSeth -> color("#414954")
            CobrandType.GetsMine -> color("#AFC3CE")
            CobrandType.Grim -> color("#131313")
            CobrandType.Hodl -> color("#111111")
            CobrandType.Jr -> color("#1C1C1C")
            CobrandType.Kaspa -> color("#545C5C")
            CobrandType.Kaspa2 -> color("#353535")
            CobrandType.KaspaReseller -> color("#3B3D3A")
            CobrandType.Kaspa3 -> color("#85CBC1")
            CobrandType.Kasper -> color("#34302E")
            CobrandType.Kaspy -> color("#DEC764")
            CobrandType.Keiro -> color("#4D645C")
            CobrandType.KishuInu -> color("#2DA4CE")
            CobrandType.Kango -> color("#5BA495")
            CobrandType.Konan -> color("#64C9C9")
            CobrandType.Kroak -> color("#8EB8AF")
            CobrandType.Neiro -> color("#E7A524")
            CobrandType.NewWorldElite -> color("#292722")
            CobrandType.PassimPay -> color("#6A4361")
            CobrandType.Pastel -> color("#FFC7A4", "#84A479", "#6FB5BB")
            CobrandType.Pepecoin -> color("#303439")
            CobrandType.RamenCat -> color("#DEBE88")
            CobrandType.RedPanda -> color("#C5C5C5")
            CobrandType.Rizo -> color("#1765A7")
            CobrandType.Sakura -> color("#F0E9C4")
            CobrandType.SatoshiFriends -> color("#242424")
            CobrandType.SinCity -> color("#D3C487")
            CobrandType.SpringBloom -> color("#FAC73A")
            CobrandType.StealthCard -> color("#555557")
            CobrandType.SunDrop -> color("#FEC035")
            CobrandType.Trillant -> color("#955091")
            CobrandType.Tron -> color("#D4221D")
            CobrandType.Upbit -> color("#2B2B2B")
            CobrandType.USA -> color("#0D185F")
            CobrandType.VeChain -> color("#5186A2")
            CobrandType.Vivid -> color("#D3CF09", "#F76952", "#2BCAD1")
            CobrandType.Vnish -> color("#292522")
            CobrandType.VoltInu -> color("#34312B")
            CobrandType.WhiteTangem -> color("#D3D3D3")
            CobrandType.WildGoat -> color("#1B1B1B")
            CobrandType.Winter -> color("#7FB9C8", "#80BDE8", "#B2C6E4")
            CobrandType.WinterSakura -> color("#88B9E9")
            CobrandType.LockedMoney -> color("#272625")
            CobrandType.Ghoad -> color("#6EC5C5")
            CobrandType.BlushSky -> color("#C1E9E8", "#FACAD3", "#DCCEE0")
            CobrandType.ElectraSea -> color("#0D5A67", "#29939E", "#30C6B1")
            CobrandType.Football -> color("#1AA71E")
            CobrandType.French -> color("#3249A1")
            CobrandType.HyperBlue -> color("#0F397C", "#1474D3", "#0BC9EC")
            CobrandType.Lunar -> color("#B0313A")
            CobrandType.Metaplanet -> color("#635955")
            null -> null
        }
    }
}

private enum class CobrandType(val batchIds: List<String>) {
    Avrora(listOf("AF18")),
    BabyDoge(listOf("AF51")),
    Bad(listOf("AF09")),
    BitcoinGold(listOf("AF71", "AF990016", "AF990009")),
    BitcoinPizzaDay(listOf("AF33")),
    BitcoinPizza2(listOf("AF990019")),
    BTC365(listOf("AF97")),
    CashClubGold(listOf("BB000004")),
    Changenow(listOf("BB000013")),
    Chilliz(listOf("BB000016")),
    CoinMetrica(listOf("AF27")),
    COQ(listOf("AF28")),
    CryptoCasey(listOf("AF21", "AF22", "AF23")),
    CryptoOrg(listOf("AF57")),
    CryptoSeth(listOf("AF32")),
    GetsMine(listOf("BB000008")),
    Grim(listOf("AF13")),
    Hodl(listOf("BB000009")),
    Jr(listOf("AF14")),
    Kaspa(listOf("AF08")),
    Kaspa2(listOf("AF25", "AF61", "AF72")),
    KaspaReseller(listOf("AF31")),
    Kaspa3(listOf("AF73")),
    Kasper(listOf("AF96")),
    Kaspy(listOf("AF95")),
    Keiro(listOf("BB000017")),
    KishuInu(listOf("AF52")),
    Kango(listOf("BB000006")),
    Konan(listOf("AF93")),
    Kroak(listOf("BB000011")),
    Neiro(listOf("AF98")),
    NewWorldElite(listOf("AF26")),
    PassimPay(listOf("BB000007")),
    Pastel(listOf("AF43", "AF44", "AF45", "AF78", "AF79", "AF80")),
    Pepecoin(listOf("BB000015")),
    RamenCat(listOf("AF990006", "AF990007", "AF990008")),
    RedPanda(listOf("AF34", "BB000038")),
    Rizo(listOf("BB000012")),
    Sakura(listOf("AF990029", "AF990030", "AF990031", "AF990071", "AF990072", "AF990073")),
    SatoshiFriends(listOf("AF19")),
    SinCity(listOf("BB000010")),
    SpringBloom(listOf("AF990001", "AF990002", "AF990004")),
    StealthCard(listOf("AF60", "AF74", "AF88")),
    SunDrop(listOf("AF990005", "AF990003")),
    Trillant(listOf("AF16")),
    Tron(listOf("AF07")),
    Upbit(listOf("BB000019")),
    USA(listOf("AF91", "AF990017", "AF990056")),
    VeChain(listOf("AF29")),
    Vivid(listOf("AF40", "AF41", "AF42", "AF75", "AF76", "AF77")),
    Vnish(listOf("BB000005")),
    VoltInu(listOf("AF35")),
    WhiteTangem(listOf("AF15")),
    WildGoat(listOf("BB000001")),
    Winter(listOf("AF85", "AF86", "AF87", "AF990013", "AF990012", "AF990011")),
    WinterSakura(listOf("AF990053", "AF990054", "AF990055")),
    LockedMoney(listOf("AF63")),
    Ghoad(listOf("AF89")),
    BlushSky(listOf("AF990020", "AF990021", "AF990022")),
    ElectraSea(listOf("AF990023", "AF990024", "AF990025")),
    Football(listOf("AF990090", "AF990089", "AF990088")),
    French(listOf("AF990084", "AF990085", "AF990086")),
    HyperBlue(listOf("AF990026", "AF990027", "AF990028", "AF990050", "AF990051", "AF990052")),
    Lunar(listOf("AF990057", "AF990058", "AF990059")),
    Metaplanet(listOf("BB000040")),
}

private enum class OtherCardType(val mainColor: String) {
    NoteXRP("#726799"),
    NoteDoge("#BFB565"),
    NoteEthereum("#989C9F"),
    NoteBinance("#C9B87C"),
    NoteCardano("#5979AD"),
    NoteBitcoin("#EABE8B"),
    Starts2com("#356F99"),
    Wallet1("#2E3944"),
    Twins("#B8B7B6"),
    Devkit("#938C92"),
    WhiteWallet("#DDDDDD"),
    Shiba("#D5963A"),
}