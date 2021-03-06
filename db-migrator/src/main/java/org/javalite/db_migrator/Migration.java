package org.javalite.db_migrator;

import org.javalite.activejdbc.Base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.SQLException;


public class Migration implements Comparable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String DEFAULT_DELIMITER = ";";
    private static final String DEFAULT_DELIMITER_KEYWORD = "DELIMITER";

    private File migrationFile;
    private String version;

    public Migration(String version, File migrationFile) {
        this.migrationFile = migrationFile;
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return migrationFile.getName();
    }

    public void migrate() throws Exception {

        StringBuffer command = null;
        try {
            String delimiter = DEFAULT_DELIMITER;
            LineNumberReader lineReader = new LineNumberReader(new FileReader(migrationFile));
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuffer();
                }

                line = line.trim(); // Strip extra whitespace too?

                if (line.length() < 1) {
                    // Do nothing, it's an empty line.
                } else if (commentLine(line)) {
                    logger.debug(line);
                } else {
                    if (startsWithIgnoreCase(line, DEFAULT_DELIMITER_KEYWORD)) {
                        delimiter = line.substring(10).trim();
                    } else if ((command.length() == 0) && startsWithIgnoreCase(line, "create ") && containsIgnoreCase(line, " as ")) {
                        delimiter = line.substring(line.toLowerCase().lastIndexOf(" as ") + 4);
                        command.append(line);
                        command.append(" ");
                    } else if (line.contains(delimiter)) {
                        if (line.startsWith(delimiter)) {
                            delimiter = DEFAULT_DELIMITER;
                        }

                        if (line.endsWith(delimiter)) {
                            command.append(line.substring(0, line.lastIndexOf(delimiter)));
                            Base.exec(command.toString().trim());
                            command = null;
                        }
                    } else {
                        command.append(line);
                        command.append(" ");
                    }
                }
            }

            // Check to see if we have an unexecuted statement in command.
            if (command != null && command.length() > 0) {
                //Last statement in script is missing a terminating delimiter, executing anyway.
                Base.exec(command.toString().trim());
            }

        } catch (Exception e) {
            logger.error("Error executing: " + command, e);
            throw e;
        }
    }

    private boolean containsIgnoreCase(String line, String sub) {
        return line.toLowerCase().contains(sub.toLowerCase());
    }

    private boolean startsWithIgnoreCase(String line, String sub) {
        return line.toLowerCase().startsWith(sub.toLowerCase());
    }

    private boolean commentLine(String line) {
        return line.startsWith("--") || line.startsWith("#") || line.startsWith("//");
    }

    public int compareTo(Object o) {
        Migration other = (Migration) o;
        return this.getVersion().compareTo(other.getVersion());
    }
}
