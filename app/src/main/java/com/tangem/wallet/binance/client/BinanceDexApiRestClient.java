package com.tangem.wallet.binance.client;

import com.tangem.wallet.TangemContext;
import com.tangem.wallet.binance.BinanceData;
import com.tangem.wallet.binance.client.domain.*;
import com.tangem.wallet.binance.client.domain.broadcast.*;
import com.tangem.wallet.binance.client.domain.request.ClosedOrdersRequest;
import com.tangem.wallet.binance.client.domain.request.OpenOrdersRequest;
import com.tangem.wallet.binance.client.domain.request.TradesRequest;
import com.tangem.wallet.binance.client.domain.request.TransactionsRequest;
import com.tangem.wallet.binance.client.encoding.message.TransactionRequestAssemblerExtSign;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import okhttp3.RequestBody;

public interface BinanceDexApiRestClient {
    Time getTime();

    Infos getNodeInfo();

    Validators getValidators();

    List<Peer> getPeers();

    List<Market> getMarkets();

    Account getAccount(String address);

    AccountSequence getAccountSequence(String address);

    TransactionMetadata getTransactionMetadata(String hash);

    List<Token> getTokens();

    OrderBook getOrderBook(String symbol, Integer limit);

    List<Candlestick> getCandleStickBars(String symbol, CandlestickInterval interval);

    List<Candlestick> getCandleStickBars(String symbol, CandlestickInterval interval, Integer limit, Long startTime, Long endTime);

    OrderList getOpenOrders(String address);

    OrderList getOpenOrders(OpenOrdersRequest request);

    OrderList getClosedOrders(String address);

    OrderList getClosedOrders(ClosedOrdersRequest request);

    Order getOrder(String id);

    List<TickerStatistics> get24HrPriceStatistics();

    TradePage getTrades();

    TradePage getTrades(TradesRequest request);

    TransactionPage getTransactions(String address);

    TransactionPage getTransactions(TransactionsRequest request);

    public List<TransactionMetadata> broadcastNoWallet(RequestBody requestBody, boolean sync) throws BinanceDexApiException;

    List<TransactionMetadata> newOrder(NewOrder newOrder, Wallet wallet, TransactionOption options, boolean sync)
            throws IOException, NoSuchAlgorithmException;

    List<TransactionMetadata> cancelOrder(CancelOrder cancelOrder, Wallet wallet, TransactionOption options, boolean sync)
            throws IOException, NoSuchAlgorithmException;

    List<TransactionMetadata> transfer(Transfer transfer, Wallet wallet, TransactionOption options, boolean sync)
            throws IOException, NoSuchAlgorithmException;

    TransactionRequestAssemblerExtSign prepareTransfer(Transfer transfer, BinanceData binanceData, byte[] pubKey, TransactionOption options, boolean sync)
            throws IOException, NoSuchAlgorithmException;

    List<TransactionMetadata> freeze(TokenFreeze freeze, Wallet wallet, TransactionOption options, boolean sync)
            throws IOException, NoSuchAlgorithmException;

    List<TransactionMetadata> unfreeze(TokenUnfreeze unfreeze, Wallet wallet, TransactionOption options, boolean sync)
            throws IOException, NoSuchAlgorithmException;
}
