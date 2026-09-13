package com.tomyn.bambureader

import kotlin.math.sqrt

/**
 * Deux niveaux de recherche du nom de couleur :
 * 1. Table officielle Bambu : correspondance EXACTE sur le code hexadecimal. Certains codes
 *    hex sont partages par plusieurs gammes (ex: #000000 = "Black" en PLA Basic/ABS et
 *    "Charcoal" en PLA Matte) - dans ce cas, le nom de matiere deja detecte ailleurs (indice)
 *    sert a choisir la bonne entree plutot que d'afficher toutes les options a la fois.
 * 2. Si pas de correspondance exacte : couleur usuelle la plus proche, via la formule
 *    "redmean" (ponderee par canal, plus proche de la perception humaine qu'une simple
 *    distance euclidienne RVB brute - notamment pour bien distinguer marrons/gris fonces)
 */
object NomCouleur {

    private data class CouleurNommee(val nom: String, val r: Int, val g: Int, val b: Int)

    // Chaque code hex pointe vers une liste de (mot-cle de gamme, nom officiel).
    // Quand une seule gamme utilise ce code, la liste n'a qu'un element.
    private val tableOfficielle: Map<String, List<Pair<String, String>>> = mapOf(
        "FFFFFF" to listOf("PLA Basic" to "Jade White", "PLA Matte" to "Ivory White", "ABS" to "White", "TPU" to "Frozen (bicolore)", "PLA Pure" to "Pure White", "PLA Silk" to "White"),
        "F7E6DE" to listOf("" to "Beige"),
        "D1D3D5" to listOf("" to "Light Gray"),
        "A6A9AA" to listOf("" to "Silver"),
        "8E9089" to listOf("" to "Gray"),
        "EC008C" to listOf("" to "Magenta"),
        "F55A74" to listOf("" to "Pink"),
        "F5547C" to listOf("" to "Hot Pink"),
        "FF6A13" to listOf("PLA Basic" to "Orange", "ABS" to "Orange"),
        "FF9016" to listOf("" to "Pumpkin Orange"),
        "E4BD68" to listOf("" to "Gold"),
        "FEC600" to listOf("" to "Sunflower Yellow"),
        "F4EE2A" to listOf("" to "Yellow"),
        "BECF00" to listOf("" to "Bright Green"),
        "00AE42" to listOf("PLA Basic" to "Bambu Green", "ABS" to "Bambu Green"),
        "3F8E43" to listOf("" to "Mistletoe Green"),
        "847D48" to listOf("" to "Bronze"),
        "6F5034" to listOf("PLA Basic" to "Cocoa Brown"),
        "9D432C" to listOf("" to "Brown"),
        "9D2235" to listOf("" to "Maroon Red"),
        "C12E1F" to listOf("" to "Red"),
        "00B1B7" to listOf("" to "Turquoise"),
        "0086D6" to listOf("" to "Cyan"),
        "0A2989" to listOf("" to "Blue"),
        "0056B8" to listOf("" to "Cobalt Blue"),
        "5E43B7" to listOf("" to "Purple"),
        "482960" to listOf("" to "Indigo Purple"),
        "5B6579" to listOf("" to "Blue Grey"),
        "545454" to listOf("" to "Dark Gray"),
        "000000" to listOf("PLA Basic" to "Black", "ABS" to "Black", "TPU" to "Black", "PETG" to "Black", "PLA Matte" to "Charcoal", "PLA Pure" to "Absolute Black"),
        // --- Gamme PLA Matte ---
        "CBC6B8" to listOf("PLA Matte" to "Bone White"),
        "E8DBB7" to listOf("PLA Matte" to "Desert Tan"),
        "D3B7A7" to listOf("PLA Matte" to "Latte Brown"),
        "AE835B" to listOf("PLA Matte" to "Caramel"),
        "B15533" to listOf("PLA Matte" to "Terracotta"),
        "7D6556" to listOf("PLA Matte" to "Dark Brown"),
        "4D3324" to listOf("PLA Matte" to "Dark Chocolate"),
        "AE96D4" to listOf("PLA Matte" to "Lilac Purple"),
        "E8AFCF" to listOf("PLA Matte" to "Sakura Pink"),
        "F99963" to listOf("PLA Matte" to "Mandarin Orange"),
        "F7D959" to listOf("PLA Matte" to "Lemon Yellow"),
        "950051" to listOf("PLA Matte" to "Plum"),
        "DE4343" to listOf("PLA Matte" to "Scarlet Red"),
        "BB3D43" to listOf("PLA Matte" to "Dark Red"),
        "68724D" to listOf("PLA Matte" to "Dark Green"),
        "61C680" to listOf("PLA Matte" to "Grass Green"),
        "C2E189" to listOf("PLA Matte" to "Apple Green"),
        "A3D8E1" to listOf("PLA Matte" to "Ice Blue"),
        "56B7E6" to listOf("PLA Matte" to "Sky Blue"),
        "0078BF" to listOf("PLA Matte" to "Marine Blue"),
        "042F56" to listOf("PLA Matte" to "Dark Blue"),
        "9B9EA0" to listOf("PLA Matte" to "Ash Gray"),
        "757575" to listOf("PLA Matte" to "Nardo Gray"),
        // --- Gamme ABS ---
        "789D4A" to listOf("ABS" to "Olive"),
        "489FDF" to listOf("ABS" to "Azure"),
        "0C2340" to listOf("ABS" to "Navy Blue"),
        "0A2CA5" to listOf("ABS" to "Blue"),
        "FFC72C" to listOf("ABS" to "Tangerine Yellow"),
        "D32941" to listOf("ABS" to "Red"),
        "AF1685" to listOf("ABS" to "Purple"),
        "87909A" to listOf("ABS" to "Silver"),
        // --- Gamme TPU 90A ---
        "FFFFEE" to listOf("TPU" to "White"),
        "D6ABFF" to listOf("TPU" to "Grape Jelly"),
        "7EB4E1" to listOf("TPU" to "Crystal Blue"),
        "5C4738" to listOf("TPU" to "Cocoa Brown"),
        "9EA2A2" to listOf("TPU" to "Quicksilver"),
        "F1AAA8" to listOf("TPU" to "Blaze (bicolore)"),
        "D21B3C" to listOf("TPU" to "Blaze (bicolore)"),
        "40B6E4" to listOf("TPU" to "Frozen (bicolore)"),
        // --- Gamme PETG-CF ---
        "9F332A" to listOf("PETG" to "Brick Red"),
        "583061" to listOf("PETG" to "Violet Purple"),
        "324585" to listOf("PETG" to "Indigo Blue"),
        "16B08E" to listOf("PETG" to "Malachite Green"),
        "565656" to listOf("PETG" to "Titan Gray"),
        // --- Gamme PLA Pure ---
        "FFB673" to listOf("PLA Pure" to "Apricot"),
        "F7CED7" to listOf("PLA Pure" to "Milky Pink"),
        "A4DBE8" to listOf("PLA Pure" to "Baby Blue"),
        // --- Gamme PLA Silk+ ---
        "C8C8C8" to listOf("PLA Silk" to "Silver"),
        "F3CFB2" to listOf("PLA Silk" to "Champagne"),
        "F7ADA6" to listOf("PLA Silk" to "Pink"),
        "BA9594" to listOf("PLA Silk" to "Rose Gold"),
        "D02727" to listOf("PLA Silk" to "Candy Red"),
        "F4A925" to listOf("PLA Silk" to "Gold"),
        "96DCB9" to listOf("PLA Silk" to "Mint"),
        "018814" to listOf("PLA Silk" to "Candy Green"),
        "A8C6EE" to listOf("PLA Silk" to "Baby Blue"),
        "008BDA" to listOf("PLA Silk" to "Blue"),
        "8671CB" to listOf("PLA Silk" to "Purple"),
        "5F6367" to listOf("PLA Silk" to "Titan Gray")
    )

    // Liste elargie pour l'approximation quand aucune correspondance exacte n'est trouvee
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
     * @param indiceMatiere texte deja detecte ailleurs (ex: nom du filament type "Bambu ABS")
     *        utilise pour choisir la bonne gamme quand un code hex est partage par plusieurs.
     *        Optionnel - si vide ou sans correspondance, toutes les gammes possibles sont listees.
     */
    // Traduction anglais -> francais des noms officiels Bambu (affichee entre parentheses)
    private val traductions = mapOf(
        "Jade White" to "Blanc jade",
        "Beige" to "Beige",
        "Light Gray" to "Gris clair",
        "Silver" to "Argent",
        "Gray" to "Gris",
        "Magenta" to "Magenta",
        "Pink" to "Rose",
        "Hot Pink" to "Rose vif",
        "Orange" to "Orange",
        "Pumpkin Orange" to "Orange citrouille",
        "Gold" to "Or",
        "Sunflower Yellow" to "Jaune tournesol",
        "Yellow" to "Jaune",
        "Bright Green" to "Vert vif",
        "Bambu Green" to "Vert Bambu",
        "Mistletoe Green" to "Vert gui",
        "Bronze" to "Bronze",
        "Cocoa Brown" to "Marron cacao",
        "Brown" to "Marron",
        "Maroon Red" to "Rouge bordeaux",
        "Red" to "Rouge",
        "Turquoise" to "Turquoise",
        "Cyan" to "Cyan",
        "Blue" to "Bleu",
        "Cobalt Blue" to "Bleu cobalt",
        "Purple" to "Violet",
        "Indigo Purple" to "Violet indigo",
        "Blue Grey" to "Gris bleute",
        "Dark Gray" to "Gris fonce",
        "Black" to "Noir",
        "Ivory White" to "Blanc ivoire",
        "White" to "Blanc",
        "Frozen (bicolore)" to "Givre (bicolore)",
        "Charcoal" to "Anthracite",
        "Absolute Black" to "Noir absolu",
        "Pure White" to "Blanc pur",
        "Bone White" to "Blanc os",
        "Desert Tan" to "Beige desert",
        "Latte Brown" to "Marron latte",
        "Caramel" to "Caramel",
        "Terracotta" to "Terre cuite",
        "Dark Brown" to "Marron fonce",
        "Dark Chocolate" to "Chocolat noir",
        "Lilac Purple" to "Violet lilas",
        "Sakura Pink" to "Rose sakura",
        "Mandarin Orange" to "Orange mandarine",
        "Lemon Yellow" to "Jaune citron",
        "Plum" to "Prune",
        "Scarlet Red" to "Rouge ecarlate",
        "Dark Red" to "Rouge fonce",
        "Dark Green" to "Vert fonce",
        "Grass Green" to "Vert herbe",
        "Apple Green" to "Vert pomme",
        "Ice Blue" to "Bleu glace",
        "Sky Blue" to "Bleu ciel",
        "Marine Blue" to "Bleu marine",
        "Dark Blue" to "Bleu fonce",
        "Ash Gray" to "Gris cendre",
        "Nardo Gray" to "Gris Nardo",
        "Olive" to "Olive",
        "Azure" to "Azur",
        "Navy Blue" to "Bleu marine fonce",
        "Tangerine Yellow" to "Jaune mandarine",
        "Grape Jelly" to "Confiture de raisin",
        "Crystal Blue" to "Bleu cristal",
        "Quicksilver" to "Vif-argent",
        "Blaze (bicolore)" to "Flamme (bicolore)",
        "Brick Red" to "Rouge brique",
        "Violet Purple" to "Violet",
        "Indigo Blue" to "Bleu indigo",
        "Malachite Green" to "Vert malachite",
        "Titan Gray" to "Gris titane",
        "Apricot" to "Abricot",
        "Milky Pink" to "Rose laiteux",
        "Baby Blue" to "Bleu layette",
        "Champagne" to "Champagne",
        "Rose Gold" to "Or rose",
        "Candy Red" to "Rouge bonbon",
        "Mint" to "Menthe",
        "Candy Green" to "Vert bonbon"
    )

    private fun avecTraduction(nomAnglais: String): String {
        val traduction = traductions[nomAnglais]
        return if (traduction != null) "$nomAnglais ($traduction)" else nomAnglais
    }

    fun trouverNom(hexRGB: String, indiceMatiere: String = ""): ResultatCouleur {
        val hexNormalise = hexRGB.uppercase()
        val entrees = tableOfficielle[hexNormalise]
        if (entrees != null) {
            if (entrees.size == 1) {
                return ResultatCouleur("${avecTraduction(entrees[0].second)} (Bambu, officiel)", true)
            }
            val indiceNormalise = indiceMatiere.uppercase()
            val correspondance = entrees.firstOrNull { (ligne, _) ->
                ligne.isNotEmpty() && indiceNormalise.contains(ligne.uppercase())
            }
            if (correspondance != null) {
                return ResultatCouleur("${avecTraduction(correspondance.second)} (Bambu ${correspondance.first}, officiel)", true)
            }
            // Aucun indice de matiere ne permet de trancher : on liste toutes les options connues
            val toutesLesOptions = entrees.joinToString(" / ") { (ligne, nom) ->
                val nomTraduit = avecTraduction(nom)
                if (ligne.isEmpty()) nomTraduit else "$nomTraduit ($ligne)"
            }
            return ResultatCouleur("$toutesLesOptions - Bambu, officiel", true)
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
