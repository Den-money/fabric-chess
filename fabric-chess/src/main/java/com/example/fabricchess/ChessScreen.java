package com.example.fabricchess;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChessScreen extends Screen {
    private final ChessEngine engine = new ChessEngine();
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<ChessEngine.Move> validMovesForSelected = new ArrayList<>();
    private int botDelayTicks = 0;

    private static final int TILE_SIZE = 24;

    public ChessScreen() {
        super(Text.literal("Шахматы с ботом"));
    }

    @Override
    protected void init() {
        int boardSize = TILE_SIZE * 8;
        int startX = (this.width - boardSize) / 2;
        int startY = (this.height - boardSize) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Новая игра"), button -> {
            engine.reset();
            selectedRow = -1;
            selectedCol = -1;
            validMovesForSelected.clear();
        }).dimensions(startX, startY + boardSize + 8, boardSize, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (!engine.whiteTurn) {
            botDelayTicks++;
            if (botDelayTicks >= 10) {
                engine.makeBotMove();
                botDelayTicks = 0;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int boardSize = TILE_SIZE * 8;
        int startX = (this.width - boardSize) / 2;
        int startY = (this.height - boardSize) / 2;

        context.drawCenteredTextWithShadow(this.textRenderer, engine.status, this.width / 2, startY - 16, 0xFFFFFF);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int x = startX + c * TILE_SIZE;
                int y = startY + r * TILE_SIZE;

                boolean isLight = (r + c) % 2 == 0;
                int color = isLight ? 0xFFEEEED2 : 0xFF769656;

                if (r == selectedRow && c == selectedCol) {
                    color = 0xFFBACA44;
                }

                context.fill(x, y, x + TILE_SIZE, y + TILE_SIZE, color);

                for (ChessEngine.Move m : validMovesForSelected) {
                    if (m.r2() == r && m.c2() == c) {
                        context.fill(x + 7, y + 7, x + TILE_SIZE - 7, y + TILE_SIZE - 7, 0x99333333);
                    }
                }

                int piece = engine.board[r][c];
                if (piece != 0) {
                    String symbol = getPieceSymbol(piece);
                    int textColor = piece > 0 ? 0xFFFFFFFF : 0xFF111111;
                    context.drawCenteredTextWithShadow(
                            this.textRenderer,
                            symbol,
                            x + TILE_SIZE / 2,
                            y + (TILE_SIZE - 8) / 2,
                            textColor
                    );
                }
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && engine.whiteTurn) {
            int boardSize = TILE_SIZE * 8;
            int startX = (this.width - boardSize) / 2;
            int startY = (this.height - boardSize) / 2;

            if (mouseX >= startX && mouseX < startX + boardSize && mouseY >= startY && mouseY < startY + boardSize) {
                int col = (int) ((mouseX - startX) / TILE_SIZE);
                int row = (int) ((mouseY - startY) / TILE_SIZE);

                if (selectedRow != -1 && engine.tryPlayerMove(selectedRow, selectedCol, row, col)) {
                    selectedRow = -1;
                    selectedCol = -1;
                    validMovesForSelected.clear();
                    return true;
                }

                if (engine.board[row][col] > 0) {
                    selectedRow = row;
                    selectedCol = col;
                    validMovesForSelected.clear();
                    for (ChessEngine.Move m : engine.getLegalMoves(true)) {
                        if (m.r1() == row && m.c1() == col) {
                            validMovesForSelected.add(m);
                        }
                    }
                } else {
                    selectedRow = -1;
                    selectedCol = -1;
                    validMovesForSelected.clear();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String getPieceSymbol(int piece) {
        return switch (piece) {
            case 6 -> "♔";
            case 5 -> "♕";
            case 4 -> "♖";
            case 3 -> "♗";
            case 2 -> "♘";
            case 1 -> "♙";
            case -6 -> "♚";
            case -5 -> "♛";
            case -4 -> "♜";
            case -3 -> "♝";
            case -2 -> "♞";
            case -1 -> "♟";
            default -> "";
        };
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}