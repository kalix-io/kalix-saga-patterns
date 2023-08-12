package com.example.wallet.domain;

import kalix.javasdk.annotations.TypeName;

import java.math.BigDecimal;

public sealed interface WalletEvent {

  @TypeName("wallet-created")
  record WalletCreated(String walletId, BigDecimal initialAmount) implements WalletEvent {
  }

  @TypeName("wallet-charged")
  record WalletCharged(String walletId, BigDecimal amount, String expenseId, String commandId) implements WalletEvent {
  }

  @TypeName("wallet-refunded")
  record WalletRefunded(String walletId, BigDecimal amount, String expenseId, String commandId) implements WalletEvent {
  }

  @TypeName("funds-deposited")
  record FundsDeposited(String walletId, BigDecimal amount, String commandId) implements WalletEvent {
  }

  @TypeName("wallet-charge-rejected")
  record WalletChargeRejected(String walletId, String expenseId, String commandId) implements WalletEvent {
  }
}
