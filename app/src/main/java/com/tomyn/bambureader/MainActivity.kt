package com.tomyn.bambureader

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var txtResultat: TextView
    private lateinit var txtStatut: TextView
    private lateinit var vuCouleur: View
    private lateinit var imgNfc: ImageView
    private lateinit var layoutLignesInfo: LinearLayout
    private var dernierDumpTexte: String = ""
    private var dernierResume: String = ""
    private var dernierNomFilament: String? = null
    private var dernierNomCouleur: String? = null
    private var dernierNomCouleurEtiquette: String? = null
    private var dernierCouleurArgb: Int? = null
    private var dernierPoidsGrammes: Int? = null
    private var dernierTempBuseTexte: String? = null
    private var dernierTempPlateau: Int? = null
    private var animationPulse: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtResultat = findViewById(R.id.txtResultat)
        txtStatut = findViewById(R.id.txtStatut)
        vuCouleur = findViewById(R.id.vuCouleur)
        imgNfc = findViewById(R.id.imgNfc)
        layoutLignesInfo = findViewById(R.id.layoutLignesInfo)

        val btnExporter = findViewById<Button>(R.id.btnExporter)
        btnExporter.setOnClickListener { exporterDump() }

        val btnImprimerEtiquette = findViewById<Button>(R.id.btnImprimerEtiquette)
        btnImprimerEtiquette.setOnClickListener { imprimerEtiquette() }

        val btnCopier = findViewById<Button>(R.id.btnCopier)
        btnCopier.setOnClickListener { copierResume() }

        val btnPartager = findViewById<Button>(R.id.btnPartager)
        btnPartager.setOnClickListener { partagerResume() }

        val btnHistorique = findViewById<Button>(R.id.btnHistorique)
        btnHistorique.setOnClickListener { afficherHistorique() }

        demarrerPulseNfc()

        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            txtStatut.text = "Ce telephone n'a pas de puce NFC."
            return
        }
        nfcAdapter = adapter
    }

    private fun demarrerPulseNfc() {
        val animateur = ObjectAnimator.ofFloat(imgNfc, "scaleX", 1f, 1.15f, 1f)
        animateur.duration = 1200
        animateur.repeatCount = ValueAnimator.INFINITE
        val animateurY = ObjectAnimator.ofFloat(imgNfc, "scaleY", 1f, 1.15f, 1f)
        animateurY.duration = 1200
        animateurY.repeatCount = ValueAnimator.INFINITE
        animateur.start()
        animateurY.start()
        animationPulse = animateur
    }

    private fun arreterPulseNfc() {
        animationPulse?.cancel()
        imgNfc.scaleX = 1f
        imgNfc.scaleY = 1f
    }

    override fun onResume() {
        super.onResume()
        if (!::nfcAdapter.isInitialized) return

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        val filtres = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
        val techListes = arrayOf(arrayOf(MifareClassic::class.java.name))
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filtres, techListes)
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            lireTag(tag)
        }
    }

    private fun ajouterLigneInfo(icone: Int, texte: String) {
        val ligne = LinearLayout(this)
        ligne.orientation = LinearLayout.HORIZONTAL
        ligne.gravity = Gravity.CENTER_VERTICAL
        val paddingPx = (6 * resources.displayMetrics.density).toInt()
        ligne.setPadding(0, paddingPx, 0, paddingPx)

        val img = ImageView(this)
        img.setImageResource(icone)
        val tailleIcone = (20 * resources.displayMetrics.density).toInt()
        val paramsImg = LinearLayout.LayoutParams(tailleIcone, tailleIcone)
        paramsImg.marginEnd = (10 * resources.displayMetrics.density).toInt()
        img.layoutParams = paramsImg

        val txt = TextView(this)
        txt.text = texte
        txt.setTextColor(resources.getColor(R.color.texte_principal, theme))
        txt.textSize = 14f

        ligne.addView(img)
        ligne.addView(txt)
        layoutLignesInfo.addView(ligne)

        val animation = AnimationUtils.loadAnimation(this, R.anim.apparition_ligne)
        animation.startOffset = (layoutLignesInfo.childCount - 1) * 90L
        ligne.startAnimation(animation)
    }

    private fun lireTag(tag: Tag) {
        arreterPulseNfc()
        layoutLignesInfo.removeAllViews()
        txtResultat.visibility = View.GONE

        val mifare = MifareClassic.get(tag)
        if (mifare == null) {
            txtStatut.text = "Ce tag n'est pas un MIFARE Classic (ou ton telephone ne le supporte pas)."
            demarrerPulseNfc()
            return
        }

        val uid = tag.id
        val uidHex = uid.joinToString("") { String.format("%02X", it) }

        val rapport = StringBuilder()
        val tousLesBlocsLisibles = StringBuilder()
        val blocsParNumero = mutableMapOf<Int, ByteArray>()
        rapport.append("=== Dump tag Bambu Lab ===\n")
        rapport.append("Date : ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(Date())}\n")
        rapport.append("UID : $uidHex\n")
        rapport.append("Nb secteurs : ${mifare.sectorCount}\n\n")

        val clesA = BambuKeyDeriver.deriverClesA(uid)

        try {
            var connecte = false
            var derniereErreurConnexion: Exception? = null
            for (tentative in 1..3) {
                try {
                    mifare.connect()
                    connecte = true
                    break
                } catch (e: Exception) {
                    derniereErreurConnexion = e
                    try { Thread.sleep(150) } catch (ignored: InterruptedException) {}
                }
            }
            if (!connecte) {
                throw derniereErreurConnexion ?: Exception("Connexion impossible apres 3 tentatives")
            }

            for (secteur in 0 until mifare.sectorCount) {
                val cle = if (secteur < clesA.size) clesA[secteur] else null
                if (cle == null) {
                    rapport.append("[Secteur $secteur] Pas de cle derivee, ignore\n")
                    continue
                }

                val authOk = run {
                    for (tentative in 1..2) {
                        try {
                            if (mifare.authenticateSectorWithKeyA(secteur, cle)) return@run true
                        } catch (e: Exception) {
                            try { Thread.sleep(80) } catch (ignored: InterruptedException) {}
                        }
                    }
                    false
                }

                if (!authOk) {
                    rapport.append("[Secteur $secteur] Echec d'authentification (cle : ${cle.joinToString("") { String.format("%02X", it) }})\n")
                    continue
                }

                val premierBlocSecteur = mifare.sectorToBlock(secteur)
                val nbBlocsSecteur = mifare.getBlockCountInSector(secteur)

                rapport.append("[Secteur $secteur] OK (cle : ${cle.joinToString("") { String.format("%02X", it) }})\n")
                for (i in 0 until nbBlocsSecteur) {
                    val numBloc = premierBlocSecteur + i
                    try {
                        var donnees: ByteArray? = null
                        var derniereErreurBloc: Exception? = null
                        for (tentative in 1..2) {
                            try {
                                donnees = mifare.readBlock(numBloc)
                                break
                            } catch (e: Exception) {
                                derniereErreurBloc = e
                                try { Thread.sleep(80) } catch (ignored: InterruptedException) {}
                            }
                        }
                        if (donnees == null) throw derniereErreurBloc ?: Exception("echec de lecture")
                        val hex = donnees.joinToString(" ") { String.format("%02X", it) }
                        rapport.append("  Bloc $numBloc : $hex\n")
                        tousLesBlocsLisibles.append(String(donnees, Charsets.ISO_8859_1))
                        blocsParNumero[numBloc] = donnees
                    } catch (e: Exception) {
                        rapport.append("  Bloc $numBloc : erreur de lecture (${e.message})\n")
                    }
                }
                rapport.append("\n")
            }

            mifare.close()
        } catch (e: Exception) {
            rapport.append("\nErreur generale de connexion au tag : ${e.message}\n")
            rapport.append("(Si ca echoue systematiquement des la connexion, ton telephone ne supporte probablement pas nativement le MIFARE Classic - limitation materielle, pas un bug de l'appli.)\n")
        }

        val infoFilament = BambuTagDecoder.decoder(blocsParNumero)
        val resultatMatiere = infoFilament.codeMatiere?.let { MaterialIdLookup.trouverEtTraduire(it) }
            ?: MaterialIdLookup.trouverEtTraduire(tousLesBlocsLisibles.toString())

        val resumeTexte = StringBuilder()
        resumeTexte.append("UID du tag : $uidHex\n")

        val nomFilament: String? = resultatMatiere?.second ?: infoFilament.typeDetaille ?: infoFilament.typeFilament
        val codeAffiche: String? = resultatMatiere?.first ?: infoFilament.codeMatiere

        if (nomFilament != null) {
            txtStatut.text = "Filament detecte"
            ajouterLigneInfo(R.drawable.ic_materiau, nomFilament + if (codeAffiche != null) " ($codeAffiche)" else "")
            resumeTexte.append("Filament : $nomFilament${if (codeAffiche != null) " ($codeAffiche)" else ""}\n")
            dernierNomFilament = nomFilament
        } else {
            txtStatut.text = "Filament non identifie"
            dernierNomFilament = null
        }

        if (infoFilament.couleurHex != null) {
            try {
                val hexPur = infoFilament.couleurHex.removePrefix("#")
                if (hexPur.length == 8) {
                    val hexRGB = hexPur.substring(0, 6)
                    val r = hexPur.substring(0, 2).toInt(16)
                    val g = hexPur.substring(2, 4).toInt(16)
                    val b = hexPur.substring(4, 6).toInt(16)
                    val a = hexPur.substring(6, 8).toInt(16)
                    val resultatCouleur = NomCouleur.trouverNom(hexRGB, nomFilament ?: "")
                    val suffixe = if (resultatCouleur.estExact) "" else " (approximatif)"
                    ajouterLigneInfo(R.drawable.ic_couleur, "${resultatCouleur.nom}$suffixe")
                    resumeTexte.append("Couleur : ${resultatCouleur.nom}$suffixe (${infoFilament.couleurHex})\n")
                    vuCouleur.backgroundTintList = ColorStateList.valueOf(Color.argb(a, r, g, b))
                    vuCouleur.visibility = View.VISIBLE
                    imgNfc.visibility = View.GONE
                    dernierNomCouleur = "${resultatCouleur.nom}$suffixe"
                    dernierNomCouleurEtiquette = "${resultatCouleur.nomCourt}$suffixe"
                    dernierCouleurArgb = Color.argb(a, r, g, b)
                }
            } catch (e: Exception) {
                vuCouleur.visibility = View.GONE
                imgNfc.visibility = View.VISIBLE
                dernierNomCouleur = null
                dernierNomCouleurEtiquette = null
                dernierCouleurArgb = null
            }
        } else {
            vuCouleur.visibility = View.GONE
            imgNfc.visibility = View.VISIBLE
            dernierNomCouleur = null
            dernierNomCouleurEtiquette = null
            dernierCouleurArgb = null
        }

        if (infoFilament.poidsGrammes != null && infoFilament.poidsGrammes in 1..10000) {
            ajouterLigneInfo(R.drawable.ic_materiau, "Poids bobine : ${infoFilament.poidsGrammes}g")
            resumeTexte.append("Poids bobine : ${infoFilament.poidsGrammes}g\n")
            dernierPoidsGrammes = infoFilament.poidsGrammes
        } else {
            dernierPoidsGrammes = null
        }
        if (infoFilament.tempBuseMin != null && infoFilament.tempBuseMax != null &&
            infoFilament.tempBuseMin in 0..500 && infoFilament.tempBuseMax in 0..500) {
            ajouterLigneInfo(R.drawable.ic_temperature, "Buse : ${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C")
            resumeTexte.append("Temperature buse : ${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C\n")
            dernierTempBuseTexte = "${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C"
        } else {
            dernierTempBuseTexte = null
        }
        if (infoFilament.tempPlateau != null && infoFilament.tempPlateau in 0..200) {
            ajouterLigneInfo(R.drawable.ic_temperature, "Plateau : ${infoFilament.tempPlateau}C")
            resumeTexte.append("Temperature plateau : ${infoFilament.tempPlateau}C\n")
            dernierTempPlateau = infoFilament.tempPlateau
        } else {
            dernierTempPlateau = null
        }
        if (infoFilament.tempSechage != null && infoFilament.tempSechage in 0..150) {
            ajouterLigneInfo(R.drawable.ic_temperature, "Sechage : ${infoFilament.tempSechage}C / ${infoFilament.dureeSechage ?: "?"}h")
            resumeTexte.append("Sechage recommande : ${infoFilament.tempSechage}C pendant ${infoFilament.dureeSechage ?: "?"}h\n")
        }

        val detectionReussie = nomFilament != null

        dernierDumpTexte = resumeTexte.toString() + "\n\n--- DETAIL TECHNIQUE COMPLET (pour export) ---\n\n" + rapport.toString()
        dernierResume = resumeTexte.toString()
        txtResultat.text = dernierDumpTexte

        if (detectionReussie) {
            vibrerConfirmation()
            val couleurPourHistorique = infoFilament.couleurHex ?: ""
            enregistrerDansHistorique(uidHex, nomFilament ?: "Inconnu", couleurPourHistorique)
        } else {
            demarrerPulseNfc()
        }
    }

    private fun vibrerConfirmation() {
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        } catch (e: Exception) {
            // Pas grave si la vibration echoue, ce n'est qu'un confort
        }
    }

    private fun enregistrerDansHistorique(uid: String, nom: String, couleurHex: String) {
        try {
            val fichier = File(getExternalFilesDir(null), "historique_scans.csv")
            val ligne = "${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())};$uid;$nom;$couleurHex\n"
            fichier.appendText(ligne)
        } catch (e: Exception) {
            // Pas grave si l'ecriture de l'historique echoue
        }
    }

    private fun afficherHistorique() {
        try {
            val fichier = File(getExternalFilesDir(null), "historique_scans.csv")
            if (!fichier.exists() || fichier.readText().isBlank()) {
                AlertDialog.Builder(this)
                    .setTitle("Historique des scans")
                    .setMessage("Aucun scan enregistre pour l'instant.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }
            val lignes = fichier.readLines().reversed()
            val texteAffiche = lignes.joinToString("\n\n") { ligne ->
                val parts = ligne.split(";")
                if (parts.size >= 3) {
                    "${parts[0]}\n${parts[2]}${if (parts.size >= 4 && parts[3].isNotBlank()) " (${parts[3]})" else ""}"
                } else ligne
            }
            AlertDialog.Builder(this)
                .setTitle("Historique des scans (${lignes.size})")
                .setMessage(texteAffiche)
                .setPositiveButton("Fermer", null)
                .setNegativeButton("Vider l'historique") { _, _ ->
                    fichier.delete()
                    Toast.makeText(this, "Historique efface.", Toast.LENGTH_SHORT).show()
                }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur lecture historique : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copierResume() {
        if (dernierResume.isEmpty()) {
            Toast.makeText(this, "Rien a copier pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Resultat Bambu RFID", dernierResume))
        Toast.makeText(this, "Copie dans le presse-papier.", Toast.LENGTH_SHORT).show()
    }

    private fun partagerResume() {
        if (dernierResume.isEmpty()) {
            Toast.makeText(this, "Rien a partager pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, dernierResume)
        startActivity(Intent.createChooser(intent, "Partager le resultat"))
    }

    private fun imprimerEtiquette() {
        if (dernierNomFilament == null) {
            Toast.makeText(this, "Aucun filament identifie pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }

        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            var document: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                document = PdfDocument()
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("etiquette_filament.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = document!!.startPage(pageInfo)
                dessinerEtiquette(page.canvas)
                document!!.finishPage(page)

                try {
                    document!!.writeTo(FileOutputStream(destination.fileDescriptor))
                } catch (e: IOException) {
                    callback.onWriteFailed(e.message)
                    return
                } finally {
                    document!!.close()
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        }

        printManager.print("Etiquette filament Bambu", adapter, PrintAttributes.Builder().build())
    }

    private fun dessinerEtiquette(canvas: Canvas) {
        val margeGauche = 60f
        val y = 90f

        // Taille reelle d'une petite etiquette (environ 6cm x 3.7cm)
        val largeurEtiquette = 170f
        val hauteurEtiquette = 105f

        val paintTitre = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 7f
        }
        val paintMatiere = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val paintTexte = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 8f
        }
        val paintBordure = Paint().apply {
            color = android.graphics.Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val paintSwatch = Paint().apply {
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(margeGauche, y, margeGauche + largeurEtiquette, y + hauteurEtiquette, 5f, 5f, paintBordure)

        val margeInterne = margeGauche + 8f
        var yInterne = y + 13f
        canvas.drawText("Bambu RFID Reader", margeInterne, yInterne, paintTitre)

        yInterne += 14f
        canvas.drawText(dernierNomFilament ?: "Filament inconnu", margeInterne, yInterne, paintMatiere)

        yInterne += 16f
        if (dernierCouleurArgb != null) {
            paintSwatch.color = dernierCouleurArgb!!
            canvas.drawCircle(margeInterne + 5f, yInterne - 3f, 5.5f, paintSwatch)
            val paintCercleBordure = Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            canvas.drawCircle(margeInterne + 5f, yInterne - 3f, 5.5f, paintCercleBordure)
            canvas.drawText(dernierNomCouleurEtiquette ?: "", margeInterne + 16f, yInterne, paintTexte)
            yInterne += 12f
        }

        if (dernierPoidsGrammes != null) {
            canvas.drawText("Poids : ${dernierPoidsGrammes}g", margeInterne, yInterne, paintTexte)
            yInterne += 11f
        }

        if (dernierTempBuseTexte != null) {
            canvas.drawText("Buse : $dernierTempBuseTexte", margeInterne, yInterne, paintTexte)
            yInterne += 11f
        }

        if (dernierTempPlateau != null) {
            canvas.drawText("Plateau : ${dernierTempPlateau}C", margeInterne, yInterne, paintTexte)
        }

        val paintDate = Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = 5.5f
        }
        canvas.drawText(
            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date()),
            margeInterne,
            y + hauteurEtiquette - 6f,
            paintDate
        )
    }

    private fun exporterDump() {
        if (dernierDumpTexte.isEmpty()) {
            Toast.makeText(this, "Aucun dump a exporter pour l'instant, scanne d'abord un tag.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dossier = File(getExternalFilesDir(null), "dumps_bambu")
            if (!dossier.exists()) dossier.mkdirs()
            val nomFichier = "dump_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date()) + ".txt"
            val fichier = File(dossier, nomFichier)
            fichier.writeText(dernierDumpTexte)
            Toast.makeText(this, "Dump enregistre : ${fichier.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur export : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
