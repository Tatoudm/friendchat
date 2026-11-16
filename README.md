# FriendChat

Tabbed chat for Minecraft 26.2 (Fabric, client-side only): **General**, **Friends**, **DM**.

## How it works

Every incoming message is kept in a bounded history (`historySize`). The chat shows only
the messages of the active tab; switching tab clears the display and replays the stored
messages of the new tab, so hidden messages are never lost.

- **General** — everything.
- **Friends** — only messages sent by players in your friends list (plus private messages with friends,
  and system messages that mention a friend, e.g. "Bob joined the game").
- **DM** — only your private conversation with the targeted player. Everything you type
  in this tab (not starting with `/`) is sent as `/<servermsg> <player> <message>`.

Tabs are drawn between the chat history and the input box; click to switch.

## Commands (all client-side)

- `/chatfriend add|remove <player>`, `/chatfriend list`
- `/chatmp <player>` — open the DM tab for that player; `/chatmp close` closes it
- `/chatblock <player> [all|mp|friend|normal|messages]`, `/chatblock <player> unblock [scope]`, `/chatblock list`
- `/chatclearhistory`
- `/friendchatconfig` — show the config; `/friendchatconfig <key> <value>` to change it:

| key | meaning |
|---|---|
| `servermsg` | private-message command used by the DM tab (`msg`, `tell`, `w`, `pm`...) |
| `mpalwaysinfriendstab` | show every private message in the Friends tab |
| `mppublic` | DM tab also shows the target's public messages |
| `systemmessages` | show system messages in all tabs (default: General only) |
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

Minecraft 26.2, Fabric Loader 0.20.1+, Fabric API 0.161.0+26.2, Java 25+.

MIT
