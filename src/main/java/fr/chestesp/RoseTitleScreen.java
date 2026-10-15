package fr.chestesp;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Ecran titre minimaliste du RoseClient : fond sombre, petales qui tombent, la rose, 4 boutons. */
public class RoseTitleScreen extends Screen {

    private static final String[] ITEMS = {"Solo", "Multijoueur", "Options", "Quitter"};
    private static final int[] PETAL_COLORS = {0x88FB7185, 0x88E11D48, 0x88F9A8D4};

    private int sel = -1;
    private int bx, by;
    private static final int BW = 200, BH = 24, GAP = 6;

    public RoseTitleScreen() {
        super(Text.literal("RoseClient"));
    }

    @Override
    protected void init() {
        bx = this.width / 2 - BW / 2;
        int top = Math.max(8, this.height / 2 - 120);
        by = top + RoseLogo.HEIGHT * 5 + 62;
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        TextRenderer tr = this.textRenderer;
        long t = System.currentTimeMillis();

        c.fillGradient(0, 0, this.width, this.height, 0xFF0B0813, 0xFF1A1030);

        // petales
        for (int i = 0; i < 36; i++) {
            double sx = ((i * 7919) % 1000) / 1000.0 * this.width;
            double speed = 0.018 + ((i * 31) % 7) * 0.006;
            double y = ((t * speed + i * 61) % (this.height + 20)) - 10;
            double x = sx + Math.sin(t / 900.0 + i) * 12;
            int size = 2 + (i % 3);
            c.fill((int) x, (int) y, (int) x + size, (int) y + size, PETAL_COLORS[i % 3]);
        }

        int top = Math.max(8, this.height / 2 - 120);
        int scale = 5;
        RoseLogo.draw(c, this.width / 2 - RoseLogo.WIDTH * scale / 2, top, scale);

        // titre (x2)
        int ty = top + RoseLogo.HEIGHT * scale + 10;
        c.getMatrices().push();
        c.getMatrices().scale(2f, 2f, 1f);
        c.drawCenteredTextWithShadow(tr, "ROSECLIENT", (int) (this.width / 4f), ty / 2,
                Ui.lerp(Ui.ACCENT2, 0xFFFFFFFF, Ui.pulse() * 0.35f));
        c.getMatrices().pop();
        c.drawCenteredTextWithShadow(tr, Text.literal("minimal  -  pvp  -  build"), this.width / 2, ty + 24, Ui.MUTED);

        // boutons
        for (int i = 0; i < ITEMS.length; i++) {
            int y = by + i * (BH + GAP);
            boolean hover = mx >= bx && mx <= bx + BW && my >= y && my <= y + BH;
            Ui.rowBg(c, bx, y, BW, BH, hover, i == sel);
            c.fill(bx, y, bx + 2, y + BH, i == 3 ? Ui.RED : Ui.ACCENT);
            c.drawCenteredTextWithShadow(tr, Text.literal(ITEMS[i]), bx + BW / 2, y + 8, Ui.TEXT);
        }

        c.drawText(tr, "RoseClient 1.21.4", 6, this.height - 12, Ui.MUTED, false);
        String hint = "G en jeu : menu";
        c.drawText(tr, hint, this.width - 6 - tr.getWidth(hint), this.height - 12, Ui.MUTED, false);
    }

    private void open(int i) {
        switch (i) {
            case 0 -> this.client.setScreen(new SelectWorldScreen(this));
            case 1 -> this.client.setScreen(new MultiplayerScreen(this));
            case 2 -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            default -> this.client.scheduleStop();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < ITEMS.length; i++) {
                int y = by + i * (BH + GAP);
                if (mx >= bx && mx <= bx + BW && my >= y && my <= y + BH) {
                    open(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN -> { sel = Math.min(ITEMS.length - 1, sel + 1); return true; }
            case GLFW.GLFW_KEY_UP -> { sel = Math.max(0, sel - 1); return true; }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (sel >= 0) open(sel);
                return true;
            }
            default -> { return super.keyPressed(keyCode, scanCode, modifiers); }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
