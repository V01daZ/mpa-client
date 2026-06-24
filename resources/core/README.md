# resources/core

Сюда автоматически кладутся (скриптом `scripts/fetch-core.mjs`,
запускается перед `npm run dev` и `npm run build`):

- `sing-box.exe` - ядро (VLESS + Reality + Vision, обычный прокси-режим и TUN)
- `wintun.dll` - драйвер TUN-адаптера для Windows (нужен только для TUN-режима)

Если автозагрузка не сработала (нет интернета на этапе сборки и т.п.):

- sing-box: https://github.com/SagerNet/sing-box/releases - архив `*-windows-amd64.zip`,
  внутри лежит `sing-box.exe`
- wintun: https://www.wintun.net/ - в архиве `wintun/bin/amd64/wintun.dll`

Оба файла кладутся прямо в эту папку (`resources/core/sing-box.exe`,
`resources/core/wintun.dll`), без подпапок.
