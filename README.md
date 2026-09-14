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

## AVANT DE COMPILER : verifie que ton telephone supporte le MIFARE Classic

Beaucoup de telephones Android recents (notamment pas mal de Pixel et certains Samsung)
n'implementent PAS le support MIFARE Classic au niveau materiel/OS, pour des raisons de
licence sur le chiffrement Crypto1 utilise par ce type de tag. Ce n'est pas un probleme de
code, c'est une limitation de la puce NFC elle-meme.

Pour verifier rapidement : installe une appli comme "NFC TagInfo" (gratuite sur le Play
Store), scanne n'importe quel tag MIFARE Classic (meme un badge d'acces classique), et
regarde si l'appli arrive a en lire le contenu. Si oui, ton telephone est compatible.

## Telephones confirmes compatibles (retours utilisateurs)

Cette liste s'allonge au fil des retours sur le forum. Si tu testes sur un telephone qui
n'y est pas encore, n'hesite pas a partager ton retour pour qu'on l'ajoute.

| Telephone | Statut | Source |
|---|---|---|
| Samsung Galaxy S20 FE | Compatible (teste sur PLA et ABS) | Retour forum - Zetif |

## Comment obtenir le .apk (le plus simple : sans rien installer)

Ce projet est configure pour se compiler automatiquement sur les serveurs de GitHub
(gratuit), sans avoir besoin d'installer Android Studio ni quoi que ce soit sur ton PC.

1. Cree un compte GitHub gratuit si t'en as pas deja un (github.com)
2. Cree un nouveau depot (repository), par exemple "BambuRfidReader"
3. Sur la page du depot, clique sur "uploading an existing file" et glisse-depose TOUT
   le contenu de ce dossier (garde bien la structure des sous-dossiers)

   ATTENTION : le dossier ".github" (avec le point devant) est cache par defaut dans
   l'explorateur de fichiers Windows/Mac. Si tu le glisses pas, la compilation
   automatique ne se declenchera pas. Active "Afficher les elements caches" dans ton
   explorateur de fichiers avant de faire le glisser-deposer, pour etre sur de bien
   inclure ce dossier.
4. Valide l'envoi (bouton vert "Commit changes")
5. Va dans l'onglet "Actions" du depot en haut de la page
6. Une compilation se lance automatiquement (ca prend 2-3 minutes)
7. Une fois termine (coche verte), clique dessus, puis clique sur "BambuRfidReader-apk"
   tout en bas de la page pour telecharger le fichier .apk

8. Transfere ce .apk sur ton telephone (mail, cle USB, Google Drive...) et installe-le
   (il faudra peut-etre autoriser "sources inconnues" dans les parametres Android)

## Alternative : compiler toi-meme avec Android Studio

1. Installe Android Studio (gratuit, https://developer.android.com/studio)
2. Ouvre ce dossier entier comme projet ("Open an existing project")
3. Laisse Android Studio telecharger les dependances Gradle (peut prendre quelques minutes
   la premiere fois)
4. Branche ton telephone en USB avec le mode developpeur + debogage USB actives
5. Clique sur le bouton "Run" (triangle vert) pour installer et lancer l'appli directement
   sur ton telephone

## Utilisation

1. Lance l'appli, elle affiche "Approche une bobine Bambu..."
2. Pose le dos du telephone sur le tag RFID d'une bobine Bambu (generalement colle sur le
   carton central)
3. L'appli affiche le dump complet (UID + contenu de chaque secteur/bloc en hexadecimal)
4. Bouton "Exporter le dernier dump" pour sauvegarder ca dans un fichier .txt sur le
   telephone (dossier Android/data/com.tomyn.bambureader/files/dumps_bambu/)

## Pour aller plus loin : interpreter le dump

Ce dump brut donne des donnees hexadecimales, pas encore "PLA Basic, Rouge, 220C" en clair.
Pour decoder precisement ce que chaque bloc represente (materiau, couleur, temperature...),
la documentation complete est ici :
https://github.com/Bambu-Research-Group/RFID-Tag-Guide/blob/main/BambuLabRfid.md

Si tu veux, on peut ajouter cette couche d'interpretation a l'appli une fois que t'auras
confirme que la lecture brute fonctionne deja sur ton telephone.
