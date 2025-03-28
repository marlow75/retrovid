# Simple Video Converter for retro machines

Now you can make a short clip for your retro computer. Just open your favorite movie, select scene (slider, preview button) and grab it (grab button). Thats all.

## Versions

* 1.2 - NN reworked - slow but produces better output
* 1.1 - Improved palette & color handling.
* 1.0 - All charset characters are made by neural net, minor changes.
* 0.1 - C64 PETSCII and CHARSET Super CPU supported.

## Installation: 

* install JRE16 or newer
* install video library for JAVA [javacv](https://sourceforge.net/projects/javacv.mirror/)
* download libs directory and put JAVACV library there

Java users can run java builder with <b>mvn clean package</b> command. Maven builder is required to do that. Running from console can give you some hints and additional info about errors, missing libs, movie parameters etc.

Just type <b>java -jar retropic.jar</b> and do not close console window.

CRT directory contains cartridge examples

Credits to https://github.com/danielkleebinder - RetroVID uses new neural network approach with softmax to speed up learing (multithreaded).

## Usage

![C64 Petscii](c64petscii.png)

Medium

- PRG - C64 program, simply type RUN command in BASIC
- CRT - Game System C64 cartridge capable of holding 512 kB, clips can be 2 minuts long
- CRT audio - Game System C64 cartridge with 4,4 kHz digitized sound. Playback can by jazzy, fast scenes requires more CPU power.
 
Compression mode

- Code and color - all changes are recorded, screen codes and colors
- Codes - only changed screen codes and theirs colors are recorded

Converter mode

- Semigraphics - prefers semigraphics
- Characters - prefers characters

Contrast processing

- none - no postprocessing at all
- HE - color histogram equalizer (global)
- CLAHE - clipped adaptive color histogram equalizer (local) with sharpen level 

![Super CPU Charset](retrovid.png)

New method with neural net learns new charset definitions. Movie key frames are divided into 8x8 blocks which feads net. Smoother pictures but takes some time. You can try to run cartridges on C64. Some movies, with slow action will work nicely. 

Medium

- CRT - Game System C64 cartridge capable of holding 512 kB, clips can be 2 minuts long
- CRT audio - Game System C64 cartridge with 4,4 kHz digitized sound with simply low band filter 2,2 kHz cutoff
 
Compression mode

- Code and color - all changes are recorded, screen codes and colors
- Codes - only changed screen codes and theirs colors are recorded

Contrast processing

- none - no postprocessing at all
- HE - color histogram equalizer (global)
- CLAHE - clipped adaptive color histogram equalizer (local) with sharpen level 

