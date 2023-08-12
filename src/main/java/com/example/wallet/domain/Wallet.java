package com.example.wallet.domain;

import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletCommand.Refund;
import com.example.wallet.domain.WalletEvent.FundsDeposited;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import com.example.wallet.domain.WalletEvent.WalletRefunded;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Either;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static com.example.wallet.domain.WalletCommandError.DEPOSIT_LE_ZERO;
import static com.example.wallet.domain.WalletCommandError.DUPLICATED_COMMAND;
import static com.example.wallet.domain.WalletCommandError.EXPENSE_NOT_EXISTS;
import static com.example.wallet.domain.WalletCommandError.WALLET_ALREADY_EXISTS;
import static com.example.wallet.domain.WalletCommandError.WALLET_NOT_EXISTS;
import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

public record Wallet(String id, BigDecimal balance, Map<String, Expense> expenses, Set<String> commandIds) {

  public Wallet(String id, BigDecimal balance) {
    this(id, balance, HashMap.empty(), HashSet.empty());
  }

  public static final String EMPTY_WALLET_ID = "";
  public static Wallet EMPTY_WALLET = new Wallet(EMPTY_WALLET_ID, BigDecimal.ZERO, HashMap.empty(), HashSet.empty());

  public Either<WalletCommandError, WalletEvent> process(WalletCommand command) {
    if (isDuplicate(command)) {
      return Either.left(DUPLICATED_COMMAND);
    } else {
      return switch (command) {
        case CreateWallet create -> handleCreate(create);
        case ChargeWallet charge -> ifExists(() -> handleCharge(charge));
        case Refund refund -> ifExists(() -> handleRefund(refund));
        case DepositFunds depositFunds -> ifExists(() -> handleDeposit(depositFunds));
      };
    }
  }

  private boolean isDuplicate(WalletCommand command) {
    if (command instanceof WalletCommand.RequiresDeduplicationCommand c) {
      return commandIds.contains(c.commandId());
    } else {
      return false;
    }
  }

  private Either<WalletCommandError, WalletEvent> ifExists(Supplier<Either<WalletCommandError, WalletEvent>> processingResultSupplier) {
    if (isEmpty()) {
      return left(WALLET_NOT_EXISTS);
    } else {
      return processingResultSupplier.get();
    }
  }

  private Either<WalletCommandError, WalletEvent> handleCreate(CreateWallet createWallet) {
    if (isEmpty()) {
      return right(new WalletCreated(createWallet.walletId(), createWallet.initialAmount()));
    } else {
      return left(WALLET_ALREADY_EXISTS);
    }
  }

  private Either<WalletCommandError, WalletEvent> handleDeposit(DepositFunds depositFunds) {
    if (depositFunds.amount().compareTo(BigDecimal.ZERO) <= 0) {
      return left(DEPOSIT_LE_ZERO);
    } else {
      return right(new FundsDeposited(id, depositFunds.amount(), depositFunds.commandId()));
    }
  }

  private Either<WalletCommandError, WalletEvent> handleCharge(ChargeWallet charge) {
    if (balance.compareTo(charge.amount()) < 0) {
      return right(new WalletChargeRejected(id, charge.expenseId(), charge.commandId()));
    } else {
      return right(new WalletCharged(id, charge.amount(), charge.expenseId(), charge.commandId()));
    }
  }

  private Either<WalletCommandError, WalletEvent> handleRefund(Refund refund) {
    return expenses.get(refund.expenseId()).fold(
      () -> left(EXPENSE_NOT_EXISTS),
      expense -> right(new WalletRefunded(id, expense.amount(), expense.expenseId(), refund.commandId()))
    );
  }

  public Wallet apply(WalletEvent event) {
    return switch (event) {
      case WalletCreated walletCreated -> new Wallet(walletCreated.walletId(), walletCreated.initialAmount(), expenses, commandIds);
      case WalletCharged charged -> {
        Expense expense = new Expense(charged.expenseId(), charged.amount());
        yield new Wallet(id, balance.subtract(charged.amount()), expenses.put(expense.expenseId(), expense), commandIds.add(charged.commandId()));
      }
      case WalletRefunded refunded ->
        new Wallet(id, balance.add(refunded.amount()), expenses.remove(refunded.expenseId()), commandIds.add(refunded.commandId()));
      case FundsDeposited deposited -> new Wallet(id, balance.add(deposited.amount()), expenses, commandIds.add(deposited.commandId()));
      case WalletChargeRejected __ -> this;
    };
  }

  public boolean isEmpty() {
    return id.equals(EMPTY_WALLET_ID);
  }
}
