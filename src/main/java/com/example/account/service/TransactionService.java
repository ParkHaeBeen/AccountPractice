package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountuserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.transaction.TransactionalException;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.example.account.type.TransactionResultType.*;
import static com.example.account.type.TransactionType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountuserRepository accountuserRepository;
    private final AccountRepository accountRepository;

    /*
    사용자 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우,
    계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우,
    거래금액이 너무 작거나 큰 경우 실패 응답
    */
    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount){
        AccountUser user = accountuserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.NOT_ACCOUNT_EXIST));

        validateUseBalance(user,account,amount);

        account.useBalance(amount);

        Transaction save = saveandGetTransaction(USE,S, account, amount);

        return TransactionDto.fromEntity(save);
    }

    private void validateUseBalance(AccountUser user,Account account,Long amount) {
        if (user.getId() != account.getAccountUser().getId()) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UNMACH);
        }

        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERD);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }

    }
    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account=accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountException(ErrorCode.NOT_ACCOUNT_EXIST));

        saveandGetTransaction(USE,F, account, amount);

    }

    private Transaction saveandGetTransaction(TransactionType transactionType, TransactionResultType transactionResultType, Account account, Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }
    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.NOT_ACCOUNT_EXIST));

        validateCancelBalance(transaction,account,amount);

        account.cancelBalance(amount);

        Transaction save = saveandGetTransaction(CANCEL,S, account, amount);

        return TransactionDto.fromEntity(save);
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if(transaction.getAccount().getId()!=account.getId()){
            throw  new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }

        if(transaction.getAmount()!=amount){
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }

        if(transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))){
            throw new AccountException(ErrorCode.TOO_OLD_FOR_CANCEL);
        }


    }
    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account=accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new AccountException(ErrorCode.NOT_ACCOUNT_EXIST));

        saveandGetTransaction(CANCEL,F, account, amount);
    }

    @Transactional
    public TransactionDto queryTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        return TransactionDto.fromEntity(transaction);
    }
}
