package com.valkryst.VLineIn;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@Log4j2
public class LineInComboBox extends JComboBox<String> {
    /** {@link AudioFormat} to filter input sources by, or {@code null} if all input sources should be displayed. */
    private AudioFormat audioFormat;

    /** {@link SwingWorker} used to refresh the list of input sources in the background, to prevent the UI from freezing. */
    private SwingWorker<List<String>, Void> refreshWorker;

    /** Whether to refresh the list of input sources when this {@link LineInComboBox} gains focus. */
    private boolean refreshOnFocus = false;
    /** Whether to refresh the list of input sources when this {@link LineInComboBox} is opened. */
    private boolean refreshOnOpen = false;
    /** Whether to refresh the list of input sources when this {@link LineInComboBox} is made visible. */
    private boolean refreshOnVisible = false;

    /** Constructs a new {@link LineInComboBox}, displaying all input sources. */
    public LineInComboBox() {
        this(null);
    }

    /**
     * Constructs a new {@link LineInComboBox}, displaying input sources that match the specified {@link AudioFormat}
     * or all input sources if the {@link AudioFormat} is {@code null}.
     *
     * @param format {@link AudioFormat} to filter input sources by, or {@code null} if all input sources should be displayed.
     */
    public LineInComboBox(final AudioFormat format) {
        this.audioFormat = format;
        this.refreshInputSources();

        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                if (refreshOnFocus) {
                    refreshInputSources();
                }
            }
        });

        // Enable horizontal scrolling, for long input source names.
        this.setUI(new BasicComboBoxUI() {
            @Override
            protected ComboPopup createPopup() {
                return new BasicComboPopup(this.comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        return new JScrollPane(
                            this.list,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                        );
                    }
                };
            }
        });
    }

    /** Refreshes the displayed list of input sources. */
    public void refreshInputSources() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.setEnabled(false);

        if (refreshWorker != null && !refreshWorker.isDone()) {
            refreshWorker.cancel(true);
        }

        final String currentSelection = (String) this.getSelectedItem();
        refreshWorker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                final var sources = audioFormat == null ? LineIn.getInputSources() : LineIn.getInputSources(audioFormat);
                return sources.keySet().stream().sorted().collect(Collectors.toList());
            }

            @Override
            protected void done() {
                try {
                    removeAllItems();
                    this.get().forEach(LineInComboBox.this::addItem);
                } catch (final Exception e) {
                    log.error("Encountered an error while refreshing input sources.", e);
                } finally {
                    // Attempt to re-select the previously selected item, if it still exists.
                    if (currentSelection != null && currentSelection.isBlank()) {
                        setSelectedIndex(-1);
                    } else {
                        setSelectedItem(currentSelection);
                    }

                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                    setEnabled(true);
                    if (getItemCount() == 0) {
                        setPlaceholderText("No Input Sources Found");
                    }
                }

                super.done();
            }
        };
        refreshWorker.execute();
    }

    /**
     * Sets a new {@link AudioFormat} to filter input sources by, and refreshes the list of input sources.
     *
     * @param audioFormat {@link AudioFormat} to filter input sources by, or {@code null} if all input sources should be displayed.
     */
    public void setAudioFormat(final AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
        this.refreshInputSources();
    }

    @Override
    public void setEnabled(boolean enabled) {
        enabled &= getItemCount() > 0;
        enabled &= refreshWorker != null && !refreshWorker.isCancelled();
        super.setEnabled(enabled);
    }

    /**
     * Sets the text to display when no item is selected.
     *
     * @param text Text to display, or {@code null} to display nothing.
     */
    public void setPlaceholderText(final String text) {
        if (text == null || text.isBlank()) {
            this.setRenderer(null);
            return;
        }

        this.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // When rendering dropdown list items
                if (index >= 0) {
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }

                // When rendering the selected item display area
                if (value == null || getSelectedIndex() == -1) {
                    return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                }

                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    @Override
    public void setPopupVisible(boolean v) {
        if (refreshOnOpen) {
            this.refreshInputSources();
        }

        super.setPopupVisible(v);
    }

    @Override
    public void setVisible(final boolean aFlag) {
        if (refreshOnVisible) {
            this.refreshInputSources();
        }

        super.setVisible(aFlag);
    }
}
