package com.example.fabricchess;

import java.util.ArrayList;
import java.util.List;

public class ChessEngine {
    public int[][] board = new int[8][8];
    public boolean whiteTurn = true;
    public String status = "Ваш ход (Белые)";

    public record Move(int r1, int c1, int r2, int c2) {}

    public ChessEngine() {
        reset();
    }

    public void reset() {
        board = new int[][]{
            {-4, -2, -3, -5, -6, -3, -2, -4},
            {-1, -1, -1, -1, -1, -1, -1, -1},
            { 0,  0,  0,  0,  0,  0,  0,  0},
            { 0,  0,  0,  0,  0,  0,  0,  0},
            { 0,  0,  0,  0,  0,  0,  0,  0},
            { 0,  0,  0,  0,  0,  0,  0,  0},
            { 1,  1,  1,  1,  1,  1,  1,  1},
            { 4,  2,  3,  5,  6,  3,  2,  4}
        };
        whiteTurn = true;
        status = "Ваш ход (Белые)";
    }

    public List<Move> getLegalMoves(boolean isWhite) {
        List<Move> legal = new ArrayList<>();
        for (Move m : getPseudoMoves(isWhite)) {
            int captured = makeMoveTemp(m);
            if (!isKingInCheck(isWhite)) {
                legal.add(m);
            }
            undoMoveTemp(m, captured);
        }
        return legal;
    }

    public boolean tryPlayerMove(int r1, int c1, int r2, int c2) {
        if (!whiteTurn) return false;
        for (Move m : getLegalMoves(true)) {
            if (m.r1 == r1 && m.c1 == c1 && m.r2 == r2 && m.c2 == c2) {
                applyMove(m);
                whiteTurn = false;
                updateGameState();
                return true;
            }
        }
        return false;
    }

    public void makeBotMove() {
        if (whiteTurn) return;
        List<Move> moves = getLegalMoves(false);
        if (moves.isEmpty()) {
            updateGameState();
            return;
        }

        Move bestMove = moves.get(0);
        int bestScore = Integer.MAX_VALUE;

        for (Move m : moves) {
            int captured = makeMoveTemp(m);
            int score = minimax(2, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
            undoMoveTemp(m, captured);
            if (score < bestScore) {
                bestScore = score;
                bestMove = m;
            }
        }

        applyMove(bestMove);
        whiteTurn = true;
        updateGameState();
    }

    private void applyMove(Move m) {
        int piece = board[m.r1][m.c1];
        board[m.r2][m.c2] = piece;
        board[m.r1][m.c1] = 0;
        if (piece == 1 && m.r2 == 0) board[m.r2][m.c2] = 5;
        if (piece == -1 && m.r2 == 7) board[m.r2][m.c2] = -5;
    }

    private int makeMoveTemp(Move m) {
        int captured = board[m.r2][m.c2];
        board[m.r2][m.c2] = board[m.r1][m.c1];
        board[m.r1][m.c1] = 0;
        return captured;
    }

    private void undoMoveTemp(Move m, int captured) {
        board[m.r1][m.c1] = board[m.r2][m.c2];
        board[m.r2][m.c2] = captured;
    }

    private void updateGameState() {
        List<Move> nextMoves = getLegalMoves(whiteTurn);
        if (nextMoves.isEmpty()) {
            if (isKingInCheck(whiteTurn)) {
                status = whiteTurn ? "Мат! Победил бот." : "Мат! Вы победили!";
            } else {
                status = "Пат! Ничья.";
            }
        } else {
            String check = isKingInCheck(whiteTurn) ? " (ШАХ!)" : "";
            status = (whiteTurn ? "Ваш ход" : "Бот думает...") + check;
        }
    }

    private int minimax(int depth, int alpha, int beta, boolean maximizingWhite) {
        if (depth == 0) return evaluateBoard();

        List<Move> moves = getLegalMoves(maximizingWhite);
        if (moves.isEmpty()) {
            if (isKingInCheck(maximizingWhite)) return maximizingWhite ? -10000 : 10000;
            return 0;
        }

        if (maximizingWhite) {
            int maxEval = Integer.MIN_VALUE;
            for (Move m : moves) {
                int cap = makeMoveTemp(m);
                int eval = minimax(depth - 1, alpha, beta, false);
                undoMoveTemp(m, cap);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move m : moves) {
                int cap = makeMoveTemp(m);
                int eval = minimax(depth - 1, alpha, beta, true);
                undoMoveTemp(m, cap);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private int evaluateBoard() {
        int score = 0;
        int[] values = {0, 100, 320, 330, 500, 900, 20000};
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p > 0) score += values[p];
                else if (p < 0) score -= values[-p];
            }
        }
        return score;
    }

    private boolean isKingInCheck(boolean isWhite) {
        int king = isWhite ? 6 : -6;
        int kr = -1, kc = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == king) { kr = r; kc = c; }
            }
        }
        for (Move m : getPseudoMoves(!isWhite)) {
            if (m.r2 == kr && m.c2 == kc) return true;
        }
        return false;
    }

    private List<Move> getPseudoMoves(boolean isWhite) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p == 0 || (p > 0) != isWhite) continue;
                int type = Math.abs(p);

                if (type == 1) {
                    int dir = isWhite ? -1 : 1;
                    int startRow = isWhite ? 6 : 1;
                    if (inBounds(r + dir, c) && board[r + dir][c] == 0) {
                        moves.add(new Move(r, c, r + dir, c));
                        if (r == startRow && board[r + 2 * dir][c] == 0) {
                            moves.add(new Move(r, c, r + 2 * dir, c));
                        }
                    }
                    for (int dc : new int[]{-1, 1}) {
                        if (inBounds(r + dir, c + dc)) {
                            int target = board[r + dir][c + dc];
                            if (target != 0 && (target > 0) != isWhite) {
                                moves.add(new Move(r, c, r + dir, c + dc));
                            }
                        }
                    }
                } else if (type == 2) {
                    int[][] jumps = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
                    for (int[] j : jumps) addIfValid(moves, r, c, r + j[0], c + j[1], isWhite);
                } else if (type == 6) {
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr != 0 || dc != 0) addIfValid(moves, r, c, r + dr, c + dc, isWhite);
                        }
                    }
                } else {
                    int[][] dirs = type == 4 ? new int[][]{{-1,0},{1,0},{0,-1},{0,1}} :
                                   type == 3 ? new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}} :
                                               new int[][]{{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
                    for (int[] d : dirs) {
                        int nr = r + d[0], nc = c + d[1];
                        while (inBounds(nr, nc)) {
                            int target = board[nr][nc];
                            if (target == 0) {
                                moves.add(new Move(r, c, nr, nc));
                            } else {
                                if ((target > 0) != isWhite) moves.add(new Move(r, c, nr, nc));
                                break;
                            }
                            nr += d[0];
                            nc += d[1];
                        }
                    }
                }
            }
        }
        return moves;
    }

    private void addIfValid(List<Move> moves, int r1, int c1, int r2, int c2, boolean isWhite) {
        if (inBounds(r2, c2)) {
            int t = board[r2][c2];
            if (t == 0 || (t > 0) != isWhite) moves.add(new Move(r1, c1, r2, c2));
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < 8 && c >= 0 && c < 8;
    }
}