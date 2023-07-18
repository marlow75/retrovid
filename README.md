# Simple Video Converter for retro machines

You can make a short clip for your retro computer. Just open your favorite movie, select scene and grab it. Thats all.

For now only PETSCII C64 is supported.

![PetsciiVID](retrovid.png)

Compression mode

- Code and color - all changes are recorded, screen codes and colors
- Codes - only changed screen codes and theirs colors are recorded

Converter mode

- One hidden layer - one layer neural network, prefers semigraphics
- Two hidden layers - two layers neural network, prefers characters

Contrast processing

- none - no postprocessing at all
- HE - histogram equalizer (global)
- CLAHE - clipped adaptive histogram equalizer (local) with sharpen level 

Requirements: please install video library for JAVA, [javacv](https://sourceforge.net/projects/javacv.mirror/)

All javacv libraries must be put next to retrovid.jar file in directory named retrovid_lib.