# ============================================
# REGLAS GENERALES DE OFUSCACIÓN
# ============================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ============================================
# FIREBASE - Necesario para que funcione
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ============================================
# MODELOS DE DATOS - Necesario para Firebase
# ============================================
-keep class com.example.Cortex_LaSecuencia.RegistroData { *; }
-keep class com.example.Cortex_LaSecuencia.Operador { *; }
-keep class com.example.Cortex_LaSecuencia.SolicitudDesbloqueo { *; }

# ============================================
# TENSORFLOW LITE
# ============================================
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

# ============================================
# ML KIT
# ============================================
-keep class com.google.mlkit.** { *; }

# ============================================
# CAMERAХ
# ============================================
-keep class androidx.camera.** { *; }

# ============================================
# EXCEL / PDF (Apache POI + iText)
# ============================================
-keep class org.apache.poi.** { *; }
-keep class com.itextpdf.** { *; }

# ============================================
# KOTLIN
# ============================================
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# ============================================
# ACTIVIDADES Y VISTAS
# ============================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# ============================================
# EVITAR INGENIERÍA INVERSA
# ============================================
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-repackageclasses 'cortex'