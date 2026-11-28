package com.example.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollPinManagerTest {
  @Test
  fun acceptsOnlyExactlySixNumericDigits() {
    assertTrue(PayrollPinManager.isValidPin("123456"))
    assertFalse(PayrollPinManager.isValidPin("12345"))
    assertFalse(PayrollPinManager.isValidPin("1234567"))
    assertFalse(PayrollPinManager.isValidPin("12a456"))
    assertFalse(PayrollPinManager.isValidPin("１２３４５６"))
    assertFalse(PayrollPinManager.isValidPin(""))
  }
}
