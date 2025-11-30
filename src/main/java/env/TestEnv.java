package env;

public class TestEnv implements EnvConfig {
    private String baseUrl = "https://open.spotify.com/";
    private String browser = "chrome";
    private boolean headless = false;
    private int implicitWaitSeconds = 5;
    private int pageLoadTimeoutSeconds = 60;

    public TestEnv() {}

    @Override
    public String getBaseUrl() { return baseUrl; }

    @Override
    public String getBrowser() { return browser; }

    @Override
    public boolean isHeadless() { return headless; }

    @Override
    public int getImplicitWaitSeconds() { return implicitWaitSeconds; }

    @Override
    public int getPageLoadTimeoutSeconds() { return pageLoadTimeoutSeconds; }

    @Override
    public void applySystemOverrides() {
        String b = System.getProperty("browser");
        if (b != null && !b.isEmpty()) this.browser = b;
        String url = System.getProperty("baseUrl");
        if (url != null && !url.isEmpty()) this.baseUrl = url;
        String head = System.getProperty("headless");
        if (head != null) this.headless = Boolean.parseBoolean(head);
        String iw = System.getProperty("implicitWait");
        if (iw != null) this.implicitWaitSeconds = Integer.parseInt(iw);
        String pl = System.getProperty("pageLoadTimeout");
        if (pl != null) this.pageLoadTimeoutSeconds = Integer.parseInt(pl);
    }
}
