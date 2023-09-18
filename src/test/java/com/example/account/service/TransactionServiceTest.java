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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

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
                .id(1L)
                .name("Pobi").build();
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
                .id(12L)
                .build();

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
                .id(12L)
                .build();

        AccountUser other = AccountUser.builder()
                .name("Dalbeen")
                .id(13L)
                .build();

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
                .id(12L)
                .build();

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
                .id(1L)
                .name("Pobi").build();
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
                .id(1L)
                .name("Pobi").build();
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

}