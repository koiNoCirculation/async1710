package org.tgt.async1710;

public interface WorldUtils {
    void setThreadName(String threadName);
    String getThreadName();
    void stop();
    boolean getRunning();
    boolean getExit();
}
