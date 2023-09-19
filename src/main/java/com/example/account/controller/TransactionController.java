package com.example.account.controller;

import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.QueryTransactionResponse;
import com.example.account.dto.TransactionDto;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/*
* 잔액관련
* 1. 잔액사용
* 2. 잔액사용취소
* 3. 거래확인
* */

@RestController
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(@Valid @RequestBody UseBalance.Request request) throws InterruptedException {

        TransactionDto transactionDto = transactionService.useBalance(request.getUserId(), request.getAccountNumber()
                , request.getAmount());

        //exception발생시 어떻게 결과를 저장할 것인가?
        try {
            Thread.sleep(3000L);
            return UseBalance.Response.from(transactionDto);
        }catch (AccountException e){
            log.error("Failed to use balance");
            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }

    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(@Valid @RequestBody CancelBalance.Request request){


        //exception발생시 어떻게 결과를 저장할 것인가?
        try {
            return CancelBalance.Response.from(
                    transactionService.cancelBalance(request.getTransactionId()
                            ,request.getAccountNumber(),request.getAmount())
            );
        }catch (AccountException e){
            log.error("Failed to use balance");
            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(@PathVariable String transactionId){
        TransactionDto transactionDto = transactionService.queryTransaction(transactionId);

        return QueryTransactionResponse.from(transactionDto);
    }

}
