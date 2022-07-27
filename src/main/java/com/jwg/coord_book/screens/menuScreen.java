package com.jwg.coord_book.screens;

import com.jwg.coord_book.CoordBook;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;

import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.awt.event.KeyEvent.getKeyText;

@Environment(EnvType.CLIENT)
public class menuScreen extends Screen {

    boolean nextCharacterSpecial = false;
    public static int page = 0;
    public static final Identifier BOOK_TEXTURE = new Identifier("textures/gui/book.png");
    private List<OrderedText> cachedPage;
    private Text pageIndexText;
    private final boolean pageTurnSound;
    private String contents;

    public menuScreen(BookScreen.Contents contents) {
        this(contents, true);
    }

    private menuScreen(BookScreen.Contents contents, boolean bl) {
        super(NarratorManager.EMPTY);
        this.contents = "";
        this.cachedPage = Collections.emptyList();
        this.pageIndexText = ScreenTexts.EMPTY;
        this.pageTurnSound = bl;
    }

    protected void init() {
        this.addButtons();
    }
    protected void addButtons() {
        //Done button
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, 196, 200, 20, ScreenTexts.DONE, (button) -> {
            assert this.client != null;
            this.client.setScreen(null);
        }));
        //Page buttons (arrows)
        int i = (this.width - 192) / 2;
        this.addDrawableChild(new PageTurnWidget(i + 116, 159, true, (button) -> {
            this.goToNextPage();
            assert this.client != null;
            this.client.setScreen(this);
        }, this.pageTurnSound));
        this.addDrawableChild(new PageTurnWidget(i + 43, 159, false, (button) -> {
            this.goToPreviousPage();
            assert this.client != null;
            this.client.setScreen(this);
        }, this.pageTurnSound));

    }
    protected void goToPreviousPage() {
        --page;
        if (page <= -1) {
            page = 0;
        }
    }

    protected void goToNextPage() {
        ++page;
        if (!new File("CoordinateBook/"+page+".json").exists()) {
            try {
                if (new File("CoordinateBook/"+page+".json").createNewFile()) {
                    CoordBook.LOGGER.info("page {} has been created", page);
                }
            } catch (IOException e) {
                CoordBook.LOGGER.error("page {} is unable to be created", page);
                throw new RuntimeException(e);
            }
        }
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_TEXTURE);
        int i = (this.width - 192) / 2;
        this.drawTexture(matrices, i, 2, 0, 0, 192, 192);
        this.pageIndexText = Text.translatable("book.pageIndicator", page + 1, Math.max((Objects.requireNonNull(new File("CoordinateBook/").list()).length), 1));

        StringBuilder fulldata = new StringBuilder();
        try {
            Scanner readPageContent = new Scanner(new File("CoordinateBook/"+page+".json"));
            while (readPageContent.hasNextLine()) {
                String data = readPageContent.nextLine();
                if (!fulldata.toString().equals("")) {
                    data = "\n" + data;
                }
                fulldata.append(data);
            }
            readPageContent.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.contents = String.valueOf(fulldata);
        StringVisitable stringVisitable = StringVisitable.plain(this.contents);
        this.cachedPage = this.textRenderer.wrapLines(stringVisitable, 114);


        int k = this.textRenderer.getWidth(this.pageIndexText);
        this.textRenderer.draw(matrices, this.pageIndexText, (float)(i - k + 192 - 44), 18.0F, 0);
        Objects.requireNonNull(this.textRenderer);
        int l = Math.min(128 / 9, this.cachedPage.size());

        for(int m = 0; m < l; ++m) {
            OrderedText orderedText = this.cachedPage.get(m);
            TextRenderer var10000 = this.textRenderer;
            float var10003 = (float)(i + 36);
            Objects.requireNonNull(this.textRenderer);
            var10000.draw(matrices, orderedText, var10003, (float)(32 + m * 9), 0);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style style = this.getTextStyleAt(mouseX, mouseY);
            if (style != null && this.handleTextClick(style)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    int o = 0;
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        String key = getKeyText(keyCode).toLowerCase(Locale.ROOT);
        ++o;
        if (key.equals("period")) {
            key = ".";
        } else if (key.equals("space")) {
            key = " ";
        } else if (key.equals("unknown keycode: 0x154")) {
            key = "";
            nextCharacterSpecial = true;
            o = 1;
        } else if (key.equals("japanese katakana")) {
            key = "";
            if (!Objects.equals(this.contents, "")) {
                this.contents = this.contents.substring(0, this.contents.length() - 1);
                try {
                    FileWriter updatePage = new FileWriter(new File("CoordinateBook/" + page + ".json"));
                    updatePage.write(this.contents);
                    updatePage.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (nextCharacterSpecial && o == 2) {
            key = key.toUpperCase(Locale.ROOT);

            nextCharacterSpecial = false;
        }
        StringBuilder fulldata = new StringBuilder();
        try {
            Scanner readPageContent = new Scanner(new File("CoordinateBook/"+page+".json"));
            while (readPageContent.hasNextLine()) {
                String data = readPageContent.nextLine();
                if (!fulldata.toString().equals("")) {
                    data = "\n" + data;
                }
                fulldata.append(data);
            }
            readPageContent.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        fulldata = new StringBuilder(fulldata + key);
        this.contents = String.valueOf(fulldata);
        try {
            FileWriter updatePage = new FileWriter(new File("CoordinateBook/"+page+".json"));
            updatePage.write(String.valueOf(fulldata));
            updatePage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.client.setScreen(this);
        return false;
    }

    @Nullable
    public Style getTextStyleAt(double x, double y) {
        if (this.cachedPage.isEmpty()) {
            return null;
        } else {
            int i = MathHelper.floor(x - (double)((this.width - 192) / 2) - 36.0);
            int j = MathHelper.floor(y - 2.0 - 30.0);
            if (i >= 0 && j >= 0) {
                Objects.requireNonNull(this.textRenderer);
                int k = Math.min(128 / 9, this.cachedPage.size());
                if (i <= 114) {
                    Objects.requireNonNull(this.client.textRenderer);
                    if (j < 9 * k + k) {
                        Objects.requireNonNull(this.client.textRenderer);
                        int l = j / 9;
                        if (l >= 0 && l < this.cachedPage.size()) {
                            OrderedText orderedText = (OrderedText)this.cachedPage.get(l);
                            return this.client.textRenderer.getTextHandler().getStyleAt(orderedText, i);
                        }

                        return null;
                    }
                }

                return null;
            } else {
                return null;
            }
        }
    }
}