package com.example.demo;

import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Item;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;

public class ItemTests {

    @Test
    public void equalsTest() {
        Item i1 = new Item();
        Item i2 = new Item();
        Assertions.assertEquals(i1, i2);

        i1.setId(1L);
        i2.setId(2L);
        Assertions.assertNotEquals(i1, i2);

        i2.setId(1L);
        Assertions.assertEquals(i1, i2);
    }
}
