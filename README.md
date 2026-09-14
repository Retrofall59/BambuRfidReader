# Bambu RFID Reader - lecture et dump de tags Bambu Lab

## Ce que fait cette appli

- Detecte un tag RFID Bambu Lab (MIFARE Classic 13.56MHz) approche du dos du telephone
- Derive automatiquement les 16 cles de secteur a partir de l'UID du tag, via l'algorithme
  HKDF-SHA256 publie par Bambu-Research-Group (voir BambuKeyDeriver.kt pour le detail)
- Lit tous les blocs de tous les secteurs accessibles et les affiche en hexadecimal
- Permet d'exporter le dump dans un fichier texte sur le telephone

## Ce qu'elle NE fait PAS (et ne pourra jamais faire avec un AMS standard)

Ecrire un tag avec des donnees personnalisees que l'AMS accepterait. Chaque tag est signe
avec une cle RSA privee que seul Bambu Lab possede - sans cette cle, impossible de generer
une signature valide. Voir https://github.com/Bambu-Research-Group/RFID-Tag-Guide pour le
detail technique (section "Custom Tags" de leur FAQ).



## Utilisation

1. Lance l'appli, elle affiche "Approche une bobine Bambu..."
2. Pose le dos du telephone sur le tag RFID d'une bobine Bambu (generalement colle sur le
   carton central)
3. L'appli affiche le dump complet (UID + contenu de chaque secteur/bloc en hexadecimal)
4. Bouton "Exporter le dernier dump" pour sauvegarder ca dans un fichier .txt sur le
   telephone (dossier Android/data/com.tomyn.bambureader/files/dumps_bambu/)

