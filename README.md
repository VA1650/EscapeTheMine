# ⛏️ EscapeTheMine (ETM)

**EscapeTheMine** is a dynamic mini-game mode for Minecraft 1.12.2, inspired by classic prison escape games. Players are divided into two teams: the vigilant Guards and the resourceful Prisoners.

## 🎮 Gameplay

### 🔒 Prisoners 
Your goal is to stage a daring escape!
* **Repairing:** Locate crafting tables scattered across the map and repair them with a right-click.
* **Cooperation:** If a teammate is captured by a guard or trapped in a cell, hit them to set them free!
* **Escape:** Repair all crafting tables before time runs out to secure your path to freedom.

### 👮 Guards
Your mission is to suppress the rebellion!
* **Capture:** Strike prisoners with an iron sword to seize them.
* **Escorting:** Captured prisoners will follow you. Lead them directly to the cell.
* **Victory:** Capture all prisoners or hold them until the timer expires.

---

## ✨ Features
* **🌐 Multi-Arena System:** Full support for running multiple independent matches simultaneously on a single server.
* **Scoreboard Teams:** Individual `[P]` and `[G]` prefixes, plus colored nicknames in the TAB list and above player heads for each arena.
* **Progress Tracking:** Dynamic action bar repair meter with a timer, unique to every match.
* **Fair Randomization:** Improved role distribution algorithm (Double Shuffle) to prevent players from getting stuck with the same role.
* **Anti-Bug Logic:** Checks for distance limits, restricted actions while imprisoned, and ally rescue mechanics.

---

## 🛠️ Commands & Permissions
Managing arenas can be done via console or in-game:
* `/etm join <arena>` — Join a specific arena.
* `/etm create <arena>` — Create a new arena from a template.
* `/etm spectate <arena>` — Observe a game in progress.
* `/lobby` — Return to the lobby.

---

## 🏗️ Technical Stack
* **Version:** Spigot 1.12.2
* **Libraries:** Lombok, Bukkit API.
* **Architecture:** Object-oriented arena model with independent session managers.

---

Developed with ❤️ by **Annie312**.
