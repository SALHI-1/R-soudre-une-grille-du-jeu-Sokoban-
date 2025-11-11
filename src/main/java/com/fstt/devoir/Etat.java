package com.fstt.devoir;

import java.util.*;

/**
 * Représente un état du plateau pour l'algorithme A*.
 * Un état contient :
 * - la position du joueur,
 * - l'ensemble des caisses (position + nom),
 * - la grille visuelle locale et les coûts A* (g, h, f),
 * ainsi que le parent et l'action utilisée pour arriver ici.
 */
class Etat implements Comparable<Etat> {

    // --- Données partagées entre tous les états ---
    // La grille statique (murs et cibles) ne change pas pendant la recherche.
    private static char[][] STATIC_BOARD;
    // Positions des cibles sur le plateau (constantes pour le niveau).
    private static Set<Position> TARGETS = new HashSet<>();
    private static int ROWS;
    private static int COLS;

    // --- État spécifique à cette instance ---
    // 'board' contient l'affichage local (joueur, caisses) basé sur STATIC_BOARD.
    public char[][] board; // grille affichée pour cet état
    public int playerR, playerC; // position du joueur
    // ensemble immuable (conceptuellement) des caisses : position + nom
    public Set<BoxPosition> boxCoords;

    // --- Coûts pour A* ---
    // g_cost : coût réel depuis l'état initial (ici le nombre de poussées)
    // h_cost : heuristique estimée (somme des distances de Manhattan)
    // f_cost : g + h, utilisé pour ordonner la PriorityQueue
    public int g_cost;
    public int h_cost;
    public int f_cost;

    // --- Traçabilité pour reconstruire la solution ---
    // 'parent' pointe vers l'état précédent. 'action' décrit le mouvement (ex: "PUSH 'a' UP").
    public Etat parent;
    public String action;

    /**
    * Constructeur initial à partir de la représentation textuelle du niveau.
    * Sépare les éléments statiques (murs, cibles) des éléments mobiles (joueur, caisses)
    * et initialise les coûts A*.
     */
    public Etat(String[] level) {
        this.ROWS = level.length;
        this.COLS = level[0].length();
        this.board = new char[ROWS][COLS];
        this.boxCoords = new HashSet<>();

        // Initialisation unique des structures statiques (plateau et cibles)
        // lors de la création du premier Etat pour ce niveau.
        if (STATIC_BOARD == null) {
            STATIC_BOARD = new char[ROWS][COLS];

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    char cell = level[r].charAt(c);

                    if (cell == SokobanSolver.WALL) {
                        STATIC_BOARD[r][c] = SokobanSolver.WALL;
                    } else if (cell == SokobanSolver.TARGET || cell == SokobanSolver.PLAYER_ON_TARGET || (cell >= 'A' && cell <= 'D')) {
                        // cible ou objet posé sur une cible -> marquer comme TARGET
                        STATIC_BOARD[r][c] = SokobanSolver.TARGET;
                        TARGETS.add(new Position(r, c));
                    } else {
                        // sol libre
                        STATIC_BOARD[r][c] = SokobanSolver.FLOOR;
                    }
                }
            }
        }

        // Analyse les éléments MOBILES (Joueur, Caisses)
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                char cell = level[r].charAt(c);

                if (cell == SokobanSolver.PLAYER || cell == SokobanSolver.PLAYER_ON_TARGET) {
                    // position du joueur (sur cible ou non)
                    this.playerR = r;
                    this.playerC = c;
                    this.board[r][c] = STATIC_BOARD[r][c] == SokobanSolver.TARGET ? SokobanSolver.PLAYER_ON_TARGET : SokobanSolver.PLAYER;
                } else if (SokobanSolver.isBoxSymbol(cell)) {
                    // caisse trouvée : l'ajouter à l'ensemble avec son nom
                    this.boxCoords.add(new BoxPosition(r, c, SokobanSolver.getBoxName(cell)));
                    // afficher la caisse (majuscule si sur cible)
                    this.board[r][c] = STATIC_BOARD[r][c] == SokobanSolver.TARGET ? SokobanSolver.BOX_TO_TARGET_MAP.get(SokobanSolver.getBoxName(cell)) : SokobanSolver.getBoxName(cell);
                } else {
                    // mur ou sol statique -> utiliser STATIC_BOARD
                    this.board[r][c] = STATIC_BOARD[r][c];
                }
            }
        }

        // Coûts initiaux : aucune poussée (g=0), heuristique calculée
        this.g_cost = 0;
        this.h_cost = calculateHeuristic();
        this.f_cost = this.g_cost + this.h_cost;
        this.parent = null;
        this.action = null;
    }

    /**
     * Copie profonde d'un état existant. Utilisé pour générer successeurs.
     * Le champ 'parent' est fixé à l'état source.
     */
    public Etat(Etat other) {
        this.parent = other; // Le parent est l'état 'other'
        this.g_cost = other.g_cost; // g_cost sera incrémenté si PUSH

        // Copie simple
        this.playerR = other.playerR;
        this.playerC = other.playerC;

        // Copie profonde de la grille (board)
        this.board = new char[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            System.arraycopy(other.board[i], 0, this.board[i], 0, COLS);
        }

        // Copier les caisses (BoxPosition est immuable dans notre conception)
        this.boxCoords = new HashSet<>();
        for (BoxPosition box : other.boxCoords) {
            this.boxCoords.add(box);
        }
    }

    /**
     * Crée une clé texte unique pour l'état : position du joueur + positions triées des caisses.
     * Cette clé sert à détecter les états déjà visités (closedList).
     */
    public String getUniqueKey() {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(playerR).append(",").append(playerC);

    // On tri les caisses pour garantir qu'une même configuration
    // produira toujours la même clé, indépendamment de l'ordre interne.
        List<BoxPosition> sortedBoxes = new ArrayList<>(boxCoords);
        Collections.sort(sortedBoxes); // Utilise 'compareTo' de BoxPosition

        for (BoxPosition c : sortedBoxes) {
            sb.append("_B").append(c.name).append("(").append(c.r).append(",").append(c.c).append(")");
        }
        return sb.toString();
    }

    /**
     * Retourne vrai si toutes les caisses sont placées sur des cibles.
     */
    public boolean isGoal() {
        // Le but est atteint si TOUTES les positions des caisses
        // sont DANS l'ensemble des positions des CIBLES.
        for (BoxPosition box : boxCoords) {
            if (!TARGETS.contains(new Position(box.r, box.c))) {
                return false; // Une caisse n'est pas sur une cible
            }
        }
        return true; // Toutes les caisses sont sur des cibles
    }

    /**
     * Heuristique : somme des distances de Manhattan entre chaque caisse
     * et la cible la plus proche. Estimation optimiste du nombre de poussées restantes.
     */
    private int calculateHeuristic() {
        int h = 0;

        // Pour chaque caisse...
        for (BoxPosition box : boxCoords) {
            int minManhattan = Integer.MAX_VALUE;

            // ...trouver la distance à la CIBLE la plus proche
            for (Position target : TARGETS) {
                // Si la caisse est déjà sur cette cible, la distance est 0
                if (box.r == target.r && box.c == target.c) {
                    minManhattan = 0;
                    break;
                }

                // Distance de Manhattan: |x1 - x2| + |y1 - y2|
                int manhattan = Math.abs(box.r - target.r) + Math.abs(box.c - target.c);
                if (manhattan < minManhattan) {
                    minManhattan = manhattan;
                }
            }
            h += minManhattan;
        }
        return h;
    }

    /**
     * Génère les états accessibles depuis celui-ci en un mouvement du joueur.
     * Peut produire :
     * - un 'MOVE' (déplacement sans pousser) : coût g inchangé,
     * - un 'PUSH' (pousser une caisse) : incrémente g de 1.
     */
    public List<Etat> generateSuccessors() {
        List<Etat> successors = new ArrayList<>();

        // Itération sur les 4 directions (Haut, Bas, Gauche, Droite)
        for (int i = 0; i < 4; i++) {
            int dr = SokobanSolver.DIRS[i][0];
            int dc = SokobanSolver.DIRS[i][1];
            String actionDirection = SokobanSolver.DIR_NAMES[i];

            // Case adjacente vers laquelle le joueur souhaite se déplacer
            int nextPR = playerR + dr;
            int nextPC = playerC + dc;

            // Si c'est un mur, mouvement impossible
            if (STATIC_BOARD[nextPR][nextPC] == SokobanSolver.WALL) {
                continue;
            }

            char cellAtNextPos = this.board[nextPR][nextPC];

            // --- CAS 1 : pousser une caisse (si la case adjacente contient une caisse)
            if (SokobanSolver.isBoxSymbol(cellAtNextPos)) {

                // Position derrière la caisse (cible de la poussée)
                int targetR = nextPR + dr;
                int targetC = nextPC + dc;

        // Vérifier que la case derrière la caisse est libre (pas mur, pas autre caisse)
                if (STATIC_BOARD[targetR][targetC] != SokobanSolver.WALL &&
                        !SokobanSolver.isBoxSymbol(this.board[targetR][targetC]))
                {
                    // --- Poussée valide ---
                    Etat newState = new Etat(this); // Crée une copie

                    // Une poussée coûte 1 (g_cost représente le nombre de poussées)
                    newState.g_cost += 1;

                    // Trouver la caisse qui est poussée
                    BoxPosition boxToPush = null;
                    for(BoxPosition b : newState.boxCoords) {
                        if(b.r == nextPR && b.c == nextPC) {
                            boxToPush = b;
                            break;
                        }
                    }

                    newState.action = "PUSH '" + boxToPush.name + "' " + actionDirection;

                    // 1) Mettre à jour l'ensemble des caisses : retirer l'ancienne et ajouter la nouvelle
                    newState.boxCoords.remove(boxToPush);
                    newState.boxCoords.add(new BoxPosition(targetR, targetC, boxToPush.name));

                    // 2) Mettre à jour la position du joueur (il se place là où était la caisse)
                    newState.playerR = nextPR;
                    newState.playerC = nextPC;

                    // 3) Mettre à jour la grille affichée :
                    // A) ancienne position du joueur redevient sol ou cible
                    newState.board[playerR][playerC] = STATIC_BOARD[playerR][playerC]; // (devient T ou □)

                    // B) l'emplacement de la caisse devient la position du joueur
                    newState.board[nextPR][nextPC] = (STATIC_BOARD[nextPR][nextPC] == SokobanSolver.TARGET) ? SokobanSolver.PLAYER_ON_TARGET : SokobanSolver.PLAYER;

            // C) nouvelle position de la caisse (majuscule si sur cible)
                    newState.board[targetR][targetC] = (STATIC_BOARD[targetR][targetC] == SokobanSolver.TARGET)
                            ? SokobanSolver.BOX_TO_TARGET_MAP.get(boxToPush.name) // (devient 'A', 'B'...)
                            : boxToPush.name; // (devient 'a', 'b'...)

                    // 4) recalculer heuristique et coût total
                    newState.h_cost = newState.calculateHeuristic();
                    newState.f_cost = newState.g_cost + newState.h_cost;

                    successors.add(newState);
                }
            }
            // --- CAS 2 : déplacement simple du joueur (MOVE) ---
            else {
                Etat newState = new Etat(this); // Crée une copie
                newState.action = "MOVE " + actionDirection; // Action (pas de coût)

                // Les déplacements sans pousser ne changent pas le coût g (on ne compte que les poussées)

                // Mettre à jour la position du joueur sur la copie
                newState.playerR = nextPR;
                newState.playerC = nextPC;

                // Mettre à jour la grille affichée : ancienne et nouvelle position du joueur
                newState.board[playerR][playerC] = STATIC_BOARD[playerR][playerC]; // (devient T ou □)
                // B. Nouvelle position du Joueur
                newState.board[nextPR][nextPC] = (STATIC_BOARD[nextPR][nextPC] == SokobanSolver.TARGET) ? SokobanSolver.PLAYER_ON_TARGET : SokobanSolver.PLAYER;

                // recalculer h et f (h reste identique si seules les cases du joueur changent)
                newState.h_cost = newState.calculateHeuristic();
                newState.f_cost = newState.g_cost + newState.h_cost;

                successors.add(newState);
            }
        }
        return successors;
    }

    /**
     * Comparateur pour la PriorityQueue.
     * Compare les états par 'f_cost' (le plus bas en premier).
     */
    @Override
    public int compareTo(Etat other) {
        return Integer.compare(this.f_cost, other.f_cost);
    }
}