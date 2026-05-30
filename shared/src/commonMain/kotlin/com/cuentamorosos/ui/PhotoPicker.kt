package com.cuentamorosos.ui

/**
 * Callback invoked when a photo has been selected, uploaded to Firebase Storage,
 * and a download URL is available.
 *
 * This bridges the platform-specific photo picker (Android ActivityResultContracts)
 * with the common ViewModel layer. The Android side handles:
 * 1. Opening the system photo picker
 * 2. Uploading the selected image to Firebase Storage
 * 3. Obtaining the download URL
 * 4. Calling this callback with the URL
 *
 * The [downloadUrl] is then passed to [AccountViewModel.updatePhoto] to persist
 * the photo URL in Firestore and local cache.
 */
typealias OnPhotoReady = (downloadUrl: String) -> Unit
