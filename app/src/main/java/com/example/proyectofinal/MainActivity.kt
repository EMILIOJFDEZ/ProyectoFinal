package com.example.proyectofinal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.proyectofinal.clases.Juego
import com.example.proyectofinal.clases.Pregunta
import android.media.MediaPlayer
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import android.graphics.BitmapFactory
import java.io.FileOutputStream
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File

class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var nombre = ""
    private var correo = ""
    private var clave = ""
    private lateinit var juego: Juego
    private lateinit var preguntasDisponibles: MutableList<Pregunta>
    private var preguntaActualIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mostrarLayoutLogin()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.dificultad1 ->
                Toast.makeText(this, "Has elegido la dificultad: 'Fácil'", Toast.LENGTH_SHORT).show()
            R.id.dificultad2 ->
                Toast.makeText(this, "Has elegido la dificultad: 'Medio'", Toast.LENGTH_SHORT).show()
            R.id.dificultad3 ->
                Toast.makeText(this, "Has elegido la dificultad: 'Difícil'", Toast.LENGTH_SHORT).show()
            R.id.dificultad4 ->
                Toast.makeText(this, "Has elegido la dificultad: 'Experto'", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun mostrarLayoutLogin() {
        try {
            setContentView(R.layout.layout_login)
            val editTextNombre = findViewById<EditText>(R.id.editTextNombre)
            val editTextCorreo = findViewById<EditText>(R.id.editTextCorreo)
            val editTextClave = findViewById<EditText>(R.id.editTextClave)
            val buttonConfirmar = findViewById<Button>(R.id.buttonEnviar)
            val buttonSalir = findViewById<Button>(R.id.buttonSalir)

            buttonConfirmar.setOnClickListener {
                nombre = editTextNombre.text.toString().trim()
                correo = editTextCorreo.text.toString().trim()
                clave = editTextClave.text.toString().trim()
                if (nombre.isBlank()) {
                    Toast.makeText(this, "Por favor, introduce tu nombre", Toast.LENGTH_SHORT).show()
                } else if (correo.isBlank()) {
                    Toast.makeText(this, "Por favor, introduce tu correo electrónico", Toast.LENGTH_SHORT).show()
                } else if (clave.isBlank()) {
                    Toast.makeText(this, "Por favor, introduce tu contraseña", Toast.LENGTH_SHORT).show()
                }
                else {
                    mostrarLayoutCategorias()
                }
            }

            buttonSalir.setOnClickListener { finish() }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en login layout", e)
            Toast.makeText(this, "Ocurrió un error al iniciar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun llamadaHttp() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://pruebaemilio.atwebpages.com/api/gestiones/leer.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    withContext(Dispatchers.Main) {
                        Log.d("HTTP", response)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HTTP", "Error en llamada HTTP", e)
                    Toast.makeText(this@MainActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarLayoutCategorias() {
        try {
            llamadaHttp()
            detenerAudio()
            setContentView(R.layout.layout_categorias)
            val botones = mapOf(
                R.id.buttonVideojuegos to "Videojuegos",
                R.id.buttonPeliculas to "Peliculas",
                R.id.buttonMusica to "Música",
                R.id.buttonHistoria to "Historia",
                R.id.buttonLiteratura to "Literatura",
                R.id.buttonGeografia to "Geografía"
            )
            botones.forEach { (id, tematica) ->
                findViewById<Button>(id).setOnClickListener { iniciarJuego(tematica) }
            }
            findViewById<Button>(R.id.buttonSalir).setOnClickListener { finish() }
            findViewById<Button>(R.id.buttonVolver).setOnClickListener { mostrarLayoutLogin() }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en categorias layout", e)
            Toast.makeText(this, "No se pudo cargar categorías", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iniciarJuego(tematica: String) {
        detenerAudio()
        juego = Juego()
        val idCategoria = when (tematica) {
            "Videojuegos" -> 7
            "Peliculas" -> 8
            "Música" -> 9
            "Historia" -> 10
            "Literatura" -> 11
            "Geografía" -> 12
            else -> -1
        }
        if (idCategoria == -1) {
            Toast.makeText(this, "Categoría inválida", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val preguntas = obtenerPreguntasPorCategoria(idCategoria)
                if (!preguntas.isNullOrEmpty()) {
                    preguntasDisponibles = preguntas.shuffled().toMutableList()
                    preguntaActualIndex = 0
                    setContentView(R.layout.layout_cuestionario)
                    findViewById<Button>(R.id.buttonSalir).setOnClickListener { finish() }
                    findViewById<Button>(R.id.buttonCuestionario).setOnClickListener { mostrarLayoutCategorias() }
                    mostrarSiguientePregunta()
                } else {
                    Toast.makeText(this@MainActivity, "No se cargaron preguntas", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("Juego", "Error iniciando juego", e)
                Toast.makeText(this@MainActivity, "Error al iniciar juego", Toast.LENGTH_SHORT).show()
                mostrarLayoutCategorias()
            }
        }
    }

    private suspend fun obtenerPreguntasPorCategoria(idCategoria: Int): List<Pregunta>? {
        return try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val url = "http://pruebaemilio.atwebpages.com/api/juego/obtenerpregunta.php?id_cuestionario=$idCategoria"
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()?.let { parsearJsonAPreguntasJSON(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("HTTP", "Error obteniendo preguntas", e)
            null
        }
    }

    private fun parsearJsonAPreguntasJSON(json: String): List<Pregunta> {
        return try {
            val lista = mutableListOf<Pregunta>()
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val preguntas = Pregunta(
                    obj.getString("contenido"),
                    obj.getString("respuestaCorrecta"),
                    mutableListOf<String>().apply {
                        val arr = obj.getJSONArray("respuestasIncorrectas")
                        for (j in 0 until arr.length()) add(arr.getString(j))
                    },
                    obj.getString("imagen"),
                    obj.getString("sonido")
                )
                lista.add(preguntas)
            }
            lista
        } catch (e: Exception) {
            Log.e("JSON", "Error parseando preguntas JSON", e)
            emptyList()
        }
    }

    private fun mostrarSiguientePregunta() {
        try {
            if (preguntaActualIndex >= preguntasDisponibles.size) {
                mostrarResultados()
            } else {
                actualizarUI(preguntasDisponibles[preguntaActualIndex])
            }
        } catch (e: Exception) {
            Log.e("Cuestionario", "Error mostrando siguiente pregunta", e)
        }
    }

    private fun actualizarUI(pregunta: Pregunta) {
        try {
            findViewById<TextView>(R.id.textViewPregunta).text = pregunta.pregunta
            findViewById<TextView>(R.id.textViewPuntos).text = "Puntos: ${juego.obtenerPuntos()}"

            val imageView = findViewById<ImageView>(R.id.imageView)
            if (pregunta.imagenBase64.isNotEmpty()) {
                val bytes = Base64.decode(pregunta.imagenBase64, Base64.DEFAULT)
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_background)
            }

            detenerAudio()
            if (pregunta.sonidoBase64.isNotEmpty()) {
                val tmp = File(cacheDir, "audio.mp3")
                FileOutputStream(tmp).use { it.write(Base64.decode(pregunta.sonidoBase64, Base64.DEFAULT)) }
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tmp.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
            }

            val opciones = pregunta.obtenerOpciones()
            listOf(
                R.id.buttonPregunta1,
                R.id.buttonPregunta2,
                R.id.buttonPregunta3,
                R.id.buttonPregunta4
            ).forEachIndexed { i, id ->
                findViewById<Button>(id).apply {
                    text = opciones[i]
                    setOnClickListener {
                        if (text == pregunta.respuestaCorrecta) juego.sumarPuntos()
                        preguntaActualIndex++
                        mostrarSiguientePregunta()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UI", "Error actualizando UI", e)
            mostrarResultados()
        }
    }

    private fun mostrarResultados() {
        try {
            detenerAudio()
            setContentView(R.layout.layout_resultados)
            findViewById<TextView>(R.id.textViewNombreFinal).text = "Nombre: $nombre"
            findViewById<TextView>(R.id.textViewPuntuacion).text = "Puntos: ${juego.obtenerPuntos()}"
            findViewById<Button>(R.id.buttonRepetir).setOnClickListener { mostrarLayoutCategorias() }
            findViewById<Button>(R.id.buttonSalir).setOnClickListener { finish() }
        } catch (e: Exception) {
            Log.e("Resultados", "Error mostrando resultados", e)
            finish()
        }
    }

    private fun detenerAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}