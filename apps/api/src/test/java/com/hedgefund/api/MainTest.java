package com.hedgefund.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainTest {
    @Test
    void mainStarts() {
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }
}
