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
    // Variables de clase
    private var nombre = ""
    private lateinit var juego: Juego
    private lateinit var preguntasDisponibles: MutableList<Pregunta>
    private var preguntaActualIndex = 0

    // Métodos de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_nombre) // Cargar layout inicial
        formularionombre()
    }

    private fun formularionombre() {
        setContentView(R.layout.layout_nombre)
        val buttonConfirmar = findViewById<Button>(R.id.buttonEnviar)
        val buttonSalir = findViewById<Button>(R.id.buttonSalir)

        buttonConfirmar.setOnClickListener { configurarBotonesLayoutCategorias() } // Salir de la app
        buttonSalir.setOnClickListener { finish() } // Salir de la app
    }



    private fun reproducirAudio(cancionResId: Int) {
        // Si ya hay un MediaPlayer reproduciendo, detenerlo y liberarlo
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.reset() // Reiniciamos para reutilizarlo
        } ?: run {
            // Si es nulo, creamos una nueva instancia
            mediaPlayer = MediaPlayer()
        }

        // Configuramos el MediaPlayer para reproducir el recurso de audio de manera asíncrona
        try {
            val afd = resources.openRawResourceFd(cancionResId)
            mediaPlayer?.apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true // Hacer que el audio se reproduzca en bucle
                setOnPreparedListener { mp -> mp.start() }
                prepareAsync() // Prepara el audio de manera asíncrona
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Configura los botones del layout inicial
    private fun configurarBotonesLayoutCategorias() {

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
        buttonSalir.setOnClickListener { formularionombre() }
    }

    // Modificar iniciarJuego para detener audio antes de cambiar de pantalla
    private fun iniciarJuego(tematica: String) {
        detenerAudio() // Detener cualquier audio en reproducción
        juego = Juego()

        val preguntasCategoria = listaPreguntas[tematica] ?: emptyList()
        preguntasDisponibles = preguntasCategoria.shuffled().toMutableList()

        setContentView(R.layout.layout_cuestionario)
        mostrarSiguientePregunta()
    }

    // Modificar mostrarSiguientePregunta para detener audio antes de volver al inicio
    private fun mostrarSiguientePregunta() {
        if (preguntaActualIndex >= preguntasDisponibles.size) {
            detenerAudio() // Detener el audio antes de volver al menú
            preguntaActualIndex = 0
            juego = Juego()
            configurarBotonesLayoutCategorias()
        } else {
            val preguntaActual = preguntasDisponibles[preguntaActualIndex]
            actualizarUI(preguntaActual)
        }
    }

    private fun actualizarUI(pregunta: Pregunta) {
        // Actualizar texto de pregunta y puntos
        findViewById<TextView>(R.id.textViewPregunta).text = pregunta.pregunta
        findViewById<TextView>(R.id.textViewPuntos).text = "Puntuación: ${juego.obtenerPuntos()}"

        // Actualizar imagen
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageResource(pregunta.imagenResId)

        // Reproducir la canción asociada de forma asíncrona
        reproducirAudio(pregunta.cancionResId)

        // Configurar botones de opciones
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

    // Preguntas organizadas por categoría
    private val listaPreguntas = mapOf(
        "Videojuegos" to listOf(
            Pregunta(
                "¿Cómo se puede mejorar el nivel del Dead Eye en Red Dead Redemption 2?",
                "Cazando, pescando y completando desafíos",
                listOf("Comprando mejoras en tiendas", "Usando tónicos constantemente", "Disparando sin apuntar"),
                R.drawable.rdr2,
                R.raw.rdr2
            ),
            Pregunta(
                "¿Qué ocurre en minecraft si decides poner una cama y dormir en el nether?",
                "La cama explota",
                listOf("Se hace de día", "No puedes poner camas", "No ocurre nada"),
                R.drawable.minecraft,
                R.raw.minecraft
            ),
            Pregunta(
                "¿Qué característica define al combate de Doom 2016?",
                "Movimiento rápido y Glory Kills",
                listOf("Uso de coberturas", "Sigilo y estrategia", "Recoger munición de los enemigos caídos"),
                R.drawable.doom,
                R.raw.doom
            ),
            Pregunta(
                "¿Cuál es el primer nivel en Super Mario 64?",
                "Bob-omb Battlefield",
                listOf("Whomp’s Fortress", "Cool, Cool Mountain", "Shifting Sand Land"),
                R.drawable.mario,
                R.raw.mario
            ),
            Pregunta(
                "¿Qué sucede si atacas a los pollos en The Legend of Zelda: Twilight Princess muchas veces?",
                "Tomas el control del pollo por unos segundos",
                listOf("Te atacan en enjambre", "Se convierten en un jefe", "Nada"),
                R.drawable.zelda,
                R.raw.zelda
            ),
            Pregunta(
                "¿Cuál es el nombre de M. Bison en la versión japonesa de Street Fighter?",
                "Vega",
                listOf("Balrog", "Michael Bison", "Master Vega"),
                R.drawable.streetfighter,
                R.raw.streetfighter
            ),
            Pregunta(
                "¿Cuál es la nacionalidad del protagonista de GTA IV?",
                "Serbia",
                listOf("Yugoslavia", "Rumanía", "Croacia"),
                R.drawable.gta,
                R.raw.gta
            ),
            Pregunta(
                "¿Qué celebridad apareció por primera vez en Fortnite?",
                "Keanu Reeves",
                listOf("Travis Scott", "Ninja", "Marshmello"),
                R.drawable.fortnite,
                R.raw.fortnite
            ),
            Pregunta(
                "¿En qué juego de Sonic se introduce a Blaze the Cat por primera vez?",
                "Sonic Rush",
                listOf("Sonic Heroes", "Sonic Adventure 2", "Sonic Unleashed"),
                R.drawable.sonic,
                R.raw.sonic
            ),
            Pregunta(
                "¿Cuál de los siguientes juegos de la saga Persona tiene un método de invocación igual al de Persona 1?",
                "Persona 2",
                listOf("Persona 3", "Persona 4", "Persona 5"),
                R.drawable.persona,
                R.raw.persona
            )
        ),
        "Peliculas" to listOf(
            Pregunta(
                "¿Cuál de estas películas de animación no fue producida por Studio Ghibli?",
                "Akira",
                listOf("Ponyo en el acantilado", "Porco Rosso", "El chico y la garza"),
                R.drawable.ghibli,
                R.raw.ghibli
            ),
            Pregunta(
                "¿Cuál de estas canciones forma parte de la banda sonora de la película Django Unchained?",
                "Who did that to you? - John Legend",
                listOf("Bang bang - Nancy Sinatra", "The ecstasy of gold - Ennio Morricone", "I walk the line - Johnny Cash"),
                R.drawable.django,
                R.raw.django
            ),
            Pregunta(
                "¿En qué personaje está inspirado el gato con botas de Shrek?",
                "El zorro",
                listOf("Don Quijote", "Robin Hood", "El Cid Campeador"),
                R.drawable.gato,
                R.raw.gato
            ),
            Pregunta(
                "¿Cuál fué la última película en la que actuó Paul Walker?",
                "Fast and Furious 7",
                listOf("Fast and furious 6", "Brick mansions", "The lazarus project"),
                R.drawable.paul,
                R.raw.paul
            ),
            Pregunta(
                "¿Cuál fué la primera película en la que actuó Will Smith?",
                "Donde te lleve el día",
                listOf("Independence Day", "Hombres de negro", "Bad boys"),
                R.drawable.will,
                R.raw.will
            ),
            Pregunta(
                "¿Como se llama el personaje del que hace Terry Crews en Dos rubias de pelo en pecho?",
                "Latrell Spencer",
                listOf("DeShawn Mitchell", "Jamal Perkins", "Tyrone McArthur"),
                R.drawable.terry,
                R.raw.terry
            ),
            Pregunta(
                "¿Cual de las siguientes películas es propiedad de Disney?",
                "Anastasia",
                listOf("El gigante de hierro", "Fievel y el nuevo mundo", "8 noches locas"),
                R.drawable.disney,
                R.raw.disney
            ),
            Pregunta(
                "¿Cuál de estas películas impulsó la carrera de Christopher Nolan y lo hizo famoso?",
                "Memento",
                listOf("Following", "Interstellar", "The Dark Knight"),
                R.drawable.nolan,
                R.raw.nolan
            ),
            Pregunta(
                "¿Cuál de estas películas fue dirigida por James Cameron y se convirtió en la película más taquillera durante años?",
                "Avatar",
                listOf("Titanic", "Terminator 2: Judgment Day", "Aliens"),
                R.drawable.james,
                R.raw.james
            ),
            Pregunta(
                "¿En que año se estrenó la película de los Simpson?",
                "2007",
                listOf("2008", "2006", "2005"),
                R.drawable.simpson,
                R.raw.simpson
            )
        ),
        "Música" to listOf(
            Pregunta(
                "¿Qué famosa banda de rock compuso la canción Roundabout?",
                "Yes",
                listOf("King Crimson", "Genesis", "Rush"),
                R.drawable.rondabout,
                R.raw.rondabout
            ),
            Pregunta(
                "¿Cual de estos géneros musicales no se originó en EEUU?",
                "Afrobeat",
                listOf("Country", "Rap", "Rock"),
                R.drawable.usa,
                R.raw.usa
            ),
            Pregunta(
                "¿A que película de Torrente pertenece esta canción?",
                "Torrente",
                listOf("Torrente 3", "Torrente 5", "Torrente 6"),
                R.drawable.torrente,
                R.raw.torrente
            ),
            Pregunta(
                "¿Que grupo compuso esta canción?",
                "Ost Front",
                listOf("Rammstein", "Rage", "Bassinvaders"),
                R.drawable.metal,
                R.raw.metal
            ),
            Pregunta(
                "¿Cuál de estos grupos es de origen británico?",
                "The beatles",
                listOf("ACDC", "Nirvana", "Green Day"),
                R.drawable.uk,
                R.raw.uk
            ),
            Pregunta(
                "¿Que grupo compuso la intro de la serie española Física o química?",
                "Despistaos",
                listOf("Zodiacs", "Pereza", "La habitación roja"),
                R.drawable.foq,
                R.raw.foq
            ),
            Pregunta(
                "¿En cuantos idiomas cantó Phil Colling esta canción?",
                "7",
                listOf("5", "3", "4"),
                R.drawable.tarzan,
                R.raw.tarzan
            ),
            Pregunta(
                "¿Que como se llama esta canción?",
                "Tamacún - Rodrigo y Gabriela",
                listOf("Negro y azul - Los cuates de Sinaloa", "Apocalypshit - Molotov", "Alma de guitarra - Antonio Banderas"),
                R.drawable.heisenberg,
                R.raw.heisenberg
            ),
            Pregunta(
                "¿Cuál de estas personas es un exmiembro de la banda Imagine Dragons?",
                "Theresa Danielle Flaminio Romack",
                listOf("Benjamin Arthur McKee", "Daniel Coulter Reynolds", "Daniel Wayne Sermon"),
                R.drawable.imaginedragons,
                R.raw.imaginedragons
            ),
            Pregunta(
                "¿Que grupo compuso esta canción?",
                "Led Zeppelin",
                listOf("The beatles", "Rolling Stones", "Kiss"),
                R.drawable.stairway,
                R.raw.stairway
            )
        ),
        "Historia" to listOf(
            Pregunta(
                "Que año se independizó EEUU del Reino Unido",
                "1776",
                listOf("1773", "1774", "1775"),
                R.drawable.independencia,
                R.raw.independencia
            ),
            Pregunta(
                "¿Cuanto duró la guerra de las islas malvinas?",
                "74 días",
                listOf("1 mes y medio", "2 años", "63 días"),
                R.drawable.malvinas,
                R.raw.malvinas
            ),
            Pregunta(
                "¿Cuál de los siguientes territorios fué el último en independizarse del imperio español?",
                "Puerto Rico, Cuba y Filipinas",
                listOf("México, Argentina y Colombia", "Panamá, Bolivia y Perú", "Venezuela, Guinea Ecuatorial y Uruguay"),
                R.drawable.america,
                R.raw.america
            ),
            Pregunta(
                "¿Cual es la sociedad más antigua del mundo descubierta?",
                "Aborígenas australianos",
                listOf("Sumerios", "Egipcios", "Los pueblos del mar"),
                R.drawable.antiguo,
                R.raw.antiguo
            ),
            Pregunta(
                "¿Cuál de los siguientes filósofos era conocido como El perro",
                "Diógenes de Sinope",
                listOf("Sócrates de Atenas", "Friedrich Wilhelm Nietzsche", "René Descartes"),
                R.drawable.perro,
                R.raw.perro
            ),
            Pregunta(
                "¿Que países participaron en la guerra de los boxers?",
                "Reino Unido, China, Francia, Japón, Rusia, Estados Unidos, Alemania, Italia, Austria-Hungría",
                listOf("Corea, Unión soviética, Austria-Hungría, México, China, Polonia, Alemania del este", "India, Australia, Nueva Zelanda, España, Alemania del este, Bielorrusia", "Corea del norte, Unión soviética, Austria-Hungría, Alemania del oeste, Unión ibérica, Checoslovaquia, Prusia"),
                R.drawable.boxers,
                R.raw.boxers
            ),
            Pregunta(
                "¿Que países apoyaron al bando repúblicano en la guerra civil española?",
                "Unión soviética",
                listOf("Alemania e Italia", "Estados Unidos", "Suiza"),
                R.drawable.guerra,
                R.raw.guerra
            ),
            Pregunta(
                "¿En que año se cayó el muro de Berlín?",
                "1989",
                listOf("1990", "1991", "1988"),
                R.drawable.berlin,
                R.raw.berlin
            ),
            Pregunta(
                "¿Fue el rol de la Armada Invencible la causa principal de la derrota de España ante Inglaterra en 1588?",
                "El rol de la Armada Invencible fue clave, pero factores económicos, políticos y estratégicos también influyeron en la derrota española",
                listOf("La derrota española se debió únicamente a la superioridad militar inglesa", "La Armada Invencible perdió porque sus barcos eran demasiado pequeños y débiles", "España fue derrotada porque sus tropas nunca llegaron a enfrentarse a los ingleses."),
                R.drawable.armada,
                R.raw.armada
            ),
            Pregunta(
                "¿Qué factor fue determinante para la rendición de Japón en 1945?",
                "La combinación de los bombardeos atómicos y la invasión soviética.",
                listOf("Solo los bombardeos atómicos.", "Solo la invasión soviética.", "La rendición fue voluntaria sin presión externa."),
                R.drawable.japon,
                R.raw.japon
            ),
            Pregunta(
                "¿Porqué el ataque al Pearl Harbor fue clave para la entrada de EEUU en la Segunda Guerra Mundial?",
                "Causó la entrada de EEUU en la guerra pese a las restricciones de la Ley de Neutralidad.",
                listOf("EEUU ya estaba en guerra antes del ataque.", "El ataque no afectó a la decisión de EEUU.", "Japón nunca atacó el Pearl Harbor."),
                R.drawable.pearl,
                R.raw.pearl
            ),
            Pregunta(
                "¿Cómo influyó la Segunda Guerra Mundial en el rol y estatus social de la mujer en EEUU.?",
                "La guerra impulsó temporalmente la integración femenina en el trabajo y el ejército, sentando las bases del movimiento feminista pese al retorno de roles tradicionales tras 1945.",
                listOf("La guerra no tuvo ningún impacto en el rol de la mujer en la sociedad estadounidense.", "Las mujeres reemplazaron permanentemente a los hombres en el mercado laboral y no enfrentaron retrocesos tras la guerra.", "La Segunda Guerra Mundial redujo la participación de las mujeres en la vida pública y limitó sus oportunidades laborales."),
                R.drawable.mujer,
                R.raw.mujer
            ),
            Pregunta(
                "¿Hasta qué punto fue determinante el concepto de Lebensraum en la invasión nazi a la URSS en 1941?",
                "Fue un factor clave, ya que Hitler buscaba expandir territorio y recursos para Alemania, pero la URSS usó esos mismos recursos para derrotarlo.",
                listOf("No tuvo relación, ya que la invasión fue solo por motivos ideológicos.", "Alemania ya tenía suficiente territorio y solo buscaba debilitar a la URSS.", "La invasión ocurrió por error estratégico, sin planificación previa."),
                R.drawable.lebensraum,
                R.raw.lebensraum
            ),
            Pregunta(
                "¿En qué medida la ideología nazi fue un factor determinante en el inicio de la Segunda Guerra Mundial en Europa?",
                "La ideología nazi fue un factor indirecto que impulsó políticas exteriores agresivas, contribuyendo al inicio de la Segunda Guerra Mundial.",
                listOf("La ideología nazi fue el único factor que causó la Segunda Guerra Mundial.", "La ideología nazi no tuvo ninguna influencia en la política exterior de Alemania.", "La ideología nazi se centró exclusivamente en la economía, sin afectar las decisiones militares."),
                R.drawable.nazi,
                R.raw.nazi
            ),
            // Preguntas generadas para completar los huecos faltantes:
            Pregunta(
                "¿Cuál fue la causa principal de la caída del Imperio Romano?",
                "Invasión de los pueblos bárbaros",
                listOf("Crisis económica", "Plagas y enfermedades", "Conflictos internos"),
                R.drawable.romanos,
                R.raw.romanos
            ),
            Pregunta(
                "¿Quién fue el primer presidente de los Estados Unidos?",
                "George Washington",
                listOf("Thomas Jefferson", "Abraham Lincoln", "John Adams"),
                R.drawable.presidente,
                R.raw.presidente
            ),
            Pregunta(
                "¿Qué tratado puso fin a la Primera Guerra Mundial?",
                "Tratado de Versalles",
                listOf("Tratado de París", "Tratado de Gante", "Tratado de Brest-Litovsk"),
                R.drawable.ww1,
                R.raw.ww1
            ),
            Pregunta(
                "¿En qué año se inició la Revolución Francesa?",
                "1789",
                listOf("1776", "1804", "1799"),
                R.drawable.francia,
                R.raw.francia
            ),
            Pregunta(
                "¿Qué civilización construyó Machu Picchu?",
                "Incas",
                listOf("Mayas", "Aztecas", "Olmecas"),
                R.drawable.peru,
                R.raw.peru
            ),
            Pregunta(
                "¿Quién lideró el movimiento de independencia de la India?",
                "Mahatma Gandhi",
                listOf("Jawaharlal Nehru", "Subhas Chandra Bose", "Indira Gandhi"),
                R.drawable.india,
                R.raw.india
            )
        ),
        "Literatura" to listOf(
            Pregunta(
                "¿En que año se publicó 'Don Quijote'?",
                "1605",
                listOf("1604", "1603", "1602"),
                R.drawable.quijote,
                R.raw.quijote
            ),
            Pregunta(
                "¿De qué libro proviene la frase 'Ser o no ser'?",
                "Hamlet",
                listOf("Romeo y Julieta", "Otelo", "El rey Lear"),
                R.drawable.shakespeare,
                R.raw.shakespeare
            ),
            Pregunta(
                "¿Cuál es el libro japonés más antiguo?",
                "Kojiki",
                listOf("Manyoshu", "Tagosaku to Mokube no Tokyo Kenbutsu", "La historia de Genji"),
                R.drawable.japan,
                R.raw.japan
            ),
            Pregunta(
                "¿Quién escribió 1984?",
                "George Orwell",
                listOf("Aldous Huxley", "Ray Bradbury", "Philip K. Dick"),
                R.drawable.l1984,
                R.raw.l1984
            ),
            Pregunta(
                "¿Qué poeta chileno ganó el Premio Nobel de Literatura en 1971?",
                "Pablo Neruda",
                listOf("Vicente Huidobro", "Gabriela Mistral", "Nicanor Parra"),
                R.drawable.nobel,
                R.raw.nobel
            ),
            Pregunta(
                "¿Cuál es el libro más traducido del mundo después de la biblia?",
                "El principito",
                listOf("Romeo y Julieta", "Don Quijote de la Mancha", "El Corán"),
                R.drawable.libro,
                R.raw.libro
            ),
            Pregunta(
                "¿Durante que dinastía se escribió el libro 'El arte de la guerra'?",
                "Dinastía Zhou",
                listOf("Dinastía Song", "Dinastía Qing", "Dinastía Tang"),
                R.drawable.suntzu,
                R.raw.suntzu
            ),
            Pregunta(
                "¿Quién escribió 'Matilda'?",
                "Roald Dahl",
                listOf("J.K. Rowling", "Dr. Seuss", "Beatrix Potter"),
                R.drawable.matilda,
                R.raw.matilda
            ),
            Pregunta(
                "¿Quién escribió La Odisea?",
                "Homero",
                listOf("Virgilio", "Sófocles", "Ovidio"),
                R.drawable.odisea,
                R.raw.odisea
            ),
            Pregunta(
                "¿Qué novela fue escrita por Albert Camus?",
                "El extranjero",
                listOf("La peste escarlata", "El túnel", "La náusea"),
                R.drawable.camus,
                R.raw.camus
            )
        ),
        "Geografía" to listOf(
            Pregunta(
                "¿Cuál es la capital de Estados Unidos?",
                "Washington D.C",
                listOf("Nueva York", "Chicago", "San Francisco"),
                R.drawable.eeuu,
                R.raw.eeuu
            ),
            Pregunta(
                "¿Qué país hace frontera con Rusia?",
                "Kazajistán",
                listOf("Corea del sur", "Nepal", "Bután"),
                R.drawable.rusia,
                R.raw.rusia
            ),
            Pregunta(
                "¿Cuál de estos territorios de China fué un territorio portugués?",
                "Macao",
                listOf("Shanghái", "Hong Kong", "Pekín"),
                R.drawable.china,
                R.raw.china
            ),
            Pregunta(
                "¿Qué ciudad alemana es famosa por su festival de cerveza, el Oktoberfest?",
                "Múnich",
                listOf("Berlín", "Frankfurt", "Stuttgart"),
                R.drawable.alemania,
                R.raw.alemania
            ),
            Pregunta(
                "¿Cuál es estos países nunca fué miembro de la unión europea?",
                "Andorra",
                listOf("Reino Unido", "Polonia", "Chipre"),
                R.drawable.ue,
                R.raw.ue
            ),
            Pregunta(
                "¿Cuál de estos grupos corresponde a los cinco miembros originales del BRICS?",
                "Brasil, Rusia, India, China y Sudáfrica",
                listOf(
                    "Bolivia, Rusia, India, China y Singapur",
                    "Bielorrusia, Rumanía, India, Cuba y Sudáfrica",
                    "Brasil, Rusia, Irán, China y Sudán"
                ),
                R.drawable.brics,
                R.raw.brics
            ),
            Pregunta(
                "¿Cuál es estos países lleva más tiempo en la OTAN?",
                "Portugal",
                listOf("Estonia", "España", "Alemania"),
                R.drawable.otan,
                R.raw.otan
            ),
            Pregunta(
                "¿A que país pertenece el primer territorio conquistado por el imperio español cuando visitó américa?",
                "República Dominicana",
                listOf("Cuba", "Honduras", "Nicaragua"),
                R.drawable.imperio,
                R.raw.imperio
            ),
            Pregunta(
                "¿Cuál de los siguientes países fué miembro de la Unión Soviética?",
                "Georgia",
                listOf("Polonia", "Rumanía", "Eslovaquia"),
                R.drawable.urss,
                R.raw.urss
            ),
            Pregunta(
                "¿Cuál es estos países no tiene territorios en más de un continente?",
                "Brasil",
                listOf("España", "Turquía", "Rusia"),
                R.drawable.mundo,
                R.raw.mundo
            )
        )
    )
}