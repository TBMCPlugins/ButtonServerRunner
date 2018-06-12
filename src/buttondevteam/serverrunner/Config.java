package buttondevteam.serverrunner;

public class Config {
    public String serverVersion;
    public String serverParams;
    public int restartAt;

    public Config(String serverVersion, String serverParams, int restartAt) {
        this.serverVersion = serverVersion;
        this.serverParams = serverParams;
        this.restartAt = restartAt;
    }
    public Config() {
        this.serverVersion = "1.12.2";
        this.serverParams = "-Djline.terminal=jline.UnixTerminal -Xms4G -Xmx6G";
        this.restartAt = 12;
    }
}
