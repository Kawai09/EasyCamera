# Easy Camera Mod

![Modrinth](https://img.shields.io/modrinth/dt/easycamera?color=00AF5C&label=downloads&logo=modrinth)

A Minecraft mod that allows players to stream their webcam in-game, displaying it as a circle above their head and on the HUD. This project consists of a **Fabric mod** for the client and a **Spigot plugin** for the server to enable server-side control and communication.

*(Image: A screenshot showing several players in-game with webcam circles above their heads.)*

## ✨ Inspiration

This project was heavily inspired by the unique webcam feature seen on the FreakLand server. My goal was to recreate and improve upon that amazing idea, making it available for everyone to enjoy on their own Fabric and Spigot servers.

## 🚀 Features

- **Real-time Webcam Streaming**: Broadcast your webcam directly into the game.
- **In-Game Player Indicator**: See a circle with the player's webcam feed right above their head.
- **Customizable HUD**: Display your own or another player's webcam in a customizable circle on your screen.
- **Mute System**: Easily mute/unmute other players' webcams through a simple UI (', COMMA' key by default).
- **Server-Side Control**: The Spigot plugin allows servers to manage which players' data is broadcast.
- **Performance Optimized**: Lightweight and designed to minimize network and client-side lag.
- **Configurable Keybinds**: Change the keybinds for settings (. PERIOD') and the mute menu (', COMMA') in Minecraft's controls menu.

## 🔧 Installation & Usage

This project requires both a client-side mod and a server-side plugin to function fully.

### For Players (Client-Side)

You need to install the Fabric mod to see and broadcast webcams.

1.  Download and install the [Fabric Loader](https://fabricmc.net/use/installer/).
2.  Download the latest **Fabric mod `.jar`** file from the [Modrinth Page](https://modrinth.com/plugin/easycamera).
3.  Place the `.jar` file into your `.minecraft/mods` folder.
4.  Launch the game. Use the **'. PERIOD'** key to open settings and the **', COMMA'** key to open the mute menu.

### For Server Admins (Server-Side)

You need to install the Spigot plugin to enable communication between clients.

1.  Ensure your server is running a Spigot-compatible version (like Spigot, Paper, Purpur, etc.).
2.  Download the latest **Spigot plugin `.jar`** file from the [Modrinth Page](https://modrinth.com/plugin/easycamera).
3.  Place the `.jar` file into your server's `plugins` folder.
4.  Restart or reload your server.

## 🛠️ Building from Source

If you want to compile the project yourself, follow these steps:

1.  **Prerequisites:**
    - You will need **Gradle 8.8** installed to build the project.

2.  **Clone the repository:**
    ```sh
    git clone https://github.com/Kawai09/EasyCamera.git
    cd EasyCamera
    ```

3.  **Build the project:**
    - On Windows:
      ```sh
      gradle build
      ```
    - On macOS/Linux:
      ```sh
      gradle build
      ```

4.  The compiled `.jar` files will be located in:
    - `build/libs/` for the Fabric mod.
    - `spigot/build/libs/` for the Spigot plugin.

## 🙌 Contributing

Contributions are welcome! If you have ideas for new features or find a bug, feel free to open an issue or submit a pull request.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for more details. 