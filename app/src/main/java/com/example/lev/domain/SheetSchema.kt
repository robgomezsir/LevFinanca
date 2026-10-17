package com.example.lev.domain

/**
 * Abas de dados da planilha (M7 §A). Linha 1 = cabeçalho em pt-BR, congelada.
 * Colunas SEMPRE mapeadas pelo NOME do cabeçalho, nunca pela posição; novas
 * colunas só no final. Abas de relatório têm prefixo `rel_` e o sync NUNCA
 * escreve nelas.
 */
data class SheetTab(
    val key: String,
    val name: String,
    val headers: List<String>,
)

object SheetSchema {
    val ACCOUNTS =
        SheetTab(
            key = "accounts",
            name = "Contas",
            headers =
                listOf(
                    "ID", "Nome", "Tipo", "Saldo Inicial (R$)", "Moeda", "Cor", "Ícone",
                    "Limite (R$)", "Dia Fechamento", "Dia Vencimento", "Conta Pagamento ID",
                    "Na Soma", "Arquivada", "Ordem", "Atualizado Em", "Excluído",
                ),
        )

    val CATEGORIES =
        SheetTab(
            key = "categories",
            name = "Categorias",
            headers =
                listOf(
                    "ID", "Nome", "Tipo", "Categoria Pai ID", "Cor", "Ícone",
                    "Arquivada", "Ordem", "Atualizado Em", "Excluído",
                ),
        )

    val TRANSACTIONS =
        SheetTab(
            key = "transactions",
            name = "Lancamentos",
            headers =
                listOf(
                    "ID", "Tipo", "Descrição", "Valor (R$)", "Data", "Vencimento", "Pago Em",
                    "Status", "Conta ID", "Conta", "Conta Destino ID", "Categoria ID",
                    "Categoria", "Favorecido", "Tags", "Observações", "Parcelamento ID",
                    "Parcela", "Total Parcelas", "Recorrência ID", "Fatura ID", "Meta ID",
                    "Conciliado", "Criado Em", "Atualizado Em", "Excluído",
                ),
        )

    val SPLITS =
        SheetTab(
            key = "splits",
            name = "Divisoes",
            headers =
                listOf(
                    "Lançamento ID",
                    "Categoria ID",
                    "Valor (R$)",
                    "Observação",
                    "Atualizado Em",
                    "Excluído",
                ),
        )

    val INVOICES =
        SheetTab(
            key = "invoices",
            name = "Faturas",
            headers =
                listOf(
                    "ID", "Cartão ID", "Referência (AAAA-MM)", "Fechamento", "Vencimento",
                    "Status", "Lançamento Pagamento ID", "Atualizado Em", "Excluído",
                ),
        )

    val BUDGETS =
        SheetTab(
            key = "budgets",
            name = "Orcamentos",
            headers =
                listOf(
                    "ID",
                    "Mês",
                    "Categoria ID",
                    "Planejado (R$)",
                    "Rollover",
                    "Atualizado Em",
                    "Excluído",
                ),
        )

    val GOALS =
        SheetTab(
            key = "goals",
            name = "Metas",
            headers =
                listOf(
                    "ID", "Nome", "Valor Alvo (R$)", "Prazo", "Conta ID", "Status",
                    "Cor", "Ícone", "Atualizado Em", "Excluído",
                ),
        )

    val GOAL_CONTRIBUTIONS =
        SheetTab(
            key = "goal_contributions",
            name = "AportesMeta",
            headers =
                listOf(
                    "ID",
                    "Meta ID",
                    "Valor (R$)",
                    "Data",
                    "Observação",
                    "Lançamento ID",
                    "Atualizado Em",
                    "Excluído",
                ),
        )

    val RECURRENCES =
        SheetTab(
            key = "recurrences",
            name = "Recorrencias",
            headers =
                listOf(
                    "ID", "Descrição", "Tipo", "Valor (R$)", "Valor Variável", "Frequência",
                    "Intervalo", "Dia", "Início", "Fim", "Conta ID", "Categoria ID",
                    "Favorecido", "Observações", "Ativa", "Atualizado Em", "Excluído",
                ),
        )

    val RULES =
        SheetTab(
            key = "rules",
            name = "Regras",
            headers =
                listOf(
                    "ID", "Campo", "Tipo Correspondência", "Padrão", "Categoria ID",
                    "Favorecido", "Tags", "Prioridade", "Ativa", "Atualizado Em", "Excluído",
                ),
        )

    val ARCHIVE =
        SheetTab(
            key = "archive",
            name = "Arquivo",
            headers =
                listOf(
                    "Mês",
                    "Receitas (R$)",
                    "Despesas (R$)",
                    "Saldo (R$)",
                    "Fechado Em",
                    "Observação",
                    "Atualizado Em",
                    "Excluído",
                ),
        )

    val CONFIG =
        SheetTab(
            key = "config",
            name = "Config",
            headers = listOf("Chave", "Valor", "Atualizado Em"),
        )

    /** Abas de dados sincronizáveis, na ordem de push/pull (pais antes de filhos). */
    val DATA_TABS: List<SheetTab> =
        listOf(
            ACCOUNTS, CATEGORIES, TRANSACTIONS, SPLITS, INVOICES, BUDGETS,
            GOALS, GOAL_CONTRIBUTIONS, RECURRENCES, RULES, ARCHIVE, CONFIG,
        )

    val REPORT_TABS = listOf("rel_Resumo", "rel_Categorias", "rel_FluxoCaixa", "rel_Patrimonio")

    fun byKey(key: String): SheetTab? = DATA_TABS.firstOrNull { it.key == key }

    fun byName(name: String): SheetTab? = DATA_TABS.firstOrNull { it.name == name }
}
