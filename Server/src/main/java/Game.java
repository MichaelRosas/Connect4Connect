public class Game {
    private int[][] board;
    String player1;
    String player2;
    private String currentPlayer;
    private boolean gameActive;

    public Game(String player1, String player2) {
        this.board = new int[6][7];
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.gameActive = true;
    }

    // Attempt to place a piece in the specified column
    // Returns false if move is invalid (wrong turn, full column, or out of bounds)
    public boolean makeMove(String player, int column) {
        if (!gameActive || !player.equals(currentPlayer) || column < 0 || column >= 7) {
            return false;
        }

        // Find lowest available row in the column (gravity effect)
        int row = -1;
        for (int r = 5; r >= 0; r--) {
            if (board[r][column] == 0) {
                row = r;
                break;
            }
        }

        if (row == -1) {
            return false;
        }

        int playerNum;
        if (player.equals(player1)) {
            playerNum = 1;
        } else {
            playerNum = 2;
        }
        board[row][column] = playerNum;

        if (currentPlayer.equals(player1)) {
            currentPlayer = player2;
        } else {
            currentPlayer = player1;
        }

        return true;
    }

    public boolean checkWin() {
        // horizontal
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c <= 3; c++) {
                if (board[r][c] != 0 && board[r][c] == board[r][c+1] && board[r][c] == board[r][c+2] && board[r][c] == board[r][c+3]) {
                    return true;
                }
            }
        }

        // vertical
        for (int r = 0; r <= 2; r++) {
            for (int c = 0; c < 7; c++) {
                if (board[r][c] != 0 && board[r][c] == board[r+1][c] && board[r][c] == board[r+2][c] && board[r][c] == board[r+3][c]) {
                    return true;
                }
            }
        }

        // diagonal1 (top-left to bottom-right)
        for (int r = 0; r <= 2; r++) {
            for (int c = 0; c <= 3; c++) {
                if (board[r][c] != 0 && board[r][c] == board[r+1][c+1] && board[r][c] == board[r+2][c+2] && board[r][c] == board[r+3][c+3]) {
                    return true;
                }
            }
        }

        // diagonal2 (bottom-left to top-right)
        for (int r = 3; r < 6; r++) {
            for (int c = 0; c <= 3; c++) {
                if (board[r][c] != 0 && board[r][c] == board[r-1][c+1] && board[r][c] == board[r-2][c+2] && board[r][c] == board[r-3][c+3]) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkDraw() {
        for (int c = 0; c < 7; c++) {
            if (board[0][c] == 0) {
                return false;
            }
        }
        return true;
    }

    public int[][] getBoard() {
        int[][] copy = new int[6][7];
        for (int r = 0; r < 6; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, 7);
        }
        return copy;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public void reset() {
        board = new int[6][7];
        currentPlayer = player1;
        gameActive = true;
    }

    public void setGameActive(boolean active) {
        this.gameActive = active;
    }
}