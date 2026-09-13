package com.tomyn.bambureader

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var txtResultat: TextView
    private lateinit var vuCouleur: View
    private var dernierDumpTexte: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtResultat = findViewById(R.id.txtResultat)
        vuCouleur = findViewById(R.id.vuCouleur)
        val btnExporter = findViewById<Button>(R.id.btnExporter)
        btnExporter.setOnClickListener { exporterDump() }

        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) {
            txtResultat.text = "Ce telephone n'a pas de puce NFC."
            return
        }
        nfcAdapter = adapter

        txtResultat.text = "Approche une bobine Bambu du dos du telephone..."
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

    private fun lireTag(tag: Tag) {
        val mifare = MifareClassic.get(tag)
        if (mifare == null) {
            txtResultat.text = "Ce tag n'est pas un MIFARE Classic (ou ton telephone ne le supporte pas)."
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
            mifare.connect()

            for (secteur in 0 until mifare.sectorCount) {
                val cle = if (secteur < clesA.size) clesA[secteur] else null
                if (cle == null) {
                    rapport.append("[Secteur $secteur] Pas de cle derivee, ignore\n")
                    continue
                }

                val authOk = try {
                    mifare.authenticateSectorWithKeyA(secteur, cle)
                } catch (e: Exception) {
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
                        val donnees = mifare.readBlock(numBloc)
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

        val resume = StringBuilder()
        resume.append("=== Bambu RFID Reader ===\n")
        resume.append("UID du tag : $uidHex\n\n")

        if (resultatMatiere != null) {
            resume.append("FILAMENT DETECTE\n")
            resume.append("${resultatMatiere.second}\n")
            resume.append("Code interne : ${resultatMatiere.first}\n\n")
        } else if (infoFilament.typeDetaille != null || infoFilament.typeFilament != null) {
            resume.append("FILAMENT DETECTE\n")
            resume.append("${infoFilament.typeDetaille ?: infoFilament.typeFilament}\n\n")
        } else {
            resume.append("Filament non identifie\n\n")
        }

        if (infoFilament.couleurHex != null) {
            resume.append("Couleur : ${infoFilament.couleurHex}\n")
            try {
                val hexPur = infoFilament.couleurHex.removePrefix("#")
                if (hexPur.length == 8) {
                    val r = hexPur.substring(0, 2).toInt(16)
                    val g = hexPur.substring(2, 4).toInt(16)
                    val b = hexPur.substring(4, 6).toInt(16)
                    val a = hexPur.substring(6, 8).toInt(16)
                    vuCouleur.setBackgroundColor(Color.argb(a, r, g, b))
                    vuCouleur.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                vuCouleur.visibility = View.GONE
            }
        } else {
            vuCouleur.visibility = View.GONE
        }
        if (infoFilament.poidsGrammes != null && infoFilament.poidsGrammes in 1..10000) {
            resume.append("Poids bobine : ${infoFilament.poidsGrammes}g\n")
        }
        if (infoFilament.tempBuseMin != null && infoFilament.tempBuseMax != null &&
            infoFilament.tempBuseMin in 0..500 && infoFilament.tempBuseMax in 0..500) {
            resume.append("Temperature buse : ${infoFilament.tempBuseMin}-${infoFilament.tempBuseMax}C\n")
        }
        if (infoFilament.tempPlateau != null && infoFilament.tempPlateau in 0..200) {
            resume.append("Temperature plateau : ${infoFilament.tempPlateau}C\n")
        }
        if (infoFilament.tempSechage != null && infoFilament.tempSechage in 0..150) {
            resume.append("Sechage recommande : ${infoFilament.tempSechage}C pendant ${infoFilament.dureeSechage ?: "?"}h\n")
        }

        if (resultatMatiere == null && infoFilament.typeDetaille == null && infoFilament.couleurHex == null) {
            resume.append("(code matiere introuvable - verifie les erreurs d'authentification\nen exportant le dump complet pour voir le detail)\n")
        }

        dernierDumpTexte = resume.toString() + "\n\n--- DETAIL TECHNIQUE COMPLET (pour export) ---\n\n" + rapport.toString()
        txtResultat.text = resume.toString()
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
