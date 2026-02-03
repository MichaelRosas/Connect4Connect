# 🔴🟡 Connect4Connect

A real-time multiplayer Connect-4 game featuring a modern JavaFX interface, lobby-based matchmaking, and live chat functionality. Challenge friends or other players in this classic strategy game with smooth animations and responsive gameplay.

## 📋 Overview

Connect4Connect is a client-server application that enables multiple players to compete in Connect-4 matches through an intuitive graphical interface. Players can browse available opponents in a lobby, send challenges, and engage in real-time games with built-in chat support.

## ✨ Key Features

### Gameplay

- **Real-time Multiplayer**: Play against other connected players with instant move synchronization
- **Animated Piece Drops**: Smooth physics-based animations with bounce effects
- **Win Detection**: Automatic detection of horizontal, vertical, and diagonal wins
- **Draw Detection**: Recognizes when the board is full with no winner
- **Turn Indicators**: Clear visual feedback showing whose turn it is
- **Rematch System**: Both players must agree before starting a new game

### Lobby System

- **Player List**: Browse all available players in the lobby
- **Challenge Mechanism**: Send challenges to specific players
- **Accept/Decline**: Modal dialogs for responding to incoming challenges
- **Dynamic Updates**: Real-time lobby updates when players join, leave, or enter games
- **Auto-filtering**: Players in active games are automatically hidden from the lobby

### Communication

- **In-game Chat**: Direct messaging between matched opponents
- **System Messages**: Notifications for game events (joins, wins, disconnections)
- **Real-time Messaging**: Instant message delivery using socket streams

### User Experience

- **Username Validation**: Enforces unique usernames with length limits (max 20 characters)
- **Gradient UI**: Beautiful color gradients for login, lobby, and game screens
- **Custom Animations**: Scale transitions and glow effects for game events
- **Disconnect Handling**: Graceful cleanup and opponent notification on disconnection
- **Resource Management**: Proper socket and stream cleanup prevents memory leaks

## 🛠️ Tech Stack

### Core Technologies

- **Java 8** - Core programming language
- **JavaFX 19** - Modern UI framework with rich graphics capabilities
- **Maven** - Dependency management and build automation

### Networking

- **Java Sockets** - TCP/IP communication between client and server
- **ObjectInputStream/ObjectOutputStream** - Serialized message passing
- **Multi-threading** - Each client connection runs on a separate thread
- **TCP_NODELAY** - Disabled Nagle's algorithm for reduced latency

### Design Patterns

- **Client-Server Architecture** - Centralized game state management
- **Observer Pattern** - Callback-based message handling
- **State Management** - Synchronized game state across clients
- **Message-based Protocol** - Typed messages for different operations

## 🏗️ Project Architecture

```
Connect4Connect/
├── Client/
│   ├── src/main/java/
│   │   ├── GuiClient.java       # JavaFX GUI and game logic
│   │   ├── Client.java          # Network client with socket handling
│   │   ├── Message.java         # Serializable message object
│   │   └── MessageType.java     # Enum for message types
│   └── pom.xml                  # Maven configuration
│
└── Server/
    ├── src/main/java/
    │   ├── GuiServer.java       # Server GUI for monitoring
    │   ├── Server.java          # Multi-threaded server logic
    │   ├── Game.java            # Connect-4 game engine
    │   ├── Message.java         # Serializable message object
    │   └── MessageType.java     # Enum for message types
    └── pom.xml                  # Maven configuration
```

### Key Components

**GuiClient.java** - Main client application with three scenes:
- Login screen with username validation
- Lobby screen with player list and challenge buttons
- Game screen with 6x7 board, chat, and status display

**Server.java** - Handles multiple concurrent clients:
- Player registration and username uniqueness
- Challenge routing between players
- Game state synchronization
- Message broadcasting and direct messaging
- Resource cleanup on disconnect

**Game.java** - Core game engine:
- 6x7 board representation (2D integer array)
- Move validation (turn checking, gravity simulation)
- Win detection (4 directions: horizontal, vertical, 2 diagonals)
- Draw detection (full board check)
- Turn management and game state control

**Message Protocol** - Type-safe communication:
- `LOGIN` / `LOGIN_SUCCESS` / `LOGIN_ERROR`
- `LOBBY_UPDATE` - Broadcast available players
- `CHALLENGE_REQUEST` / `CHALLENGE_ACCEPT` / `CHALLENGE_DECLINE`
- `GAME_STATE` / `GAME_MOVE` - Board synchronization
- `GAME_WIN` / `GAME_LOSE` / `GAME_DRAW`
- `TEXT` - Chat messages
- `DISCONNECT` - Player leaving

## 🚀 Getting Started

### Prerequisites

- **Java JDK 8+** - Required for running the application
- **Maven 3.6+** - For dependency management and building
- **JavaFX SDK 19** - Bundled via Maven dependencies

### Installation

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/Connect4Connect.git
cd Connect4Connect
```

2. **Build the projects**

```bash
# Build server
cd Server
mvn clean compile

# Build client
cd ../Client
mvn clean compile
```

3. **Start the server**

```bash
cd Server
mvn javafx:run
```

The server will start listening on port `5555` and display a monitoring GUI showing connected users and game events.

4. **Launch client instances** (in separate terminals)

```bash
cd Client
mvn javafx:run
```

Launch multiple client instances to simulate different players. Each client connects to `localhost:5555`.

## 🎮 How to Play

1. **Login**: Enter a unique username (1-20 characters)
2. **Lobby**: Browse the list of available players
3. **Challenge**: Click the "Challenge" button next to a player's name
4. **Accept/Decline**: The challenged player receives a modal dialog
5. **Play**: Click columns to drop your colored piece
   - Red pieces for Player 1
   - Yellow pieces for Player 2
6. **Win**: Connect 4 pieces horizontally, vertically, or diagonally
7. **Rematch**: Both players can request a rematch after the game ends
8. **Chat**: Use the chat box to communicate with your opponent

### Game Rules

- Players alternate turns
- Pieces drop to the lowest available position in a column
- First to connect 4 pieces in a row wins
- If the board fills with no winner, the game is a draw
- Clicking during piece animation is disabled to prevent double-moves

## 🔧 Configuration

### Server Port

To change the server port, edit the port number in:
- `Server/src/main/java/Server.java` - Line with `new ServerSocket(5555)`
- `Client/src/main/java/Client.java` - Line with `new Socket("127.0.0.1", 5555)`

### Server IP Address

For network play across different machines:
- Update `Client.java` to replace `"127.0.0.1"` with the server's IP address
- Ensure firewall rules allow TCP connections on port 5555

## 📝 Available Scripts

```bash
# Server
mvn javafx:run          # Start server GUI
mvn clean               # Clean build artifacts
mvn compile             # Compile source code

# Client
mvn javafx:run          # Start client GUI
mvn clean               # Clean build artifacts
mvn compile             # Compile source code
```

## 🔐 Network Protocol

### Connection Flow

1. Client opens socket to server on port 5555
2. Client sends `LOGIN` message with username
3. Server validates username (unique, not empty, ≤20 chars)
4. Server responds with `LOGIN_SUCCESS` or `LOGIN_ERROR`
5. Client receives `LOBBY_UPDATE` with available players

### Game Flow

1. Player A sends `CHALLENGE_REQUEST` with Player B's username
2. Server forwards challenge to Player B
3. Player B responds with `CHALLENGE_ACCEPT` or `CHALLENGE_DECLINE`
4. On accept: Server pairs players and sends `GAME_STATE` to both
5. Players alternate sending `GAME_MOVE` with column index
6. Server validates moves and broadcasts updated `GAME_STATE`
7. Server detects win/draw and sends result messages

### Disconnect Handling

- Server detects broken socket connection
- Automatically cleans up player from all data structures
- Notifies opponent with `DISCONNECT` message
- Opponent returns to lobby
- Resources (streams, sockets) are properly closed