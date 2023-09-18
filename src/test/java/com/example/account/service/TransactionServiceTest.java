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
                ()->accountService.deleteAccount(1L, "1234597890"));

        //then
        assertEquals(ErrorCode.NOT_ACCOUNT_EXIST,exception.getErrorCode());

    }
}