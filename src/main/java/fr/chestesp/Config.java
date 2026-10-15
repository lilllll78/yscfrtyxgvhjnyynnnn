package fr.chestesp;

/** Options modifiables depuis le menu. */
public final class Config {
    public static boolean chestEsp = true;
    public static boolean anchorEsp = true;
    public static boolean playerEsp = true;
    public static boolean tracers = true;
    public static boolean anchorFarm = false;
    /** Rotations fluides, delais de reaction, petites pauses. */
    public static boolean human = true;
    // --- PvP / HUD ---
    public static boolean hudFps = true;
    public static boolean hudCps = true;
    public static boolean hudCoords = false;
    public static boolean hudKeystrokes = false;
    public static boolean hudArmor = true;
    public static boolean toggleSprint = false;
    public static boolean zoom = true;

    /** Ramasse seul les ancres cassees qui tombent pres de toi. */
    public static boolean autoPickup = true;
    /** Plus d'ancres -> /rtp. */
    public static boolean autoRtp = true;
    /** Danger -> /home 1. */
    public static boolean autoHome = true;

    /** Portee d'affichage de l'ESP (blocs). */
    public static double espRange = 128.0;
    /** Portee de recherche de l'AutoFarm (blocs). */
    public static double farmRange = 128.0;
    /** Vie (en demi-coeurs) en dessous de laquelle on envoie /home 1. */
    public static float homeHp = 6f;

    private Config() {}
}
