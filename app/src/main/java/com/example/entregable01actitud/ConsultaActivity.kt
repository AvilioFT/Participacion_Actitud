package com.example.entregable01actitud

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.android.material.datepicker.MaterialDatePicker
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConsultaActivity : AppCompatActivity() {

    private lateinit var apiSeleccionada: String

    private lateinit var txtApi: TextView
    private lateinit var btnConsultar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtError: TextView

    private lateinit var layoutUsgs: View
    private lateinit var layoutNasa: View
    private lateinit var layoutGeoref: View
    private lateinit var layoutTc: View

    private var edtFechaInicio: EditText? = null
    private var edtFechaFin: EditText? = null
    private var edtMagnitud: EditText? = null

    private var edtFechaNasa: EditText? = null

    private var edtLocalidad: EditText? = null

    private var edtTcInicio: EditText? = null
    private var edtTcFin: EditText? = null
    private var edtMonto: EditText? = null
    private var radioGroupDir: RadioGroup? = null

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_consulta)

        apiSeleccionada =
            intent.getStringExtra("API_SELECCIONADA") ?: ""

        txtApi = findViewById(R.id.txtApi)
        btnConsultar = findViewById(R.id.btnConsultar)
        progressBar = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)

        layoutUsgs = findViewById(R.id.layoutUsgs)
        layoutNasa = findViewById(R.id.layoutNasa)
        layoutGeoref = findViewById(R.id.layoutGeoref)
        layoutTc = findViewById(R.id.layoutTc)

        txtApi.text = "API seleccionada: $apiSeleccionada"

        ocultarTodosLosLayouts()

        when (apiSeleccionada) {

            "USGS" -> configurarUsgs()

            "NASA" -> configurarNasa()

            "GEOREF" -> configurarGeoref()

            "TCAMBIO" -> configurarTc()

            else -> {
                txtError.text = "API no reconocida."
            }
        }
    }

    private fun ocultarTodosLosLayouts() {

        layoutUsgs.visibility = View.GONE
        layoutNasa.visibility = View.GONE
        layoutGeoref.visibility = View.GONE
        layoutTc.visibility = View.GONE
    }

    private fun configurarUsgs() {

        layoutUsgs.visibility = View.VISIBLE

        edtFechaInicio =
            findViewById(R.id.edtFechaInicio)

        edtFechaFin =
            findViewById(R.id.edtFechaFin)

        edtMagnitud =
            findViewById(R.id.edtMagnitud)

        configurarSelectorFecha(edtFechaInicio)
        configurarSelectorFecha(edtFechaFin)

        btnConsultar.setOnClickListener {
            consultarUsgs()
        }
    }

    private fun configurarNasa() {

        layoutNasa.visibility = View.VISIBLE

        edtFechaNasa =
            findViewById(R.id.edtFechaNasa)

        configurarSelectorFecha(edtFechaNasa)

        btnConsultar.setOnClickListener {
            consultarNasa()
        }
    }

    private fun configurarGeoref() {

        layoutGeoref.visibility = View.VISIBLE

        edtLocalidad =
            findViewById(R.id.edtLocalidad)

        btnConsultar.setOnClickListener {
            consultarGeoref()
        }
    }

    private fun configurarTc() {

        layoutTc.visibility = View.VISIBLE

        edtTcInicio =
            findViewById(R.id.edtTcInicio)

        edtTcFin =
            findViewById(R.id.edtTcFin)

        edtMonto =
            findViewById(R.id.edtMonto)

        radioGroupDir =
            findViewById(R.id.radioGroupDir)

        configurarSelectorFecha(edtTcInicio)
        configurarSelectorFecha(edtTcFin)

        btnConsultar.setOnClickListener {
            consultarTc()
        }
    }

    private fun consultarUsgs() {

        val fechaInicio =
            edtFechaInicio?.text.toString().trim()

        val fechaFin =
            edtFechaFin?.text.toString().trim()

        val magnitudTexto =
            edtMagnitud?.text.toString().trim()

        val magnitud =
            magnitudTexto.toDoubleOrNull()

        if (fechaInicio.isEmpty() ||
            fechaFin.isEmpty() ||
            magnitud == null
        ) {

            txtError.text =
                "Completa correctamente los campos."

            return
        }

        if (!esFechaValida(fechaInicio) || !esFechaValida(fechaFin)) {

            txtError.text =
                "Usa el formato AAAA-MM-DD en las fechas."

            return
        }

        txtError.text = ""
        mostrarCarga(true)

        executor.execute {

            try {

                val uri =
                    "https://earthquake.usgs.gov/fdsnws/event/1/query".toUri()
                        .buildUpon()
                        .appendQueryParameter(
                            "format",
                            "geojson"
                        )
                        .appendQueryParameter(
                            "starttime",
                            fechaInicio
                        )
                        .appendQueryParameter(
                            "endtime",
                            fechaFin
                        )
                        .appendQueryParameter(
                            "minmagnitude",
                            magnitud.toString()
                        )
                        .appendQueryParameter(
                            "orderby",
                            "time"
                        )
                        .appendQueryParameter(
                            "limit",
                            "20"
                        )
                        .build()

                val respuesta =
                    ejecutarPeticion(uri.toString())

                val resultados =
                    procesarUsgs(respuesta)

                abrirResultados(resultados)

            } catch (e: Exception) {

                mostrarError(
                    "Error al consultar USGS:\n${e.message}"
                )
            }
        }
    }

    private fun procesarUsgs(json: String): String {

        val objeto = JSONObject(json)
        val features = objeto.getJSONArray("features")

        if (features.length() == 0) {
            return "No se encontraron terremotos."
        }

        val resultado = StringBuilder()

        resultado.append(
            "Resultados encontrados: ${features.length()}\n\n"
        )

        for (i in 0 until features.length()) {

            val feature =
                features.getJSONObject(i)

            val properties =
                feature.getJSONObject("properties")

            val geometry =
                feature.optJSONObject("geometry")

            val magnitud =
                if (properties.isNull("mag"))
                    "N/D"
                else
                    properties.getDouble("mag").toString()

            val lugar =
                properties.optString(
                    "place",
                    "N/D"
                )

            val timestamp =
                if (properties.isNull("time"))
                    null
                else
                    properties.getLong("time")

            val fecha =
                if (timestamp != null)
                    formatearFecha(timestamp)
                else
                    "N/D"

            val coordenadas =
                geometry?.optJSONArray("coordinates")

            val longitud =
                coordenadas?.optDouble(0, Double.NaN) ?: Double.NaN

            val latitud =
                coordenadas?.optDouble(1, Double.NaN) ?: Double.NaN

            val profundidad =
                coordenadas?.optDouble(2, Double.NaN) ?: Double.NaN

            resultado.append(
                "Terremoto #${i + 1}\n"
            )

            resultado.append(
                "Magnitud: $magnitud\n"
            )

            resultado.append(
                "Lugar: $lugar\n"
            )

            resultado.append(
                "Fecha: $fecha\n"
            )

            resultado.append(
                "Latitud: ${formatearCoordenada(latitud)}\n"
            )

            resultado.append(
                "Longitud: ${formatearCoordenada(longitud)}\n"
            )

            resultado.append(
                "Profundidad: ${formatearCoordenada(profundidad)} km\n\n"
            )
        }

        return resultado.toString()
    }

    private fun consultarNasa() {

        val fecha =
            edtFechaNasa?.text.toString().trim()

        if (fecha.isEmpty()) {

            txtError.text =
                "Introduce una fecha."

            return
        }

        if (!esFechaValida(fecha)) {

            txtError.text =
                "Usa el formato AAAA-MM-DD."

            return
        }

        txtError.text = ""
        mostrarCarga(true)

        executor.execute {

            try {

                val uri =
                    "https://api.nasa.gov/planetary/apod".toUri()
                        .buildUpon()
                        .appendQueryParameter(
                            "api_key",
                            "DEMO_KEY"
                        )
                        .appendQueryParameter(
                            "date",
                            fecha
                        )
                        .build()

                val respuesta =
                    ejecutarPeticion(uri.toString())

                val resultado =
                    procesarNasa(respuesta)

                abrirResultados(
                    resultado.first,
                    null,
                    resultado.second,
                    resultado.third
                )

            } catch (e: Exception) {

                mostrarError(
                    "Error al consultar NASA:\n${e.message}"
                )
            }
        }
    }

    private fun procesarNasa(json: String): Triple<String, String?, String> {

        val objeto =
            JSONObject(json)

        if (objeto.has("code") || objeto.has("error")) {
            val mensaje = objeto.optString("msg", objeto.optString("error", "Error desconocido de NASA."))
            throw Exception(mensaje)
        }

        val fecha =
            objeto.optString(
                "date",
                "N/D"
            )

        val titulo =
            objeto.optString(
                "title",
                "Sin título"
            )

        val explicacion =
            objeto.optString(
                "explanation",
                "Sin explicación."
            )

        val tipo =
            objeto.optString(
                "media_type",
                "N/D"
            )

        val url =
            objeto.optString(
                "url",
                "N/D"
            )

        val texto = """
            NASA - Astronomy Picture of the Day

            Fecha: $fecha

            Título: $titulo

            Tipo de contenido: $tipo

            Descripción:
            $explicacion

            URL:
            $url
        """.trimIndent()

        val imagen = url.takeIf {
            tipo == "image" && it.startsWith("http")
        }

        return Triple(texto, imagen, tipo)
    }

    private fun consultarGeoref() {

        val localidad =
            edtLocalidad?.text.toString().trim()

        if (localidad.isEmpty()) {

            txtError.text =
                "Introduce una localidad."

            return
        }

        txtError.text = ""
        mostrarCarga(true)

        executor.execute {

            try {

                val uri =
                    "https://apis.datos.gob.ar/georef/api/v2.0/localidades".toUri()
                        .buildUpon()
                        .appendQueryParameter(
                            "nombre",
                            localidad
                        )
                        .appendQueryParameter(
                            "max",
                            "10"
                        )
                        .appendQueryParameter(
                            "campos",
                            "id,nombre"
                        )
                        .build()

                val respuesta =
                    ejecutarPeticion(uri.toString())

                val resultado =
                    procesarGeoref(respuesta)

                abrirResultados(resultado)

            } catch (e: Exception) {

                mostrarError(
                    "Error al consultar Georef:\n${e.message}"
                )
            }
        }
    }

    private fun procesarGeoref(json: String): String {

        val objeto =
            JSONObject(json)

        val localidades =
            objeto.optJSONArray("localidades")
                ?: JSONArray()

        if (localidades.length() == 0) {

            return "No se encontraron localidades."
        }

        val resultado =
            StringBuilder()

        resultado.append(
            "Localidades encontradas: " +
                    localidades.length() +
                    "\n\n"
        )

        for (i in 0 until localidades.length()) {

            val localidad =
                localidades.optJSONObject(i)
                    ?: continue

            val nombre =
                localidad.optString(
                    "nombre",
                    "N/D"
                )

            val id =
                localidad.optString(
                    "id",
                    "N/D"
                )

            resultado.append(
                "Localidad #${i + 1}\n"
            )

            resultado.append(
                "Nombre: $nombre\n"
            )

            resultado.append(
                "ID: $id\n\n"
            )
        }

        return resultado.toString()
    }

    private fun consultarTc() {

        val inicioTexto =
            edtTcInicio?.text.toString().trim()

        val finTexto =
            edtTcFin?.text.toString().trim()

        val montoTexto =
            edtMonto?.text.toString().trim()

        if (!esFechaValida(inicioTexto) ||
            !esFechaValida(finTexto)
        ) {
            txtError.text =
                "Usa el formato AAAA-MM-DD en las fechas."
            return
        }

        val monto = montoTexto.toDoubleOrNull()

        if (monto == null || monto <= 0) {
            txtError.text =
                "Introduce un monto mayor que cero."
            return
        }

        val formatoDia =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                isLenient = false
            }

        val inicio: Date
        val fin: Date

        try {
            inicio = formatoDia.parse(inicioTexto)!!
            fin = formatoDia.parse(finTexto)!!
        } catch (e: Exception) {
            txtError.text = "Fechas no válidas."
            return
        }

        if (fin.before(inicio)) {
            txtError.text =
                "La fecha final debe ser posterior a la inicial."
            return
        }

        val dias =
            TimeUnit.MILLISECONDS.toDays(
                fin.time - inicio.time
            ).toInt()

        if (dias > 14) {
            txtError.text =
                "El rango máximo es de 15 días."
            return
        }

        val dolASol =
            radioGroupDir?.checkedRadioButtonId == R.id.rdbDolASol

        txtError.text = ""
        mostrarCarga(true)

        executor.execute {

            try {

                val diasLista = listarDias(inicio, fin, formatoDia)

                var filas: List<FilaTc>
                var fuente: String

                try {
                    filas = consultarTcBcrp(
                        inicio, fin, diasLista, monto, dolASol
                    )
                    fuente = "BCRPData oficial (SBS)"
                } catch (e: Exception) {
                    filas = emptyList()
                    fuente = ""
                }

                if (filas.none { it.tieneTc }) {
                    filas = consultarTcReferencial(
                        diasLista, monto, dolASol
                    )
                    fuente = "TC referencial internacional"
                }

                if (filas.none { it.tieneTc }) {
                    throw Exception(
                        "Sin cotización para esas fechas."
                    )
                }

                val textos = construirTextoTc(
                    monto, dolASol, inicioTexto,
                    finTexto, filas, fuente
                )

                abrirResultados(textos.first, textos.second)

            } catch (e: Exception) {

                mostrarError(
                    "Error al consultar tipo de cambio:\n${e.message}"
                )
            }
        }
    }

    private data class FilaTc(
        val fecha: String,
        val compra: Double?,
        val venta: Double?,
        val convertido: Double?,
        val moneda: String
    ) {
        val tieneTc: Boolean
            get() = compra != null && venta != null
    }

    private fun listarDias(
        inicio: Date,
        fin: Date,
        formato: SimpleDateFormat
    ): List<String> {

        val dias = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.time = inicio

        while (!cal.time.after(fin)) {
            dias.add(formato.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dias
    }

    private fun consultarTcBcrp(
        inicio: Date,
        fin: Date,
        dias: List<String>,
        monto: Double,
        dolASol: Boolean
    ): List<FilaTc> {

        val cal = Calendar.getInstance()

        cal.time = inicio
        val periodoIni =
            "${cal.get(Calendar.YEAR)}-" +
                    "${cal.get(Calendar.MONTH) + 1}"

        cal.time = fin
        var periodoFin =
            "${cal.get(Calendar.YEAR)}-" +
                    "${cal.get(Calendar.MONTH) + 1}"

        if (periodoFin == periodoIni) {
            cal.add(Calendar.MONTH, 1)
            periodoFin =
                "${cal.get(Calendar.YEAR)}-" +
                        "${cal.get(Calendar.MONTH) + 1}"
        }

        val url =
            "https://estadisticas.bcrp.gob.pe/estadisticas/series/api/" +
                    "PD04639PD-PD04640PD/json/" +
                    "$periodoIni/$periodoFin/esp"

        val respuesta = ejecutarPeticion(url)

        val objeto = JSONObject(respuesta)
        val periodos = objeto.optJSONArray("periods")
            ?: JSONArray()

        val mapa = mutableMapOf<String, Pair<Double, Double>>()

        for (i in 0 until periodos.length()) {
            val p = periodos.optJSONObject(i) ?: continue
            val nombre = p.optString("name", "")
            val valores = p.optJSONArray("values") ?: continue
            if (valores.length() < 2) continue

            val compra = valores.optString(0, "").toDoubleOrNull()
            val venta = valores.optString(1, "").toDoubleOrNull()
            if (compra == null || venta == null) continue

            val fecha = parsearFechaBcrp(nombre) ?: continue
            mapa[fecha] = compra to venta
        }

        val moneda = if (dolASol) "PEN" else "USD"

        return dias.map { fecha ->
            val tc = mapa[fecha]
            if (tc == null) {
                FilaTc(fecha, null, null, null, moneda)
            } else {
                val convertido = if (dolASol)
                    monto * tc.first
                else
                    monto / tc.second
                FilaTc(fecha, tc.first, tc.second, convertido, moneda)
            }
        }
    }

    private fun parsearFechaBcrp(
        nombre: String
    ): String? {

        val partes = nombre.split(".")
        if (partes.size != 3) return null

        val dia = partes[0].toIntOrNull() ?: return null
        val anio2 = partes[2].toIntOrNull() ?: return null

        val meses = mapOf(
            "ene" to 1, "feb" to 2, "mar" to 3,
            "abr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "ago" to 8, "set" to 9,
            "oct" to 10, "nov" to 11, "dic" to 12
        )

        val mes = meses[partes[1].lowercase(Locale.US)]
            ?: return null

        val anio = if (anio2 < 100) 2000 + anio2 else anio2

        return String.format(
            Locale.US, "%04d-%02d-%02d", anio, mes, dia
        )
    }

    private fun consultarTcReferencial(
        dias: List<String>,
        monto: Double,
        dolASol: Boolean
    ): List<FilaTc> {

        val moneda = if (dolASol) "PEN" else "USD"

        return dias.map { fecha ->
            try {
                val url =
                    "https://cdn.jsdelivr.net/npm/" +
                            "@fawazahmed0/currency-api@$fecha/" +
                            "v1/currencies/usd.json"

                val respuesta = ejecutarPeticion(url)
                val tasa = JSONObject(respuesta)
                    .optJSONObject("usd")
                    ?.optDouble("pen", Double.NaN)
                    ?: Double.NaN

                if (tasa.isNaN() || tasa <= 0) {
                    FilaTc(fecha, null, null, null, moneda)
                } else {
                    val convertido = if (dolASol)
                        monto * tasa
                    else
                        monto / tasa
                    FilaTc(fecha, tasa, tasa, convertido, moneda)
                }
            } catch (e: Exception) {
                FilaTc(fecha, null, null, null, moneda)
            }
        }
    }

    private fun construirTextoTc(
        monto: Double,
        dolASol: Boolean,
        inicio: String,
        fin: String,
        filas: List<FilaTc>,
        fuente: String
    ): Pair<String, String> {

        val origen = if (dolASol)
            "$monto USD → PEN (usa TC compra)"
        else
            "$monto PEN → USD (usa TC venta)"

        val txt = StringBuilder()
        txt.append("Tipo de Cambio S/ - $ (Perú)\n")
        txt.append("Fuente: $fuente\n\n")
        txt.append("Monto: $origen\n")
        txt.append("Días: $inicio al $fin\n")
        txt.append("s/d = sin cotización (finde/feriado)\n\n")

        val csv = StringBuilder()
        csv.append("fecha,compra,venta,convertido,moneda\n")

        for (f in filas) {
            if (f.tieneTc) {
                val compraTxt = String.format(
                    Locale.US, "%.4f", f.compra!!
                )
                val ventaTxt = String.format(
                    Locale.US, "%.4f", f.venta!!
                )
                val convTxt = String.format(
                    Locale.US, "%.2f", f.convertido!!
                )
                txt.append(
                    "${f.fecha}  C:$compraTxt  " +
                            "V:$ventaTxt  = $convTxt ${f.moneda}\n"
                )
                csv.append(
                    "${f.fecha},$compraTxt,$ventaTxt," +
                            "$convTxt,${f.moneda}\n"
                )
            } else {
                txt.append("${f.fecha}  s/d\n")
            }
        }

        return txt.toString() to csv.toString()
    }

    private fun ejecutarPeticion(
        urlString: String
    ): String {

        val url =
            URL(urlString)

        val connection =
            url.openConnection()
                    as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {

            val responseCode =
                connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {

                val detalle =
                    try {
                        (connection.errorStream
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?.take(300))
                            ?.let { " - $it" }
                            ?: ""
                    } catch (_: Exception) {
                        ""
                    }

                throw Exception(
                    "Código HTTP: $responseCode$detalle"
                )
            }

            return connection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        } finally {

            connection.disconnect()
        }
    }

    private fun abrirResultados(
        resultados: String,
        csv: String? = null,
        imagenUrl: String? = null,
        tipoMedio: String? = null
    ) {

        runOnUiThread {

            mostrarCarga(false)

            val intent =
                Intent(
                    this,
                    ResultadosActivity::class.java
                )

            intent.putExtra(
                "API_SELECCIONADA",
                apiSeleccionada
            )

            intent.putExtra(
                "RESULTADOS",
                resultados
            )

            if (!csv.isNullOrEmpty()) {
                intent.putExtra(
                    "RESULTADOS_CSV",
                    csv
                )
            }

            if (!imagenUrl.isNullOrEmpty()) {
                intent.putExtra(
                    "IMAGEN_URL",
                    imagenUrl
                )
            }

            if (!tipoMedio.isNullOrEmpty()) {
                intent.putExtra(
                    "TIPO_MEDIO",
                    tipoMedio
                )
            }

            startActivity(intent)
        }
    }

    private fun mostrarCarga(
        cargando: Boolean
    ) {

        if (android.os.Looper.myLooper() ==
            android.os.Looper.getMainLooper()
        ) {
            aplicarEstadoCarga(cargando)
        } else {
            runOnUiThread {
                aplicarEstadoCarga(cargando)
            }
        }
    }

    private fun aplicarEstadoCarga(
        cargando: Boolean
    ) {
        progressBar.visibility =
            if (cargando)
                View.VISIBLE
            else
                View.GONE

        btnConsultar.isEnabled =
            !cargando
    }

    private fun mostrarError(
        mensaje: String
    ) {

        runOnUiThread {

            aplicarEstadoCarga(false)

            txtError.text = mensaje
        }
    }

    private fun esFechaValida(
        fecha: String
    ): Boolean {
        return Regex("^\\d{4}-\\d{2}-\\d{2}$")
            .matches(fecha)
    }

    private fun configurarSelectorFecha(
        campo: EditText?
    ) {
        campo?.apply {
            isFocusable = false
            isClickable = true
            inputType = InputType.TYPE_NULL
            setOnClickListener {
                mostrarCalendario(this)
            }
        }
    }

    private fun mostrarCalendario(
        campo: EditText
    ) {
        val zonaUtc = TimeZone.getTimeZone("UTC")
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = zonaUtc
            isLenient = false
        }

        val actual = try {
            formato.parse(campo.text.toString().trim())?.time
        } catch (e: Exception) {
            null
        } ?: MaterialDatePicker.todayInUtcMilliseconds()

        val selector = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.titulo_calendario))
            .setSelection(actual)
            .build()

        selector.addOnPositiveButtonClickListener { millis ->
            campo.setText(formato.format(Date(millis)))
            txtError.text = ""
        }

        selector.show(supportFragmentManager, "selector_fecha")
    }

    private fun formatearCoordenada(
        valor: Double
    ): String {
        return if (valor.isNaN()) "N/D" else valor.toString()
    }

    private fun formatearFecha(
        timestamp: Long
    ): String {

        val formato =
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm 'UTC'",
                Locale.getDefault()
            )

        formato.timeZone =
            TimeZone.getTimeZone("UTC")

        return formato.format(
            Date(timestamp)
        )
    }

    override fun onDestroy() {

        super.onDestroy()

        executor.shutdown()
    }
}