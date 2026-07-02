package com.example.api.orcamentos.domain

enum class OrcamentoStatus(val isImutavel: Boolean) {
    RASCUNHO(false),
    EM_REVISAO(false), // Aprovadores podem editar
    APROVADO(true),
    ENVIADO_CLIENTE(true),
    ACEITO(true),
    REJEITADO(true),
    CANCELADO(true)
}
