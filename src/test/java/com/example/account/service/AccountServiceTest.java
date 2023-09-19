package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountuserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountuserRepository accountuserRepository;
    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();

        pobi.setId(12L);
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012")
                        .accountUser(pobi)
                        .build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1000000013")
                        .build());

        ArgumentCaptor<Account> captor=ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository,times((1))).save(captor.capture());
        assertEquals(12L,accountDto.getUserId());
        assertEquals("1000000013",accountDto.getAccountNumber());
    }

    @Test
    void createFirstAccountSuccess(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(12L);
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1000000000")
                        .build());

        ArgumentCaptor<Account> captor=ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository,times((1))).save(captor.capture());
        assertEquals(12L,accountDto.getUserId());
        assertEquals("1000000000",accountDto.getAccountNumber());
    }

    @Test
    void createAccountUserNotFound(){
        //give
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND,exception.getErrorCode());
    }

    @Test
    @DisplayName("유저당 계좌 최대  10개")
    void createAccount_maxAccount(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();

        pobi.setId(15L);
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10,exception.getErrorCode());
    }

    @Test
    void deleteAccountSuccess(){
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
                        .accountUser(pobi)
                        .balance(0L)
                        .build()));

        ArgumentCaptor<Account> captor=ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository,times(1)).save(captor.capture());
        assertEquals(AccountStatus.UNREGISTERED,captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("사용자가 존재하지 않아 계좌해지 실패")
    void deleteAccount_fail(){
        //given
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->accountService.deleteAccount(1L, "1234597890"));

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
                ()->accountService.deleteAccount(1L, "1234597890"));

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
                ()->accountService.deleteAccount(1L, "1234597890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UNMACH,exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액이 남아있음")
    void delteFail_BalanceLeft(){
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
                        .build()));

        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->accountService.deleteAccount(1L, "1234597890"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY,exception.getErrorCode());

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
                ()->accountService.deleteAccount(1L, "1234597890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERD,exception.getErrorCode());

    }

    @Test
    void getAccountsByUserId(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();

        pobi.setId(12L);

        List<Account> accounts= Arrays.asList(
                Account.builder()
                        .accountNumber("123457890")
                        .balance(1000L)
                        .accountUser(pobi)
                        .build(),
                Account.builder()
                        .accountNumber("1111111111")
                        .balance(12000L)
                        .accountUser(pobi)
                        .build()
        );
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);
        //then
        assertEquals(2,accountDtos.size());
        assertEquals("123457890",accountDtos.get(0).getAccountNumber());
        assertEquals(1000L,accountDtos.get(0).getBalance());
    }

    @Test
    void failedToGetAccounts(){
        //given
        given(accountuserRepository.findById(anyLong()))
                .willReturn(Optional.empty());
        //when
        AccountException exception=assertThrows(AccountException.class,
                ()->accountService.getAccountsByUserId(1L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND,exception.getErrorCode());
    }


}