package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollAccessPolicyTest {
  @Test
  fun onlyOwnerCanViewTeamPayrollUntilAccountantAuthorizationExists() {
    assertTrue(MemberRole.OWNER.canViewTeamPayroll())
    assertFalse(MemberRole.MANAGER.canViewTeamPayroll())
    assertFalse(MemberRole.EMPLOYEE.canViewTeamPayroll())
  }
}
