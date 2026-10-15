package fr.chestesp;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/** Menu principal : onglets, interrupteurs animes, curseurs, boutons. Souris + clavier. */
public class EspMenuScreen extends Screen {

    private static int lastTab = 0;
    private static final String[] TAB_NAMES = {"ESP", "PvP", "AutoFarm", "Build", "Aide"};

    private int tab = lastTab;
    private int scroll = 0;
    private int selected = -1;
    private SliderRow dragging = null;
    private final List<List<Row>> tabs = new ArrayList<>();

    private int wx, wy, ww, wh;      // fenetre
    private int cx, cy, cw, ch;      // zone de contenu

    public EspMenuScreen() {
        super(Text.literal("RoseClient"));
    }

    // ================================================================ lignes

    private interface Row {
        int height();
        void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel);
        default boolean click(double mx, double my, int x, int y, int w) { return false; }
        default void activate() {}
        default void adjust(int dir) {}
        default boolean selectable() { return true; }
    }

    private static final class HeaderRow implements Row {
        private final String text;
        HeaderRow(String text) { this.text = text; }
        public int height() { return 18; }
        public boolean selectable() { return false; }
        public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel) {
            c.drawText(tr, text.toUpperCase(), x + 2, y + 8, Ui.ACCENT2, true);
            c.fill(x, y + 17, x + w, y + 18, Ui.BORDER);
        }
    }

    private static final class InfoRow implements Row {
        private final Supplier<String> text;
        private final int color;
        InfoRow(Supplier<String> text, int color) { this.text = text; this.color = color; }
        public int height() { return 14; }
        public boolean selectable() { return false; }
        public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel) {
            c.drawText(tr, text.get(), x + 4, y + 3, color, true);
        }
    }

    private static final class ToggleRow implements Row {
        private final String label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float anim = -1f;
        ToggleRow(String label, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = label; this.get = get; this.set = set;
        }
        public int height() { return 22; }
        public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel) {
            Ui.rowBg(c, x, y, w, 22, hover, sel);
            boolean on = get.getAsBoolean();
            if (anim < 0) anim = on ? 1f : 0f;
            anim += ((on ? 1f : 0f) - anim) * 0.35f;
            c.drawText(tr, label, x + 8, y + 7, Ui.TEXT, true);
            int sw = 30, sh = 12, sx = x + w - sw - 8, sy = y + 5;
            c.fill(sx, sy, sx + sw, sy + sh, Ui.lerp(Ui.OFF, Ui.ACCENT, anim));
            int kx = sx + 1 + Math.round(anim * (sw - sh));
            c.fill(kx, sy + 1, kx + sh - 2, sy + sh - 1, 0xFFFFFFFF);
            String st = on ? "ON" : "OFF";
            c.drawText(tr, st, sx - tr.getWidth(st) - 6, y + 7, on ? Ui.GREEN : Ui.MUTED, true);
        }
        public boolean click(double mx, double my, int x, int y, int w) { activate(); return true; }
        public void activate() { set.accept(!get.getAsBoolean()); }
        public void adjust(int dir) { set.accept(dir > 0); }
    }

    private static final class SliderRow implements Row {
        private final String label, unit;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private final double min, max, step;
        private int lastX, lastW;
        SliderRow(String label, DoubleSupplier get, DoubleConsumer set, double min, double max, double step, String unit) {
            this.label = label; this.get = get; this.set = set; this.min = min; this.max = max; this.step = step; this.unit = unit;
        }
        public int height() { return 32; }
        public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel) {
            Ui.rowBg(c, x, y, w, 32, hover, sel);
            double v = get.getAsDouble();
            float t = (float) ((v - min) / (max - min));
            c.drawText(tr, label, x + 8, y + 5, Ui.TEXT, true);
            String vs = ((int) Math.round(v)) + unit;
            c.drawText(tr, vs, x + w - 8 - tr.getWidth(vs), y + 5, Ui.ACCENT2, true);
            int tx = x + 8, tw = w - 16, ty = y + 21;
            lastX = tx; lastW = tw;
            c.fill(tx, ty, tx + tw, ty + 4, Ui.OFF);
            c.fill(tx, ty, tx + (int) (tw * t), ty + 4, Ui.ACCENT);
            int kx = tx + (int) (tw * t);
            c.fill(kx - 3, ty - 3, kx + 3, ty + 7, 0xFFFFFFFF);
        }
        void setFromMouse(double mx) {
            if (lastW <= 0) return;
            double t = Math.max(0.0, Math.min(1.0, (mx - lastX) / lastW));
            double v = min + t * (max - min);
            v = Math.round(v / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }
        public boolean click(double mx, double my, int x, int y, int w) { setFromMouse(mx); return true; }
        public void adjust(int dir) { set.accept(Math.max(min, Math.min(max, get.getAsDouble() + dir * step))); }
    }

    private static final class ButtonRow implements Row {
        private final Supplier<String> label;
        private final Runnable action;
        private final int color;
        ButtonRow(Supplier<String> label, int color, Runnable action) {
            this.label = label; this.color = color; this.action = action;
        }
        public int height() { return 22; }
        public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel) {
            Ui.rowBg(c, x, y, w, 22, hover, sel);
            c.fill(x, y, x + 2, y + 22, color);
            String s = label.get();
            c.drawText(tr, s, x + 10, y + 7, Ui.TEXT, true);
            c.drawText(tr, ">", x + w - 14, y + 7, color, true);
        }
        public boolean click(double mx, double my, int x, int y, int w) { action.run(); return true; }
        public void activate() { action.run(); }
    }

    private static final class StepRow implements Row {
        private final String label;
        private final Runnable minus, plus;
        StepRow(String label, Runnable minus, Runnable plus) { this.label = label; this.minus = minus; this.plus = plus; }
        public int height() { return 22; }
        public void render(DrawContext c, TextRenderer tr, int x, int y, int w, boolean hover, boolean sel) {
            Ui.rowBg(c, x, y, w, 22, hover, sel);
            c.drawText(tr, label, x + 8, y + 7, Ui.TEXT, true);
            c.fill(x + w - 54, y + 4, x + w - 32, y + 18, Ui.OFF);
            c.fill(x + w - 28, y + 4, x + w - 6, y + 18, Ui.ACCENT);
            c.drawText(tr, "-", x + w - 46, y + 7, 0xFFFFFFFF, true);
            c.drawText(tr, "+", x + w - 20, y + 7, 0xFFFFFFFF, true);
        }
        public boolean click(double mx, double my, int x, int y, int w) {
            if (mx >= x + w - 54 && mx <= x + w - 32) { minus.run(); return true; }
            if (mx >= x + w - 28 && mx <= x + w - 6) { plus.run(); return true; }
            return false;
        }
        public void adjust(int dir) { if (dir > 0) plus.run(); else minus.run(); }
    }

    // ============================================================ contenu

    private BlockPos aimedAdjacent() {
        if (this.client.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            return b.getBlockPos().offset(b.getSide());
        }
        return this.client.player != null ? this.client.player.getBlockPos() : null;
    }

    private void nudge(int dx, int dy, int dz) {
        if (BuildState.origin != null) BuildState.origin = BuildState.origin.add(dx, dy, dz);
    }

    private void startAndClose(BuildBot.Mode m) {
        if (BuildBot.start(this.client, m)) this.client.setScreen(null);
    }

    @Override
    protected void init() {
        ww = Math.min(470, this.width - 16);
        wh = Math.min(316, this.height - 16);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2;
        cx = wx + 120;
        cy = wy + 36;
        cw = ww - 120 - 10;
        ch = wh - 36 - 10;

        tabs.clear();

        // --- ESP
        List<Row> esp = new ArrayList<>();
        esp.add(new HeaderRow("Affichage"));
        esp.add(new ToggleRow("Chest ESP", () -> Config.chestEsp, v -> Config.chestEsp = v));
        esp.add(new ToggleRow("Anchor ESP", () -> Config.anchorEsp, v -> Config.anchorEsp = v));
        esp.add(new ToggleRow("Player ESP", () -> Config.playerEsp, v -> Config.playerEsp = v));
        esp.add(new ToggleRow("Lignes (tracers)", () -> Config.tracers, v -> Config.tracers = v));
        esp.add(new HeaderRow("Portée"));
        esp.add(new SliderRow("Portée de l'ESP", () -> Config.espRange, v -> Config.espRange = v, 32, 256, 8, " blocs"));
        tabs.add(esp);

        // --- PvP (HUD et confort)
        List<Row> pvp = new ArrayList<>();
        pvp.add(new HeaderRow("Affichage (HUD)"));
        pvp.add(new ToggleRow("FPS", () -> Config.hudFps, v -> Config.hudFps = v));
        pvp.add(new ToggleRow("CPS", () -> Config.hudCps, v -> Config.hudCps = v));
        pvp.add(new ToggleRow("Coordonnées", () -> Config.hudCoords, v -> Config.hudCoords = v));
        pvp.add(new ToggleRow("Touches (WASD / souris)", () -> Config.hudKeystrokes, v -> Config.hudKeystrokes = v));
        pvp.add(new ToggleRow("Armure", () -> Config.hudArmor, v -> Config.hudArmor = v));
        pvp.add(new HeaderRow("Confort"));
        pvp.add(new ToggleRow("Sprint automatique", () -> Config.toggleSprint, v -> Config.toggleSprint = v));
        pvp.add(new ToggleRow("Zoom (maintenir C)", () -> Config.zoom, v -> Config.zoom = v));
        pvp.add(new HeaderRow("Joueurs"));
        pvp.add(new ToggleRow("Player ESP", () -> Config.playerEsp, v -> Config.playerEsp = v));
        pvp.add(new ToggleRow("Lignes (tracers)", () -> Config.tracers, v -> Config.tracers = v));
        tabs.add(pvp);

        // --- AutoFarm
        List<Row> farm = new ArrayList<>();
        farm.add(new HeaderRow("Anchor AutoFarm"));
        farm.add(new ToggleRow("AutoFarm", () -> Config.anchorFarm, v -> {
            Config.anchorFarm = v;
            if (v) BuildBot.stop(null);
        }));
        farm.add(new ToggleRow("Mode humain", () -> Config.human, v -> Config.human = v));
        farm.add(new HeaderRow("Automatismes"));
        farm.add(new ToggleRow("Ramassage auto des ancres", () -> Config.autoPickup, v -> Config.autoPickup = v));
        farm.add(new ToggleRow("Auto /rtp (plus d'ancres)", () -> Config.autoRtp, v -> Config.autoRtp = v));
        farm.add(new ToggleRow("Auto /home 1 (danger)", () -> Config.autoHome, v -> Config.autoHome = v));
        farm.add(new SliderRow("Seuil de vie pour /home", () -> Config.homeHp / 2.0,
                v -> Config.homeHp = (float) (v * 2.0), 1, 10, 1, " coeurs"));
        farm.add(new SliderRow("Portée de recherche", () -> Config.farmRange, v -> Config.farmRange = v, 32, 160, 8, " blocs"));
        farm.add(new HeaderRow("État"));
        farm.add(new InfoRow(() -> "AutoFarm : " + (Config.anchorFarm ? "actif" : "inactif"), Ui.TEXT));
        farm.add(new InfoRow(() -> "Ancres détectées : " + ChestEspClient.anchors.size(), Ui.TEXT));
        farm.add(new InfoRow(() -> "Ancres minées (session) : " + AnchorFarm.mined, Ui.TEXT));
        tabs.add(farm);

        // --- Build
        List<Row> build = new ArrayList<>();
        build.add(new HeaderRow("Schéma"));
        build.add(new InfoRow(() -> BuildState.schem == null ? "Aucun schéma chargé"
                : BuildState.schemName + "  (" + BuildState.schem.sx + "x" + BuildState.schem.sy + "x" + BuildState.schem.sz + ")", Ui.MUTED));
        build.add(new InfoRow(() -> BuildState.origin == null ? "Origine : non définie"
                : "Origine : " + BuildState.origin.getX() + " " + BuildState.origin.getY() + " " + BuildState.origin.getZ(), Ui.MUTED));
        build.add(new ButtonRow(() -> "Charger un schéma...", Ui.ACCENT,
                () -> this.client.setScreen(new SchematicListScreen(this))));
        build.add(new ButtonRow(() -> "Origine : bloc visé", Ui.ACCENT, () -> {
            BlockPos p = aimedAdjacent();
            if (p != null) BuildState.origin = p;
        }));
        build.add(new ButtonRow(() -> "Origine : ma position", Ui.ACCENT, () -> {
            if (this.client.player != null) BuildState.origin = this.client.player.getBlockPos();
        }));
        build.add(new StepRow("Origine X", () -> nudge(-1, 0, 0), () -> nudge(1, 0, 0)));
        build.add(new StepRow("Origine Y", () -> nudge(0, -1, 0), () -> nudge(0, 1, 0)));
        build.add(new StepRow("Origine Z", () -> nudge(0, 0, -1), () -> nudge(0, 0, 1)));
        build.add(new ButtonRow(() -> "Rotation : " + BuildState.rot * 90 + "°", Ui.ACCENT,
                () -> BuildState.setRotation(BuildState.rot + 1)));
        build.add(new ToggleRow("Plan fantôme", () -> BuildState.showGhost, v -> BuildState.showGhost = v));
        build.add(new ButtonRow(() -> "Construire le schéma", Ui.GREEN, () -> startAndClose(BuildBot.Mode.SCHEM)));
        build.add(new HeaderRow("Sélection"));
        build.add(new InfoRow(() -> {
            int[] sb = BuildState.selBounds();
            return sb == null ? "Définis 2 coins (touches H et J)"
                    : "Zone : " + (sb[3] - sb[0] + 1) + " x " + (sb[4] - sb[1] + 1) + " x " + (sb[5] - sb[2] + 1);
        }, Ui.MUTED));
        build.add(new ButtonRow(() -> "Coin 1 : bloc visé", Ui.CYAN, () -> BuildState.setCorner(this.client, 1)));
        build.add(new ButtonRow(() -> "Coin 2 : bloc visé", Ui.CYAN, () -> BuildState.setCorner(this.client, 2)));
        build.add(new ButtonRow(() -> "Remplir (bloc en main)", Ui.GREEN, () -> startAndClose(BuildBot.Mode.FILL)));
        build.add(new ButtonRow(() -> "Vider la sélection", Ui.RED, () -> startAndClose(BuildBot.Mode.CLEAR)));
        build.add(new ButtonRow(() -> "Effacer la sélection", Ui.MUTED, () -> {
            BuildState.pos1 = null;
            BuildState.pos2 = null;
        }));
        build.add(new HeaderRow("Contrôle"));
        build.add(new InfoRow(() -> "Build : " + (BuildBot.mode == BuildBot.Mode.NONE ? "arrêté" : BuildBot.mode.toString()), Ui.TEXT));
        build.add(new ButtonRow(() -> "STOP build", Ui.RED, () -> BuildBot.stop("Build arrêté")));
        tabs.add(build);

        // --- Aide
        List<Row> help = new ArrayList<>();
        help.add(new HeaderRow("Touches"));
        help.add(new InfoRow(() -> "G : ouvrir / fermer ce menu", Ui.TEXT));
        help.add(new InfoRow(() -> "H / J : coin 1 / coin 2 (vise un bloc)", Ui.TEXT));
        help.add(new InfoRow(() -> "C (maintenu) : zoom", Ui.TEXT));
        help.add(new HeaderRow("Menu"));
        help.add(new InfoRow(() -> "Souris : clic, glisser les curseurs, molette", Ui.TEXT));
        help.add(new InfoRow(() -> "Clavier : haut/bas, Entrée, gauche/droite, Tab", Ui.TEXT));
        help.add(new HeaderRow("Schémas"));
        help.add(new InfoRow(() -> "Dossier : .minecraft/schematics", Ui.TEXT));
        help.add(new InfoRow(() -> ".litematic  .schem  .nbt", Ui.MUTED));
        help.add(new HeaderRow("ESP"));
        help.add(new InfoRow(() -> "Cyan : ancre   Rouge : joueur", Ui.TEXT));
        help.add(new InfoRow(() -> "Orange : coffre   Violet : ender chest", Ui.TEXT));
        tabs.add(help);

        if (tab >= tabs.size()) tab = 0;
    }

    // ============================================================ rendu

    private int totalHeight(List<Row> rows) {
        int t = 0;
        for (Row r : rows) t += r.height() + 4;
        return Math.max(0, t - 4);
    }

    private void clampScroll() {
        int max = Math.max(0, totalHeight(tabs.get(tab)) - ch);
        scroll = Math.max(0, Math.min(max, scroll));
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, this.width, this.height, Ui.DIM);
        Ui.window(c, wx, wy, ww, wh);

        TextRenderer tr = this.textRenderer;

        // en-tete
        c.fillGradient(wx, wy, wx + ww, wy + 28, 0xFF1A1230, Ui.BG);
        c.fill(wx, wy, wx + ww, wy + 2, Ui.lerp(Ui.ACCENT, Ui.CYAN, Ui.pulse()));
        RoseLogo.draw(c, wx + 10, wy + 6, 1);
        c.drawText(tr, "ROSECLIENT", wx + 30, wy + 10, Ui.lerp(Ui.ACCENT2, 0xFFFFFFFF, Ui.pulse() * 0.4f), true);
        String st = Config.anchorFarm ? "AutoFarm actif" : "AutoFarm inactif";
        c.drawText(tr, st, wx + ww - 30 - tr.getWidth(st), wy + 10, Config.anchorFarm ? Ui.GREEN : Ui.MUTED, true);
        boolean xHover = mx >= wx + ww - 22 && mx <= wx + ww - 6 && my >= wy + 6 && my <= wy + 22;
        c.drawText(tr, "x", wx + ww - 17, wy + 10, xHover ? Ui.RED : Ui.MUTED, true);

        // barre laterale
        c.fill(wx, wy + 28, wx + 112, wy + wh, Ui.SIDE);
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int ty = wy + 40 + i * 26;
            boolean hover = mx >= wx + 8 && mx <= wx + 104 && my >= ty && my <= ty + 22;
            if (i == tab) {
                c.fill(wx + 8, ty, wx + 104, ty + 22, 0x33A855F7);
                c.fill(wx + 8, ty, wx + 10, ty + 22, Ui.ACCENT);
            } else if (hover) {
                c.fill(wx + 8, ty, wx + 104, ty + 22, Ui.ROW_HOVER);
            }
            c.drawText(tr, TAB_NAMES[i], wx + 18, ty + 7, i == tab ? Ui.TEXT : Ui.MUTED, true);
        }
        c.drawText(tr, "Tab : onglet suivant", wx + 8, wy + wh - 16, Ui.MUTED, false);

        // contenu
        clampScroll();
        List<Row> rows = tabs.get(tab);
        c.enableScissor(cx, cy, cx + cw, cy + ch);
        int ry = cy - scroll;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int h = r.height();
            if (ry + h >= cy && ry <= cy + ch) {
                boolean hover = r.selectable() && mx >= cx && mx <= cx + cw && my >= Math.max(ry, cy)
                        && my <= Math.min(ry + h, cy + ch);
                r.render(c, tr, cx, ry, cw, hover, i == selected);
            }
            ry += h + 4;
        }
        c.disableScissor();

        // barre de defilement
        int total = totalHeight(rows);
        if (total > ch) {
            int barH = Math.max(20, ch * ch / total);
            int barY = cy + (int) ((long) (ch - barH) * scroll / Math.max(1, total - ch));
            c.fill(cx + cw + 3, cy, cx + cw + 5, cy + ch, Ui.OFF);
            c.fill(cx + cw + 3, barY, cx + cw + 5, barY + barH, Ui.ACCENT);
        }
    }

    // ========================================================= interactions

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        if (mx >= wx + ww - 22 && mx <= wx + ww - 6 && my >= wy + 6 && my <= wy + 22) {
            this.close();
            return true;
        }
        for (int i = 0; i < TAB_NAMES.length; i++) {
            int ty = wy + 40 + i * 26;
            if (mx >= wx + 8 && mx <= wx + 104 && my >= ty && my <= ty + 22) {
                tab = i;
                lastTab = i;
                scroll = 0;
                selected = -1;
                return true;
            }
        }
        if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch) {
            List<Row> rows = tabs.get(tab);
            int ry = cy - scroll;
            for (int i = 0; i < rows.size(); i++) {
                Row r = rows.get(i);
                int h = r.height();
                if (my >= ry && my <= ry + h) {
                    if (r.selectable()) {
                        selected = i;
                        if (r instanceof SliderRow s) dragging = s;
                        r.click(mx, my, cx, ry, cw);
                    }
                    return true;
                }
                ry += h + 4;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.setFromMouse(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll -= (int) (vertical * 22);
        clampScroll();
        return true;
    }

    private void changeTab(int dir) {
        tab = (tab + dir + TAB_NAMES.length) % TAB_NAMES.length;
        lastTab = tab;
        scroll = 0;
        selected = -1;
    }

    private void moveSelection(int dir) {
        List<Row> rows = tabs.get(tab);
        int i = selected;
        for (int n = 0; n < rows.size(); n++) {
            i += dir;
            if (i < 0 || i >= rows.size()) return;
            if (rows.get(i).selectable()) {
                selected = i;
                ensureVisible(i);
                return;
            }
        }
    }

    private void ensureVisible(int index) {
        List<Row> rows = tabs.get(tab);
        int top = 0;
        for (int i = 0; i < index; i++) top += rows.get(i).height() + 4;
        int bottom = top + rows.get(index).height();
        if (top < scroll) scroll = top;
        else if (bottom > scroll + ch) scroll = bottom - ch;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ChestEspClient.menuKey != null && ChestEspClient.menuKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        List<Row> rows = tabs.get(tab);
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN -> {
                if (selected < 0) { selected = -1; moveSelection(1); } else moveSelection(1);
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                moveSelection(-1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (selected >= 0 && selected < rows.size()) rows.get(selected).activate();
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (selected >= 0 && selected < rows.size()) rows.get(selected).adjust(-1); else changeTab(-1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (selected >= 0 && selected < rows.size()) rows.get(selected).adjust(1); else changeTab(1);
                return true;
            }
            case GLFW.GLFW_KEY_TAB -> {
                changeTab(1);
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
