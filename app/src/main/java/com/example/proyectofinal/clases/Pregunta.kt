package com.example.proyectofinal.clases

data class Pregunta(
    val pregunta: String,
    val respuestaCorrecta: String,
    val respuestasIncorrectas: List<String>,
    val imagenBase64: String
) {
    fun obtenerOpciones(): List<String> =
        (respuestasIncorrectas + respuestaCorrecta).shuffled()
}