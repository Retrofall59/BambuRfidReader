        if (infoFilament.couleurHex != null) {
            try {
                val hexPur = infoFilament.couleurHex.removePrefix("#")
                if (hexPur.length == 8) {
                    val r = hexPur.substring(0, 2).toInt(16)
                    val g = hexPur.substring(2, 4).toInt(16)
                    val b = hexPur.substring(4, 6).toInt(16)
                    val a = hexPur.substring(6, 8).toInt(16)
                    val nomCouleur = NomCouleur.trouverNomProche(r, g, b)
                    resume.append("Couleur : $nomCouleur (${infoFilament.couleurHex})\n")
                    vuCouleur.setBackgroundColor(Color.argb(a, r, g, b))
                    vuCouleur.visibility = View.VISIBLE
                } else {
                    resume.append("Couleur : ${infoFilament.couleurHex}\n")
                }
            } catch (e: Exception) {
                resume.append("Couleur : ${infoFilament.couleurHex}\n")
                vuCouleur.visibility = View.GONE
            }
        } else {
            vuCouleur.visibility = View.GONE
        }
