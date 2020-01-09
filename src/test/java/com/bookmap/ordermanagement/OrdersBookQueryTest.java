package com.bookmap.ordermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.bookmap.ordermanagement.BasicOrder.OrderSide.ASK;
import static com.bookmap.ordermanagement.BasicOrder.OrderSide.BID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by igorye on 09.01.2020.
 */
class OrdersBookQueryTest {

    private OrdersBook book;

    @BeforeEach
    void setUp() {
        book = new OrdersBook();
        book.addOrder(new BasicOrder(10, 1, ASK));
        book.addOrder(new BasicOrder(5, 1, BID));
    }

    @ParameterizedTest
    @MethodSource("com.bookmap.ordermanagement.BasicOrderParams#invalidArgs")
    void addingInvalidOrdersDoesntChangeBookState(int price, int size) {
        final int bestAskPrice = book.queryBestAsk();
        final int bestAskSize = book.querySize(bestAskPrice);

        final int bestBidPrice = book.queryBestBid();
        final int bestBidSize = book.querySize(bestBidPrice);

        assertThrows(IllegalArgumentException.class, () -> book.addOrder(new BasicOrder(price, size, ASK)));
        assertEquals(bestAskPrice, book.queryBestAsk());
        assertEquals(bestAskSize, book.querySize(book.queryBestAsk()));
        assertEquals(bestBidPrice, book.queryBestBid());
        assertEquals(bestBidSize, book.querySize(book.queryBestBid()));

        assertThrows(IllegalArgumentException.class, () -> book.addOrder(new BasicOrder(price, size, BID)));
        assertEquals(bestAskPrice, book.queryBestAsk());
        assertEquals(bestAskSize, book.querySize(book.queryBestAsk()));
        assertEquals(bestBidPrice, book.queryBestBid());
        assertEquals(bestBidSize, book.querySize(book.queryBestBid()));
    }

    @ParameterizedTest
    @MethodSource("samePriceLevels")
    void addingOrderAtSamePriceIncreasingSize(int price, int size, BasicOrder.OrderSide side) {
        int expected = size + book.querySize(price);
        book.addOrder(new BasicOrder(price, size, side));
        assertEquals(expected, book.querySize(price));
    }

    static Stream<Arguments> samePriceLevels() {
        return Stream.of(Arguments.of(10, 2, ASK), Arguments.of(5, 3, BID));
    }

    @ParameterizedTest
    @MethodSource("differentPriceLevels")
    void addingOrderAtDifferentPriceDoesntChangeBestAskAndBestBid( int price, BasicOrder.OrderSide side ) {
        int bestAskSize = book.querySize(book.queryBestAsk());
        int bestBidSize = book.querySize(book.queryBestBid());
        book.addOrder(new BasicOrder(price, 1, side));
        assertEquals(bestAskSize, book.querySize(price));
        assertEquals(bestBidSize, book.querySize(price));
    }

    static Stream<Arguments> differentPriceLevels() {
        return Stream.of(Arguments.of(11, ASK), Arguments.of(4, BID));
    }


    @Test
    void addingBidHigherThanBestBidChangesTheLatter() {
        int bestBidPrice = book.queryBestBid();
        book.addOrder(new BasicOrder(bestBidPrice + 1, 1, BID));
        final int newBestBid = book.queryBestBid();
        assertEquals(bestBidPrice + 1, newBestBid);
        assertTrue(newBestBid > bestBidPrice);
    }

    @Test
    void addingAskLowerThanBestAskChangesTheLatter() {
        int bestAskPrice = book.queryBestAsk();
        book.addOrder(new BasicOrder(bestAskPrice - 1, 1, ASK));
        final int newBestAsk = book.queryBestAsk();
        assertEquals(bestAskPrice - 1, newBestAsk);
        assertTrue(newBestAsk < bestAskPrice);
    }

    @Test
    void buyingBestAskSizeAmountIncreasesBestAsk() {
        final int bestAsk = book.queryBestAsk();
        book.addOrder(new BasicOrder(bestAsk + 1, 1, ASK));
        book.buy(book.querySize(bestAsk));
        assertTrue(book.queryBestAsk() > bestAsk);
    }

    @Test
    void sellingBestBidSizeAmountDecreasesBestBid() {
        final int bestBid = book.queryBestBid();
        book.addOrder(new BasicOrder(bestBid - 1, 1, BID));
        book.sell(book.querySize(bestBid));
        assertTrue(book.queryBestBid() < bestBid);
    }
}