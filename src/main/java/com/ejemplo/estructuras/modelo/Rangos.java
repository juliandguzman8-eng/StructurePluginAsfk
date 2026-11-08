package com.ejemplo.estructuras.modelo;

import java.util.Random;

/** Lee valores de configuracion que pueden ser un numero ("3") o un rango ("1-4"). */
public final class Rangos {

    private Rangos() {
    }

    /** Devuelve {min, max}. Acepta Integer, "3" o "1-4". */
    public static int[] leer(Object valor, int minPorDefecto, int maxPorDefecto) {
        if (valor == null) {
            return new int[]{minPorDefecto, maxPorDefecto};
        }
        if (valor instanceof Number numero) {
            int n = numero.intValue();
            return new int[]{n, n};
        }
        String texto = valor.toString().trim();
        try {
            int guion = texto.indexOf('-', 1);
            if (guion > 0) {
                int min = Integer.parseInt(texto.substring(0, guion).trim());
                int max = Integer.parseInt(texto.substring(guion + 1).trim());
                return new int[]{Math.min(min, max), Math.max(min, max)};
            }
            int n = Integer.parseInt(texto);
            return new int[]{n, n};
        } catch (NumberFormatException e) {
            return new int[]{minPorDefecto, maxPorDefecto};
        }
    }

    public static int aleatorio(Random rnd, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + rnd.nextInt(max - min + 1);
    }
}
