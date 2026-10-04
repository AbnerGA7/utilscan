package com.abnerga.utilscan.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolSupplyCatalogTest {

    @Test
    fun `traduce etiquetas sin importar mayusculas ni guiones bajos`() {
        assertEquals("Regla", SchoolSupplyCatalog.find("Ruler")?.nameEs)
        assertEquals("Tajador", SchoolSupplyCatalog.find("pencil_sharpener")?.nameEs)
        assertEquals("Celular", SchoolSupplyCatalog.find("Mobile phone")?.nameEs)
    }

    @Test
    fun `objetos que no son utiles no se reconocen`() {
        assertNull(SchoolSupplyCatalog.find("Dog"))
        assertFalse(SchoolSupplyCatalog.isSchoolSupply("Car"))
        assertEquals("Dog", SchoolSupplyCatalog.displayName("Dog"))
    }

    @Test
    fun `obtiene indices de clases escolares`() {
        val labels = listOf("Dog", "Pen", "Car", "Scissors")
        assertEquals(setOf(1, 3), SchoolSupplyCatalog.schoolClassIndices(labels))
        assertTrue(SchoolSupplyCatalog.displayName("Scissors").endsWith("Tijeras"))
    }
}
