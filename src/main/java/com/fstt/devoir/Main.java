package com.fstt.devoir;

import java.util.LinkedList;
import java.util.List;

/**
 * Point d'entrée du programme et script de démonstration.
 * Définit des niveaux d'exemple, lance le solveur et affiche les résultats.
 */
public class Main {

    /**
     * Exécution principale : lance les simulations de test.
     */
    public static void main(String[] args) {
        // les tests donnée dans le devoire
        System.out.println("--- Résolution Sokoban: 1er test ---");
        // NOTE: Les caisses '$' ont été renommées 'a', 'b', 'c', 'd'
        // pour correspondre à la logique du solveur qui suit les caisses individuellement.
        String[] test1 = {
                "■■■■■■■■■■",
                "■□□□□□□□□■",
                "■□■■□■■□□■",
                "■□a□T□b□□■",
                "■□■□@□■□□■",
                "■□c□T□d□□■",
                "■□■■□■■□□■",
                "■□□T□□T□□■",
                "■□□□□□□□□■",
                "■■■■■■■■■■"
        };
        // Appel de la méthode statique 'solve' de la classe SokobanSolver
        simuler(test1, "Test 1");

        System.out.println("\n--- Résolution Sokoban 2ème test ---");
        String[] test2 = {
                "■■■■■■■■■■",
                "■T□■□□■□T■",
                "■□■a□□b■□■",
                "■□■□□□□■□■",
                "■□□□@□□□□■",
                "■□■□□□□■□■",
                "■□■c□□d■□■",
                "■T□■□□■□T■",
                "■□□□□□□□□■",
                "■■■■■■■■■■"
        };
        // Appel de la méthode statique 'solve' de la classe SokobanSolver
        simuler(test2, "Test 2");
    }

    /**
     * Lance la résolution d'un niveau et affiche les informations utiles :
     * temps écoulé, si une solution a été trouvée et reconstruction du chemin.
     */
    private static void simuler(String[] grille, String nomTest) {

    // 1) mesurer le temps de résolution
        long startTime = System.currentTimeMillis();

    // 2) lancer le solveur (retourne l'état final si trouvé)
        Etat etatFinal = SokobanSolver.solve(grille);

        long endTime = System.currentTimeMillis();

        // 3) afficher les résultats et métriques
        if (etatFinal != null) {
            System.out.println("\n" + nomTest + " - Solution trouvée!");
            // Métrique: Temps de résolution
            System.out.println("Temps de résolution: " + (endTime - startTime) + " ms");
            // Métrique: Nombre de nœuds explorés (géré par solve())

            // 4. Reconstruire et afficher le chemin
            reconstruireChemin(etatFinal);

        } else {
            System.out.println("\n" + nomTest + " - Solution non trouvée!");
        }
    }

    /**
     * Reconstruit la séquence d'états à partir de l'état final et affiche
     * seulement les étapes où une poussée a eu lieu, avec la grille.
     */
    private static void reconstruireChemin(Etat etatFinal) {
        LinkedList<Etat> solutionEtats = new LinkedList<>();
        Etat courant = etatFinal;

        // remonter la liste parentale jusqu'à l'état initial
        while (courant != null) {
            solutionEtats.addFirst(courant);
            courant = courant.parent;
        }

    // afficher la longueur optimale (g_cost = nombre de poussées)
        System.out.println("Longueur de la solution optimale: " + etatFinal.g_cost + " poussées");
        System.out.println("Chemin des poussées avec visualisation:");

        int pushCount = 0;
        for (Etat etat : solutionEtats) {
            // afficher l'état initial
            if (etat.parent == null) {
                System.out.println("\n--- ÉTAT INITIAL ---");
                SokobanSolver.displayBoard(etat.board);
            }
            // n'afficher que les états résultant d'une poussée (les autres sont des mouvements gratuits)
            else if (etat.action != null && etat.action.startsWith("PUSH")) {
                pushCount++;
                System.out.println("\n" + pushCount + ". " + etat.action);
                SokobanSolver.displayBoard(etat.board);
            }
        }
        System.out.println("\n--- FIN DE LA SOLUTION ---");
    }
}