package org.tmatesoft.svn.core.javahl17;

import org.apache.subversion.javahl.ClientException;
import org.apache.subversion.javahl.ISVNConfig;
import org.apache.subversion.javahl.types.Tristate;
import org.tmatesoft.svn.core.internal.wc.SVNCompositeConfigFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JavaHLConfigCategory implements ISVNConfig.Category {

    private static final Set<String> TRUE_VALUES = new HashSet<String>(Arrays.<String>asList("yes", "on", "true"));
    private static final Set<String> FALSE_VALUES = new HashSet<String>(Arrays.<String>asList("no", "off", "false"));
    private static final String ASK_VALUE = "ask";

    private final SVNCompositeConfigFile configFile;

    public JavaHLConfigCategory(SVNCompositeConfigFile configFile) {
        this.configFile = configFile;
    }

    @Override
    public String get(String section, String option, String defaultValue) {
        final String propertyValue = configFile.getPropertyValue(section, option);
        return propertyValue == null ? defaultValue : propertyValue;
    }

    @Override
    public boolean get(String section, String option, boolean defaultValue) throws ClientException {
        final String propertyValue = configFile.getPropertyValue(section, option);
        return propertyValue == null ? defaultValue : TRUE_VALUES.contains(propertyValue.toLowerCase());
    }

    @Override
    public long get(String section, String option, long defaultValue) throws ClientException {
        final String propertyValue = configFile.getPropertyValue(section, option);
        try {
            return propertyValue == null ? defaultValue : Long.parseLong(propertyValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Tristate get(String section, String option, String unknown, Tristate defaultValue) throws ClientException {
        final String propertyValue = configFile.getPropertyValue(section, option);
        if (propertyValue == null) {
            return defaultValue;
        } else if (TRUE_VALUES.contains(propertyValue.toLowerCase())) {
            return Tristate.True;
        } else if (FALSE_VALUES.contains(propertyValue.toLowerCase())) {
            return Tristate.False;
        } else {
            return Tristate.Unknown;
        }
    }

    @Override
    public String getYesNoAsk(String section, String option, String defaultValue) throws ClientException {
        final String propertyValue = configFile.getPropertyValue(section, option);
        return propertyValue == null ? defaultValue : propertyValue;
    }

    @Override
    public void set(String section, String option, String value) {
        configFile.setPropertyValue(section, option, value, true);
    }

    @Override
    public void set(String section, String option, boolean value) {
        configFile.setPropertyValue(section, option, String.valueOf(value), true);
    }

    @Override
    public void set(String section, String option, long value) {
        configFile.setPropertyValue(section, option, String.valueOf(value), true);
    }

    @Override
    public Iterable<String> sections() {
        return configFile.getGroupNames();
    }

    @Override
    public void enumerate(String section, ISVNConfig.Enumerator handler) {
        final Map properties = configFile.getProperties(section);
        final Set<Map.Entry<String, String>> entrySet = properties.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            final String option = entry.getKey();
            final String value = entry.getValue();

            handler.option(option, value);
        }
    }
}
