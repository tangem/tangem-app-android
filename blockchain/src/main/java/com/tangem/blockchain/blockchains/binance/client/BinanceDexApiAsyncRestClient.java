package com.tangem.blockchain.blockchains.binance.client;

import com.tangem.blockchain.blockchains.binance.client.domain.Account;
import com.tangem.blockchain.blockchains.binance.client.domain.AccountSequence;
import com.tangem.blockchain.blockchains.binance.client.domain.Candlestick;
import com.tangem.blockchain.blockchains.binance.client.domain.CandlestickInterval;
import com.tangem.blockchain.blockchains.binance.client.domain.Infos;
import com.tangem.blockchain.blockchains.binance.client.domain.Market;
import com.tangem.blockchain.blockchains.binance.client.domain.Order;
import com.tangem.blockchain.blockchains.binance.client.domain.OrderBook;
import com.tangem.blockchain.blockchains.binance.client.domain.OrderList;
import com.tangem.blockchain.blockchains.binance.client.domain.Peer;
import com.tangem.blockchain.blockchains.binance.client.domain.TickerStatistics;
import com.tangem.blockchain.blockchains.binance.client.domain.Time;
import com.tangem.blockchain.blockchains.binance.client.domain.Token;
import com.tangem.blockchain.blockchains.binance.client.domain.TradePage;
import com.tangem.blockchain.blockchains.binance.client.domain.TransactionMetadata;
import com.tangem.blockchain.blockchains.binance.client.domain.TransactionPage;
import com.tangem.blockchain.blockchains.binance.client.domain.Validators;
import com.tangem.blockchain.blockchains.binance.client.domain.request.ClosedOrdersRequest;
import com.tangem.blockchain.blockchains.binance.client.domain.request.OpenOrdersRequest;
import com.tangem.blockchain.blockchains.binance.client.domain.request.TradesRequest;
import com.tangem.blockchain.blockchains.binance.client.domain.request.TransactionsRequest;

import java.util.List;

public interface BinanceDexApiAsyncRestClient {
    void getTime(BinanceDexApiCallback<Time> callback);

    void getNodeInfo(BinanceDexApiCallback<Infos> callback);

    void getValidators(BinanceDexApiCallback<Validators> callback);

    void getPeers(BinanceDexApiCallback<List<Peer>> callback);

    void getMarkets(BinanceDexApiCallback<List<Market>> callback);

    void getAccount(String address, BinanceDexApiCallback<Account> callback);

    void getAccountSequence(String address, BinanceDexApiCallback<AccountSequence> callback);

    void getTransactionMetadata(String hash, BinanceDexApiCallback<TransactionMetadata> callback);

    void getTokens(BinanceDexApiCallback<List<Token>> callback);

    void getOrderBook(String symbol, Integer limit, BinanceDexApiCallback<OrderBook> callback);

    void getCandleStickBars(String symbol, CandlestickInterval interval,
                            BinanceDexApiCallback<List<Candlestick>> callback);

    void getCandleStickBars(String symbol, CandlestickInterval interval, Integer limit, Long startTime, Long endTime,
                            BinanceDexApiCallback<List<Candlestick>> callback);

    void getOpenOrders(String address, BinanceDexApiCallback<OrderList> callback);

    void getOpenOrders(OpenOrdersRequest request, BinanceDexApiCallback<OrderList> callback);

    void getClosedOrders(String address, BinanceDexApiCallback<OrderList> callback);

    void getClosedOrders(ClosedOrdersRequest request, BinanceDexApiCallback<OrderList> callback);

    void getOrder(String id, BinanceDexApiCallback<Order> callback);

    void get24HrPriceStatistics(BinanceDexApiCallback<List<TickerStatistics>> callback);

    void getTrades(BinanceDexApiCallback<TradePage> callback);

    void getTrades(TradesRequest request, BinanceDexApiCallback<TradePage> callback);

    void getTransactions(String address, BinanceDexApiCallback<TransactionPage> callback);

    void getTransactions(TransactionsRequest request, BinanceDexApiCallback<TransactionPage> callback);

    // Do not support async broadcast due to account sequence
}
