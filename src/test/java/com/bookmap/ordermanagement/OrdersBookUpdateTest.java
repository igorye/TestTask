package com.bookmap.ordermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.bookmap.ordermanagement.BasicOrder.OrderSide.ASK;
import static com.bookmap.ordermanagement.BasicOrder.OrderSide.BID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by igorye on 08.01.2020.
 */
class OrdersBookUpdateTest {

    OrdersBook book;

    @BeforeEach
    void setUp() {
        book = new OrdersBook(true);
    }

    @ParameterizedTest
    @MethodSource("com.bookmap.ordermanagement.BasicOrderParams#invalidArgs")
    void shouldThrowOnInvalidOrder(int price, int size) {
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(new BasicOrder(price, size, ASK)));
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(new BasicOrder(price, size, BID)));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 9})
    void shouldThrowOnAddAskEqualOrLowerThanBestBid(int priceLevel) {
        book.addOrder(new BasicOrder(10, 1, BID));
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(new BasicOrder(priceLevel, 1, ASK)));
    }

    @Test
    void addOneAsk() {
        assertEquals(0, book.queryBestAsk());
        book.addOrder(new BasicOrder(10, 1, ASK));
        assertEquals(10, book.queryBestAsk());
        assertEquals(1, book.querySize(10));
        assertEquals(0, book.queryBestBid());
    }

    @Test
    void addFewAsks() {
        book.addOrder(new BasicOrder(10, 1, ASK));
        book.addOrder(new BasicOrder(10, 2, ASK));
        assertEquals(10, book.queryBestAsk());
        assertEquals(3, book.querySize(10));
        assertEquals(0, book.queryBestBid());
    }

    @Test
    void addAsksOnFewPriceLevels() {
        book.addOrder(new BasicOrder(10, 1, ASK));
        assertEquals(10, book.queryBestAsk());
        book.addOrder(new BasicOrder(11, 2, ASK));
        assertEquals(10, book.queryBestAsk());
        assertEquals(1, book.querySize(10));
        assertEquals(2, book.querySize(11));
        assertEquals(0, book.queryBestBid());
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 10})
    void shouldThrowOnAddBidEqualOrHigherThanBestAsk(int priceLevel) {
        book.addOrder(new BasicOrder(9, 1, ASK));
        assertThrows(IllegalArgumentException.class, () -> book.addOrder(new BasicOrder(priceLevel, 1, BID)));
    }

    @Test
    void addOneBid() {
        assertEquals(0, book.queryBestBid());
        book.addOrder(new BasicOrder(10, 1, BID));
        assertEquals(10, book.queryBestBid());
        assertEquals(1, book.querySize(10));
        assertEquals(0, book.queryBestAsk());
    }

    @Test
    void addFewBids() {
        book.addOrder(new BasicOrder(10, 1, BID));
        book.addOrder(new BasicOrder(10, 2, BID));
        assertEquals(10, book.queryBestBid());
        assertEquals(3, book.querySize(10));
        assertEquals(0, book.queryBestAsk());
    }

    @Test
    void addBidsOnFewPriceLevels() {
        book.addOrder(new BasicOrder(10, 1, BID));
        assertEquals(10, book.queryBestBid());
        book.addOrder(new BasicOrder(11, 2, BID));
        assertEquals(11, book.queryBestBid());
        assertEquals(1, book.querySize(10));
        assertEquals(2, book.querySize(11));
        assertEquals(0, book.queryBestAsk());
    }

}