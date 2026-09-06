# Participación y Actitud — API Explorer

App Android (nota de participación y actitud) que consulta APIs públicas, guarda cada consulta en un historial local
(`SharedPreferences`) y permite exportar los resultados en varios formatos de documento.

## Funcionalidades

1. **Consulta de APIs públicas**
   - **USGS Earthquakes**: terremotos por rango de fechas y magnitud mínima.
   - **NASA APOD**: foto astronómica del día por fecha.
   - **Georef (Argentina)**: búsqueda de localidades.
   - **Tipo de Cambio S/ $ (Perú)**: conversión soles ↔ dólares por rango de días
     (máx. 15). Fuente primaria: **BCRP oficial** (series diarias SBS
     `PD04639PD` compra / `PD04640PD` venta); si el BCRP no devuelve datos
     (protección anti-bots), usa automáticamente un TC referencial internacional
     por día. La fuente siempre se indica en el resultado.
2. **Historial de consultas** (`SharedPreferences`): cada resultado se guarda con
   API, fecha y contenido; se puede ver, eliminar y re-descargar.
3. **Exportación multi-formato**: TXT, CSV, JSON, XML, HTML, Markdown, Word
   (`.docx`) y Excel (`.xlsx`) mediante el selector de formato. Los `.docx` y
   `.xlsx` generados son archivos Office válidos (sin librerías externas).

## Diseño

Tema Material 3 con paleta **Gruvbox** (modo claro y oscuro según el sistema),
tarjetas, campos `TextInputLayout` y chips de selección de formato.

## Cómo ejecutarlo

1. Abrir la carpeta en **Android Studio** (Ladybug o superior).
2. Dejar que Gradle sincronice el proyecto (se requiere conexión a internet).
3. Ejecutar en un emulador o dispositivo físico (`Run > Run 'app'`).
   - `compileSdk 37`, `minSdk 24`.

## Estructura

```
app/src/main/java/com/example/entregable01actitud/
├── MainActivity.kt        # Menú de APIs
├── ConsultaActivity.kt    # Formularios + consumo de APIs (BCRP, USGS, NASA, Georef)
├── ResultadosActivity.kt  # Muestra el resultado
├── TextosActivity.kt      # Historial (SharedPreferences) + descarga multi-formato
└── FormatoDocumento.kt    # Generadores TXT/CSV/JSON/XML/HTML/MD/DOCX/XLSX
```

## Notas

- La API de NASA usa `DEMO_KEY` (límite de cortesía; ante error 429 reintentar más tarde).
- El BCRP puede bloquear clientes automatizados; la app lo detecta y aplica el
  fallback referencial, indicando la fuente en el documento generado.
