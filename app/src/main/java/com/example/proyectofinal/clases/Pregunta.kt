package com.example.proyectofinal.clases

class Pregunta(
    val pregunta: String,
    val respuestaCorrecta: String,
    val respuestasIncorrectas: List<String>,
    val imagenResId: Int,
    val cancionResId: Int
) {
    fun obtenerOpciones(): List<String> {
        return (respuestasIncorrectas + respuestaCorrecta).shuffled()
    }
}
