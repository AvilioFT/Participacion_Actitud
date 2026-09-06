package com.example.entregable01actitud

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import java.net.URL
import java.util.concurrent.Executors

class ResultadosActivity : AppCompatActivity() {

    private lateinit var apiSeleccionada: String
    private lateinit var resultados: String
    private var resultadosCsv: String? = null
    private var imagenUrl: String? = null
    private var tipoMedio: String? = null

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultados)

        apiSeleccionada =
            intent.getStringExtra("API_SELECCIONADA") ?: "API"

        resultados =
            intent.getStringExtra("RESULTADOS") ?: "Sin resultados."

        resultadosCsv =
            intent.getStringExtra("RESULTADOS_CSV")

        imagenUrl =
            intent.getStringExtra("IMAGEN_URL")

        tipoMedio =
            intent.getStringExtra("TIPO_MEDIO")

        val txtTituloResultados =
            findViewById<TextView>(R.id.txtTituloResultados)

        val txtResultados =
            findViewById<TextView>(R.id.txtResultados)

        val btnGenerarTexto =
            findViewById<Button>(R.id.btnGenerarTexto)

        val cardImagen =
            findViewById<View>(R.id.cardImagen)

        val imgNasa =
            findViewById<ImageView>(R.id.imgNasa)

        val progressImagen =
            findViewById<ProgressBar>(R.id.progressImagen)

        val btnAbrirVideo =
            findViewById<Button>(R.id.btnAbrirVideo)

        txtTituloResultados.text = apiSeleccionada
        txtResultados.text = resultados

        if (!imagenUrl.isNullOrEmpty()) {
            cardImagen.visibility = View.VISIBLE
            progressImagen.visibility = View.VISIBLE
            cargarImagen(imagenUrl!!, imgNasa, progressImagen)
        } else {
            cardImagen.visibility = View.GONE
        }

        if (tipoMedio == "video") {
            val urlVideo = extraerUrlVideo(resultados)
            if (urlVideo != null) {
                btnAbrirVideo.visibility = View.VISIBLE
                btnAbrirVideo.setOnClickListener {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            urlVideo.toUri()
                        )
                    )
                }
            } else {
                btnAbrirVideo.visibility = View.GONE
            }
        } else {
            btnAbrirVideo.visibility = View.GONE
        }

        btnGenerarTexto.setOnClickListener {

            val intent =
                Intent(this, TextosActivity::class.java)

            intent.putExtra(
                "API_SELECCIONADA",
                apiSeleccionada
            )

            intent.putExtra(
                "TEXTO_BASE",
                resultados
            )

            if (!resultadosCsv.isNullOrEmpty()) {
                intent.putExtra(
                    "TEXTO_CSV",
                    resultadosCsv
                )
            }

            if (!imagenUrl.isNullOrEmpty()) {
                intent.putExtra(
                    "TEXTO_IMAGEN",
                    imagenUrl
                )
            }

            startActivity(intent)
        }
    }

    private fun cargarImagen(
        url: String,
        vista: ImageView,
        progreso: ProgressBar
    ) {
        executor.execute {
            try {
                val conexion = URL(url).openConnection()
                conexion.connectTimeout = 15_000
                conexion.readTimeout = 15_000
                val bitmap = conexion.getInputStream().use {
                    BitmapFactory.decodeStream(it)
                }
                runOnUiThread {
                    progreso.visibility = View.GONE
                    if (bitmap != null) {
                        vista.setImageBitmap(bitmap)
                    } else {
                        vista.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progreso.visibility = View.GONE
                    vista.visibility = View.GONE
                }
            }
        }
    }

    private fun extraerUrlVideo(texto: String): String? {
        val linea = texto.split("\n")
            .firstOrNull { it.trim().startsWith("http") }
            ?.trim()
        return linea?.takeIf { it.startsWith("http") }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
