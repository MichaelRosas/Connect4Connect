import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class GuiClient extends Application {

	private TextField usernameField;
	private Button loginButton;
	private TextField chatField;
	private Button sendButton;
	private ListView<String> chatList;
	private GridPane gameBoard;
	private Label statusLabel;
	private Button restartButton;

	private String username;
	private String opponent;
	private boolean myTurn = false;
	private boolean gameActive = false;
	private int[][] board = new int[6][7];

	private Client clientConnection;
	private Stage primaryStage;
	private Scene loginScene;
	private Scene gameScene;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		primaryStage.setTitle("Connect 4");

		createLoginScene();
		createGameScene();
		setupClientConnection();

		primaryStage.setScene(loginScene);
		primaryStage.show();

		primaryStage.setOnCloseRequest(e -> {
			if (clientConnection != null) {
				clientConnection.send(new Message(MessageType.DISCONNECT, username));
			}
			Platform.exit();
			System.exit(0);
		});
	}

	private void createLoginScene() {
		VBox loginLayout = new VBox(15);
		loginLayout.setPadding(new Insets(30));
		loginLayout.setAlignment(Pos.CENTER);
		loginLayout.setStyle("-fx-background-color: #f0f8ff;");

		Label titleLabel = new Label("Connect 4");
		titleLabel.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0066cc;");

		usernameField = new TextField();
		usernameField.setPromptText("Username");
		usernameField.setPrefWidth(220);

		loginButton = new Button("Login");
		loginButton.setStyle("-fx-background-color: #0066cc; -fx-text-fill: white;");
		loginButton.setPrefWidth(120);
		loginButton.setOnAction(e -> login());

		Label errorLabel = new Label("");
		errorLabel.setTextFill(Color.RED);

		loginLayout.getChildren().addAll(titleLabel, new Label("Enter your username:"), usernameField, loginButton, errorLabel);
		loginScene = new Scene(loginLayout, 350, 300);
	}

	private void createGameScene() {
		BorderPane gameLayout = new BorderPane();
		gameLayout.setPadding(new Insets(15));

		HBox topBar = new HBox(20);
		topBar.setPadding(new Insets(10));
		topBar.setAlignment(Pos.CENTER);

		statusLabel = new Label("Waiting for opponent...");
		statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

		restartButton = new Button("Play Again");
		restartButton.setDisable(true);
		restartButton.setOnAction(e -> restartGame());

		topBar.getChildren().addAll(statusLabel, restartButton);
		gameLayout.setTop(topBar);

		gameBoard = new GridPane();
		gameBoard.setAlignment(Pos.CENTER);
		gameBoard.setHgap(5);
		gameBoard.setVgap(5);
		gameBoard.setStyle("-fx-background-color: #0066cc; -fx-padding: 15;");

		gameLayout.setCenter(new VBox(10, gameBoard));

		VBox chatBox = new VBox(10);
		chatBox.setPadding(new Insets(10));
		chatBox.setStyle("-fx-background-color: white;");

		chatList = new ListView<>();
		chatList.setPrefHeight(300);

		chatField = new TextField();
		chatField.setPromptText("Type chat message");

		sendButton = new Button("Send");
		sendButton.setOnAction(e -> sendChat());

		HBox chatInputBox = new HBox(5, chatField, sendButton);

		chatBox.getChildren().addAll(new Label("Chat with Opponent"), chatList, chatInputBox);
		gameLayout.setRight(chatBox);

		updateGameBoardUI();
		gameScene = new Scene(gameLayout, 800, 600);
	}

	private void updateGameBoardUI() {
		gameBoard.getChildren().clear();

		for (int c = 0; c < 7; c++) {
			StackPane columnPane = new StackPane();
			columnPane.setPadding(new Insets(5));

			VBox column = new VBox(5);
			column.setAlignment(Pos.BOTTOM_CENTER);
			columnPane.getChildren().add(column);

			final int colIndex = c;
			columnPane.setOnMouseClicked(e -> makeMove(colIndex));

			columnPane.setOnMouseEntered(e -> {
				if (gameActive && myTurn) {
					columnPane.setStyle("-fx-background-color: #3399ff;");
				}
			});

			columnPane.setOnMouseExited(e -> columnPane.setStyle(""));

			for (int r = 0; r < 6; r++) {
				Circle circle = new Circle(20);

				if (board[r][c] == 1) {
					circle.setFill(Color.RED);
				} else if (board[r][c] == 2) {
					circle.setFill(Color.YELLOW);
				} else {
					circle.setFill(Color.WHITE);
				}

				circle.setStroke(Color.BLACK);
				circle.setStrokeWidth(1.5);

				column.getChildren().add(circle);
			}
			gameBoard.add(columnPane, c, 0);
		}
	}

	private void setupClientConnection() {
		clientConnection = new Client(data -> {
			Platform.runLater(() -> {
				switch (data.type) {
					case SIMPLE:
						chatList.getItems().add("SYSTEM: " + data.message);
						break;

					case LOGIN_SUCCESS:
						username = data.username;
						opponent = data.recipient;
						primaryStage.setTitle("Connect 4: " + username);
						primaryStage.setScene(gameScene);
						break;

					case LOGIN_ERROR:
						Alert alert = new Alert(Alert.AlertType.ERROR);
						alert.setTitle("Login Error");
						alert.setHeaderText("Username already taken");
						alert.setContentText(data.message);
						alert.showAndWait();
						break;

					case TEXT:
						chatList.getItems().add(data.username + ": " + data.message);
						break;

					case GAME_STATE:
						board = data.board;
						myTurn = data.isPlayerTurn;
						updateGameBoardUI();
						updateStatus();
						gameActive = true;
						break;

					case GAME_MOVE:
						board = data.board;
						myTurn = data.isPlayerTurn;
						updateGameBoardUI();
						updateStatus();
						gameActive = true;
						break;

					case GAME_WIN:
						board = data.board;
						updateGameBoardUI();
						statusLabel.setText("You won!");
						statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #008000;");
						chatList.getItems().add("SYSTEM: You won the game!");
						gameActive = false;
						restartButton.setDisable(false);
						break;

					case GAME_LOSE:
						board = data.board;
						updateGameBoardUI();
						statusLabel.setText("You lost!");
						statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff6a00;");
						chatList.getItems().add("SYSTEM: You lost the game.");
						gameActive = false;
						restartButton.setDisable(false);
						break;

					case GAME_DRAW:
						board = data.board;
						updateGameBoardUI();
						statusLabel.setText("Game ended in a draw!");
						statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #666666;");
						chatList.getItems().add("SYSTEM: Game ended in a draw!");
						gameActive = false;
						restartButton.setDisable(false);
						break;

					case NEWUSER:
						if (opponent == null) {
							opponent = data.username;
							chatList.getItems().add("SYSTEM: " + opponent + " has joined the game");
						}
						break;

					case DISCONNECT:
						opponent = null;
						statusLabel.setText("Opponent disconnected. Waiting for new opponent...");
						statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: black;");
						chatList.getItems().add("SYSTEM: Your opponent has disconnected");
						gameActive = false;
						restartButton.setDisable(true);
						break;
				}
			});
		});
		clientConnection.start();
	}

	private void login() {
		String name = usernameField.getText().trim();
		if (name.isEmpty()) {
			new Alert(Alert.AlertType.ERROR, "Username cannot be empty").show();
			return;
		}
		clientConnection.send(new Message(MessageType.LOGIN, name));
	}

	private void makeMove(int column) {
		if (!gameActive || !myTurn) return;
		clientConnection.send(new Message(username, column));
	}

	private void sendChat() {
		String message = chatField.getText();
		if (message.isEmpty() || opponent == null) return;

		clientConnection.send(new Message(username, opponent, message));
		chatList.getItems().add("You: " + message);
		chatField.clear();
	}

	private void restartGame() {
		clientConnection.send(new Message(MessageType.GAME_RESTART, username));
		restartButton.setDisable(true);
		statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
	}

	private void updateStatus() {
		if (myTurn) {
			statusLabel.setText("Your turn");
			statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: green;");
		} else {
			statusLabel.setText("Opponent's turn");
			statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff6a00;");
		}
	}
}