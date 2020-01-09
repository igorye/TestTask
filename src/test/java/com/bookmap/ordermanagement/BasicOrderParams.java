package com.bookmap.ordermanagement;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Created by igorye on 09.01.2020.
 */
public class BasicOrderParams {
    static Stream<Arguments> invalidArgs() {
        return Stream.of(
                Arguments.of(-1, 1),
                Arguments.of(0, 1),
                Arguments.of(1, -1),
                Arguments.of(1, 0)
        );
    }

}
