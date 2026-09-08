package com.cuentamorosos.data.repository

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

/**
 * Instancias únicas de Firestore y Auth.
 *
 * Cada repositorio hacía su propio `Firebase.firestore`, cinco en total. En iOS
 * eso hace crashear la app al arrancar:
 *
 *     FIRIllegalStateException: Firestore instance has already been started and
 *     its settings can no longer be changed.
 *
 * gitlive aplica settings al envolver la instancia nativa, y a partir del
 * segundo envoltorio Firestore ya ha arrancado y el SDK de iOS rechaza el
 * cambio. Android lo tolera, así que el fallo solo se manifestaba en iOS.
 *
 * Compartir una sola instancia es además lo correcto de por sí: los cinco
 * envoltorios apuntaban al mismo singleton nativo.
 */
internal object FirebaseClients {
    val firestore: FirebaseFirestore by lazy { Firebase.firestore }
    val auth: FirebaseAuth by lazy { Firebase.auth }
}
