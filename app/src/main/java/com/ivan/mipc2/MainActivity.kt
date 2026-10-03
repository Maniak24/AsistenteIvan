package com.ivan.mipc2

import android.net.Uri
import android.app.AlertDialog
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.MotionEvent
import android.media.ToneGenerator
import android.media.AudioManager
import android.view.inputmethod.InputMethodManager
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var conversationVoiceMode = false
    private var conversationProcessing = false
    private lateinit var ggufTv: TextView
    private lateinit var messagesRv: RecyclerView
    private lateinit var userInputEt: EditText
    private lateinit var userActionFab: FloatingActionButton
    private var generationJob: Job? = null
    private lateinit var voiceManager: VoiceManager
    private lateinit var catalogRepository: CatalogRepository
    private var isModelReady = false
    private val messages = mutableListOf<Message>()
    private val lastAssistantMsg = StringBuilder()
    private val messageAdapter = MessageAdapter(messages)

    private fun playMicStartSound() {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 80)
        tone.release()
    }

    private fun hideKeyboardPreservingText() {
        val imm = getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(userInputEt.windowToken, 0)
    }

            Log.e(TAG, "No se pudo preparar la voz", e)
            Toast.makeText(this, "No se pudo iniciar la voz de Mi PC.", Toast.LENGTH_LONG).show()
            return
        }
        startConversationListening()
    }

        if (!conversationVoiceMode || isListening || conversationProcessing) return
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true; VoiceConversationActivity.setState("listening") }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val spokenText = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                    if (!spokenText.isNullOrEmpty() && conversationVoiceMode) {
                        conversationProcessing = true
                        VoiceConversationActivity.setState("processing")
                        userInputEt.setText(spokenText)
                        userInputEt.setSelection(userInputEt.text.length)
                        handleUserInput()
                    }
                }
                override fun onError(error: Int) {
                    isListening = false
                    if (conversationVoiceMode) lifecycleScope.launch { kotlinx.coroutines.delay(500); startConversationListening() }
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopConversationMode() {
        conversationVoiceMode = false
        isListening = false
        conversationProcessing = false
        speechRecognizer?.cancel()
        if (::voiceManager.isInitialized) voiceManager.stop()
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "El reconocimiento de voz no está disponible.", Toast.LENGTH_SHORT).show(); return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2001); return
        }
        playMicStartSound()
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        userInputEt.setText(it)
                        userInputEt.setSelection(userInputEt.text.length)
                        if (isModelReady && it.isNotBlank()) handleUserInput()
                    }
                    isListening = false
                }
                override fun onError(error: Int) { isListening = false; Toast.makeText(this@MainActivity, "No pude reconocer la voz.", Toast.LENGTH_SHORT).show() }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-AR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                300
            )
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContentView(R.layout.activity_main)

        
        
        
        
        
        findViewById<android.widget.TextView>(R.id.btnTramites).setOnClickListener {
            startActivity(Intent(this, TramitesActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.btnMapa).setOnClickListener {
            startActivity(Intent(this, MapaReclamosActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.btnObras).setOnClickListener {
            startActivity(Intent(this, ObrasActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.btnNoticias).setOnClickListener {
            startActivity(Intent(this, NoticiasActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.btnMisReclamos).setOnClickListener {
            startActivity(Intent(this, MisReclamosActivity::class.java))
        }

        
        findViewById<android.widget.TextView>(R.id.btnInformacion).setOnClickListener {
            startActivity(Intent(this, InformacionActivity::class.java))
        }

        findViewById<android.widget.TextView>(R.id.btnPanelMunicipal).setOnClickListener {
            val entrada = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                hint = "PIN"
            }

            android.app.AlertDialog.Builder(this)
                .setTitle("Acceso municipal")
                .setMessage("Ingresá el PIN para continuar")
                .setView(entrada)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ingresar") { _, _ ->
                    if (entrada.text.toString() == "2468") {
                        startActivity(Intent(this, PanelMunicipalActivity::class.java))
                    } else {
                        android.widget.Toast.makeText(
                            this,
                            "PIN incorrecto",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .show()
        }

        findViewById<android.widget.TextView>(R.id.btnReclamo).setOnClickListener {
            startActivity(Intent(this, ReclamoActivity::class.java))
        }
        catalogRepository = CatalogRepository(applicationContext)

        findViewById<View>(R.id.btn_mic).setOnClickListener { startVoiceInput() }
        findViewById<View>(R.id.btn_call).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 2001)
            } else {
                startActivityForResult(Intent(this, VoiceConversationActivity::class.java), 3001)
            }
        }

        val btnMenu = findViewById<View>(R.id.btn_menu)
        val sideMenu = findViewById<View>(R.id.side_menu)
        findViewById<View>(R.id.btn_close_menu).setOnClickListener { sideMenu.visibility = View.GONE }
        btnMenu.setOnClickListener { sideMenu.visibility = if (sideMenu.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        findViewById<View>(R.id.menu_new_chat).setOnClickListener { messages.clear(); messageAdapter.notifyDataSetChanged(); sideMenu.visibility = View.GONE; userInputEt.text.clear() }
        findViewById<View>(R.id.menu_files).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, 1001)
            sideMenu.visibility = View.GONE
        }
        findViewById<View>(R.id.menu_catalog).setOnClickListener { startActivity(Intent(this, CatalogActivity::class.java)); sideMenu.visibility = View.GONE }
        findViewById<View>(R.id.menu_history).setOnClickListener {
            AlertDialog.Builder(this).setTitle("Historial").setMessage("Todavía no hay conversaciones guardadas.").setPositiveButton("Cerrar", null).show(); sideMenu.visibility = View.GONE
        }
        findViewById<View>(R.id.menu_settings).setOnClickListener {
            val opciones = arrayOf("Modelo de IA", "Voz de Mi PC", "Reconocimiento de voz", "Comportamiento", "Apariencia", "Información del local", "Borrar conversaciones", "Acerca de Mi PC")
            AlertDialog.Builder(this).setTitle("Configuración").setItems(opciones) { _, which ->
                when (which) {
                    0 -> AlertDialog.Builder(this).setTitle("Modelo de IA").setMessage(if (isModelReady) "Gemini está conectado y funcionando online." else "La IA online no está disponible.").setPositiveButton("Cerrar", null).show()
                    1 -> AlertDialog.Builder(this).setTitle("Voz de Mi PC").setMessage("Voz de Mi PC mediante síntesis de voz local.").setPositiveButton("Cerrar", null).show()
                    2 -> AlertDialog.Builder(this).setTitle("Reconocimiento de voz").setMessage("Idioma configurado: Español (Argentina)").setPositiveButton("Cerrar", null).show()
                    3 -> AlertDialog.Builder(this).setTitle("Comportamiento").setItems(arrayOf("Respuestas breves", "Respuestas normales", "Respuestas detalladas"), null).show()
                    4 -> AlertDialog.Builder(this).setTitle("Apariencia").setItems(arrayOf("Oscuro", "Claro", "Usar configuración del dispositivo"), null).show()
                    5 -> mostrarInformacionLocal()
                    6 -> AlertDialog.Builder(this).setTitle("Borrar conversaciones").setMessage("¿Querés eliminar todos los mensajes de esta conversación?").setNegativeButton("Cancelar", null).setPositiveButton("Borrar") { _, _ -> messages.clear(); messageAdapter.notifyDataSetChanged() }.show()
                    7 -> AlertDialog.Builder(this).setTitle("Mi PC").setMessage("Asistente inteligente\n\nIA online con Gemini\nVoz local\n\nFundador y CEO: Yvan Reinaldo Veron\nColonia Victoria, Misiones, Argentina\n\nMi PC — El futuro es hoy.").setPositiveButton("Cerrar", null).show()
                }
            }.show()
            sideMenu.visibility = View.GONE
        }

        val connectionStatus = findViewById<TextView>(R.id.connection_status)
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(network)
        connectionStatus.text = if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) "●  En línea  •  Asistente inteligente" else "●  Sin conexión  •  Asistente inteligente"

        onBackPressedDispatcher.addCallback { Log.w(TAG, "Ignore back press for simplicity") }
        ggufTv = findViewById(R.id.gguf)
        messagesRv = findViewById(R.id.messages)
        messagesRv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messagesRv.adapter = messageAdapter

        messages.add(
            Message(
                id = UUID.randomUUID().toString(),
                content = "Hola 👋 Soy Mi PC.\n\nPuedo ayudarte con preguntas, archivos, catálogo, soporte técnico y tareas del día a día.\n\nEscribime abajo para comenzar.",
                isUser = false
            )
        )
        messageAdapter.notifyItemInserted(0)
        messagesRv.post {
            messagesRv.scrollToPosition(messages.lastIndex)
        }

        userInputEt = findViewById(R.id.user_input)
        val btnMic = findViewById<View>(R.id.btn_mic)
        val btnCall = findViewById<View>(R.id.btn_call)
        userInputEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { val writing = !s.isNullOrBlank(); btnMic.visibility = if (writing) View.GONE else View.VISIBLE; btnCall.visibility = if (writing) View.GONE else View.VISIBLE }
            override fun afterTextChanged(s: Editable?) {}
        })
        messagesRv.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) hideKeyboardPreservingText()
            false
        }

        userInputEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                userInputEt.postDelayed({
                    val imm = getSystemService(InputMethodManager::class.java)
                    imm.showSoftInput(
                        userInputEt,
                        InputMethodManager.SHOW_IMPLICIT
                    )
                }, 120)
            }
        }

        userActionFab = findViewById(R.id.fab)
        iniciarMotorIADeFormaSegura()
        userActionFab.setOnClickListener {
            if (isModelReady) handleUserInput()
        }

        userInputEt.setOnEditorActionListener { _, actionId, event ->
            val enter = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                    (event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                            event.action == android.view.KeyEvent.ACTION_DOWN)

            if (enter && !userInputEt.text.isNullOrBlank()) {
                if (isModelReady) handleUserInput()
                true
            } else false
        }

    }




    private fun handleUserInput() {
        Log.d(TAG, "HANDLE_USER_INPUT_ENTRANDO")
        val userMsg = userInputEt.text.toString().trim()
        if (userMsg.isEmpty()) return

        userInputEt.text.clear()
        userActionFab.isEnabled = false

        messages.add(
            Message(
                id = UUID.randomUUID().toString(),
                content = userMsg,
                isUser = true
            )
        )
        messageAdapter.notifyItemInserted(messages.lastIndex)
        messagesRv.scrollToPosition(messages.lastIndex)

        val assistantIndex = messages.size
        messages.add(
            Message(
                id = UUID.randomUUID().toString(),
                content = "Pensando...",
                isUser = false
            )
        )
        messageAdapter.notifyItemInserted(messages.lastIndex)
        messagesRv.scrollToPosition(messages.lastIndex)

        lifecycleScope.launch {
            try {
                val catalogContext = catalogRepository.buildContext(userMsg)

                val prompt = """
                    Sos Mi PC, un asistente inteligente en español argentino.
                    Respondé de forma clara, útil y natural.

                    REGLAS IMPORTANTES:
                    - No inventes productos, precios, stock, promociones ni datos del catálogo.
                    - Si la información no está en el catálogo, decilo claramente.
                    - No afirmes que tenés acceso a archivos o funciones que no tenés.
                    - Priorizá respuestas breves y prácticas.

                    CONTEXTO DEL CATÁLOGO:
                    ${catalogContext.text}

                    MENSAJE DEL USUARIO:
                    $userMsg
                """.trimIndent()

                val response = withContext(Dispatchers.IO) {
                    GeminiClient.ask(prompt)
                }

                messages[assistantIndex] = Message(
                    id = messages[assistantIndex].id,
                    content = response,
                    isUser = false
                )
                messageAdapter.notifyItemChanged(assistantIndex)
                messagesRv.scrollToPosition(assistantIndex)

                if (conversationVoiceMode) {
                    conversationProcessing = false
                    if (::voiceManager.isInitialized) {
                        voiceManager.speak(response)
                    }
                    VoiceConversationActivity.setState("speaking")
                    lifecycleScope.launch {
                        kotlinx.coroutines.delay(1800)
                        if (conversationVoiceMode) {
                            VoiceConversationActivity.setState("listening")
                            startConversationListening()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error consultando Gemini", e)

                messages[assistantIndex] = Message(
                    id = messages[assistantIndex].id,
                    content = "⚠️ No pude responder.\n\n${e.message ?: "Error desconocido de conexión con Gemini"}.\n\nRevisá internet y la configuración de la IA.",
                    isUser = false
                )
                messageAdapter.notifyItemChanged(assistantIndex)
                messagesRv.scrollToPosition(assistantIndex)

                if (conversationVoiceMode) {
                    conversationProcessing = false
                    VoiceConversationActivity.setState("error")
                }
            } finally {
                userActionFab.isEnabled = true
            }
        }
    }

    private fun mostrarInformacionLocal() {
        AlertDialog.Builder(this)
            .setTitle("Información del local")
            .setMessage(
                "Mi PC\n\n" +
                "Tecnología · Soporte · Soluciones\n\n" +
                "Atención y soporte técnico.\n" +
                "Colonia Victoria, Misiones, Argentina."
            )
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun iniciarMotorIADeFormaSegura() {
        isModelReady = true
        ggufTv.text = "IA online · Gemini"
        userInputEt.isEnabled = true
        userActionFab.isEnabled = true
    }

    private fun ensureModelsDirectory() = File(filesDir, DIRECTORY_MODELS).also { if (it.exists() && !it.isDirectory) it.delete(); if (!it.exists()) it.mkdir() }
    override fun onStop() { generationJob?.cancel(); super.onStop() }
    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 3001) {
            conversationVoiceMode = false
            conversationProcessing = false
            return
        }

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return

            lifecycleScope.launch {
                try {
                    val fileName = getFileName(uri)
                    val mimeType = contentResolver.getType(uri) ?: "archivo"

                    val contenido = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.use { input ->
                            input.bufferedReader(Charsets.UTF_8).readText().take(12000)
                        }
                    }

                    messages.add(
                        Message(
                            id = UUID.randomUUID().toString(),
                            content = "📎 Archivo cargado: $fileName\nTipo: $mimeType\n\n" +
                                    if (!contenido.isNullOrBlank())
                                        "Contenido:\n$contenido"
                                    else
                                        "El archivo fue seleccionado correctamente, pero no contiene texto legible.",
                            isUser = false
                        )
                    )

                    messageAdapter.notifyItemInserted(messages.lastIndex)
                    messagesRv.scrollToPosition(messages.lastIndex)

                } catch (e: Exception) {
                    Log.e(TAG, "Error leyendo archivo", e)

                    messages.add(
                        Message(
                            id = UUID.randomUUID().toString(),
                            content = "📎 Archivo seleccionado, pero no pude leer su contenido. " +
                                    "Este formato puede necesitar un lector específico.",
                            isUser = false
                        )
                    )

                    messageAdapter.notifyItemInserted(messages.lastIndex)
                    messagesRv.scrollToPosition(messages.lastIndex)
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name: String? = null

        contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index)
            }
        }

        return name ?: uri.lastPathSegment ?: "archivo"
    }
    override fun onDestroy() {
        conversationVoiceMode = false; conversationProcessing = false; speechRecognizer?.destroy(); speechRecognizer = null
        if (::voiceManager.isInitialized) voiceManager.release()
        super.onDestroy()
    }
    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val DIRECTORY_MODELS = "models"
        private const val FILE_EXTENSION_GGUF = ".gguf"
        private const val BENCH_PROMPT_PROCESSING_TOKENS = 512
        private const val BENCH_TOKEN_GENERATION_TOKENS = 128
        private const val BENCH_SEQUENCE = 1
        private const val BENCH_REPETITION = 3
    }
}

