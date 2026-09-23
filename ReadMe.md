# ⚜️ Stylométrie — Identification & Attribution de Style Littéraire

Une application desktop moderne développée en **JavaFX 21**, **GATE Embedded** et **SQLite**, dédiée à la reconnaissance d'auteur et à la comparaison stylométrique. 

L'objectif principal du projet est d'**analyser un texte quelconque** (rédigé par une personne lambda ou un auteur anonyme) et de **déterminer dynamiquement de quel grand écrivain français** (de la Monarchie à nos jours) son style d'écriture se rapproche le plus.

L'interface propose un thème personnalisé aux couleurs régaliennes (*Bleu Nuit Royal, Touches d'Or et Bordeaux*), offrant une expérience fluide avec un en-tête unifié et une barre de navigation par onglets.

---

## 📸 Aperçu des Fonctionnalités

* **Navigation par Onglets** : Accueil, Gestion du Corpus, Pipeline GATE, Statistiques & Attribution d'Auteur.
* **Gestion du Corpus & Référentiel Écrivains** : Import de textes de référence (`.txt`) par grand auteur dans la base SQLite pour constituer l'empreinte stylistique de chaque écrivain.
* **Analyse & Prédiction de Style (GATE + TAL)** : Extraction des caractéristiques linguistiques (densité lexicale, répartition des POS tags, longueurs de phrases) et algorithme de calcul de distance stylistique pour trouver l'auteur le plus proche d'un texte candidat.
* **Interface Sur-Mesure** : En-tête fenêtré personnalisé (`UNDECORATED`), menu contextuel drapé d'or, icône dynamique Fleur de Lys.

---

## 🛠️ Stack Technique & Outils Utilisés

| Composant | Technologie / Outil | Rôle / Usage |
| :--- | :--- | :--- |
| **Langage** | Java 21 (OpenJDK 21.0.2) | Cœur applicatif et logique algorithmique |
| **IHM / UI** | JavaFX 21 + FXML + CSS | Interface graphique desktop avec thème personnalisé |
| **Moteur TAL / NLP** | GATE Embedded 9.0.1 | Traitement Automatique du Langage (Tokenisation, POS Tagging) |
| **Base de Données** | SQLite 3.45.1.0 | Stockage local des auteurs, textes et empreintes stylistiques |
| **Gestionnaire de Build**| Apache Maven 3.9+ | Gestion des dépendances, compilation et génération du JAR |
| **Environnement (IDE)** | IntelliJ IDEA | Développement et débogage |

---

## 📦 Liste des Dépendances (Libraries)

Les dépendances Maven déclarées dans le projet (`pom.xml`) :

* **JavaFX Controls** (`org.openjfx:javafx-controls:21.0.2`) : Composants d'interface (Button, TableView, TabPane, ComboBox, etc.).
* **JavaFX FXML** (`org.openjfx:javafx-fxml:21.0.2`) : Chargement des vues structurées en XML.
* **SQLite JDBC** (`org.xerial:sqlite-jdbc:3.45.1.0`) : Connecteur de base de données relationnelle embarquée.
* **GATE Core Embedded** (`uk.ac.gate:gate-core:9.0.1`) : Framework NLP/TAL pour l'analyse syntaxique et morphosyntaxique des textes.
* **SLF4J Simple** (`org.slf4j:slf4j-simple:1.7.36`) : Implémentation légère du moteur de journalisation (logs).

---

## 🔌 Liste des Plugins Maven & Versions

Les plugins exploités pour la compilation, l'exécution et le packaging de l'application :

1. **Maven Compiler Plugin** (`org.apache.maven.plugins:maven-compiler-plugin:3.11.0`)
   * *Rôle* : Compilation des classes Java vers le niveau de langage Java 21.
2. **JavaFX Maven Plugin** (`org.openjfx:javafx-maven-plugin:0.0.8`)
   * *Rôle* : Lancement direct de l'application JavaFX via la commande `mvn javafx:run`.
3. **Maven Surefire Plugin** (`org.apache.maven.plugins:maven-surefire-plugin:3.1.2`)
   * *Rôle* : Exécution automatique des tests unitaires lors de la phase de test.
4. **Maven Shade Plugin** (`org.apache.maven.plugins:maven-shade-plugin:3.5.1`)
   * *Rôle* : Assemblage du projet en un fichier `.jar` monolithique ("Fat JAR") contenant toutes les dépendances.

---

## 📂 Arborescence du Projet

```text
stylometrie/
├── pom.xml
├── README.md
├── stylometrie.db (Base de données SQLite auto-générée)
└── src/
    └── main/
        ├── java/
        │   └── fr/
        │       └── unicaen/
        │           ├── App.java                   (Point d'entrée JavaFX)
        │           ├── Main.java                  (Classe Main classique)
        │           ├── controller/
        │           │   ├── MainController.java    (Contrôleur central)
        │           │   ├── HeaderController.java  (Contrôleur de l'en-tête & fenêtre)
        │           │   └── AnalysisController.java(Gestion des analyses)
        │           ├── database/
        │           │   ├── DatabaseManager.java   (Connexion SQLite)
        │           │   └── AnalysisDao.java       (Requêtes SQL & Corpus)
        │           ├── model/
        │           │   ├── Author.java            (Modèle Auteur)
        │           │   ├── Text.java              (Modèle Texte/Ouvrage)
        │           │   └── AnalysisResult.java    (Modèle Métriques Stylistiques)
        │           └── service/
        │               ├── GatePipelineService.java (Service TAL GATE)
        │               └── StylometryAnalyzer.java  (Algorithme de proximité)
        └── resources/
            └── fr/
                └── unicaen/
                    ├── style.css                  (Thème régalien Or/Bleu/Bordeaux)
                    ├── main-view.fxml             (Vue principale)
                    └── components/
                        ├── header.fxml            (En-tête, Menu & Contrôles)
                        └── footer.fxml            (Pied de page fixe)
```

---

## 📜 Répertoire Exhaustif des Commandes

Ce tableau récapitule l'ensemble des commandes disponibles, la façon de les saisir et leur action exacte sur le projet :

| Commande | Syntaxe / Appel | Action & Effet produit |
| :--- | :--- | :--- |
| **Clean** | `mvn clean` | Supprime le dossier `/target` généré lors des compilations précédentes pour repartir d'un état propre. |
| **Compile** | `mvn compile` | Compile le code source Java (`src/main/java`) et valide la syntaxe des vues FXML & CSS dans `/target/classes`. |
| **Run (JavaFX)** | `mvn javafx:run` | Lance directement l'application via le plugin JavaFX Maven en gérant le *module-path* automatiquement. |
| **Package** | `mvn clean package` | Compile, teste et génère le fichier exécutable monolithique `.jar` autonome dans le dossier `target/`. |
| **Run JAR** | `java -jar target/stylometrie-1.0-SNAPSHOT.jar` | Exécute le package JAR compilé indépendamment de Maven ou de l'IDE. |
| **Reset DB (Win)** | `Remove-Item stylometrie.db` | Supprime le fichier de base de données SQLite sous Windows PowerShell pour réinitialiser les données de test. |
| **Reset DB (Unix)**| `rm -f stylometrie.db` | Supprime la base de données SQLite sous Linux / macOS pour repartir d'une base vierge. |

---

## 💻 Guide des Commandes par Système d'Exploitation

### 🪟 Windows (Invite de commande / PowerShell)

```powershell
# 1. Nettoyer les fichiers temporaires et compiler le projet
mvn clean compile

# 2. Lancer l'application immédiatement
mvn javafx:run

# 3. Créer le package JAR exécutable autonome
mvn clean package

# 4. Lancer le JAR autonome généré
java -jar target/stylometrie-1.0-SNAPSHOT.jar

# 5. Réinitialiser la base de données SQLite locale
Remove-Item stylometrie.db -ErrorAction SilentlyContinue
```

### 🐧 Linux (Bash / Terminal)

```bash
# 1. Accorder les droits d'exécution au wrapper (si utilisé)
chmod +x mvnw

# 2. Nettoyer et recompiler les sources
mvn clean compile

# 3. Exécuter l'application JavaFX
mvn javafx:run

# 4. Compiler et empaqueter au format JAR
mvn clean package
java -jar target/stylometrie-1.0-SNAPSHOT.jar

# 5. Supprimer la base SQLite de test
rm -f stylometrie.db
```

### 🍏 macOS (Terminal zsh)

```zsh
# 1. Nettoyer et compiler le projet
mvn clean compile

# 2. Lancer l'application
mvn javafx:run

# 3. Créer le package JAR
mvn clean package

# 4. Exécuter le JAR produit
java -jar target/stylometrie-1.0-SNAPSHOT.jar

# 5. Réinitialiser la base SQLite
rm -f stylometrie.db
```

---

## 🚀 Lancement sous IntelliJ IDEA

1. Ouvre le dossier du projet sous IntelliJ IDEA.
2. Ouvre la classe `src/main/java/fr/unicaen/Main.java`.
3. Effectue un clic droit sur le fichier -> **Run 'Main.main()'**.

---

## 🎓 Auteurs & Contextualisation

Projet réalisé dans le cadre du cours de **Technologies du Langage (TAL)** — **L3 Informatique, Université de Caen Normandie (UNICAEN)**.