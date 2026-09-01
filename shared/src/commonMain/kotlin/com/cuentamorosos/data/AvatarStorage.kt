package com.cuentamorosos.data

/**
 * Convenciones de almacenamiento de fotos de perfil, compartidas por todos los
 * hosts.
 *
 * La subida en sí **no** puede vivir aquí: `dev.gitlive.firebase.storage.Data`
 * es una `expect class` distinta en cada plataforma (`ByteArray` en Android,
 * `NSData` en iOS) y no se puede construir desde commonMain. Cada host sube el
 * archivo con su propio SDK.
 *
 * Lo que sí tiene que coincidir es *dónde* y *cómo*: si iOS escribiera en otra
 * ruta o con otro tamaño, los avatares subidos desde un móvil no se verían en el
 * otro. Esas decisiones viven aquí y son las únicas que ambos deben respetar.
 */
object AvatarStorage {

    /** Lado del cuadrado, en píxeles, al que se escala la foto antes de subirla. */
    const val TARGET_SIZE_PX = 256

    /** Calidad JPEG (0-100) usada al comprimir. */
    const val JPEG_QUALITY = 85

    /** Tipo MIME con el que se etiqueta el objeto en Storage. */
    const val CONTENT_TYPE = "image/jpeg"

    /** Ruta del avatar de un usuario dentro del bucket. */
    fun pathFor(uid: String): String = "avatars/$uid/profile.jpg"
}
