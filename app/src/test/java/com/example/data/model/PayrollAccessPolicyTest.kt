package com.example.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollAccessPolicyTest {
  @Test
  fun ownerAndAssignedAccountantCanViewTeamPayroll() {
    assertTrue(MemberRole.OWNER.canViewTeamPayroll())
    assertTrue(MemberRole.ACCOUNTANT.canViewTeamPayroll())
  }

  @Test
  fun onlyOwnerCanManageTeamPayroll() {
    assertTrue(MemberRole.OWNER.canManageTeamPayroll())
    assertFalse(MemberRole.ACCOUNTANT.canManageTeamPayroll())
    assertFalse(MemberRole.MANAGER.canManageTeamPayroll())
    assertFalse(MemberRole.EMPLOYEE.canManageTeamPayroll())
  }

  @Test
  fun managerAndEmployeeCannotViewTeamPayroll() {
    assertFalse(MemberRole.MANAGER.canViewTeamPayroll())
    assertFalse(MemberRole.EMPLOYEE.canViewTeamPayroll())
  }

  @Test
  fun unknownRoleDefaultsToEmployee() {
    assertTrue(MemberRole.fromWire("unknown").canViewTeamPayroll().not())
  }
}
