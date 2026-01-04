package com.vty.vty.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDesc {
    GENERAL_ERROR("Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyiniz."),
    GENERAL_NOT_FOUND_ERROR("Aradığınız kayıt bulunamadı."),
    INVALID_OPERATION("Bu işlem gerçekleştirilemez."),
    UNAUTHORIZED("Bu işlem için yetkiniz bulunmamakta."),
    PASSWORD_INCORRECT("Şifreniz hatalıdır."),
    ;

    private final String errorDescription;
}
