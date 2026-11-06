---
name: MPA
description: Private VPN client for Android. The connection is a boarding pass; servers are a departure board ranked by ping.
colors:
  ground: "#18181B"
  board: "#202024"
  board-raised: "#29292E"
  rule: "#34343A"
  on-board: "#E7E6E2"
  on-board-muted: "#9C9CA4"
  paper: "#E9E7E2"
  paper-ink: "#18181B"
  paper-muted: "#66656B"
  paper-rule: "#C4C2BC"
  signal: "#EE8A6E"
  signal-wash: "#EE8A6E26"
  go: "#6FC495"
  go-deep: "#2F7D53"
  fail: "#F07B7B"
  fail-deep: "#B4413F"
typography:
  display:
    fontFamily: "JetBrains Mono, monospace"
    fontSize: "36sp"
    fontWeight: 700
    lineHeight: "44sp"
    letterSpacing: "-0.5sp"
  headline:
    fontFamily: "Golos Text, sans-serif"
    fontSize: "24sp"
    fontWeight: 700
    lineHeight: "32sp"
  title:
    fontFamily: "Golos Text, sans-serif"
    fontSize: "22sp"
    fontWeight: 700
    lineHeight: "28sp"
  title-medium:
    fontFamily: "Golos Text, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "24sp"
  body:
    fontFamily: "Golos Text, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  button:
    fontFamily: "Golos Text, sans-serif"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "20sp"
  label:
    fontFamily: "Golos Text, sans-serif"
    fontSize: "11sp"
    fontWeight: 600
    lineHeight: "14sp"
    letterSpacing: "0.9sp"
  data:
    fontFamily: "JetBrains Mono, monospace"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "18sp"
  pass-value:
    fontFamily: "JetBrains Mono, monospace"
    fontSize: "24sp"
    fontWeight: 700
rounded:
  xs: "6dp"
  sm: "10dp"
  md: "14dp"
  ticket: "18dp"
  lg: "20dp"
  xl: "28dp"
  full: "9999dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  gutter: "16dp"
  pass: "20dp"
components:
  pass-button-off:
    backgroundColor: "{colors.paper-ink}"
    textColor: "{colors.paper}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    height: "56dp"
  pass-button-connected:
    backgroundColor: "{colors.go}"
    textColor: "{colors.paper-ink}"
    typography: "{typography.button}"
    rounded: "{rounded.md}"
    height: "56dp"
  pass-button-disabled:
    backgroundColor: "{colors.paper-rule}"
    textColor: "{colors.paper-muted}"
    rounded: "{rounded.md}"
    height: "56dp"
  button-paper:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.paper-ink}"
    typography: "{typography.button}"
    rounded: "{rounded.full}"
    height: "48dp"
  boarding-pass:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.paper-ink}"
    rounded: "{rounded.ticket}"
    padding: "18dp 20dp 20dp 20dp"
  board-row:
    textColor: "{colors.on-board}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    height: "56dp"
  board-row-active:
    backgroundColor: "{colors.board-raised}"
    textColor: "{colors.on-board}"
    rounded: "{rounded.md}"
  board-row-changed:
    backgroundColor: "{colors.signal-wash}"
    textColor: "{colors.on-board}"
    rounded: "{rounded.md}"
  board-header:
    textColor: "{colors.on-board-muted}"
    typography: "{typography.label}"
  dialog:
    backgroundColor: "{colors.board}"
    textColor: "{colors.on-board}"
    rounded: "{rounded.xl}"
---

# Design System: MPA

## Overview

**Creative North Star: "The Gate at Night"**

The app is an airport gate after dark. The ground is graphite, the server list is a departure board printed in pale ink on that graphite, and the active connection is a paper boarding pass held up against it. One object on the screen is warm and tactile (the pass); everything else is the quiet, dense, readable board. This replaces the category default of a glowing round power button on a dark gradient.

The palette was softened by the user after the first build ("Графит + коралл"): no pure white, no acid tones. Paper is warm matte (#E9E7E2), graphite is slightly cool. Colour carries meaning and nothing else: green means connected, coral means something changed or needs attention, red means failure. Every state also has a word ("Подключено", "Не защищено", "Ошибка") so colour is never the only signal.

Words are set in Golos Text; every number, address and timer is set in JetBrains Mono, so ping and elapsed time read like a board's tabular figures. Density is calm: one pass, one primary button, then single-line rows.

**Key Characteristics:**
- Graphite board ground with one warm paper pass as the only light object.
- Pass silhouette: rounded ticket, semicircle notches, dashed perforation.
- Three signal colours, each with exactly one meaning.
- Golos Text for words, JetBrains Mono for numbers.
- Flat: depth comes from tone (ground, board, raised) and paper contrast, never shadow.

## Colors

A cool graphite board, warm paper, and three strictly assigned signals.

### Primary
- **Gate Green** (`go`): connected, and only connected. Fills the pass button when it reads «Отключить», the selection dot of the server you are connected through, the launcher icon's light. Mapped to Material `primary`.
- **Deep Gate Green** (`go-deep`): the "Подключено" status dot on paper, where light green would be too weak.

### Secondary
- **Soft Coral Signal** (`signal`): change and attention. Ping text of a changed row, progress spinners, active split-tunnel action icon, update banner border and action. Mapped to Material `secondary`/`tertiary`.
- **Coral Wash** (`signal-wash`, coral at 15% alpha): background of a board row whose ping changed noticeably; held until the user next interacts.

### Tertiary
- **Soft Fail Red** (`fail`): failure on the board. "нет связи" ping, «Удалить» in row menus. Mapped to Material `error`.
- **Deep Fail Red** (`fail-deep`): failure on paper. Error message under the pass status, "×" ping, "Ошибка" dot.

### Neutral
- **Graphite Ground** (`ground`): screen background, top app bars, list item containers.
- **Board** (`board`): dialogs, empty-board card, banners.
- **Raised Board** (`board-raised`): selected board row, checked split-tunnel item.
- **Board Rule** (`rule`): 1dp dividers, board header rule, unselected radio ring, progress track.
- **Board Ink** (`on-board`): primary text on graphite.
- **Board Muted** (`on-board-muted`): column labels, secondary text, icon tint.
- **Warm Paper** (`paper`): the boarding pass, paper-filled secondary buttons, selected-but-not-connected dot, QR viewfinder frame.
- **Paper Ink** (`paper-ink`): text on paper, and the pass button fill in every state except connected.
- **Paper Muted** (`paper-muted`): pass field labels, "мс" unit, arrow.
- **Paper Rule** (`paper-rule`): perforation dashes, disabled pass button.

### Named Rules
**The Green Means Connected Rule.** Green appears only when the tunnel is up. The pass button is Paper Ink with paper text for «Подключить», «Повторить», «Отменить» and «Добавить сервер»; it turns green only as «Отключить». "Не защищено" must never look like "all good".

**The Coral Is News Rule.** Coral marks something that changed or wants a look (re-ranked ping, in-progress work, an available update). It is never decoration and never a failure colour for the connection.

**The Red Is Failure Rule.** Red is reserved for failure and destructive actions: no connection, error text, «Удалить».

**The No Pure White Rule.** The lightest value is Warm Paper (#E9E7E2). No #FFFFFF anywhere.

## Typography

**Body Font:** Golos Text (variable, bundled in res/font)
**Label/Mono Font:** JetBrains Mono (variable, bundled in res/font)

**Character:** Golos is a Russian grotesque with first-class Cyrillic, plain and friendly for "ты" copy. JetBrains Mono gives every figure a fixed width, so ping and timers align and tick without jitter.

### Hierarchy
- **Display** (Mono 700, Material displaySmall size, -0.5sp): large figures.
- **Headline / Title** (Golos 700, 24sp / 22sp): screen titles and the pass route ("Ты", server name).
- **Title Medium / Small** (Golos 600, 16sp / 14sp): status value on the pass, group headers.
- **Body** (Golos 400, 16 / 14 / 12sp): board row server names, dialog copy, secondary lines.
- **Button** (Golos 600, 16sp): pass button and primary actions.
- **Label** (Golos 600, 11sp, +0.9sp tracking, uppercase copy): pass field labels ОТКУДА / КУДА / ПИНГ / СТАТУС / В ПУТИ and the board header СЕРВЕР / ПИНГ.
- **Data** (Mono 500, 14sp): ping in rows ("42 мс"), server address in the row menu.
- **Pass Value** (Mono 700, 24sp; 20sp for elapsed time): ping and "00:12:05" on the pass.

The top bar wordmark "MPA" is Golos 700, 22sp, +1sp tracking.

### Named Rules
**The Words and Numbers Rule.** Golos Text sets words; JetBrains Mono sets numbers, units next to them, addresses and timers. Never a number in Golos, never a sentence in Mono.

**The Field Label Rule.** Uppercase tracked labels exist only as field names on the pass and column names on the board, exactly like a printed ticket. They always label a value directly beneath or beside them.

## Layout

Single column, phone-first, edge-to-edge with system insets respected. The pass sits full width minus a 16dp gutter. Inside the pass, segments are padded 20dp horizontally, 18dp top, 20dp bottom, with 12dp between route fields and 4dp between a label and its value. The board runs below: header text inset 20dp, rows inset 8dp with 12dp inner start padding, minimum row height 56dp, minimum touch target 48dp everywhere. The board header's right inset (56dp) aligns ПИНГ over the values, left of the row menu button. Board rows animate to their new rank in place when ping re-sorts them.

## Elevation & Depth

Flat. No shadows on any surface. Depth is tonal: Ground (#18181B) under Board (#202024) under Raised Board (#29292E), plus the strong contrast step to Warm Paper for the pass. State changes animate colour (row background 400ms tween, status dot crossfade), never lift.

### Named Rules
**The Paper Is the Elevation Rule.** The only thing that "comes forward" is the paper pass, and it does so by material contrast, not by shadow.

## Shapes

Soft, rounded rectangles from the Material shape scale (6 / 10 / 14 / 20 / 28dp). Buttons and rows use 14dp; cards and banners 20dp; dialogs 28dp. The pass is the signature silhouette: two stacked segments with 18dp outer corners, flat inner corners, and two 10dp-radius semicircle notches cut where they meet, joined by a 2dp dashed perforation (6dp dash, 5dp gap) in Paper Rule. Status and selection marks are 10-12dp circles. Dividers are 1dp hairlines.

## Components

### Buttons
- **Shape:** gently rounded (14dp) for the pass button; Material default pill for other filled buttons.
- **Pass button:** full width, 56dp tall. Paper Ink fill with paper text when off, error, connecting or empty; Gate Green fill with ink text only when connected («Отключить»). Disabled (during «Отключение…») is Paper Rule with Paper Muted text. A coral 18dp spinner precedes the label while busy.
- **Paper button:** Warm Paper fill, Paper Ink text, for the primary action on graphite («Вставить ключ», dialog confirm).
- **Outlined:** Rule outline with Board Ink text and icon («QR-код»).
- **Text:** Board Ink text, for the board's last row «Добавить сервер» with a leading plus icon.
- **Coral button:** Coral fill, ink text; only for the update banner action.

### Boarding Pass (signature)
Top segment: ОТКУДА "Ты" → КУДА server name → ПИНГ figure with "мс". Perforation. Bottom segment: СТАТУС (dot + word, polite live region) and В ПУТИ timer, an optional Deep Fail Red error message, then the pass button. Missing values show "—", unknown ping "…", failed ping "×".

### Departure Board (signature)
- **Header:** one row, СЕРВЕР left and ПИНГ right, in Board Muted labels, with one 1dp Rule beneath. No other header.
- **Rows:** single line only: selection dot, server name (Body, ellipsised), ping in Data style, overflow menu. Active row sits on Raised Board; a changed row sits on Coral Wash with coral ping text. Selection dot: green fill when connected through it, paper fill when selected, 1.5dp Rule ring otherwise.
- **Row menu:** the address and port, in muted Mono, is shown here only, never in the row; then «Удалить» in Fail Red.
- **Rules:** 1dp Rule dividers between groups, inset 20dp.
- **Group header:** subscription title, "3 сервера" count, refresh and menu icons, collapse chevron.
- **Last row:** «Добавить сервер» always ends the board.
- **Empty board:** Board card (20dp corners) with "Табло пустое", a short hint, and stacked «Вставить ключ» / «QR-код» buttons.

### Dialogs and Banners
Dialogs are Board-filled. The update banner is a Board card with a half-opacity Coral 1dp border, a coral progress bar on a Rule track, and a coral action button.

### Navigation
Material top app bar on Ground: "MPA" wordmark, Board Ink title, Board Muted action icons (coral when split tunnelling is on). Sub-screens use a back arrow in Board Ink. System Back is honoured.

## Do's and Don'ts

### Do:
- **Do** keep the pass button Paper Ink with paper text in every state except connected; only «Отключить» is green.
- **Do** set every figure (ping, timer, port, address) in JetBrains Mono, and every word in Golos Text.
- **Do** keep board rows to one line; put server details in the row's overflow menu.
- **Do** end the board with a «Добавить сервер» row.
- **Do** pair every state colour with a word, so state never relies on colour alone.
- **Do** keep touch targets at 48dp minimum and text in sp.

### Don't:
- **Don't** use green for anything but an active connection, including success toasts or "ready" states.
- **Don't** use coral for connection failure, or red for "something changed".
- **Don't** use pure white (#FFFFFF) or add shadows; depth is tonal.
- **Don't** add a second header row or per-group column headers to the board; one СЕРВЕР | ПИНГ header with one rule.
- **Don't** show server addresses in board rows.
- **Don't** reintroduce a round power button or glowing gradient as the connect control.
