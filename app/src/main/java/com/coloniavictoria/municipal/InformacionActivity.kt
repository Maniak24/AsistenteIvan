package com.coloniavictoria.municipal

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InformacionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = android.widget.ScrollView(this)
        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        fun titulo(texto: String) {
            val t = TextView(this).apply {
                text = texto
                textSize = 22f
                setTextColor(Color.rgb(20, 20, 20))
                setPadding(0, 0, 0, 20)
            }
            contenedor.addView(t)
        }

        fun bloque(titulo: String, contenido: String) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 22, 24, 22)
                setBackgroundColor(Color.rgb(245, 245, 245))
            }

            val tvTitulo = TextView(this).apply {
                text = titulo
                textSize = 18f
                setTextColor(Color.rgb(25, 25, 25))
                setPadding(0, 0, 0, 10)
            }

            val tvContenido = TextView(this).apply {
                text = contenido
                textSize = 15f
                setTextColor(Color.rgb(55, 55, 55))
                setLineSpacing(0f, 1.15f)
            }

            card.addView(tvTitulo)
            card.addView(tvContenido)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 18)
            contenedor.addView(card, params)
        }

        titulo("Información de Colonia Victoria")

        bloque(
            "🏛 Municipalidad",
            "Municipalidad de Colonia Victoria\n" +
            "Dirección publicada: Av. Misiones 100\n" +
            "Atención: lunes a viernes de 07:00 a 13:00\n" +
            "Teléfonos publicados en fuentes oficiales: (03757) 480775 / 480222 / 480663"
        )

        bloque(
            "📅 Historia",
            "Colonia Victoria conmemora su fundación el 13 de junio de 1939.\n\n" +
            "En 2026 celebró su 87.º aniversario. La Provincia de Misiones informó que durante los actos se inauguró la pavimentación de la avenida Malvinas Argentinas."
        )

        bloque(
            "📄 Registro Civil / Personas",
            "Existe un CDR (Centro de Documentación Rápida) de Colonia Victoria.\n\n" +
            "Atención general publicada por el Registro Civil de Misiones: lunes a viernes de 06:30 a 12:30.\n\n" +
            "Antes de concurrir, se recomienda verificar el trámite y horario actualizado."
        )

        bloque(
            "🏫 Educación",
            "Colonia Victoria cuenta con instituciones educativas de distintos niveles.\n\n" +
            "La documentación oficial provincial registra al IEA N.º 05 en Ruta Nacional 12, Km. 1558, Paraje Parejha.\n\n" +
            "También existen núcleos educativos del SIPTED que brindan posibilidades de finalización de estudios para jóvenes y adultos."
        )

        bloque(
            "🚓 Seguridad",
            "Colonia Victoria forma parte de la jurisdicción de la Unidad Regional III de la Policía de Misiones.\n\n" +
            "Emergencias: 911."
        )

        bloque(
            "🏥 Salud",
            "CAPS Victoria Centro\n" +
            "Atención: lunes a viernes de 06:00 a 12:00 y de 13:00 a 18:00.\n\n" +
            "Colonia Victoria integra el Área Programática XIV de Salud Pública, cuya cabecera sanitaria corresponde a la zona de Eldorado.\n\n" +
            "Ante una emergencia médica, comunicarse al 107 o dirigirse al servicio de salud más cercano."
        )

        bloque(
            "🚑 Emergencias",
            "Policía: 911\n" +
            "Bomberos: 100\n" +
            "Emergencias médicas: 107\n\n" +
            "El 911 corresponde al Centro Integral de Operaciones de la Policía de Misiones."
        )

        bloque(
            "📍 Ubicación",
            "Colonia Victoria se encuentra en el departamento Eldorado, provincia de Misiones.\n\n" +
            "Su principal vía de comunicación es la Ruta Nacional 12."
        )

        bloque(
            "📞 Teléfonos útiles",
            "Municipalidad de Colonia Victoria: 376-4447531 / 376-4447527\n\n" +
            "Policía - emergencias: 911\n" +
            "Bomberos: 100\n" +
            "Emergencias médicas: 107\n\n" +
            "Para trámites y números específicos, verificar siempre la información oficial antes de concurrir."
        )

        bloque(
            "ℹ️ Fuentes",
            "Información recopilada de organismos oficiales de la Provincia de Misiones, Registro Civil de Misiones y documentación electoral provincial.\n\n" +
            "Última revisión: septiembre de 2026."
        )

        scroll.addView(contenedor)
        setContentView(scroll)
    }
}
