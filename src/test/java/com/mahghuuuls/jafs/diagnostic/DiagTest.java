package com.mahghuuuls.jafs.diagnostic;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The diagnostic switch gates every {@link Diag#event} line: nothing reaches the logger while it
 * is off, one line per call reaches it while it is on. Captured through a Log4j appender on the
 * mod logger, which is the production path.
 */
class DiagTest {

    private final List<String> captured = new ArrayList<String>();
    private Logger logger;
    private AbstractAppender appender;
    private Level previousLevel;

    @BeforeEach
    void attach() {
        logger = (Logger) LogManager.getLogger(com.mahghuuuls.jafs.Tags.MOD_ID);
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender = new AbstractAppender("diag-test", null, null) {
            @Override
            public void append(LogEvent event) {
                captured.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        Diag.setEnabled(false);
    }

    @AfterEach
    void detach() {
        logger.removeAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
        Diag.setEnabled(false);
    }

    @Test
    void nothingIsLoggedWhileDisabled() {
        Diag.event("meal refused player={} reason={}", "Steve", "ineligible");
        assertTrue(captured.isEmpty(), captured.toString());
    }

    @Test
    void oneFormattedLinePerEventWhileEnabled() {
        Diag.setEnabled(true);
        Diag.event("meal refused player={} reason={}", "Steve", "ineligible");
        Diag.event("expired {}", "jafs:steak");
        assertEquals(2, captured.size(), captured.toString());
        assertEquals("[JAFS] meal refused player=Steve reason=ineligible", captured.get(0));
        assertEquals("[JAFS] expired jafs:steak", captured.get(1));
    }
}
