package com.example.account.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("사용자가 없습니다"),
    MAX_ACCOUNT_PER_USER_10("계좌는 최대 10개입니다"),
    NOT_ACCOUNT_EXIST("해당 계좌가 존재하지 않습니다"),
    USER_ACCOUNT_UNMACH("사용자와 계좌의 소유자가 다릅니다"),
    ACCOUNT_ALREADY_UNREGISTERD("계좌가 이미 해지되었습니다"),
    BALANCE_NOT_EMPTY("잔액이 있는 게좌를 해지할 수 없습니다"),
    AMOUNT_EXCEED_BALANCE("거래금액이 계좌잔액보다 큽니다");

    private final String description;

}