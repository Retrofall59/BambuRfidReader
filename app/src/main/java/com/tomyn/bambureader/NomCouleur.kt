package com.tomyn.bambureader

import kotlin.math.sqrt

/**
 * Deux niveaux de recherche du nom de couleur :
 * 1. Table officielle Bambu (PLA Basic) : correspondance EXACTE sur le code hexadecimal
 * 2. Si pas de correspondance exacte : couleur usuelle la plus proche par distance RGB
 */
object NomCouleur {

    private data class CouleurNommee(val nom: String, val r: Int, val g: Int, val b: Int)

    // Table officielle Bambu Lab (PLA Basic), source : PDF Bambu_PLA_Basic_Hex_Code.pdf
    private val tableOfficielle = mapOf(
        "FFFFFF" to "Jade White (Bambu, officiel)",
        "F7E6DE" to "Beige (Bambu, officiel)",
        "D1D3D5" to "Light Gray (Bambu, officiel)",
        "A6A9AA" to "Silver (Bambu, officiel)",
        "8E9089" to "Gray (Bambu, officiel)",
        "EC008C" to "Magenta (Bambu, officiel)",
        "F55A74" to "Pink (Bambu, officiel)",
        "F5547C" to "Hot Pink (Bambu, officiel)",
        "FF6A13" to "Orange (Bambu, officiel)",
        "FF9016" to "Pumpkin Orange (Bambu, officiel)",
        "E4BD68" to "Gold (Bambu, officiel)",
        "FEC600" to "Sunflower Yellow (Bambu, officiel)",
        "F4EE2A" to "Yellow (Bambu, officiel)",
        "BECF00" to "Bright Green (Bambu, officiel)",
        "00AE42" to "Bambu Green (Bambu, officiel)",
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
        "000000" to "Black (Bambu, officiel)"
    )

    // Couleurs usuelles pour l'approximation de secours (si pas de correspondance exacte)
    private val couleursApprox = listOf(
        CouleurNommee("Blanc", 255, 255, 255),
        CouleurNommee("Noir", 0, 0, 0),
        CouleurNommee("Gris", 128, 128, 128),
        CouleurNommee("Gris clair", 200, 200, 200),
        CouleurNommee("Gris fonce", 64, 64, 64),
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
        CouleurNommee("Marron", 139, 69, 19),
        CouleurNommee("Beige", 222, 184, 135),
        CouleurNommee("Or / dore", 212, 175, 55),
        CouleurNommee("Argent / gris metal", 192, 192, 192),
        CouleurNommee("Bronze / cuivre", 184, 115, 51),
        CouleurNommee("Transparent / naturel", 240, 240, 235)
    )

    data class ResultatCouleur(val nom: String, val estExact: Boolean)

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
            val dr = (r - c.r).toDouble()
            val dg = (g - c.g).toDouble()
            val db = (b - c.b).toDouble()
            val distance = sqrt(dr * dr + dg * dg + db * db)
            if (distance < meilleureDistance) {
                meilleureDistance = distance
                meilleurNom = c.nom
            }
        }
        return ResultatCouleur(meilleurNom, false)
    }
}
