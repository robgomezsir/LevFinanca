package com.example.lev.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A barra inferior carrega apenas destinos sem outro ponto de entrada. Se alguém reAdicionar
 * Contas, Categorias ou Configurações aqui, elas ficam duplicadas com o painel e a engrenagem.
 */
class NavItemTest {
    @Test
    fun `barra tem os quatro destinos principais na ordem`() {
        val items = bottomNavItems()

        assertEquals(
            listOf("dashboard", "transactions", "planning", "reports"),
            items.map { it.route },
        )
        assertEquals(
            listOf("Início", "Lançamentos", "Orçamento", "Relatórios"),
            items.map { it.label },
        )
    }

    @Test
    fun `destinos com outro ponto de entrada ficam fora da barra`() {
        val routes = bottomNavItems().map { it.route }

        assertTrue("accounts não deve estar na barra", "accounts" !in routes)
        assertTrue("categories não deve estar na barra", "categories" !in routes)
        assertTrue("settings não deve estar na barra", "settings" !in routes)
    }

    @Test
    fun `todo destino tem icone e rota`() {
        bottomNavItems().forEach { item ->
            assertNotNull("${item.label} sem ícone", item.icon)
            assertTrue("${item.label} sem rota", item.route.isNotBlank())
        }
    }
}
