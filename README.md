# Simple Video Converter for retro machines

Now you can make a short clip for your retro computer. Just open your favorite movie, select scene and grab it. Thats all.

For now only PETSCII C64 is supported.

Requirements: please install video library for JAVA and JRE16, [javacv](https://sourceforge.net/projects/javacv.mirror/)
All javacv libraries must be put next to retrovid.jar file in directory named libs. 

RetroVID.zip contains executable template with libs folder without FFMpeg library!!! Check version in jar manifest.

![PetsciiVID](retrovid.png)

Medium

- PRG - C64 program, simply type RUN command in BASIC
- CRT - Game System C64 cartridge capable of holding 512 kB, clips can be 2 minuts long
 
Compression mode

- Code and color - all changes are recorded, screen codes and colors
- Codes - only changed screen codes and theirs colors are recorded

Converter mode

- Semigraphics - prefers semigraphics
- Characters - prefers characters

Contrast processing

- none - no postprocessing at all
- HE - histogram equalizer (global)
- CLAHE - clipped adaptive histogram equalizer (local) with sharpen level 

