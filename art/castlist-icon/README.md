# Castlist icon sources

Direction A6: three list rows, the last one folding into a "C", with a play button. 108×108 adaptive-icon canvas.

- `bg_main.svg` / `fg_main.svg`: launcher background and foreground (release)
- `bg_debug.svg` / `fg_debug.svg`: grey variant for debug builds
- `mono.svg`: themed (monochrome) icon layer
- `sbg_*.svg`, `sw1_*.svg`, `sw2_*.svg`: splash screen layers (C-row and play, middle row, top row)

Render with `rsvg-convert -w <px> -h <px> file.svg -o file.png`.
