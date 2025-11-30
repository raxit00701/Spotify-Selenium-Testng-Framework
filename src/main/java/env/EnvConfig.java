package env;

import java.util.Properties;

public interface EnvConfig {
    String getBaseUrl();
    String getBrowser();
    boolean isHeadless();
    int getImplicitWaitSeconds();
    int getPageLoadTimeoutSeconds();

    void applySystemOverrides();

    default void loadFromProperties(Properties props) {}
}
