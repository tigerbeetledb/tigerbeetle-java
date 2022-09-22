package com.tigerbeetle;

import org.junit.Assert;
import org.junit.Test;

public class CreateAccountResultTest {

    @Test
    public void testFromValue() {
        var value = 10;
        Assert.assertEquals(CreateAccountResult.OverflowsCredits,
                CreateAccountResult.fromValue(value));
    }

    @Test
    public void testOrdinal() {
        var value = 15;
        Assert.assertEquals(CreateAccountResult.ExistsWithDifferentLedger.ordinal(), value);
    }

    @Test(expected = AssertionError.class)
    public void testInvalidValue() {
        var value = 999;
        CreateAccountResult.fromValue(value);
    }


}