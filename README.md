`VLineIn` is a Java library which simplifies reading from line-in audio devices. It can be used to read and save audio
directly to disk, or to receive raw `byte[]` data for real-time processing.

## Table of Contents

* [Installation](#installation)
    * [Gradle](#-gradle)
    * [Maven](#-maven)
    * [sbt](#-scala-sbt)
* [Examples](#examples)
  * [Save Data to Disk](#save-data-to-disk) 
  * [Output Audio Data to Console](#output-audio-data-to-console)

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
	implementation 'com.github.Valkryst:VLineIn:2024.10.21'
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
    <version>2024.10.21</version>
</dependency>
```

### ![Scala SBT](https://i.imgur.com/Nqv3mVd.png?1) Scala SBT

Add JitPack as a resolver.

```
resolvers += "jitpack" at "https://jitpack.io"
```

Add VLineIn as a dependency.

```
libraryDependencies += "com.github.Valkryst" % "VLineIn" % "2024.10.21"
```

## Examples

### Save Data to Disk

```java
public class Driver {
    public static void main(final String[] args) throws LineUnavailableException, InterruptedException {
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

        // Output the audio data, as it's received, to the console.
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
    public static void main(final String[] args) throws LineUnavailableException, InterruptedException {
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