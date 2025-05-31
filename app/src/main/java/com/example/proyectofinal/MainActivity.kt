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
import android.text.Editable
import android.text.TextWatcher
import java.io.FileOutputStream
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import com.example.proyectofinal.clases.LoginResponse
import com.example.proyectofinal.clases.Usuario
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    // Variables globales
    private var categoriaBase = -1
    private  var idCuestionario = -1
    private var idCategoria = -1
    var lista = mutableListOf<Pregunta>()
    private var usuarioActual: Usuario? = null
    // Reproductor de audio para sonidos en preguntas
    private var mediaPlayer: MediaPlayer? = null
    // Variables para almacenar datos del usuario en login
    // Instancias del juego y preguntas
    private lateinit var juego: Juego
    private lateinit var preguntasDisponibles: MutableList<Pregunta>
    private var preguntaActualIndex = 0

    // Punto de entrada de la Activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mostrar pantalla de login al iniciar la app
        mostrarLayoutLogin()
    }

    // Inflar el menú overflow con opciones de dificultad
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.overflow, menu)
        return true
    }

    // Manejar selección de opciones del menú
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

            val imageLogin = findViewById<ImageView>(R.id.imageViewLogin)
            imageLogin.setImageResource(R.drawable.login)

            val imageCorreo = findViewById<ImageView>(R.id.imageViewCorreo)
            imageCorreo.setImageResource(R.drawable.correo)

            val imageClave = findViewById<ImageView>(R.id.imageViewClave)
            imageClave.setImageResource(R.drawable.clave)


            var editTextCorreo = findViewById<EditText>(R.id.editTextCorreo)
            var editTextClave  = findViewById<EditText>(R.id.editTextClave)
            var buttonConfirmar= findViewById<Button>(R.id.buttonLogin)
            var buttonSalir    = findViewById<Button>(R.id.buttonSalir)

            // Ocultamos inicialmente
            buttonConfirmar.visibility = View.GONE

            // TextWatcher: muestra el botón solo si ambos campos tienen texto
            var textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    var correo = editTextCorreo.text.toString().trim()
                    var clave  = editTextClave.text.toString().trim()
                    buttonConfirmar.visibility =
                        if (correo.isNotEmpty() && clave.isNotEmpty()) View.VISIBLE else View.GONE
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            editTextCorreo.addTextChangedListener(textWatcher)
            editTextClave.addTextChangedListener(textWatcher)

            // Confirmar → varidación local + login remoto
            buttonConfirmar.setOnClickListener {
                var correo = editTextCorreo.text.toString().trim()
                var clave  = editTextClave.text.toString().trim()

                // varidación local
                when {
                    correo.isBlank() -> {
                        Toast.makeText(this, "Por favor, introduce tu correo electrónico", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    clave.isBlank() -> {
                        Toast.makeText(this, "Por favor, introduce tu contraseña", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                // Petición al API en background
                CoroutineScope(Dispatchers.IO).launch {
                    var response = intentarLogin(correo, clave)
                    withContext(Dispatchers.Main) {
                        when {
                            response == null -> {
                                // Error de conexión o excepción
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error de conexión. Por favor, inténtalo más tarde.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            response.status == "ok" -> {
                                // Guardamos usuario
                                usuarioActual = response.usuario!!
                                if (usuarioActual!!.Permisos.equals("Ninguno", ignoreCase = true)) {
                                    // Cuenta baneada
                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo iniciar sesión: cuenta bloqueada.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // Logout del servidor para cerrar sesión y ahorrar tráfico
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            var client = OkHttpClient()
                                            var logoutRequest = Request.Builder()
                                                .url("http://pruebaemilio.atwebpages.com/sesiones/logout.php")
                                                .get()
                                                .build()
                                            client.newCall(logoutRequest).execute().use { /* ignoramos respuesta */ }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Error al hacer logout", e)
                                        }
                                    }
                                    // Login OK → avanzamos
                                    mostrarLayoutMenu()
                                }
                            }
                            else -> {
                                // Credenciales incorrectas
                                Toast.makeText(
                                    this@MainActivity,
                                    "Usuario o contraseña incorrectos.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }

            // Salir
            buttonSalir.setOnClickListener { finish() }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error en login layout", e)
            Toast.makeText(this, "Ocurrió un error al iniciar", Toast.LENGTH_SHORT).show()
        }
    }

    // Función suspend que hace el GET al PHP y parsea JSON
    private suspend fun intentarLogin(email: String, pass: String): LoginResponse? {
        return try {
            var client = OkHttpClient()
            var url = "http://pruebaemilio.atwebpages.com/sesiones/login.php" +
                    "?correo=${URLEncoder.encode(email, "UTF-8")}" +
                    "&passUser=${URLEncoder.encode(pass, "UTF-8")}"
            var request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                var body = resp.body?.string() ?: return null
                // Usamos org.json o Moshi/Gson
                var json = JSONObject(body)
                var status = json.getString("status")
                var usuario = if (status == "ok") {
                    var u = json.getJSONObject("usuario")
                    Usuario(
                        Id_Usuario = u.getInt("Id_Usuario"),
                        Nombre     = u.getString("Nombre"),
                        Correo     = u.getString("Correo"),
                        Clave      = u.getString("Clave"),
                        Id_Tipo    = u.getInt("Id_Tipo"),
                        Permisos   = u.getString("Permisos")
                    )
                } else null
                LoginResponse(status, usuario)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Mostrar pantalla de categorías de juego
    private fun mostrarLayoutCategorias() {
        println("ID del usuario: " + usuarioActual!!.Id_Usuario)
        println("Permisos del usuario: " + usuarioActual!!.Permisos)
        println("Nombre del usuario: " + usuarioActual!!.Nombre)
        println("Email del usuario: " + usuarioActual!!.Correo)
        println("Clave del usuario: " + usuarioActual!!.Clave)
        println("ID del tipo de usuario: " + usuarioActual!!.Id_Tipo)

        try {
            detenerAudio()
            setContentView(R.layout.layout_categorias)

            var botones = mapOf(
                R.id.buttonVideojuegos to 1,
                R.id.buttonPeliculas to 2,
                R.id.buttonMusica to 3,
                R.id.buttonHistoria to 4,
                R.id.buttonLiteratura to 5,
                R.id.buttonGeografia to 6
            )

            botones.forEach { (id, categoria) ->
                findViewById<Button>(id).setOnClickListener {
                    categoriaBase = categoria
                    mostrarLayoutDificultad()
                }
            }

            findViewById<Button>(R.id.buttonSalir).setOnClickListener { finish() }
            findViewById<Button>(R.id.buttonLogout).setOnClickListener { mostrarLayoutLogin() }
            findViewById<Button>(R.id.buttonMenu).setOnClickListener { mostrarLayoutMenu() }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en categorias layout", e)
            Toast.makeText(this, "No se pudo cargar categorías", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarLayoutMenu(){
        setContentView(R.layout.layout_menu)

        findViewById<Button>(R.id.buttonCategorias).setOnClickListener { mostrarLayoutCategorias() }
        findViewById<Button>(R.id.buttonSalir).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonLogout).setOnClickListener { mostrarLayoutLogin() }
        findViewById<Button>(R.id.buttonResultados).setOnClickListener { mostrarLayoutHistorial() }
    }

    private fun mostrarLayoutHistorial() {
        try {
            setContentView(R.layout.layout_historial)

            val buttonCategorias = findViewById<Button>(R.id.ButtonCategorias)
            val buttonLogout = findViewById<Button>(R.id.buttonLogout)
            val buttonSalir = findViewById<Button>(R.id.buttonSalir)
            val buttonMenu = findViewById<Button>(R.id.buttonMenu)
            val listView = findViewById<ListView>(R.id.listViewResultados)

            buttonCategorias.setOnClickListener { mostrarLayoutCategorias() }
            buttonLogout.setOnClickListener { mostrarLayoutLogin() }
            buttonSalir.setOnClickListener { finish() }
            buttonMenu.setOnClickListener { mostrarLayoutMenu() }

            val userId = usuarioActual?.Id_Usuario ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val resultados = obtenerResultadosUsuario(userId)

                withContext(Dispatchers.Main) {
                    if (resultados != null) {
                        val adapter = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_list_item_1,
                            resultados
                        )
                        listView.adapter = adapter
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No se pudieron obtener los resultados.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en historial layout", e)
            Toast.makeText(this, "Error al mostrar historial", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun obtenerResultadosUsuario(idUsuario: Int): List<String> {
        return try {
            val client = OkHttpClient()
            val url = "http://pruebaemilio.atwebpages.com/juego/resultadosporusuario.php?Id_Usuario=$idUsuario"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            val responseBody = response.body?.string() ?: return emptyList()
            val jsonObject = JSONObject(responseBody)
            val resultadosArray = jsonObject.getJSONArray("RESULTADOS")

            val lista = mutableListOf<String>()
            for (i in 0 until resultadosArray.length()) {
                val resultado = resultadosArray.getJSONObject(i)
                val texto = """
                Cuestionario: ${resultado.getInt("Id_Cuestionario")}
                Fecha: ${resultado.getString("Fecha_Hora")}
                Puntuación: ${resultado.getInt("Puntuacion")}
                Comentario: ${resultado.getString("Comentario")}
            """.trimIndent()
                lista.add(texto)
            }

            lista
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al obtener resultados", e)
            emptyList()
        }
    }


    // Mostrar pantalla de dificultad
    fun mostrarLayoutDificultad() {
        setContentView(R.layout.layout_dificultad)

        var dificultades = mapOf(
            R.id.buttonFacil to 1,   // Fácil
            R.id.buttonMedio to 2,  // Medio
            R.id.buttonDificil to 3,  // Difícil
            R.id.buttonExperto to 4   // Experto
        )

        dificultades.forEach { (id, nivelDificultad) ->
            findViewById<Button>(id).setOnClickListener {
                idCuestionario = categoriaBase + (6 * (nivelDificultad - 1))
                println("ID Cuestionario generado: $idCuestionario")
                iniciarJuego(idCuestionario)
            }
        }
    }

    // Iniciar juego con ID combinado
    private fun iniciarJuego(idCuestionario: Int) {
        detenerAudio()
        juego = Juego()

        idCategoria = idCuestionario

        if (idCategoria < 1 || idCategoria > 24) {
            Toast.makeText(this, "Categoría inválida", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                var preguntas = obtenerPreguntasPorCuestionario(idCuestionario)
                if (!preguntas.isNullOrEmpty()) {
                    preguntasDisponibles = preguntas.shuffled().toMutableList()
                    preguntaActualIndex = 0
                    setContentView(R.layout.layout_cuestionario)
                    findViewById<Button>(R.id.buttonSalir).setOnClickListener { finish() }
                    findViewById<Button>(R.id.buttonCategorias).setOnClickListener { mostrarLayoutCategorias() }
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
    // Función suspend para obtener preguntas desde API
    private suspend fun obtenerPreguntasPorCuestionario(idCuestionario: Int): List<Pregunta>? {
        return try {
            withContext(Dispatchers.IO) {
                var client = OkHttpClient()
                var url = "http://pruebaemilio.atwebpages.com/juego/obtenerpregunta.php?id_cuestionario=$idCuestionario"
                var request = Request.Builder().url(url).get().build()
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

    // Parsear JSON de preguntas en objetos Pregunta
    private fun parsearJsonAPreguntasJSON(json: String): List<Pregunta> {
        return try {
            lista = mutableListOf<Pregunta>()
            var jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                var obj = jsonArray.getJSONObject(i)
                var preguntas = Pregunta(
                    obj.getString("contenido"),
                    obj.getString("respuestaCorrecta"),
                    mutableListOf<String>().apply {
                        var arr = obj.getJSONArray("respuestasIncorrectas")
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

    // Mostrar siguiente pregunta o resultados si no quedan
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

    // Actualizar UI con pregunta y opciones
    private fun actualizarUI(pregunta: Pregunta) {
        try {
            // Mostrar texto de pregunta y puntos actuales
            findViewById<TextView>(R.id.textViewPregunta).text = pregunta.pregunta
            findViewById<TextView>(R.id.textViewPuntos).text = "Puntos: ${juego.obtenerPuntos()}"

            // Cargar imagen decodificada o imagen por defecto
            var imageView = findViewById<ImageView>(R.id.imageView)
            if (pregunta.imagenBase64.isNotEmpty()) {
                var bytes = Base64.decode(pregunta.imagenBase64, Base64.DEFAULT)
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_background)
            }

            // Manejo de sonido asociado a la pregunta
            detenerAudio()
            if (pregunta.sonidoBase64.isNotEmpty()) {
                var tmp = File(cacheDir, "audio.mp3")
                FileOutputStream(tmp).use { it.write(Base64.decode(pregunta.sonidoBase64, Base64.DEFAULT)) }
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tmp.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
            }
            // Configurar botones de opciones y manejar selección
            var opciones = pregunta.obtenerOpciones()
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
            var textNombre   = findViewById<TextView>(R.id.textViewNombreFinal)
            var textPuntos   = findViewById<TextView>(R.id.textViewPuntuacion)
            var editComentario = findViewById<EditText>(R.id.editTextComentario)
            var btnRepetir   = findViewById<Button>(R.id.buttonRepetir)
            var btnSalir     = findViewById<Button>(R.id.buttonSalir)
            textNombre.text = "Nombre: ${usuarioActual?.Nombre ?: "No hay ningún nombre asignado"}"
            textPuntos.text = "Puntos: ${juego?.obtenerPuntos() ?: 0}"
            btnRepetir.setOnClickListener {
                var comentario = editComentario.text.toString().trim()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var client = OkHttpClient()
                        var url = "http://pruebaemilio.atwebpages.com/juego/resultadospost.php" +
                                "?Id_Usuario=${usuarioActual?.Id_Usuario}" +
                                "&Id_Cuestionario=$idCuestionario" +
                                "&Puntuacion=${juego?.obtenerPuntos()}" +
                                "&Comentario=${URLEncoder.encode(comentario, "UTF-8")}"
                        println(url)
                        var request = Request.Builder().url(url).get().build()
                        var resp = client.newCall(request).execute()
                        withContext(Dispatchers.Main) {
                            if (resp.isSuccessful) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Resultado enviado correctamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                mostrarLayoutMenu()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error al enviar resultado: código ${resp.code}.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (ex: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "No se pudo conectar con el servidor.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
            btnSalir.setOnClickListener { finish() }
        } catch (e: Exception) {
            Log.e("Resultados", "Error mostrando resultados", e)
            finish()
        }
    }
    // Detener y liberar recurso de audio
    private fun detenerAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}