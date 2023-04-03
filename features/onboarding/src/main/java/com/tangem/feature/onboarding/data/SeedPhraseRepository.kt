package com.tangem.feature.onboarding.data

import com.tangem.crypto.bip39.EntropyLength
import com.tangem.crypto.bip39.Wordlist

/**
* [REDACTED_AUTHOR]
 */
interface SeedPhraseRepository {
    fun getWordList(): Wordlist
}

class DummySeedPhraseRepository : SeedPhraseRepository {
    override fun getWordList(): Wordlist = DummyWordlist()
}

class DummyWordlist(
    entropyLength: EntropyLength = EntropyLength.Bits128Length,
    random: Boolean = false,
) : Wordlist {

    private var _wordlist: List<String> = listOf()

    override val wordlist: List<String>
        get() = _wordlist

    init {
        if (random) regenerateRandom() else regenerate(entropyLength)
    }

    fun regenerateRandom(entropyLength: EntropyLength = EntropyLength.Bits128Length) {
        val minWordLength = 4
        val maxWordLength = 10
        _wordlist = (0 until entropyLength.wordCount())
            .map {
                randomString(maxLength = randomInt(minWordLength, maxWordLength))
            }
    }

    fun regenerate(entropyLength: EntropyLength = EntropyLength.Bits128Length) {
        _wordlist = getStaticWordlist().subList(0, entropyLength.wordCount() - 1)
    }

    private fun randomString(maxLength: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (0..maxLength)
            .map { randomInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun randomInt(from: Int, to: Int): Int = kotlin.random.Random.nextInt(from, to)

    private fun getStaticWordlist(): List<String> {
        return listOf(
            "lorem",
            "ipsum",
            "dolor",
            "amet",
            "consectetur",
            "adipiscing",
            "elit",
            "eiusmod",
            "tempor",
            "incididunt",
            "labore",
            "dolore",
            "magna",
            "aliqua",
            "enim",
            "minim",
            "veniam",
            "quis",
            "nostrud",
            "exercitation",
            "ullamco",
            "laboris",
            "nisi",
            "aliquip",
            "commodo",
            "consequat",
            "duis",
            "aute",
            "irure",
            "reprehenderit",
            "voluptate",
            "velit",
            "esse",
            "cillum",
            "fugiat",
            "nulla",
            "pariatur",
            "excepteur",
            "sint",
            "occaecat",
            "cupidatat",
            "proident",
            "sunt",
            "culpa",
            "officia",
            "deserunt",
            "mollit",
            "anim",
            "laborum",
        )
    }
}
