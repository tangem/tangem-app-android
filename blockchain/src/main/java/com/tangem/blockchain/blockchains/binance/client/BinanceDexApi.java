package com.tangem.blockchain.blockchains.binance.client;

import com.tangem.blockchain.blockchains.binance.client.domain.Account;
import com.tangem.blockchain.blockchains.binance.client.domain.AccountSequence;
import com.tangem.blockchain.blockchains.binance.client.domain.Candlestick;
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

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BinanceDexApi {
    @GET("/api/v1/time")
    Call<Time> getTime();

    @GET("/api/v1/node-info")
    Call<Infos> getNodeInfo();

    @GET("/api/v1/validators")
    Call<Validators> getValidators();

    @GET("/api/v1/peers")
    Call<List<Peer>> getPeers();

    @GET("/api/v1/account/{address}")
    Call<Account> getAccount(@Path("address") String address);

    @GET("/api/v1/account/{address}/sequence")
    Call<AccountSequence> getAccountSequence(@Path("address") String address);

    @GET("/api/v1/tx/{hash}")
    Call<TransactionMetadata> getTransactionMetadata(@Path("hash") String hash);

    @GET("/api/v1/tokens")
    Call<List<Token>> getTokens();

    @GET("/api/v1/markets")
    Call<List<Market>> getMarkets();


    @GET("/api/v1/depth")
    Call<OrderBook> getOrderBook(@Query("symbol") String symbol, @Query("limit") Integer limit);

    @GET("/api/v1/klines")
    Call<List<Candlestick>> getCandlestickBars(@Query("symbol") String symbol, @Query("interval") String interval,
                                               @Query("limit") Integer limit, @Query("startTime") Long startTime,
                                               @Query("endTime") Long endTime);

    @GET("/api/v1/orders/open")
    Call<OrderList> getOpenOrders(@Query("address") String address, @Query("limit") Integer limit,
                                  @Query("offset") Integer offset, @Query("symbol") String symbol,
                                  @Query("total") Integer total);

    @GET("/api/v1/orders/closed")
    Call<OrderList> getClosedOrders(@Query("address") String address, @Query("end") Long end,
                                    @Query("limit") Integer limit, @Query("offset") Integer offset,
                                    @Query("side") String side, @Query("start") Long start,
                                    @Query("status") List<String> status, @Query("symbol") String symbol,
                                    @Query("total") Integer total);

    @GET("/api/v1/orders/{id}")
    Call<Order> getOrder(@Path("id") String id);

    @GET("/api/v1/ticker/24hr")
    Call<List<TickerStatistics>> get24HrPriceStatistics();

    @GET("/api/v1/trades")
    Call<TradePage> getTrades(@Query("address") String address,
                              @Query("buyerOrderId") String buyerOrderId, @Query("end") Long end,
                              @Query("height") Long height, @Query("limit") Integer limit,
                              @Query("offset") Integer offset, @Query("quoteAsset") String quoteAsset,
                              @Query("sellerOrderId") String sellerOrderId, @Query("side") String side,
                              @Query("start") Long start, @Query("symbol") String symbol, @Query("total") Integer total);

    @GET("/api/v1/transactions")
    Call<TransactionPage> getTransactions(@Query("address") String address, @Query("blockHeight") Long blockHeight,
                                          @Query("endTime") Long endTime, @Query("limit") Integer limit,
                                          @Query("offset") Integer offset, @Query("side") String side,
                                          @Query("startTime") Long startTime, @Query("txAsset") String txAsset,
                                          @Query("txType") String txType);

    @POST("/api/v1/broadcast")
    Call<List<TransactionMetadata>> broadcast(@Query("sync") boolean sync, @Body RequestBody transaction);
}
