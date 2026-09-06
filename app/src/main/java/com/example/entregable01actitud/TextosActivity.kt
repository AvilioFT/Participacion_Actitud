package com.example.entregable01actitud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class TextosActivity : AppCompatActivity() {

    private lateinit var radioGroupTextos: RadioGroup
    private lateinit var txtVacio: TextView
    private lateinit var txtTextoSeleccionado: TextView
    private lateinit var chipGroupFormato: ChipGroup
    private lateinit var btnVerTexto: Button
    private lateinit var btnDescargar: Button
    private lateinit var btnEliminar: Button
    private lateinit var btnNuevaConsulta: Button

    private var textoSeleccionadoId: String? = null
    private var formatoSeleccionado: FormatoDocumento.Formato? = null

    private val executorDescarga = Executors.newSingleThreadExecutor()

    private val preferences by lazy {
        getSharedPreferences(
            "textos_generados",
            MODE_PRIVATE
        )
    }

    private val crearArchivo =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("*/*")
        ) { uri: Uri? ->

            if (uri != null) {
                guardarArchivo(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_textos)

        radioGroupTextos = findViewById(
            R.id.radioGroupTextos
        )

        txtVacio = findViewById(
            R.id.txtVacio
        )

        txtTextoSeleccionado = findViewById(
            R.id.txtTextoSeleccionado
        )

        btnVerTexto = findViewById(
            R.id.btnVerTexto
        )

        btnDescargar = findViewById(
            R.id.btnDescargar
        )

        btnEliminar = findViewById(
            R.id.btnEliminar
        )

        btnNuevaConsulta = findViewById(
            R.id.btnNuevaConsulta
        )

        chipGroupFormato = findViewById(
            R.id.chipGroupFormato
        )

        radioGroupTextos.setOnCheckedChangeListener {
                group,
                checkedId ->

            if (checkedId != -1) {

                val radioButton =
                    group.findViewById<RadioButton>(
                        checkedId
                    )

                textoSeleccionadoId =
                    radioButton?.tag as? String

                chipGroupFormato.visibility = View.GONE
                formatoSeleccionado = null
            } else {
                textoSeleccionadoId = null
            }
        }

        guardarNuevoTexto()
        cargarTextos()

        btnVerTexto.setOnClickListener {
            mostrarTextoSeleccionado()
        }

        btnDescargar.setOnClickListener {
            prepararDescarga()
        }

        btnEliminar.setOnClickListener {
            eliminarSeleccionado()
        }

        btnNuevaConsulta.setOnClickListener {
            val principal = Intent(
                this,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            startActivity(principal)
            finish()
        }
    }

    private fun guardarNuevoTexto() {

        val api = intent.getStringExtra(
            "API_SELECCIONADA"
        )

        val texto = intent.getStringExtra(
            "TEXTO_BASE"
        )

        if (api.isNullOrEmpty() || texto.isNullOrEmpty()) {
            return
        }

        val textos = obtenerTextos()

        val fecha = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        val nuevoTexto = JSONObject()

        nuevoTexto.put(
            "id",
            System.currentTimeMillis().toString()
        )

        nuevoTexto.put(
            "api",
            api
        )

        nuevoTexto.put(
            "fecha",
            fecha
        )

        nuevoTexto.put(
            "contenido",
            texto
        )

        val csv = intent.getStringExtra(
            "TEXTO_CSV"
        )

        if (!csv.isNullOrEmpty()) {
            nuevoTexto.put(
                "csv",
                csv
            )
        }

        val imagen = intent.getStringExtra(
            "TEXTO_IMAGEN"
        )

        if (!imagen.isNullOrEmpty()) {
            nuevoTexto.put(
                "imagen",
                imagen
            )
        }

        textos.put(nuevoTexto)

        guardarTextos(textos)

        intent.removeExtra("API_SELECCIONADA")
        intent.removeExtra("TEXTO_BASE")
        intent.removeExtra("TEXTO_CSV")
        intent.removeExtra("TEXTO_IMAGEN")
    }

    private fun obtenerTextos(): JSONArray {

        val datos = preferences.getString(
            "lista_textos",
            "[]"
        ) ?: "[]"

        return try {

            JSONArray(datos)

        } catch (e: Exception) {

            JSONArray()
        }
    }

    private fun guardarTextos(
        textos: JSONArray
    ) {

        preferences.edit {
            putString(
                "lista_textos",
                textos.toString()
            )
        }
    }

    private fun cargarTextos() {

        radioGroupTextos.removeAllViews()

        textoSeleccionadoId = null
        formatoSeleccionado = null
        chipGroupFormato.removeAllViews()
        chipGroupFormato.visibility = View.GONE

        val textos = obtenerTextos()

        val vacio = textos.length() == 0

        txtVacio.visibility =
            if (vacio) View.VISIBLE else View.GONE

        radioGroupTextos.visibility =
            if (vacio) View.GONE else View.VISIBLE

        if (vacio) {
            return
        }

        for (i in textos.length() - 1 downTo 0) {

            val texto = textos.optJSONObject(i)
                ?: continue

            val id = texto.optString("id", "")
            if (id.isEmpty()) continue
            val api = texto.optString("api", "API")
            val fecha = texto.optString("fecha", "")

            val radioButton = RadioButton(this)

            radioButton.id = View.generateViewId()

            radioButton.tag = id

            val tieneCsv = texto.optString("csv", "").isNotEmpty()

            radioButton.text =
                if (fecha.isEmpty()) api else "$api - $fecha"

            if (tieneCsv) {
                radioButton.text = "${radioButton.text} · CSV"
            }

            radioButton.textSize = 16f

            radioButton.setPadding(
                8,
                16,
                8,
                16
            )

            radioGroupTextos.addView(
                radioButton
            )
        }
    }

    private fun mostrarTextoSeleccionado() {

        if (textoSeleccionadoId == null) {

            Toast.makeText(
                this,
                "Selecciona un texto primero.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val texto = buscarTexto(
            textoSeleccionadoId!!
        )

        if (texto == null) {

            Toast.makeText(
                this,
                "No se encontró el texto.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        txtTextoSeleccionado.text =
            texto.optString("contenido", "Sin contenido.")

        mostrarFormatos(texto)
    }

    private fun mostrarFormatos(texto: JSONObject) {
        chipGroupFormato.removeAllViews()

        val tieneCsv = texto.optString("csv", "").isNotEmpty()
        val formatos = FormatoDocumento.disponibles(tieneCsv)

        for (formato in formatos) {
            val chip = Chip(this)
            chip.text = formato.etiqueta
            chip.tag = formato
            chip.isCheckable = true
            chipGroupFormato.addView(chip)
        }

        val primero = chipGroupFormato.getChildAt(0) as? Chip
        primero?.isChecked = true
        formatoSeleccionado = formatos.firstOrNull()

        chipGroupFormato.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                formatoSeleccionado = chip?.tag as? FormatoDocumento.Formato
            }
        }

        chipGroupFormato.visibility = View.VISIBLE
    }

    private fun eliminarSeleccionado() {
        val id = textoSeleccionadoId

        if (id == null) {
            Toast.makeText(
                this,
                "Selecciona un texto primero.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val textos = obtenerTextos()
        val filtrados = JSONArray()

        for (i in 0 until textos.length()) {
            val texto = textos.optJSONObject(i) ?: continue
            if (texto.optString("id") != id) {
                filtrados.put(texto)
            }
        }

        guardarTextos(filtrados)
        cargarTextos()

        txtTextoSeleccionado.text =
            getString(R.string.placeholder_texto)

        Toast.makeText(
            this,
            "Consulta eliminada del historial.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun prepararDescarga() {

        if (textoSeleccionadoId == null) {

            Toast.makeText(
                this,
                "Selecciona un texto primero.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val texto = buscarTexto(
            textoSeleccionadoId!!
        )

        if (texto == null) {

            Toast.makeText(
                this,
                "No se encontró el texto.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (formatoSeleccionado == null) {
            Toast.makeText(
                this,
                "Pulsa VER TEXTO y elige un formato.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val formato = formatoSeleccionado!!

        if (formato.id == "csv" &&
            texto.optString("csv", "").isEmpty()
        ) {
            Toast.makeText(
                this,
                "Este texto no tiene versión CSV.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val api = texto.optString("api", "API")
        val fecha = texto.optString("fecha", "")

        val nombreArchivo =
            crearNombreArchivo(
                api,
                fecha,
                formato.extension
            )

        crearArchivo.launch(nombreArchivo)
    }

    private fun guardarArchivo(
        uri: Uri
    ) {

        if (textoSeleccionadoId == null) {
            return
        }

        val texto = buscarTexto(
            textoSeleccionadoId!!
        )

        val formato = formatoSeleccionado

        if (texto == null || formato == null) {
            return
        }

        val imagenUrl = texto.optString("imagen", "")

        if (formato.id == "docx" && imagenUrl.isNotEmpty()) {
            guardarDocxConImagen(uri, texto, imagenUrl)
            return
        }

        try {

            val bytes = FormatoDocumento.generar(
                formato.id,
                texto.optString("api", "API"),
                texto.optString("fecha", ""),
                texto.optString("contenido", ""),
                texto.optString("csv", "")
            )

            escribirBytes(uri, bytes)

            Toast.makeText(
                this,
                "Archivo ${formato.extension.uppercase()} guardado correctamente.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error al guardar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun escribirBytes(
        uri: Uri,
        bytes: ByteArray
    ) {
        contentResolver
            .openOutputStream(uri)
            ?.use { outputStream ->
                outputStream.write(bytes)
            }
    }

    private fun guardarDocxConImagen(
        uri: Uri,
        texto: JSONObject,
        imagenUrl: String
    ) {
        Toast.makeText(
            this,
            getString(R.string.msg_generando),
            Toast.LENGTH_SHORT
        ).show()

        executorDescarga.execute {
            try {
                val conexion = URL(imagenUrl).openConnection()
                conexion.connectTimeout = 15_000
                conexion.readTimeout = 15_000
                val imagenBytes = conexion.getInputStream().use {
                    it.readBytes()
                }

                val bytes = FormatoDocumento.generar(
                    "docx",
                    texto.optString("api", "API"),
                    texto.optString("fecha", ""),
                    texto.optString("contenido", ""),
                    texto.optString("csv", ""),
                    imagenBytes
                )

                escribirBytes(uri, bytes)

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Archivo DOCX con imagen guardado correctamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "No se pudo descargar la imagen; DOCX sin imagen: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                try {
                    val bytes = FormatoDocumento.generar(
                        "docx",
                        texto.optString("api", "API"),
                        texto.optString("fecha", ""),
                        texto.optString("contenido", ""),
                        texto.optString("csv", "")
                    )
                    escribirBytes(uri, bytes)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun buscarTexto(
        id: String
    ): JSONObject? {

        val textos = obtenerTextos()

        for (i in 0 until textos.length()) {

            val texto =
                textos.optJSONObject(i)
                    ?: continue

            if (
                texto.optString("id") == id
            ) {
                return texto
            }
        }

        return null
    }

    private fun crearNombreArchivo(
        api: String,
        fecha: String,
        extension: String = "txt"
    ): String {

        val apiLimpia =
            api.replace(
                Regex("[^a-zA-Z0-9_-]"),
                "_"
            )

        val fechaLimpia =
            fecha.replace(
                Regex("[^0-9-]"),
                "_"
            )

        return "${apiLimpia}_${fechaLimpia}.$extension"
    }

    override fun onDestroy() {
        super.onDestroy()
        executorDescarga.shutdown()
    }
}