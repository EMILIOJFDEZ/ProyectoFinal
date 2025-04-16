package com.example.proyectofinal.clases

class Cuestionario(private val preguntas: List<Pregunta>) {

    private var indiceActual = 0;

    fun obtenerPreguntaActual(): Pregunta? {
        return if(preguntas.isNotEmpty()) preguntas[indiceActual] else null
    }

    fun siguiente(): Pregunta? {
        if(indiceActual < preguntas.size - 1){
            indiceActual++;
        }
        return obtenerPreguntaActual()
    }

    fun anterior(): Pregunta? {
        if(indiceActual > 0){
            indiceActual --
        }
        return obtenerPreguntaActual()
    }

    fun irAPregunta(indice: Int): Pregunta? {
        if(indice in preguntas.indices){
            indiceActual = indice
        }
        return obtenerPreguntaActual()
    }

    fun total(): Int = preguntas.size


    fun indice(): Int = indiceActual

}