package com.emergencypreparedness.manager.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class NotificationSchedulerTest {

  //region Generate Request Code Test
  @Test
  public void generateRequestCode_isStableForSameInputs() {
    int a = NotificationScheduler.generateRequestCode("123", NotificationScheduler.TYPE_ITEM_ZERO);
    int b = NotificationScheduler.generateRequestCode("123", NotificationScheduler.TYPE_ITEM_ZERO);
    assertEquals(a, b);
  }
  //endregion

  //region Subtract Days Test
  @Test
  public void subtractDays_subtractsCorrectly() {
    String out = NotificationScheduler.subtractDays("03/10/2026", 2);
    assertEquals("03/08/2026", out);
  }
  //endregion
}
