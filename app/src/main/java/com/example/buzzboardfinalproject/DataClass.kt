package com.example.buzzboardfinalproject

class DataClass {

    /**
     * Firebase-friendly data class representing a post with an image and caption.
     *
     * @property imageURL URL or Base64 string of the image
     * @property caption Caption or description for the image
     */
    data class DataClass(
        var imageURL: String? = null, // Nullable for Firebase deserialization
        var caption: String? = null   // Nullable for Firebase deserialization
    ) {
        // No-argument constructor needed for Firebase
        constructor() : this(null, null)
    }



}