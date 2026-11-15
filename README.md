# FriendChat

Tabbed chat for Minecraft (Fabric, client-side only): **Général**, **Amis**, **MP**.

## How it works

Every incoming message is kept in a bounded history (`historySize`). The chat shows only
the messages of the active tab; switching tab clears the display and replays the stored
messages of the new tab, so hidden messages are never lost.

- **Général** — everything.
- **Amis** — only messages sent by players in your friends list (plus whispers with friends,
  and system messages that mention a friend, e.g. "Bob joined the game").
- **MP** — only your private conversation with the targeted player. Everything you type
  in this tab (not starting with `/`) is sent as `/<servermsg> <player> <message>`.

Tabs are drawn between the chat history and the input box; click to switch.

## Commands (all client-side)

- `/chatfriend add|remove <player>`, `/chatfriend list`
- `/chatmp <player>` — open the MP tab on that player; `/chatmp close` closes it
- `/chatblock <player> [all|mp|friend|normal|messages]`, `/chatblock <player> unblock [scope]`, `/chatblock list`
- `/chatclearhistory`
- `/friendchatconfig` — show the config; `/friendchatconfig <key> <value>` to change it:

| key | meaning |
|---|---|
| `servermsg` | private-message command used by the MP tab (`msg`, `tell`, `w`, `pm`...) |
| `mpalwaysinfriendstab` | show every whisper in the Amis tab |
| `mppublic` | MP tab also shows the target's public messages |
| `systemmessages` | show system messages in all tabs (default: Général only) |
| `historysize` | number of messages kept in memory |
| `friend` `mp` `config` `block` `clearhistory` | rename commands (restart required) |

## Config files (`config/`)

`friendchat_config.json`, `friendchat_friends.json`, `friendchat_blocked.json`.

If your server formats chat differently, edit the regex lists in `friendchat_config.json`
(each needs a named group `player`):
- `chatPatterns` — public messages (default: any message starting with `<x>`, even from the server, plus, `[Rank] Name: msg`, `Name » msg`)
- `mpPatterns` — whispers (default handles vanilla and Essentials formats)
- `alwaysShowPatterns` — messages forced into every tab

## Requirements

Minecraft 1.21.11, Fabric Loader 0.18.1+, Fabric API, Java 21+.

MIT
