package com.tangem.utils.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Inject

interface CoroutineDispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val single: CoroutineDispatcher
}

class AppCoroutineDispatcherProvider @Inject constructor() : CoroutineDispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val single: CoroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
}

class TestingCoroutineDispatcherProvider(
    override val main: CoroutineDispatcher = Dispatchers.Unconfined,
    override val io: CoroutineDispatcher = Dispatchers.Unconfined,
    override val default: CoroutineDispatcher = Dispatchers.Unconfined,
    override val single: CoroutineDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher(),
) : CoroutineDispatcherProvider

/**
* [REDACTED_TODO_COMMENT]
 *
 *  MamaLemon 5 days ago
 *  Кажется, что для для тестирования лучше будет заюзать Unconfined
 *
 *  Member
* [REDACTED_AUTHOR]
 *  @gbixahue gbixahue 5 days ago
 *  single нужен как раз для того, чтобы гарантировать, что любой запуск корутины будет выполняться только в одном
 *  потоке. Unconfined вроде как не дает такой гарантии.
 *
 *  Member
 *  @kozarezvlad kozarezvlad 5 days ago
 *  а для чего нужен тут тред пул с 1 потоком? если где то нужна синхронизация, то можно в месте использования
 *  рассмотреть потокобезопасные коллекции либо атомарные операции, но пока в голову не приходит случай где это
 *  требуется в данном функционале
 *
 *  Member
* [REDACTED_AUTHOR]
 *  @gbixahue gbixahue 4 days ago
 *  нужен при вводе сид фразы, когда происходит обновление UI с раскрашиванием, отображении подсказок и
 *  отложенная проверка. При использовании io в логах замечал наложение, с single такого не случалось. А при
 *  наложении UI вел себя не корректно
 *
 *  Member
 *  @MamaLemon MamaLemon 4 days ago
 *  При тестировании не желательно реальный поток запускать. Именно поэтому используем Unconfined в тестах
 *
 *  Member
* [REDACTED_AUTHOR]
 *  @gbixahue gbixahue 4 days ago
 *  Не желательно - здесь ключевой слово. single с Unconfined не даст то, для чего он создан и тесты в таком случае
 *  не будут работать.
 *
 *  Member
 *  @MamaLemon MamaLemon 3 days ago
 *  Можешь подробнее, пожалуйста, рассказать, почему тесты не будут работать.
 *  По сути мы же нигде не тестируем на каком потоке выполняется работа. Поэтому я не могу понять, почему мы просто
 *  не можем сразу дать понять при прогоне тестов, что нам не нужно ничего переключать, нам важно сразу получить результат.
 *  Мб у тебя есть уже написанные тесты?
 *
 *  Member
* [REDACTED_AUTHOR]
 *  @gbixahue gbixahue 2 days ago
 *  Single нужен для того, чтобы запускать корутины в едином потоке (чтобы создавалась очередь из событий и они
 *  друг за другом выполнялись). Если ты будешь писать тесты пониманием того, что single работает только в одном
 *  потоке, а он при этом будет использовать любой, то эти тесты уже будут не правильно работать (т.е. иногда
 *  правильно, иногда нет из-за гонки потоков).
 *  Нет, написанных тестов, к сожалению, нет.
 */
