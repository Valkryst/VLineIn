[![Java CI with Maven](https://github.com/Valkryst/VLineIn/actions/workflows/maven.yml/badge.svg)](https://github.com/Valkryst/VLineIn/actions/workflows/maven.yml)

`VLineIn` is a Java library which simplifies reading from line-in audio devices. It can be used to read and save audio
directly to disk, or to receive raw `byte[]` data for real-time processing.

## Table of Contents

* [Installation](#installation)
    * [Gradle](#-gradle)
    * [Maven](#-maven)
    * [sbt](#-scala-sbt)
* [Examples](#examples)
    * [Save Data to Disk](#save-audio-data-to-disk)
    * [Output Audio Data to Console](#output-audio-data-to-console)
  * [Display Input Sources in a Swing ComboBox](#display-input-sources-in-a-swing-combobox)

## Installation

VLineIn is hosted on the [JitPack package repository](https://jitpack.io/#Valkryst/VLineIn) which supports Gradle,
Maven, and sbt.

### ![Gradle](https://i.imgur.com/qtc6bXq.png?1) Gradle

Add JitPack to your `build.gradle` at the end of repositories.

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Add VLineIn as a dependency.

```
dependencies {
	implementation 'com.github.Valkryst:VLineIn:2.0.0'
}
```

### ![Maven](https://i.imgur.com/2TZzobp.png?1) Maven

Add JitPack as a repository.

``` xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
Add VLineIn as a dependency.

```xml
<dependency>
    <groupId>com.github.Valkryst</groupId>
    <artifactId>VLineIn</artifactId>
    <version>2.0.0</version>
</dependency>
```

### ![Scala SBT](https://i.imgur.com/Nqv3mVd.png?1) Scala SBT

Add JitPack as a resolver.

```
resolvers += "jitpack" at "https://jitpack.io"
```

Add VLineIn as a dependency.

```
libraryDependencies += "com.github.Valkryst" % "VLineIn" % "2.0.0"
```

## Examples

### Save Audio Data to Disk

```java
public class Driver {
    static void main(final String[] args) throws LineUnavailableException, InterruptedException {
        final var inputSources = LineIn.getInputSources();

        // Print the available input sources and their supported formats.
        for (final var source : inputSources.entrySet()) {
            System.out.println(source.getKey() + " : " + source.getValue());

            for (final var format : source.getValue().getFormats()) {
                System.out.println("\t" + format);
            }
        }

        // Create a new LineIn instance to record audio from the first input source.
        final var firstSource = inputSources.entrySet().stream().findFirst().orElseThrow();
        final var lineIn = new LineIn(
            new AudioFormat(16000, 16, 1, true, true),
            firstSource.getKey() // todo You can pull the source name from the output list and use it here.
        );

        // Output the audio data to a file.
        lineIn.startRecording(
            AudioFileFormat.Type.WAVE,
            Path.of("output.wav")
        );

        // Record audio for 4 seconds, then stop.
        Thread.sleep(4000);
        lineIn.stopRecording();
    }
}
```

### Output Audio Data to Console

```java
public class Driver {
    static void main(final String[] args) throws LineUnavailableException, InterruptedException {
        final var inputSources = LineIn.getInputSources();

        // Print the available input sources and their supported formats.
        for (final var source : inputSources.entrySet()) {
            System.out.println(source.getKey() + " : " + source.getValue());

            for (final var format : source.getValue().getFormats()) {
                System.out.println("\t" + format);
            }
        }

        // Create a new LineIn instance to record audio from the first input source.
        final var firstSource = inputSources.entrySet().stream().findFirst().orElseThrow();
        final var lineIn = new LineIn(
            new AudioFormat(16000, 16, 1, true, true),
            firstSource.getKey()
        );

        // Output the audio data, as it's received, to the console.
        lineIn.startRecording(bytes -> System.out.println(bytes.length + "\t" + Arrays.toString(bytes)));

        // Record audio for 4 seconds, then stop.
        Thread.sleep(4000);
        lineIn.stopRecording();
    }
}
```

### Display Input Sources in a Swing ComboBox

```java
public class Driver {
    static void main(final String[] args) {
    SwingUtilities.invokeLater(() -> {
      final JFrame frame = new JFrame("LineInComboBox Example");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setLayout(new GridLayout(2, 2, 10, 10));

      // Display a LineInComboBox with all available input sources:
      frame.add(new JLabel("Default Format:"));
      frame.add(new LineInComboBox());

      // Display a LineInComboBox with all available input sources, which support the custom audio format:
      frame.add(new JLabel("Custom Format (16kHz, 16-bit, mono):"));
      frame.add(new LineInComboBox(
              new AudioFormat(16000, 16, 1, true, true)
      ));

      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    });
  }
}
```