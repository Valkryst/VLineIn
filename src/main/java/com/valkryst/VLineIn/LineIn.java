package com.valkryst.VLineIn;

import lombok.NonNull;
import lombok.extern.slf4j.XSlf4j;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@XSlf4j
public class LineIn implements AutoCloseable {
    /**
     * Unique separator to use between mixer name and description in input source keys.
     */
    private static final String KEY_SEPARATOR = "\u2003";

    /** Counter for naming threads. */
    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    /** {@link AudioFormat} to record audio in. */
    private final AudioFormat audioFormat;

    /** {@link Thread} for recording audio. */
    private Thread thread;

    /** {@link TargetDataLine} to record audio from. */
    private final TargetDataLine targetDataLine;

    /**
     * Constructs a new {@link LineIn}.
     *
     * @param audioFormat {@link AudioFormat} to record audio in.
     * @param inputName Name of the input to record audio from.
     *
     * @throws IllegalArgumentException If the mixer does not support any lines matching the description.
     * @throws IllegalStateException If the input does not support the {@link LineIn#audioFormat}, or if no mixer could be found for the specified input.
     * @throws LineUnavailableException If a matching line is not available due to resource restrictions.
     * @throws SecurityException If the requested line cannot be opened due to security restrictions.
     */
    public LineIn(final @NonNull AudioFormat audioFormat, final @NonNull String inputName) throws IllegalArgumentException, IllegalStateException, LineUnavailableException, SecurityException {
        this.audioFormat = audioFormat;
        this.targetDataLine = getLineIn(inputName);
    }

    @Override
    public void close() throws Exception {
        try {
            stopRecording();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.atError().setCause(e).log("Encountered an error while stopping recording.");
        } finally {
            if (targetDataLine.isOpen()) {
                targetDataLine.close();
            }
        }
    }

    /**
     * Starts recording audio to the specified file, in the specified format.
     *
     * @param fileFormat {@link AudioFileFormat.Type} to output the audio as.
     * @param outputPath {@link Path} to the file to output the audio to.
     *
     * @throws IllegalThreadStateException If a recording is already in progress.
     */
    public synchronized void startRecording(final @NonNull AudioFileFormat.Type fileFormat, final @NonNull Path outputPath) {
        if (thread != null && thread.isAlive()) {
            throw new IllegalThreadStateException("A recording is already in progress.");
        }

        thread = new Thread(() -> {
            try {
                targetDataLine.open(audioFormat);
                targetDataLine.start();

                Files.createDirectories(outputPath.toAbsolutePath().getParent());

                AudioSystem.write(new AudioInputStream(targetDataLine), fileFormat, outputPath.toFile());
            } catch (final LineUnavailableException | SecurityException | IllegalArgumentException | IOException e) {
                log.atError().setCause(e).log("Encountered an error while starting recording.");
                thread.interrupt();
            }
        }, "LineIn-PathRecordingThread-" + threadCounter.getAndIncrement());
        thread.start();
    }

    /**
     * Starts recording and passing audio to the specified {@link Consumer}.
     *
     * @param consumer {@link Consumer} to pass the audio to.
     *
     * @throws IllegalThreadStateException If a recording is already in progress.
     */
    public synchronized void startRecording(final Consumer<byte[]> consumer) {
        if (thread != null && thread.isAlive()) {
            throw new IllegalThreadStateException("A recording is already in progress.");
        }

        thread = new Thread(() -> {
            final var buffer = new byte[targetDataLine.getBufferSize() / 4];

            try {
                targetDataLine.open(audioFormat, buffer.length);
                targetDataLine.start();

                while (!thread.isInterrupted()) {
                    targetDataLine.read(buffer, 0, buffer.length);
                    consumer.accept(buffer.clone());
                }
            } catch (final LineUnavailableException e) {
                log.atError().setCause(e).log("Encountered an error while starting recording.");
                thread.interrupt();
            }
        }, "LineIn-ConsumerRecordingThread-" + threadCounter.getAndIncrement());
        thread.start();
    }

    /**
     * Stops recording audio.
     *
     * @throws InterruptedException If the thread is interrupted while waiting for the recording to stop.
     */
    public synchronized void stopRecording() throws InterruptedException {
        if (thread == null || !thread.isAlive()) {
            thread = null;
            return;
        }

        targetDataLine.stop();

        thread.interrupt();
        thread.join(Duration.of(10, ChronoUnit.SECONDS).toMillis());
        thread = null;

        targetDataLine.close();
    }

    /**
     * <p>Retrieves the {@link Mixer.Info#getName()} and {@link TargetDataLine.Info} for all available audio inputs.</p>
     *
     * <p>The set of available inputs is determined by the underlying system, and may change between runs.</p>
     *
     * @return An immutable mapping of {@link Mixer.Info#getName()} to {@link TargetDataLine.Info}.
     */
    public static Map<String, TargetDataLine.Info> getInputSources() {
        final var sources = new HashMap<String, TargetDataLine.Info>();

        for (final var mixerInfo : AudioSystem.getMixerInfo()) {
            for (final var lineInfo : AudioSystem.getMixer(mixerInfo).getTargetLineInfo()) {
                if (lineInfo.getLineClass().equals(TargetDataLine.class)) {
                    sources.put(mixerInfo.getName() + KEY_SEPARATOR + mixerInfo.getDescription(), (TargetDataLine.Info) lineInfo);
                    break;
                }
            }
        }

        return Map.copyOf(sources);
    }

    /**
     * <p>
     *     Retrieves the {@link Mixer.Info#getName()} and {@link TargetDataLine.Info} for all available audio inputs
     *     that support the specified {@link AudioFormat}.
     * </p>
     *
     * <p>The set of available inputs is determined by the underlying system, and may change between runs.</p>
     *
     * @param format {@link AudioFormat} to check support for.
     *
     * @return An immutable mapping of {@link Mixer.Info#getName()} to {@link TargetDataLine.Info}.
     */
    public static Map<String, TargetDataLine.Info> getInputSources(final @NonNull AudioFormat format) {
        final var compatibleSources = new HashMap<String, TargetDataLine.Info>();

        for (final var entry : LineIn.getInputSources().entrySet()) {
            try (final var ignored = new LineIn(format, entry.getKey())) {
                compatibleSources.put(entry.getKey(), entry.getValue());
            } catch (final Exception ignored) {}
        }

        return Map.copyOf(compatibleSources);
    }

    /**
     * <p>Attempts to obtain a {@link TargetDataLine} for the specified input.</p>
     *
     * @param inputName Name of the input to obtain a {@link TargetDataLine} for.
     * @return A {@link TargetDataLine} for the specified input.
     *
     * @throws IllegalArgumentException If the mixer does not support any lines matching the description.
     * @throws IllegalStateException If the input does not support the {@link LineIn#audioFormat}, or if no mixer could be found for the specified input.
     * @throws LineUnavailableException If a matching line is not available due to resource restrictions.
     * @throws SecurityException If the requested line cannot be opened due to security restrictions.
     */
    private TargetDataLine getLineIn(final @NonNull String inputName) throws IllegalArgumentException, IllegalStateException, LineUnavailableException, SecurityException {
        final var dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            throw new IllegalStateException(inputName + " does not support the following audio format: " + audioFormat);
        }

        Mixer mixer = null;
        for (final var mixerInfo : AudioSystem.getMixerInfo()) {
            if (mixerInfo.getName().equals(inputName.split(KEY_SEPARATOR)[0])) {
                mixer = AudioSystem.getMixer(mixerInfo);
                break;
            }
        }

        if (mixer == null) {
            throw new IllegalStateException("No mixer could be found for the specified input: " + inputName);
        }

        return (TargetDataLine) mixer.getLine(dataLineInfo);
    }
}
