import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Glow;
import javafx.animation.TranslateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
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
	private ListView<String> lobbyPlayerList;

	private String username;
	private String opponent;
	private boolean myTurn = false;
	private boolean gameActive = false;
	private int[][] board = new int[6][7];
	private Circle[][] circleGrid = new Circle[6][7];
	private boolean animating = false;

	private Client clientConnection;
	private Stage primaryStage;
	private Scene loginScene;
	private Scene lobbyScene;
	private Scene gameScene;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		primaryStage.setTitle("Connect 4");

		createLoginScene();
		createLobbyScene();
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
		loginLayout.setStyle("-fx-background: linear-gradient(to bottom, #667eea, #764ba2);");

		Label titleLabel = new Label("Connect4Connect");
		titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 3);");

		usernameField = new TextField();
		usernameField.setPromptText("Enter your username");
		usernameField.setPrefWidth(250);
		usernameField.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-radius: 5; -fx-background-color: white; -fx-text-fill: black;");

		loginButton = new Button("Login");
		loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 40; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);");
		loginButton.setPrefWidth(150);
		loginButton.setOnAction(e -> login());
		loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 40; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);"));
		loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 40; -fx-background-radius: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 5, 0, 0, 2);"));

		Label errorLabel = new Label("");
		errorLabel.setTextFill(Color.YELLOW);
		errorLabel.setStyle("-fx-font-weight: bold;");

		loginLayout.getChildren().addAll(titleLabel, new Label(""), usernameField, loginButton, errorLabel);
		loginScene = new Scene(loginLayout, 400, 350);
	}

	private void createLobbyScene() {
		BorderPane lobbyLayout = new BorderPane();
		lobbyLayout.setPadding(new Insets(20));
		lobbyLayout.setStyle("-fx-background: linear-gradient(to bottom, #2193b0, #6dd5ed);");

		Label titleLabel = new Label("Game Lobby");
		titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 3);");
		BorderPane.setAlignment(titleLabel, Pos.CENTER);
		BorderPane.setMargin(titleLabel, new Insets(10));
		lobbyLayout.setTop(titleLabel);

		VBox centerBox = new VBox(15);
		centerBox.setAlignment(Pos.CENTER);

		Label instructionLabel = new Label("Select a player to challenge:");
		instructionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

		lobbyPlayerList = new ListView<>();
		lobbyPlayerList.setPrefHeight(350);
		lobbyPlayerList.setPrefWidth(350);
		lobbyPlayerList.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);");

		lobbyPlayerList.setCellFactory(param -> new ListCell<String>() {
			private Button challengeButton = new Button("Challenge");

			{
				challengeButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
				challengeButton.setOnMouseEntered(e -> challengeButton.setStyle("-fx-background-color: #E64A19; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;"));
				challengeButton.setOnMouseExited(e -> challengeButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;"));
				challengeButton.setOnAction(e -> {
					String selectedPlayer = getItem();
					if (selectedPlayer != null) {
						clientConnection.send(new Message(MessageType.CHALLENGE_REQUEST, username, selectedPlayer));
					}
				});
			}

			@Override
			protected void updateItem(String player, boolean empty) {
				super.updateItem(player, empty);
				if (empty || player == null) {
					setText(null);
					setGraphic(null);
				} else {
					HBox hbox = new HBox(15);
					hbox.setAlignment(Pos.CENTER_LEFT);
					hbox.setPadding(new Insets(5));
					Label playerLabel = new Label("👤 " + player);
					playerLabel.setPrefWidth(200);
					playerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
					hbox.getChildren().addAll(playerLabel, challengeButton);
					setGraphic(hbox);
				}
			}
		});

		centerBox.getChildren().addAll(instructionLabel, lobbyPlayerList);
		lobbyLayout.setCenter(centerBox);

		Label waitingLabel = new Label("⏳ Waiting for challenges...");
		waitingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-style: italic;");
		BorderPane.setAlignment(waitingLabel, Pos.CENTER);
		BorderPane.setMargin(waitingLabel, new Insets(10));
		lobbyLayout.setBottom(waitingLabel);

		lobbyScene = new Scene(lobbyLayout, 450, 550);
	}

	private void createGameScene() {
		BorderPane gameLayout = new BorderPane();
		gameLayout.setPadding(new Insets(15));
		gameLayout.setStyle("-fx-background: linear-gradient(to bottom, #1e3c72, #2a5298);");

		HBox topBar = new HBox(20);
		topBar.setPadding(new Insets(10));
		topBar.setAlignment(Pos.CENTER);

		statusLabel = new Label("Waiting for opponent...");
		statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");

		restartButton = new Button("🔄 Play Again");
		restartButton.setDisable(true);
		restartButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5;");
		restartButton.setOnAction(e -> restartGame());
		restartButton.setOnMouseEntered(e -> restartButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5;"));
		restartButton.setOnMouseExited(e -> restartButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 5;"));

		topBar.getChildren().addAll(statusLabel, restartButton);
		gameLayout.setTop(topBar);

		gameBoard = new GridPane();
		gameBoard.setAlignment(Pos.CENTER);
		gameBoard.setHgap(8);
		gameBoard.setVgap(8);
		gameBoard.setStyle("-fx-background-color: #1565C0; -fx-padding: 20; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 15, 0, 0, 5);");

		StackPane boardContainer = new StackPane(gameBoard);
		gameLayout.setCenter(boardContainer);

		VBox chatBox = new VBox(10);
		chatBox.setPadding(new Insets(10));
		chatBox.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 10;");

		Label chatLabel = new Label("💬 Chat with Opponent");
		chatLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

		chatList = new ListView<>();
		chatList.setPrefHeight(300);
		chatList.setStyle("-fx-background-radius: 5;");

		chatField = new TextField();
		chatField.setPromptText("Type chat message...");
		chatField.setStyle("-fx-background-radius: 5;");

		sendButton = new Button("Send");
		sendButton.setStyle("-fx-background-color: #0066cc; -fx-text-fill: white; -fx-background-radius: 5;");
		sendButton.setOnAction(e -> sendChat());

		HBox chatInputBox = new HBox(5, chatField, sendButton);
		HBox.setHgrow(chatField, Priority.ALWAYS);

		chatBox.getChildren().addAll(chatLabel, chatList, chatInputBox);
		gameLayout.setRight(chatBox);

		updateGameBoardUI();
		gameScene = new Scene(gameLayout, 900, 650);
	}

	private void updateGameBoardUI() {
		gameBoard.getChildren().clear();

		for (int c = 0; c < 7; c++) {
			VBox column = new VBox(8);
			column.setAlignment(Pos.TOP_CENTER);
			column.setPrefWidth(60);

			final int colIndex = c;
			column.setOnMouseClicked(e -> {
				if (!animating) makeMove(colIndex);
			});

			column.setOnMouseEntered(e -> {
				if (gameActive && myTurn && !animating) {
					column.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 8; -fx-cursor: hand;");
				}
			});

			column.setOnMouseExited(e -> column.setStyle(""));

			for (int r = 0; r < 6; r++) {
				Circle circle = new Circle(25);
				circleGrid[r][c] = circle;

				// Create gradient fills for pieces
				if (board[r][c] == 1) {
					LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
						new Stop(0, Color.rgb(255, 80, 80)),
						new Stop(1, Color.rgb(200, 20, 20)));
					circle.setFill(gradient);
					
					DropShadow shadow = new DropShadow();
					shadow.setColor(Color.rgb(150, 0, 0, 0.8));
					shadow.setRadius(8);
					circle.setEffect(shadow);
				} else if (board[r][c] == 2) {
					LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
						new Stop(0, Color.rgb(255, 235, 59)),
						new Stop(1, Color.rgb(255, 193, 7)));
					circle.setFill(gradient);
					
					DropShadow shadow = new DropShadow();
					shadow.setColor(Color.rgb(200, 150, 0, 0.8));
					shadow.setRadius(8);
					circle.setEffect(shadow);
				} else {
					circle.setFill(Color.rgb(240, 248, 255));
					
					InnerShadow innerShadow = new InnerShadow();
					innerShadow.setColor(Color.rgb(100, 100, 100, 0.3));
					innerShadow.setRadius(5);
					circle.setEffect(innerShadow);
				}

				circle.setStroke(Color.rgb(30, 60, 114, 0.5));
				circle.setStrokeWidth(2);

				column.getChildren().add(circle);
			}
			gameBoard.add(column, c, 0);
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
						primaryStage.setTitle("Connect 4: " + username);
						primaryStage.setScene(lobbyScene);
						break;

					case LOBBY_UPDATE:
						if (data.playerList != null) {
							lobbyPlayerList.getItems().clear();
							data.playerList.stream()
								.filter(player -> !player.equals(username))
								.forEach(player -> lobbyPlayerList.getItems().add(player));
						}
						break;

					case CHALLENGE_REQUEST:
						String challenger = data.username;
						Platform.runLater(() -> {
							Alert challengeAlert = new Alert(Alert.AlertType.CONFIRMATION);
							challengeAlert.setTitle("Challenge Request");
							challengeAlert.setHeaderText(challenger + " wants to play!");
							challengeAlert.setContentText("Do you accept the challenge?");
							
							challengeAlert.showAndWait().ifPresent(response -> {
								if (response == ButtonType.OK) {
									clientConnection.send(new Message(MessageType.CHALLENGE_ACCEPT, username, challenger));
								} else {
									clientConnection.send(new Message(MessageType.CHALLENGE_DECLINE, username, challenger));
								}
							});
						});
						break;

					case CHALLENGE_DECLINE:
						String decliner = data.username;
						Alert declineAlert = new Alert(Alert.AlertType.INFORMATION);
						declineAlert.setTitle("Challenge Declined");
						declineAlert.setHeaderText("Challenge declined");
						declineAlert.setContentText(decliner + " declined your challenge.");
						declineAlert.show();
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
						int[][] oldBoard = copyBoard(board);
						board = data.board;
						myTurn = data.isPlayerTurn;
						animateBoardUpdate(oldBoard, board);
						updateStatus();
						gameActive = true;
						break;

					case GAME_MOVE:
						oldBoard = copyBoard(board);
						board = data.board;
						myTurn = data.isPlayerTurn;
						animateBoardUpdate(oldBoard, board);
						updateStatus();
						gameActive = true;
						break;

					case GAME_WIN:
						board = data.board;
						updateGameBoardUI();
						playWinSound();
						animateWin();
						statusLabel.setText("🎉 You Won! 🎉");
						statusLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FFD700; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
						chatList.getItems().add("SYSTEM: You won the game!");
						gameActive = false;
						restartButton.setDisable(false);
						break;

					case GAME_LOSE:
						board = data.board;
						updateGameBoardUI();
						playLoseSound();
						statusLabel.setText("You Lost");
						statusLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ff6a00; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
						chatList.getItems().add("SYSTEM: You lost the game.");
						gameActive = false;
						restartButton.setDisable(false);
						break;

					case GAME_DRAW:
						board = data.board;
						updateGameBoardUI();
						statusLabel.setText("Draw!");
						statusLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
						chatList.getItems().add("SYSTEM: Game ended in a draw!");
						gameActive = false;
						restartButton.setDisable(false);
						break;

					case NEWUSER:
						opponent = data.username;
						chatList.getItems().clear();
						chatList.getItems().add("SYSTEM: " + opponent + " has joined the game");
						primaryStage.setScene(gameScene);
						break;

					case DISCONNECT:
						opponent = null;
						gameActive = false;
						restartButton.setDisable(true);
						
						Alert disconnectAlert = new Alert(Alert.AlertType.INFORMATION);
						disconnectAlert.setTitle("Opponent Disconnected");
						disconnectAlert.setHeaderText("Your opponent has disconnected");
						disconnectAlert.setContentText("Returning to lobby...");
						disconnectAlert.showAndWait();
						
						primaryStage.setScene(lobbyScene);
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
			statusLabel.setText("▶ Your Turn");
			statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
		} else {
			statusLabel.setText("⏸ Opponent's Turn");
			statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ff6a00; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 2);");
		}
	}

	private int[][] copyBoard(int[][] original) {
		int[][] copy = new int[6][7];
		for (int r = 0; r < 6; r++) {
			System.arraycopy(original[r], 0, copy[r], 0, 7);
		}
		return copy;
	}

	private void animateBoardUpdate(int[][] oldBoard, int[][] newBoard) {
		animating = true;
		
		// Find the new piece position
		for (int c = 0; c < 7; c++) {
			for (int r = 0; r < 6; r++) {
				if (oldBoard[r][c] != newBoard[r][c] && newBoard[r][c] != 0) {
					final int row = r;
					final int col = c;
					
					// Update board UI first
					updateGameBoardUI();
					
					// Animate the piece drop
					Circle animatedCircle = circleGrid[row][col];
					animatedCircle.setTranslateY(-400);
					
					TranslateTransition drop = new TranslateTransition(Duration.millis(400), animatedCircle);
					drop.setToY(0);
					drop.setOnFinished(e -> {
						playDropSound();
						
						// Bounce effect
						ScaleTransition bounce = new ScaleTransition(Duration.millis(100), animatedCircle);
						bounce.setFromX(1.0);
						bounce.setFromY(1.0);
						bounce.setToX(1.2);
						bounce.setToY(1.2);
						bounce.setAutoReverse(true);
						bounce.setCycleCount(2);
						bounce.setOnFinished(ev -> animating = false);
						bounce.play();
					});
					drop.play();
					return;
				}
			}
		}
		animating = false;
	}

	private void animateWin() {
		// Pulse effect on status label
		ScaleTransition pulse = new ScaleTransition(Duration.millis(500), statusLabel);
		pulse.setFromX(1.0);
		pulse.setFromY(1.0);
		pulse.setToX(1.1);
		pulse.setToY(1.1);
		pulse.setAutoReverse(true);
		pulse.setCycleCount(6);
		pulse.play();
	}

	private void playDropSound() {
		try {
			// Simple beep sound using system beep
			java.awt.Toolkit.getDefaultToolkit().beep();
		} catch (Exception e) {
			// Sound not available
		}
	}

	private void playWinSound() {
		// Play multiple tones for win
		new Thread(() -> {
			try {
				for (int i = 0; i < 3; i++) {
					java.awt.Toolkit.getDefaultToolkit().beep();
					Thread.sleep(150);
				}
			} catch (Exception e) {
				// Sound not available
			}
		}).start();
	}

	private void playLoseSound() {
		try {
			java.awt.Toolkit.getDefaultToolkit().beep();
		} catch (Exception e) {
			// Sound not available
		}
	}
}