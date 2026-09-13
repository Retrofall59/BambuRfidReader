package com.tomyn.bambureader

import kotlin.math.sqrt

/**
 * Deux niveaux de recherche du nom de couleur :
 * 1. Table officielle Bambu (PLA Basic) : correspondance EXACTE sur le code hexadecimal
 * 2. Si pas de correspondance exacte : couleur usuelle la plus proche, via la formule
 *    "redmean" (ponderee par canal, plus proche de la perception humaine qu'une simple
 *    distance euclidienne RVB brute - notamment pour bien distinguer marrons/gris fonces)
 */
object NomCouleur {

    private data class CouleurNommee(val nom: String, val r: Int, val g: Int, val b: Int)

    private val tableOfficielle = mapOf(
        "FFFFFF" to "Jade White (PLA Basic) / Ivory White (PLA Matte) / White (ABS) / Frozen (TPU 90A, bicolore) - Bambu, officiel",
        "F7E6DE" to "Beige (Bambu, officiel)",
        "D1D3D5" to "Light Gray (Bambu, officiel)",
        "A6A9AA" to "Silver (Bambu, officiel)",
        "8E9089" to "Gray (Bambu, officiel)",
        "EC008C" to "Magenta (Bambu, officiel)",
        "F55A74" to "Pink (Bambu, officiel)",
        "F5547C" to "Hot Pink (Bambu, officiel)",
        "FF6A13" to "Orange (PLA Basic / ABS) - Bambu, officiel",
        "FF9016" to "Pumpkin Orange (Bambu, officiel)",
        "E4BD68" to "Gold (Bambu, officiel)",
        "FEC600" to "Sunflower Yellow (Bambu, officiel)",
        "F4EE2A" to "Yellow (Bambu, officiel)",
        "BECF00" to "Bright Green (Bambu, officiel)",
        "00AE42" to "Bambu Green (PLA Basic / ABS) - Bambu, officiel",
        "3F8E43" to "Mistletoe Green (Bambu, officiel)",
        "847D48" to "Bronze (Bambu, officiel)",
        "6F5034" to "Cocoa Brown (Bambu, officiel)",
        "9D432C" to "Brown (Bambu, officiel)",
        "9D2235" to "Maroon Red (Bambu, officiel)",
        "C12E1F" to "Red (Bambu, officiel)",
        "00B1B7" to "Turquoise (Bambu, officiel)",
        "0086D6" to "Cyan (Bambu, officiel)",
        "0A2989" to "Blue (Bambu, officiel)",
        "0056B8" to "Cobalt Blue (Bambu, officiel)",
        "5E43B7" to "Purple (Bambu, officiel)",
        "482960" to "Indigo Purple (Bambu, officiel)",
        "5B6579" to "Blue Grey (Bambu, officiel)",
        "545454" to "Dark Gray (Bambu, officiel)",
        "000000" to "Black (PLA Basic / ABS / TPU 90A / PETG-CF) / Charcoal (PLA Matte) - Bambu, officiel",
        // --- Gamme PLA Matte (tableau officiel complet, verifie via le PDF Bambu) ---
        "CBC6B8" to "Bone White (Bambu PLA Matte, officiel)",
        "E8DBB7" to "Desert Tan (Bambu PLA Matte, officiel)",
        "D3B7A7" to "Latte Brown (Bambu PLA Matte, officiel)",
        "AE835B" to "Caramel (Bambu PLA Matte, officiel)",
        "B15533" to "Terracotta (Bambu PLA Matte, officiel)",
        "7D6556" to "Dark Brown (Bambu PLA Matte, officiel)",
        "4D3324" to "Dark Chocolate (Bambu PLA Matte, officiel)",
        "AE96D4" to "Lilac Purple (Bambu PLA Matte, officiel)",
        "E8AFCF" to "Sakura Pink (Bambu PLA Matte, officiel)",
        "F99963" to "Mandarin Orange (Bambu PLA Matte, officiel)",
        "F7D959" to "Lemon Yellow (Bambu PLA Matte, officiel)",
        "950051" to "Plum (Bambu PLA Matte, officiel)",
        "DE4343" to "Scarlet Red (Bambu PLA Matte, officiel)",
        "BB3D43" to "Dark Red (Bambu PLA Matte, officiel)",
        "68724D" to "Dark Green (Bambu PLA Matte, officiel)",
        "61C680" to "Grass Green (Bambu PLA Matte, officiel)",
        "C2E189" to "Apple Green (Bambu PLA Matte, officiel)",
        "A3D8E1" to "Ice Blue (Bambu PLA Matte, officiel)",
        "56B7E6" to "Sky Blue (Bambu PLA Matte, officiel)",
        "0078BF" to "Marine Blue (Bambu PLA Matte, officiel)",
        "042F56" to "Dark Blue (Bambu PLA Matte, officiel)",
        "9B9EA0" to "Ash Gray (Bambu PLA Matte, officiel)",
        "757575" to "Nardo Gray (Bambu PLA Matte, officiel)",
        // --- Gamme ABS (tableau officiel complet, verifie via le PDF Bambu) ---
        "789D4A" to "Olive (Bambu ABS, officiel)",
        "489FDF" to "Azure (Bambu ABS, officiel)",
        "0C2340" to "Navy Blue (Bambu ABS, officiel)",
        "0A2CA5" to "Blue (Bambu ABS, officiel)",
        "FFC72C" to "Tangerine Yellow (Bambu ABS, officiel)",
        "D32941" to "Red (Bambu ABS, officiel)",
        "AF1685" to "Purple (Bambu ABS, officiel)",
        "87909A" to "Silver (Bambu ABS, officiel)",
        // --- Gamme TPU 90A (tableau officiel complet, verifie via le PDF Bambu) ---
        "FFFFEE" to "White (Bambu TPU 90A, officiel)",
        "D6ABFF" to "Grape Jelly (Bambu TPU 90A, officiel)",
        "7EB4E1" to "Crystal Blue (Bambu TPU 90A, officiel)",
        "5C4738" to "Cocoa Brown (Bambu TPU 90A, officiel)",
        "9EA2A2" to "Quicksilver (Bambu TPU 90A, officiel)",
        // Blaze et Frozen sont des filaments bicolores (2 codes hex chacun sur le PDF officiel)
        "F1AAA8" to "Blaze, bicolore (Bambu TPU 90A, officiel)",
        "D21B3C" to "Blaze, bicolore (Bambu TPU 90A, officiel)",
        "40B6E4" to "Frozen, bicolore (Bambu TPU 90A, officiel)",
        // --- Gamme PETG-CF (tableau officiel complet, verifie via le PDF Bambu) ---
        "9F332A" to "Brick Red (Bambu PETG-CF, officiel)",
        "583061" to "Violet Purple (Bambu PETG-CF, officiel)",
        "324585" to "Indigo Blue (Bambu PETG-CF, officiel)",
        "16B08E" to "Malachite Green (Bambu PETG-CF, officiel)",
        "565656" to "Titan Gray (Bambu PETG-CF, officiel)"
    )

    // Liste elargie, avec beaucoup plus de nuances marron/brun/terre pour eviter les
    // confusions avec les gris (probleme signale : marron fonce identifie comme gris)
    private val couleursApprox = listOf(
        CouleurNommee("Blanc", 255, 255, 255),
        CouleurNommee("Noir", 0, 0, 0),
        CouleurNommee("Gris", 128, 128, 128),
        CouleurNommee("Gris clair", 200, 200, 200),
        CouleurNommee("Gris fonce", 64, 64, 64),
        CouleurNommee("Gris anthracite", 45, 45, 48),
        CouleurNommee("Gris Nardo", 117, 117, 117),
        CouleurNommee("Gris cendre", 155, 158, 160),
        CouleurNommee("Rouge", 220, 20, 20),
        CouleurNommee("Rouge fonce", 139, 0, 0),
        CouleurNommee("Rose", 255, 105, 180),
        CouleurNommee("Rose pale", 255, 182, 193),
        CouleurNommee("Orange", 255, 140, 0),
        CouleurNommee("Jaune", 255, 220, 0),
        CouleurNommee("Jaune pale", 255, 255, 150),
        CouleurNommee("Vert", 34, 139, 34),
        CouleurNommee("Vert clair", 144, 238, 144),
        CouleurNommee("Vert fonce", 0, 100, 0),
        CouleurNommee("Vert olive", 128, 128, 0),
        CouleurNommee("Cyan / turquoise", 0, 200, 200),
        CouleurNommee("Bleu", 30, 60, 200),
        CouleurNommee("Bleu clair", 135, 206, 235),
        CouleurNommee("Bleu marine", 0, 0, 128),
        CouleurNommee("Violet", 138, 43, 226),
        CouleurNommee("Mauve", 200, 150, 220),
        // --- Nuances marron/brun/terre, elargies ---
        CouleurNommee("Marron", 139, 69, 19),
        CouleurNommee("Marron fonce", 92, 51, 23),
        CouleurNommee("Marron tres fonce", 61, 38, 20),
        CouleurNommee("Chocolat", 79, 46, 26),
        CouleurNommee("Cafe", 111, 78, 55),
        CouleurNommee("Terre de Sienne", 130, 78, 44),
        CouleurNommee("Noisette", 149, 105, 68),
        CouleurNommee("Chataigne", 100, 60, 40),
        CouleurNommee("Acajou", 128, 63, 45),
        CouleurNommee("Taupe", 105, 90, 80),
        CouleurNommee("Kaki / bronze fonce", 96, 84, 56),
        CouleurNommee("Beige", 222, 184, 135),
        CouleurNommee("Beige fonce", 180, 150, 110),
        CouleurNommee("Or / dore", 212, 175, 55),
        CouleurNommee("Argent / gris metal", 192, 192, 192),
        CouleurNommee("Bronze / cuivre", 184, 115, 51),
        CouleurNommee("Transparent / naturel", 240, 240, 235)
    )

    data class ResultatCouleur(val nom: String, val estExact: Boolean)

    /**
     * Distance "redmean" (approximation perceptuelle simple et connue, meilleure que la
     * distance euclidienne RVB brute) : ponderee selon la moyenne des rouges des 2 couleurs.
     */
    private fun distancePerceptuelle(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Double {
        val rMoyen = (r1 + r2) / 2.0
        val dr = (r1 - r2).toDouble()
        val dg = (g1 - g2).toDouble()
        val db = (b1 - b2).toDouble()
        val poidsR = 2.0 + rMoyen / 256.0
        val poidsG = 4.0
        val poidsB = 2.0 + (255.0 - rMoyen) / 256.0
        return sqrt(poidsR * dr * dr + poidsG * dg * dg + poidsB * db * db)
    }

    /**
     * @param hexRGB les 6 caracteres hexadecimaux R,G,B (sans le # ni le canal alpha)
     */
    fun trouverNom(hexRGB: String): ResultatCouleur {
        val hexNormalise = hexRGB.uppercase()
        tableOfficielle[hexNormalise]?.let {
            return ResultatCouleur(it, true)
        }

        val r = hexNormalise.substring(0, 2).toInt(16)
        val g = hexNormalise.substring(2, 4).toInt(16)
        val b = hexNormalise.substring(4, 6).toInt(16)

        var meilleurNom = "Inconnu"
        var meilleureDistance = Double.MAX_VALUE
        for (c in couleursApprox) {
            val distance = distancePerceptuelle(r, g, b, c.r, c.g, c.b)
            if (distance < meilleureDistance) {
                meilleureDistance = distance
                meilleurNom = c.nom
            }
        }
        return ResultatCouleur(meilleurNom, false)
    }
}
