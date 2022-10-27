/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autumn;

/**
 *
 * @author piotr
 */
public interface Logger {
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;
    public static final int FATAL = 4;

    public void logException(int severity, String tag, Exception ex, String... info);
    public void log(int severity, String tag, String... data);
}
