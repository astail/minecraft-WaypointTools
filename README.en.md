# WaypointTools

[日本語](README.md) | **English**

A Paper plugin that safely opens up part of Minecraft's official Waypoint (Locator Bar) feature to regular players.

- **`/wlist`** … Shows the waypoint list, equivalent to `/waypoint list`
- **`/wcolor <hex>`** … Lets a player freely change their own waypoint color (equivalent to `/waypoint modify @s color hex <hex>`)

---

## Background & Purpose

The official `/waypoint` command requires permission level 2, and the only permission node Paper exposes is `minecraft.command.waypoint` — **just one**.
Granting it via LuckPerms or similar allows `list` / `modify` / `remove` **all at once**, so you cannot open up things on a per-subcommand basis like "`list` only" or "changing one's own color only".

This plugin keeps `/waypoint` itself off-limits (especially `modify` / `remove`, which could edit others) while providing only the operations regular players need as dedicated commands.

---

## Requirements

| Item | Version |
| --- | --- |
| Server | Paper **26.1.2** (verified on build 69) |
| Java | **25** (verified on 25.0.1) |
| Build | JDK 25 + Maven (`brew install openjdk@25 maven`) |
| Dependency plugins | **None** (LuckPerms optional) |

> This single jar is all you need. No extra libraries or other plugins are required (`paper-api` is `provided` scope — supplied by the server at runtime).

---

## Command Reference

| Command | Arguments | Description | Executor | Permission |
| --- | --- | --- | --- | --- |
| `/wlist` | none | Shows a list equivalent to `/waypoint list` (the broadcasting waypoint's name / color / count) | Players only | `wlist.use` |
| `/wcolor <hex>` | 6-digit hex color | Changes your own waypoint color | Players only | `wcolor.use` |
| `/wcolor reset` | `reset` / `clear` | Resets your waypoint color to the default | Players only | `wcolor.use` |

### `/wcolor` input format

All of the following are interpreted as the same color (6-digit hex, case-insensitive).

```text
/wcolor F77E31
/wcolor #F77E31
/wcolor 0xF77E31
```

- Anything other than a 6-digit hex (e.g. `/wcolor xyz`, `/wcolor 123`) returns an error message.
- `/wcolor` affects **only yourself**; it cannot change another player's color.

### Tab completion

Pressing Tab after `/wcolor` suggests `reset` along with a preset of common hex colors (filtered by prefix match against what you've typed). Any 6-digit hex can still be entered directly as before.

```text
reset  FF0000  00FF00  0000FF  FFFF00  FF00FF  00FFFF  FFFFFF  F77E31
```

`/wlist` takes no arguments, so it shows no suggestions (the default player-name completion is suppressed).
The completion list is delivered to the client when the server starts, so after updating it takes effect on **restart + reconnect**.

---

## Permissions

| Permission node | Default | Description |
| --- | --- | --- |
| `wlist.use` | `true` (everyone) | Allows use of `/wlist` |
| `wcolor.use` | `true` (everyone) | Allows use of `/wcolor` |

By default all players can use both commands (no extra LuckPerms setup needed).
Only when you want to **deny** a specific player/group, set the relevant node to `false` for that target.

```bash
# Example: deny /wcolor for a group
lp group default permission set wcolor.use false
# Example: deny /wlist for a user
lp user <name> permission set wlist.use false
```

> `/waypoint` itself remains unusable by regular players unless you grant `minecraft.command.waypoint` as before. This plugin does not persistently grant that node.

---

## How It Works (technical notes)

The two commands use different implementation approaches (because the public API covers them differently).

### `/wlist`
Runs vanilla `/waypoint list` **as a proxy on behalf of the player themselves**.

- Paper 26.1 has no public API to "enumerate broadcasting waypoints", so the vanilla command is invoked.
- `/waypoint list` enumerates **per source world (dimension)**, so making the player the executor produces correct results (also correct in the Nether/End).
- Just for the moment of execution, `minecraft.command.waypoint` is temporarily granted to satisfy the permission-level-2 constraint. Grant and removal complete synchronously on the main thread, and only `list` is dispatched (fixed), so `modify` / `remove` can never run.

### `/wcolor`
Calls the public API `Player#setWaypointColor(Color)` directly.

- Equivalent to `/waypoint modify @s color hex <hex>`, a clean implementation requiring **neither privilege escalation nor dispatch**.
- Because it is called on the `player` object, **by construction it cannot affect anyone but yourself**.

### Tab completion (common)

Both commands implement `TabExecutor`; `/wcolor` returns color presets and `/wlist` returns an empty list, thereby suppressing the default online-player-name completion.

---

## Build

JDK 25 and Maven are required (if not installed: `brew install openjdk@25 maven`).
You can build with the included `deploy.sh` (**no Docker needed**).

```bash
./deploy.sh
```

Output: `target/WaypointTools-1.0.0.jar`

`deploy.sh` internally pins JDK 25 and runs `mvn clean package`.
To use a JDK elsewhere, override with `JAVA_HOME=/path/to/jdk25 ./deploy.sh`. To build directly:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean package
```

---

## Deploying to a Server

Place the jar in the server's `plugins/` directory and restart the server. There are two ways to obtain the jar.

### A. Use a release build (no build required, recommended)

Download the latest `WaypointTools-<version>.jar` from [Releases](https://github.com/astail/minecraft-WaypointTools/releases). No JDK or Maven needed.

```bash
# Download the jar from the latest release (using the gh CLI)
gh release download --repo astail/minecraft-WaypointTools --pattern '*.jar'

# Or directly with curl (fetches latest)
curl -LO "$(curl -s https://api.github.com/repos/astail/minecraft-WaypointTools/releases/latest \
  | grep browser_download_url | grep '\.jar' | cut -d '"' -f 4)"
```

Then place the downloaded jar into `plugins/` as described under "Placement" below.

### B. Build it yourself

Follow the [Build](#build) steps to produce `target/WaypointTools-1.0.0.jar`.

### Placement

Place the obtained jar in the server's `plugins/` directory and restart the server.

```bash
# If using a bind mount (copy to the host-side plugins directory)
cp target/WaypointTools-1.0.0.jar /path/to/data/plugins/
docker restart <container-name>

# For named volumes etc. (copy directly into the container)
docker cp target/WaypointTools-1.0.0.jar <container-name>:/data/plugins/
docker restart <container-name>
```

It succeeded if the following appears in the startup log (message is in Japanese):

```text
[WaypointTools] WaypointTools を有効化しました。/wlist, /wcolor が利用可能です。
```

---

## Project Structure

```text
.
├── pom.xml
├── deploy.sh
├── README.md
└── src/main/
    ├── java/io/github/astail/waypointtools/
    │   ├── WaypointToolsPlugin.java   # main (registers the 2 commands)
    │   ├── WListCommand.java          # /wlist
    │   └── WColorCommand.java         # /wcolor
    └── resources/plugin.yml
```

> The package name (`io.github.astail.waypointtools`) / `WaypointTools` / command names can be freely renamed (change pom.xml, each `package`, and `plugin.yml` in sync).

---

## Caveats

The following concern **`/wlist` only** (`/wcolor` calls the public API directly, so it is unaffected).

- **`sendCommandFeedback` gamerule**: If `false`, vanilla query output is suppressed and `/wlist` results may not be shown (default `true`). If nothing shows, check this gamerule.
- **If LuckPerms explicitly negates `minecraft.command.waypoint`**, the negation takes priority over the temporary grant and `/wlist` may not work. In that case, switch to the `Bukkit.createCommandSender(...)` approach (execute with console-equivalent permission and forward the output to the player) — note that enumeration will then be fixed to the Overworld.
- The `paper-api` build number can track server updates (e.g. `26.1.2.build.70-stable`).
