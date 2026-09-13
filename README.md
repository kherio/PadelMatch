# PadelMatch

App Android nativa (Kotlin + Jetpack Compose) para organizar torneos de pádel
Americano y Mexicano, inspirada en PadelMix pero construida desde cero.

## Funciones incluidas

- Crear torneo: nombre, formato (Americano / Mexicano), número de pistas.
- Añadir jugadores (mínimo 4).
- Generación automática de rondas:
  - **Americano**: parejas rotativas evitando repetir compañero cuando es posible.
  - **Mexicano**: parejas según clasificación (1º+4º vs 2º+3º dentro de cada grupo de 4).
  - Rotación justa de quién descansa si el número de jugadores no es múltiplo de 4
    o excede el aforo de las pistas.
- Marcador por partido, clasificación general en vivo dentro de la app.
- Guardar y continuar torneos (persistencia local con Room — se autoguarda
  en cada cambio, no hace falta pulsar "guardar").
- Compartir: envía la clasificación actual por WhatsApp/lo que elijas mediante
  el selector nativo de Android.

## Cómo compilar el APK

1. Abre la carpeta `PadelMatch/` con Android Studio (Koala o superior).
2. Deja que Gradle sincronice (descargará las dependencias la primera vez).
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`, o ejecuta directamente
   en un dispositivo/emulador con el botón ▶.
4. El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

No he podido compilar ni verificar este proyecto en este entorno (no tengo
el SDK de Android ni acceso a los repositorios de Google), así que es
posible que al sincronizar Gradle aparezca algún error puntual de versión
de plugin o dependencia. Si te pasa, pégame el mensaje de error exacto y
lo arreglo.

## Sobre el "marcador en vivo compartible"

PadelMix comparte un enlace web que se actualiza en tiempo real gracias a
su propio servidor en la nube. Sin desplegarte un backend, esta versión
comparte la clasificación como texto a través del selector nativo de
Android (WhatsApp, Telegram, SMS, etc.), que se regenera con los datos
actuales cada vez que lo pulsas.

Si más adelante quieres el enlace web en vivo de verdad, se puede añadir
integrando Firebase Firestore (necesitarías crear tú una cuenta de Firebase
gratuita) más una pequeña página web que lea esos datos — puedo ayudarte
a montarlo cuando quieras.

## Icono de la app

El icono es el logo de Artaza Torresolo Pádel Club que subiste, generado en
todas las resoluciones mipmap necesarias (`app/src/main/res/mipmap-*/`).

## Sobre el Gradle Wrapper

He fijado la versión de Gradle en `gradle/wrapper/gradle-wrapper.properties`
(8.9, compatible con AGP 8.6.0, que exige mínimo la 8.7), pero **no incluyo
el binario `gradle-wrapper.jar`** porque este entorno no tiene acceso a los
servidores de Gradle para descargarlo. Sin ese jar, `gradlew`/`gradlew.bat`
no funcionarán desde la terminal.

La forma más rápida de arreglarlo: abre una terminal dentro de Android
Studio (pestaña "Terminal" abajo) y ejecuta:

```
gradle wrapper --gradle-version 8.9
```

Esto usa el Gradle que ya trae Android Studio instalado para generar el
`gradle-wrapper.jar` y los scripts que faltan. Alternativamente, Android
Studio suele detectar el wrapper incompleto al abrir el proyecto y ofrecer
un botón para arreglarlo automáticamente.

## Estructura

```
app/src/main/java/com/kherio/padelmatch/
├── MainActivity.kt          # Navegación entre pantallas
├── data/
│   ├── Models.kt            # Player, Tournament, Match, Round...
│   ├── PairingEngine.kt     # Algoritmos Americano / Mexicano
│   └── TournamentDao.kt     # Room (persistencia local)
│   └── TournamentRepository.kt
└── ui/
    ├── screens/              # Home, CreateTournament, Players, Tournament
    └── theme/                # Tema Material3
```
