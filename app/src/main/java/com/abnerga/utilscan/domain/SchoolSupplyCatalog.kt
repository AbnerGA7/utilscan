package com.abnerga.utilscan.domain

/** Útil escolar reconocible, con su nombre para mostrar en español. */
data class SchoolSupply(
    val nameEs: String,
    val emoji: String,
)

/**
 * Traduce las etiquetas en inglés del modelo (Open Images V7, COCO o datasets de Roboflow)
 * a útiles escolares en español. Las etiquetas que no están aquí no se consideran útiles.
 */
object SchoolSupplyCatalog {

    private val supplies: Map<String, SchoolSupply> = buildMap {
        fun add(emoji: String, nameEs: String, vararg keys: String) =
            keys.forEach { put(normalize(it), SchoolSupply(nameEs, emoji)) }

        add("🖊️", "Lapicero", "pen", "ball point", "ballpoint", "ballpoint pen", "ink pen")
        add("✏️", "Lápiz", "pencil", "pen/pencil")
        add("🖍️", "Crayón", "crayon", "crayons")
        add("🖌️", "Plumón", "marker", "highlighter", "felt pen")
        add("🧽", "Borrador", "eraser", "rubber")
        add("📏", "Regla", "ruler", "scale")
        add("📐", "Escuadra", "set square", "triangle ruler", "geometry box")
        add("🧭", "Compás", "compass", "drawing compass")
        add("🔪", "Tajador", "sharpener", "pencil sharpener")
        add("✂️", "Tijeras", "scissors", "scissor")
        add("🧴", "Goma / pegamento", "glue", "glue stick")
        add("🩹", "Cinta adhesiva", "tape", "adhesive tape")
        add("📎", "Engrapador", "stapler")
        add("🖇️", "Clip", "paper clip", "clip")
        add("📕", "Libro", "book")
        add("📓", "Cuaderno", "notebook", "copybook")
        add("🗒️", "Hoja / papel", "paper", "sheet")
        add("✉️", "Sobre", "envelope")
        add("📂", "Folder", "folder", "file folder")
        add("👝", "Cartuchera", "pencil case")
        add("🎒", "Mochila", "backpack", "school bag", "bag")
        add("🧮", "Calculadora", "calculator")
        add("💻", "Laptop", "laptop")
        add("📱", "Celular", "mobile phone", "cell phone", "smartphone")
        add("📲", "Tablet", "tablet computer", "tablet")
        add("⌨️", "Teclado", "computer keyboard", "keyboard")
        add("🖱️", "Mouse", "computer mouse", "mouse")
        add("🧴", "Tomatodo", "bottle", "water bottle")
        add("🕰️", "Reloj", "clock", "watch", "alarm clock")
        add("🖼️", "Pizarra", "whiteboard", "blackboard")
    }

    private fun normalize(label: String): String = label.trim().lowercase().replace('_', ' ')

    fun find(label: String): SchoolSupply? = supplies[normalize(label)]

    fun isSchoolSupply(label: String): Boolean = find(label) != null

    /** Nombre para la UI: el útil en español o, si no es un útil, la etiqueta original. */
    fun displayName(label: String): String = find(label)?.let { "${it.emoji} ${it.nameEs}" } ?: label

    /** Índices de las etiquetas del modelo que corresponden a útiles escolares. */
    fun schoolClassIndices(labels: List<String>): Set<Int> =
        labels.indices.filter { isSchoolSupply(labels[it]) }.toSet()
}
