# Remote PC Control System

A two-part project designed to remotely control a PC (shutdown, restart, sleep) from another device via secure communication and VPN. This setup is ideal for use with Siri Shortcuts, a Java client app, or other automation workflows.

---

## 🔧 Project Structure

### 1. **PC Daemon (Java → later Go)**

A lightweight background service running on the target PC. It listens for SSH-triggered commands and executes system-level actions like shutdown, restart, or sleep.

#### ✅ Responsibilities
- Receive commands over SSH (no open ports required)
- Execute OS-specific system commands
- Run persistently in the background
- Secure via SSH key-based authentication
- Log executed commands (optional)

#### 🚧 Development To-Do
- [x] Build initial Java-based command execution service
- [ ] Add OS-specific command logic:
  - Windows: `shutdown`, `rundll32 powrprof.dll...`
  - Linux/macOS: `systemctl`, `shutdown`, `pmset`, etc.
- [ ] Support CLI arguments (e.g., `java -jar daemon.jar shutdown`)
- [ ] Set up as a system service (Windows Task Scheduler, systemd, etc.)
- [ ] Later: Port to Go for performance and native deployment

---

### 2. **Java User App (Controller App)**

A user-facing app that connects to the PC over a private VPN and issues remote commands via SSH. The app can also manage VPN profiles for each target PC.

#### ✅ Responsibilities
- Manage per-PC VPN profiles
- Initiate and close VPN connections
- SSH into remote PC and send commands
- Provide simple UI or CLI to select action and target PC

#### 🚧 Development To-Do
- [ ] Initialize Java project (CLI or GUI)
- [ ] Integrate SSH functionality (e.g., with JSch library)
- [ ] Implement VPN management:
  - OpenVPN: `openvpn --config <file>`
  - WireGuard: `wg-quick up/down <profile>`
- [ ] UI Features:
  - [ ] PC profile selection
  - [ ] Connect VPN / Disconnect VPN
  - [ ] Send command: shutdown, restart, sleep
- [ ] Persist configuration (e.g., JSON/properties file)
- [ ] Optional: silent/automated mode (for Siri Shortcuts)

---

## 🔐 Security & Networking

- SSH is used for secure, encrypted command execution.
- No open ports are required — all communication goes through a personal VPN.
- SSH authentication is handled via public/private key pairs.

---

## 📦 Future Enhancements

- [ ] Native system tray app (JavaFX or Swing)
- [ ] Real-time online/offline status detection
- [ ] Command queue and retry logic
- [ ] GUI VPN profile manager
- [ ] Rewritten PC daemon in Go for performance and minimal memory usage
- [ ] Siri Shortcut integration using SSH over VPN

---

## 🧪 Tech Stack

| Component      | Language | Notes                                  |
|----------------|----------|----------------------------------------|
| PC Daemon      | Java → Go | Background agent, receives commands    |
| User App       | Java      | Sends commands via SSH, controls VPN   |
| VPN            | Any       | Prefer OpenVPN or WireGuard            |
| Communication  | SSH       | Secure command execution               |

---

## 📁 Example Usage

1. Start VPN to the target PC
2. Run the Java app or Siri Shortcut
3. Choose a PC and action (shutdown, restart, sleep)
4. App connects via SSH and triggers the daemon

---

## License

MIT — do whatever you want, but give credit if this helps you.
