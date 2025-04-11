package com.example.proyectofinal

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.proyectofinal.clases.Juego
import com.example.proyectofinal.clases.Pregunta
import android.media.MediaPlayer

class MainActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var nombre = ""
    private lateinit var juego: Juego
    private lateinit var preguntasDisponibles: MutableList<Pregunta>
    private var preguntaActualIndex = 0
    private val listaPreguntas = mapOf(
        "Videojuegos" to listOf(
            Pregunta(
                "¿Cómo se puede mejorar el nivel del Dead Eye en Red Dead Redemption 2?",
                "Cazando, pescando y completando desafíos",
                listOf("Comprando mejoras en tiendas", "Usando tónicos constantemente", "Disparando sin apuntar"),
                R.drawable.rdr2,
                R.raw.rdr2
            )
        )
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_login)
        login()
    }
    private fun login() {
        setContentView(R.layout.layout_login)
        val buttonConfirmar = findViewById<Button>(R.id.buttonEnviar)
        val buttonSalir = findViewById<Button>(R.id.buttonSalir)
        buttonConfirmar.setOnClickListener { configurarBotonesLayoutCategorias() }
        buttonSalir.setOnClickListener { finish() }
    }
    private fun reproducirAudio(cancionResId: Int) {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.reset()
        } ?: run {
            mediaPlayer = MediaPlayer()
        }
        try {
            val afd = resources.openRawResourceFd(cancionResId)
            mediaPlayer?.apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setOnPreparedListener { mp -> mp.start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun configurarBotonesLayoutCategorias() {
        detenerAudio()
        setContentView(R.layout.layout_categorias)
        val buttonVideojuegos = findViewById<Button>(R.id.buttonVideojuegos)
        val buttonPeliculas = findViewById<Button>(R.id.buttonPeliculas)
        val buttonMusica = findViewById<Button>(R.id.buttonMusica)
        val buttonHistoria = findViewById<Button>(R.id.buttonHistoria)
        val buttonLiteratura = findViewById<Button>(R.id.buttonLiteratura)
        val buttonGeografia = findViewById<Button>(R.id.buttonGeografia)
        val buttonSalir = findViewById<Button>(R.id.buttonSalir)
        val buttonVolver = findViewById<Button>(R.id.buttonVolver)
        buttonVideojuegos.setOnClickListener { iniciarJuego("Videojuegos") }
        buttonPeliculas.setOnClickListener { iniciarJuego("Peliculas") }
        buttonMusica.setOnClickListener { iniciarJuego("Música") }
        buttonHistoria.setOnClickListener { iniciarJuego("Historia") }
        buttonLiteratura.setOnClickListener { iniciarJuego("Literatura") }
        buttonGeografia.setOnClickListener { iniciarJuego("Geografía") }
        buttonSalir.setOnClickListener { finish() }
        buttonVolver.setOnClickListener { login() }
    }
    private fun iniciarJuego(tematica: String) {
        detenerAudio()
        juego = Juego()
        val preguntasCategoria = listaPreguntas[tematica] ?: emptyList()
        preguntasDisponibles = preguntasCategoria.shuffled().toMutableList()
        setContentView(R.layout.layout_cuestionario)
        mostrarSiguientePregunta()
    }
    private fun mostrarSiguientePregunta() {
        if (preguntaActualIndex >= preguntasDisponibles.size) {
            detenerAudio()
            preguntaActualIndex = 0
            mostrarResultados()
        } else {
            val preguntaActual = preguntasDisponibles[preguntaActualIndex]
            actualizarUI(preguntaActual)
        }
    }
    private fun mostrarResultados(){
        setContentView(R.layout.layout_resultados)
        findViewById<TextView>(R.id.textViewPuntuacion).text = juego.obtenerPuntos().toString()
        val buttonRepetir = findViewById<Button>(R.id.buttonRepetir)
        buttonRepetir.setOnClickListener { configurarBotonesLayoutCategorias() }
    }
    private fun actualizarUI(pregunta: Pregunta) {
        findViewById<TextView>(R.id.textViewPregunta).text = pregunta.pregunta
        findViewById<TextView>(R.id.textViewPuntos).text = "Puntuación: ${juego.obtenerPuntos()}"
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageResource(pregunta.imagenResId)
        reproducirAudio(pregunta.cancionResId)
        val buttonCuestionario = findViewById<Button>(R.id.buttonCuestionario)
        val buttonSalir = findViewById<Button>(R.id.buttonSalir)
        buttonSalir.setOnClickListener { finish() }
        buttonCuestionario.setOnClickListener { configurarBotonesLayoutCategorias() }
        val botones = listOf(
            findViewById<Button>(R.id.buttonPregunta1),
            findViewById<Button>(R.id.buttonPregunta2),
            findViewById<Button>(R.id.buttonPregunta3),
            findViewById<Button>(R.id.buttonPregunta4)
        )
        val opciones = pregunta.obtenerOpciones()
        botones.forEachIndexed { index, boton ->
            boton.text = opciones[index]
            boton.setOnClickListener {
                if (opciones[index] == pregunta.respuestaCorrecta) {
                    juego.sumarPuntos()
                }
                preguntaActualIndex++
                mostrarSiguientePregunta()
            }
        }
    }
    private fun detenerAudio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }
}