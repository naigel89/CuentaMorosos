package com.cuentamorosos.data.repository

import dev.gitlive.firebase.firestore.DocumentSnapshot

/**
 * Returns the raw document data as a [Map] without kotlinx.serialization.
 *
 * This bypasses the [DocumentSnapshot.data] inline function which uses
 * `kotlinx.serialization` to decode/encode the document — that fails with
 * `Map<String, Any?>` because `Any?` has no serializer.
 *
 * Instead, this calls the internal [DocumentSnapshot.encodedData] method
 * which returns the raw data from the native Firebase SDK.
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun DocumentSnapshot.getRawData(): Map<String, Any?>? =
    this.encodedData() as? Map<String, Any?>
