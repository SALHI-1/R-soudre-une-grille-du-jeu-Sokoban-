package com.fstt.devoir;

import java.util.Objects;

/**
 * Représente une caisse avec sa position (ligne, colonne) et son identifiant (lettre).
 * Utile pour suivre chaque caisse séparément (par ex. 'a', 'b', 'c', 'd').
 */

class BoxPosition implements Comparable<BoxPosition> {
    public final int r;
    public final int c;
    public final char name; // 'a', 'b', 'c', ou 'd'

    public BoxPosition(int r, int c, char name) {
        this.r = r;
        this.c = c;
            // Stocke le nom en minuscule pour un traitement uniforme (ex: 'A' -> 'a')
        this.name = (name >= 'A' && name <= 'D') ? SokobanSolver.BOX_TO_TARGET_MAP.get(name) : name;
    }

    // equals et hashCode utilisés par les collections (ex: HashSet).
    // Deux BoxPosition sont égales si elles ont la même position ET le même nom.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoxPosition that = (BoxPosition) o;
        return r == that.r && c == that.c && name == that.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, c, name);
    }

    /**
     * Permet d'ordonner les caisses de façon stable.
     * Tri par ordre : nom (lettre), ligne, colonne.
     * Ceci garantit un ordre prévisible lors de la création de clés uniques.
     */
    @Override
    public int compareTo(BoxPosition other) {
        if (this.name != other.name) {
            return Character.compare(this.name, other.name);
        }
        if (this.r != other.r) {
            return Integer.compare(this.r, other.r);
        }
        return Integer.compare(this.c, other.c);
    }
}