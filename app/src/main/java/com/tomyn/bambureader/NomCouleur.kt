package com.tomyn.bambureader

import kotlin.math.sqrt

object NomCouleur {

    private val couleurs = listOf(
        Triple("Blanc", 255, 255, 255),
        Triple("Noir", 0, 0, 0),
        Triple("Gris", 128, 128, 128),
        Triple("Gris clair", 200, 200, 200),
        Triple("Gris fonce", 64, 64, 64),
        Triple("Rouge", 220, 20, 20),
        Triple("Rouge fonce", 139, 0, 0),
        Triple("Rose", 255, 105, 180),
        Triple("Rose pale", 255, 182, 193),
        Triple("Orange", 255, 140, 0),
        Triple("Jaune", 255, 220, 0),
        Triple("Jaune pale", 255, 255, 150),
        Triple("Vert", 34, 139, 34),
        Triple("Vert clair", 144, 238, 144),
        Triple("Vert fonce", 0, 100, 0),
        Triple("Vert olive", 128, 128, 0),
        Triple("Cyan / turquoise", 0, 200, 200),
        Triple("Bleu", 30, 60, 200),
        Triple("Bleu clair", 135, 206, 235),
        Triple("Bleu marine", 0, 0, 128),
        Triple("Violet", 138, 43, 226),
        Triple("Mauve", 200, 150, 220),
        Triple("Marron", 139, 69, 19),
        Triple("Beige", 222, 184, 135),
        Triple("Or / dore", 212, 175, 55),
        Triple("Argent / gris metal", 192, 192, 192),
        Triple("Bronze / cuivre", 184, 115, 51),
        Triple("Transparent / naturel", 240, 240, 235)
    )

    fun trouverNomProche(r: Int, g: Int, b: Int): String {
        var meilleurNom = "Inconnu"
        var meilleureDistance = Double.MAX_VALUE
        for ((nom, cr, cg, cb) in couleurs) {
            val distance = sqrt(
                ((r - cr) * (r - cr) +
                 (g - cg) * (g - cg) +
                 (b - cb) * (b - cb)).toDouble()
            )
            if (distance < meilleureDistance) {
                meilleureDistance = distance
                meilleurNom = nom
            }
        }
        return meilleurNom
    }
}
