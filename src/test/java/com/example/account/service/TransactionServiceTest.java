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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    public static final long USE_AMOUNT=100L;
    public static final long CANCEL_AMOUNT=100L;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountuserRepository accountuserRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void useBalanceTest(){
        AccountUser user=AccountUser.builder()
                .name("Pobi").build();
        user.setId(1L);
        //given
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account
                .builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(1000L)
                .accountNumber("1234567890")
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("123456")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(0L)
                        .build());

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1234567890", 1000L);

        //then
        assertEquals(0L,transactionDto.getBalanceSnapshot());
        assertEquals(TransactionResultType.S,transactionDto.getTransactionResultType());
    }

    @Test
    void useBalance_UserNotFound(){
        //give
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.useBalance(1L,"1234567890",1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND,exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌없음")
    void delteFail_NoAccount(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(12L);
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.useBalance(1L,"1234567890",1000L));

        //then
        assertEquals(ErrorCode.NOT_ACCOUNT_EXIST,exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌소유주 다름")
    void delteFail_NotMathUser(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(12L);
        AccountUser other = AccountUser.builder()
                .name("Dalbeen")
                .build();

        other.setId(13L);

        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(other)
                        .accountNumber("1000000012")
                        .balance(0L)
                        .build()));

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.useBalance(1L,"1234567890",1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMACH,exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌가 이미 해제됨")
    void delteFail_AlreayUnregistered(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();

        pobi.setId(12L);

        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1000000012")
                        .balance(1200L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .build()));

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.useBalance(1L,"1234567890",1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERD,exception.getErrorCode());

    }

    @Test
    void useBalanceExceedTest(){
        AccountUser user=AccountUser.builder()
                .name("Pobi").build();

        user.setId(1L);
        //given
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));

        Account account = Account
                .builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(10L)
                .accountNumber("1234567890")
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));


        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.useBalance(1L,"1234567890",1000L));

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE,exception.getErrorCode());

    }

    @Test
    void saveFailed(){
        AccountUser user=AccountUser.builder()
                .name("Pobi").build();

        user.setId(1L);
        //given

        Account account = Account
                .builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(1000L)
                .accountNumber("1234567890")
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("123456")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(0L)
                        .build());

        ArgumentCaptor<Transaction> captor=ArgumentCaptor.forClass(Transaction.class);
        //when
        transactionService.saveFailedUseTransaction("1234567890", 1000L);

        //then
        verify(transactionRepository,times(1)).save(captor.capture());
        assertEquals(TransactionResultType.F,captor.getValue().getTransactionResultType());
    }

    @Test
    void cancelBalanceTest(){
        AccountUser user=AccountUser.builder()
                .name("Pobi").build();

        user.setId(1L);
        //given

        Account account = Account
                .builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(1000L)
                .accountNumber("1234567890")
                .build();

        Transaction transaction=Transaction.builder()
                .account(account)
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.S)
                .transactionId("123456")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(0L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(TransactionType.CANCEL)
                        .transactionResultType(TransactionResultType.S)
                        .transactionId("123456")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(1000L)
                        .build());

        //when
        TransactionDto transactionDto = transactionService.cancelBalance("123456","1234567890",CANCEL_AMOUNT);
        //then
        assertEquals(transactionDto.getTransactionType(),TransactionType.CANCEL);
    }

    @Test
    @DisplayName("계좌없음")
    void cancelFail_NoAccount(){
        //given

        Transaction transaction=Transaction.builder()
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.cancelBalance("ddddd","1234567890",1000L));

        //then
        assertEquals(ErrorCode.NOT_ACCOUNT_EXIST,exception.getErrorCode());

    }

    @Test
    @DisplayName("해당거래내역 없음")
    void cancelTransaction_NotFoundTransaction(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.cancelBalance("ddddd","1234567890",1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND,exception.getErrorCode());

    }

    @Test
    @DisplayName("1년이상된 거래내역은 취소처리불가")
    void cancelTransaction_FailOneYear(){
        AccountUser user=AccountUser.builder()
                .name("Pobi").build();

        user.setId(1L);
        //given

        Account account = Account
                .builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(1000L)
                .accountNumber("1234567890")
                .build();

        Transaction transaction=Transaction.builder()
                .transactedAt(LocalDateTime.now().minusYears(1))
                .account(account)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(1000L)
                .transactionType(TransactionType.CANCEL)
                .transactionId("ddddd")
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.cancelBalance("ddddd","1234567890",CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TOO_OLD_FOR_CANCEL,exception.getErrorCode());

    }

    @Test
    void sucessQueryTest(){
        AccountUser user=AccountUser.builder()
                .name("Pobi").build();
        user.setId(1L);
        Account account = Account
                .builder()
                .accountUser(user)
                .accountStatus(AccountStatus.IN_USE)
                .balance(1000L)
                .accountNumber("1234567890")
                .build();
        Transaction transaction=Transaction.builder()
                .transactedAt(LocalDateTime.now().minusYears(1))
                .account(account)
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(1000L)
                .transactionType(TransactionType.CANCEL)
                .transactionResultType(TransactionResultType.S)
                .transactionId("ddddd")
                .build();
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        //when

        //then
        TransactionDto transactionDto = transactionService.queryTransaction("ddddd");
        assertEquals(TransactionType.CANCEL,transactionDto.getTransactionType());
        assertEquals(TransactionResultType.S,transactionDto.getTransactionResultType());
        assertEquals(100L,transactionDto.getAmount());

    }

    @Test
    @DisplayName("해당거래내역 없음")
    void QueryTransaction_NotFoundTransaction(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->transactionService.queryTransaction(anyString()));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND,exception.getErrorCode());

    }

}