/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autumn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author piotr
 */
public class Utils {
    public static Map<String, String> parse(String format, String input) {
        Map<String, String> retVal = new HashMap<>();
        int formatIndex = 0;
        int inputIndex = 0;
        while (inputIndex < input.length()) {
            if (format.charAt(formatIndex) == '{') {
                int formatNameEnd = format.indexOf('}', formatIndex);
                int inInputTextMatchEnd;
                if (formatNameEnd + 1 < format.length()) {
                    char endOfFormat = format.charAt(formatNameEnd + 1);
                    inInputTextMatchEnd = input.indexOf(endOfFormat, inputIndex);
                } else {
                    inInputTextMatchEnd = input.length();
                }

                String inInputTextMatch = input.substring(inputIndex, inInputTextMatchEnd);
                retVal.put(format.substring(formatIndex+1, formatNameEnd), inInputTextMatch);
                formatIndex = formatNameEnd + 1;
                inputIndex = inInputTextMatchEnd;
            } else {
                int unformattedTextEnd = format.indexOf('{', formatIndex);
                if (unformattedTextEnd == -1) {
                    unformattedTextEnd = format.length(); //If the text doesn't contain formats, just compare
                }
                String formatTextRawChunk = format.substring(formatIndex, unformattedTextEnd);
                int unformattedTextLength = unformattedTextEnd - formatIndex;
                String inputUnformattedEquivalent = input.substring(inputIndex, inputIndex + unformattedTextLength);
                if (!formatTextRawChunk.equals(inputUnformattedEquivalent)) {
                    return null; //Doesn't match
                }
                formatIndex = unformattedTextEnd;
                inputIndex += unformattedTextLength;
            }
        }
        return retVal;
    }
    
    public static Map<String, Object> queryToMap(String query) {
        return queryToMap(query, "&");
    }
    
    public static Map<String, Object> queryToMap(String query, String delim) {
        Map<String, Object> result = new HashMap<>();
        if (query == null) {
            return result;
        }
        for (String p : query.split(delim)) {
            String[] entry = p.split("=");
            if (entry.length > 1) {
                try {
                    entry[0] = URLDecoder.decode(entry[0], "UTF-8");
                    entry[1] = URLDecoder.decode(entry[1], "UTF-8");
                } catch (UnsupportedEncodingException ex) {}
                if(entry[0].endsWith("[]")){
                    //Array
                    entry[0] = entry[0].substring(0, entry[0].length() - 2);
                    if(result.get(entry[0]) == null){
                        result.put(entry[0], new LinkedList<>());
                    }
                    ((List<String>) result.get(entry[0])).add(entry[1]);
                }else{
                    result.put(entry[0], entry[1]);
                }
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
    
    public static String traceToString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }
    
    public static byte[] readFully(InputStream stream, int length) throws IOException{
        byte[] buffer = new byte[4096];
        byte[] output = new byte[length];
        int read = 0;
        int readSum = 0;
        while(readSum < length){
            read = stream.read(buffer);
            System.arraycopy(buffer, 0, output, readSum, read);
            readSum += read;
        }
        return output;
    }
    
    public static int count(String string, String substr){
        return (string.length() - string.replace(substr, "").length()) / substr.length();
    }
    
    public static void copyStream(InputStream i, OutputStream o, int bufferSize) throws IOException {
        byte buffer[] = new byte[bufferSize];
        int read;
        while ((read = i.read(buffer)) != -1) {
            o.write(Arrays.copyOf(buffer, read));
        }
    }
}
