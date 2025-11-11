package com.fstt.devoir;

import java.util.*;

/**
 * Logique centrale du solveur Sokoban.
 * Contient :
 * - les symboles et utilitaires du jeu,
 * - la méthode A* ('solve') qui recherche la solution minimale en nombre de poussées,
 * - quelques fonctions d'affichage et d'aide.
 */
public class SokobanSolver {

    // --- Symboles utilisés dans la grille ---
    public static final char WALL = '■';
    public static final char TARGET = 'T';
    public static final char PLAYER = '@';
    public static final char FLOOR = '□';
    public static final char PLAYER_ON_TARGET = '+';

    // Les caisses dans un emplacement autre que la cible (nommées a, b, c, d)
    public static final char[] BOX_NAMES = {'a', 'b', 'c', 'd'};
    // Les caisses DANS leurs cibles (en majuscules)
    public static final char[] BOX_ON_TARGET_NAMES = {'A', 'B', 'C', 'D'};

    // Map pour convertir entre casse minuscule et majuscule des caisses
    public static final Map<Character, Character> BOX_TO_TARGET_MAP = Map.of(
            'a', 'A', 'b', 'B', 'c', 'C', 'd', 'D',
            'A', 'a', 'B', 'b', 'C', 'c', 'D', 'd'
    );
    // Ensemble rapide pour tester si un caractère représente une caisse
    public static final Set<Character> ALL_BOX_SYMBOLS = new HashSet<>(Arrays.asList('a', 'b', 'c', 'd', 'A', 'B', 'C', 'D'));

    // --- Directions de déplacement (delta ligne, delta colonne) ---
    public static final int[][] DIRS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };
    // Noms correspondants pour l'affichage des actions
    public static final String[] DIR_NAMES = {
            "UP", "DOWN", "LEFT", "RIGHT"
    };

    /**
    * Résout le niveau avec A* en minimisant le nombre de poussées.
    * @param level tableau de chaînes représentant la grille initiale
    * @return l'état final gagnant (si trouvé) ou null sinon
     */
    static Etat solve(String[] level) {

        // 1. Initialisation
        // Crée l'état initial en analysant la grille
        Etat etatInitial = new Etat(level);
        if (etatInitial.isGoal()) {
            System.out.println("Niveau déjà résolu!");
            return etatInitial;
        }

    // openList : PriorityQueue triée par f_cost (g + h)
        PriorityQueue<Etat> openList = new PriorityQueue<>(Comparator.comparingInt(s -> s.f_cost));

    // closedList : ensemble des clés uniques d'états déjà visités
        Set<String> closedList = new HashSet<>();

        openList.add(etatInitial);
        int exploredNodes = 0; // Métrique: Nombre de nœuds explorés

        // Boucle principale A* : on explore tant qu'il y a des états ouverts
        while (!openList.isEmpty()) {
            // Prendre le meilleur état (celui avec le plus petit f_cost)
            Etat current = openList.poll();
            exploredNodes++;

            // Ignorer si la configuration a déjà été traitée
            if (closedList.contains(current.getUniqueKey())) {
                continue;
            }

            // 3. Vérification de la Victoire
            if (current.isGoal()) {
                // Métrique: Nombre de nœuds explorés
                System.out.println("Nombre de nœuds explorés par A*: " + exploredNodes);
                return current; // Solution trouvée!
            }

            // Marquer l'état courant comme visité
            closedList.add(current.getUniqueKey());

            // Générer tous les successeurs (mouvements et poussées)
            for (Etat nextState : current.generateSuccessors()) {
                if (!closedList.contains(nextState.getUniqueKey())) {
                    openList.add(nextState);
                }
            }
        }

        // 6. Échec
        System.out.println("Nombre de nœuds explorés par A*: " + exploredNodes);
        return null; // Solution non trouvée
    }

    /**
     * Affiche la grille ligne par ligne.
     */
    public static void displayBoard(char[][] board) {
        for (char[] row : board) {
            for (char cell : row) {
                System.out.print(cell);
            }
            System.out.println();
        }
    }

    /**
     * Vérifie que (r,c) est dans les limites fournies.
     */
    public static boolean isValid(int r, int c, int rows, int cols) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    /**
     * Retourne vrai si le caractère représente une caisse (minuscule ou majuscule).
     */
    public static boolean isBoxSymbol(char c) {
        return ALL_BOX_SYMBOLS.contains(c);
    }

    /**
     * Renvoie le nom de base (minuscule) d'une caisse donnée en majuscule ou minuscule.
     */
    public static char getBoxName(char c) {
        if (c >= 'a' && c <= 'd') return c;
        if (c >= 'A' && c <= 'D') return SokobanSolver.BOX_TO_TARGET_MAP.get(c);
        return ' '; // Ne devrait pas arriver
    }
}