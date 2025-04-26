import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class Server {
	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	HashSet<String> usernames = new HashSet<>();
	HashMap<String, ClientThread> usernameToClient = new HashMap<>();
	HashMap<String, String> playerPairs = new HashMap<>();
	HashMap<String, Game> games = new HashMap<>();
	HashMap<String, Boolean> rematchRequests = new HashMap<>();
	TheServer server;
	private Consumer<Message> callback;

	Server(Consumer<Message> call) {
		callback = call;
		server = new TheServer();
		server.start();
	}

	private String findPlayer(String username) {
		for (String player : usernameToClient.keySet()) {
			if (!player.equals(username) && !playerPairs.containsKey(player) && !playerPairs.containsValue(player)) {
				return player;
			}
		}
		return null;
	}

	private void matchPlayers(String player1, String player2) {
		playerPairs.put(player1, player2);
		playerPairs.put(player2, player1);

		Game game = new Game(player1, player2);
		games.put(player1, game);

		ClientThread client1 = usernameToClient.get(player1);
		ClientThread client2 = usernameToClient.get(player2);

		try {
			Message gameStart = new Message(MessageType.GAME_STATE, game.getBoard(), true);
			client1.out.writeObject(gameStart);

			gameStart = new Message(MessageType.GAME_STATE, game.getBoard(), false);
			client2.out.writeObject(gameStart);

			Message setOpponent = new Message(MessageType.NEWUSER, player2);
			client1.out.writeObject(setOpponent);

			setOpponent = new Message(MessageType.NEWUSER, player1);
			client2.out.writeObject(setOpponent);

			callback.accept(new Message("Game started with " + player1 + " and " + player2));
		} catch (Exception e) {
			System.err.println("Error starting game");
		}
	}

	public class TheServer extends Thread {
		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555)) {
				System.out.println("Server is waiting for a client");

				while (true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					clients.add(c);
					c.start();
					count++;
				}
			} catch (Exception e) {
				callback.accept(new Message("Server did not launch"));
			}
		}
	}

	class ClientThread extends Thread {
		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;
		String username;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		public void updateClients(Message message) {
			switch (message.type) {
				case TEXT:
					String recipient = message.recipient;
					if (usernameToClient.containsKey(recipient)) {
						ClientThread recipientThread = usernameToClient.get(recipient);
						try {
							recipientThread.out.writeObject(message);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					break;

				case LOGIN:
					if (usernames.contains(message.username)) {
						try {
							Message loginError = new Message(MessageType.LOGIN_ERROR, "Username already taken");
							out.writeObject(loginError);
						} catch (Exception e) {
							System.err.println("Error with login error");
						}
					} else {
						username = message.username;
						usernames.add(username);
						usernameToClient.put(username, this);
						try {
							Message loginSuccess = new Message(MessageType.LOGIN_SUCCESS, username);
							out.writeObject(loginSuccess);

							String opp = findPlayer(username);
							if (opp != null) {
								matchPlayers(username, opp);
							}
						} catch (Exception e) {
							System.err.println("Error logging in");
						}
					}
					break;

				case GAME_MOVE:
					String opp = playerPairs.get(username);
					if (opp != null) {
						Game game = games.get(username);
						if (game == null) {
							game = games.get(opp);
						}

						if (game != null) {
							boolean validMove = game.makeMove(username, message.column);

							if (validMove) {
								if (game.checkWin()) {
									game.setGameActive(false);

									try {
										Message winMessage = new Message(MessageType.GAME_WIN,
												"You won!", game.getBoard());
										out.writeObject(winMessage);

										ClientThread opponentClient = usernameToClient.get(opp);
										Message loseMessage = new Message(MessageType.GAME_LOSE,
												"You lost!", game.getBoard());
										opponentClient.out.writeObject(loseMessage);

										callback.accept(new Message(username + " won against " + opp));
									} catch (Exception e) {
										System.err.println("Error with result");
									}
								}
								else if (game.checkDraw()) {
									game.setGameActive(false);

									try {
										Message drawMessage = new Message(MessageType.GAME_DRAW,
												"Game ended in a draw", game.getBoard());
										out.writeObject(drawMessage);

										ClientThread opponentClient = usernameToClient.get(opp);
										opponentClient.out.writeObject(drawMessage);

										callback.accept(new Message("Draw between " + username + " and " + opp));
									} catch (Exception e) {
										System.err.println("Error with result");
									}
								}
								else {
									try {
										ClientThread opponentClient = usernameToClient.get(opp);

										Message opponentTurn = new Message(MessageType.GAME_STATE,
												game.getBoard(), true);
										opponentClient.out.writeObject(opponentTurn);

										Message waitTurn = new Message(MessageType.GAME_STATE,
												game.getBoard(), false);
										out.writeObject(waitTurn);

									} catch (Exception e) {
										System.err.println("Error updating game");
									}
								}
							}
						}
					}
					break;

				case GAME_RESTART:
					String opponent = playerPairs.get(username);
					if (opponent != null) {
						Game game;
						if (games.get(username) != null) {
							game = games.get(username);
						} else {
							game = games.get(opponent);
						}

						if (game != null) {
							rematchRequests.put(username, true);

							if (rematchRequests.containsKey(opponent) && rematchRequests.get(opponent)) {
								game.reset();

								rematchRequests.remove(username);
								rematchRequests.remove(opponent);

								try {
									ClientThread player1Client = usernameToClient.get(game.player1);
									ClientThread player2Client = usernameToClient.get(game.player2);

									String currentPlayer = game.getCurrentPlayer();

									player1Client.out.writeObject(new Message(MessageType.GAME_STATE,
											game.getBoard(), currentPlayer.equals(game.player1)));
									player2Client.out.writeObject(new Message(MessageType.GAME_STATE,
											game.getBoard(), currentPlayer.equals(game.player2)));

									callback.accept(new Message("Game restarted between " + game.player1 +
											" and " + game.player2));
								} catch (Exception e) {
									System.err.println("Error restarting game");
								}
							} else {
								try {
									out.writeObject(new Message(opponent + " has received your rematch request"));

									ClientThread opponentClient = usernameToClient.get(opponent);
									opponentClient.out.writeObject(new Message(username + " has requested a rematch"));
								} catch (Exception e) {
									System.err.println("Error with rematch request");
								}
							}
						}
					}
					break;

				case DISCONNECT:
					if (username != null) {
						usernames.remove(username);
						usernameToClient.remove(username);

						String disconnectedOpponent = playerPairs.get(username);
						if (disconnectedOpponent != null) {
							ClientThread opponentClient = usernameToClient.get(disconnectedOpponent);

							try {
								Message userLeft = new Message(MessageType.DISCONNECT, username);
								opponentClient.out.writeObject(userLeft);
							} catch (Exception e) {
								System.err.println("Error with disconnect");
							}

							playerPairs.remove(username);
							playerPairs.remove(disconnectedOpponent);
							games.remove(username);
							games.remove(disconnectedOpponent);
						}
					}
					break;
			}
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);
			} catch (Exception e) {
				System.out.println("Streams not open: " + e.getMessage());
			}

			while (true) {
				try {
					Message data = (Message) in.readObject();
					if (data.type == MessageType.LOGIN) {
						if (!usernames.contains(data.username)) {
							callback.accept(data);
						}
					} else {
						callback.accept(data);
					}
					updateClients(data);
				} catch (Exception e) {
					clients.remove(this);
					break;
				}
			}
		}
	}
}