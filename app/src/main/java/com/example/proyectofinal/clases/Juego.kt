package com.example.proyectofinal.clases

class Juego {
    private var puntos: Int = 0

    fun sumarPuntos() {
        puntos += 10
    }

    fun obtenerPuntos(): Int {
        return puntos
    }
}