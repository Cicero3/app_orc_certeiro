package com.example.api.auth.api

import com.example.api.auth.api.dto.PersonalDataResponse
import com.example.api.auth.application.UserAccountService
import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoints de direitos do titular (LGPD Art. 18) sobre a própria conta.
 */
@Tag(name = "UserAccount", description = "Direitos do titular sobre a própria conta (LGPD Art. 18)")
@RestController
@RequestMapping("/api/v1/users/me")
class UserAccountController(
    private val userAccountService: UserAccountService
) {
    /** Direito de acesso: o que guardamos sobre o titular. */
    @GetMapping("/data")
    fun myData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<PersonalDataResponse>> {
        val data = userAccountService.getPersonalData(principal.id)
        return ResponseEntity.ok(ApiResponse(data = data))
    }

    /** Direito de portabilidade: mesmos dados, como arquivo para download. */
    @GetMapping("/export")
    fun exportMyData(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<PersonalDataResponse> {
        val data = userAccountService.getPersonalData(principal.id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"meus-dados.json\"")
            .body(data)
    }

    /** Direito ao apagamento: exclusão por anonimização. */
    @DeleteMapping
    fun deleteMyAccount(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        userAccountService.anonymizeAccount(principal.id)
        return ResponseEntity.noContent().build()
    }
}
