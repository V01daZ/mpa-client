---
version: 1
slug: "main-java-dev-mpa-client-ui-mainscreen-kt-f76fcb02"
primary_target: "android-client/app/src/main/java/dev/mpa/client/ui/MainScreen.kt"
related_targets: []
---

# Surface: Android main screen (and its sub-screens)

Mode: Operate. Audience: non-technical friends/bot subscribers. Task: connect in one tap, see state and server, switch server, add key, exclude apps. Constraints: Material 3 controls, Russian copy, system Back, insets.

## Direction contract

THESIS: The connection is your boarding pass; servers are a departure board ranked by ping. Refuses the category default of a glowing round power button on a dark gradient.

OWN-WORLD: Graphite ground #18181B, the pass printed on soft warm paper #E9E7E2 (never pure white) with a dashed perforation and semicircle notches, tabular JetBrains Mono for every number, Golos Text for words. Soft green #6FC495 only for "connected" (and the Отключить button); coral #EE8A6E only for changes and attention; red #F07B7B only for failure. Palette revised by the user after the build: chosen "Графит + коралл", softened.

STORY: User sees at once whether they are protected, through which server, how long; presses one button; scans the board to pick a faster server.

FIRST VIEWPORT: Top bar (MPA, app-filter action). Paper pass full width minus 16dp: segments ОТКУДА/КУДА/ПИНГ, perforation, СТАТУС/В ПУТИ, full-width primary button. Below: board header СЕРВЕР/ПИНГ and rows.

FORM: Boarding pass + gate board (challenger vernacular-ephemera-boarding-pass-and-gate-board, chosen by user). seed:2b7b4e436fc5835b

Signature move: board rows rerank by ping in place with animated item placement; a row whose state changed holds a coral wash until next interaction.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
