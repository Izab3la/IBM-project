/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autumn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 *
 * @author piotr
 */
public abstract class State {
    private final Map<String, List<String>> inboundHeaders;
    private final Map<String, List<String>> outboundHeaders = new HashMap<>();
    private final OutputStream output;
    private final InputStream input;
    private final URI uri;
    private boolean hasBodyBeenParsed;
        
    protected int outputLength = -1;

    public State(Map<String, List<String>> inboundHeaders, OutputStream output, InputStream input, URI uri) {
        this.inboundHeaders = inboundHeaders;
        this.output = output;
        this.input = input;
        this.uri = uri;
    }

    public boolean hasBodyBeenParsed() {
        return hasBodyBeenParsed;
    }

    public void setHasBodyBeenParsed(boolean hasBodyBeenParsed) {
        this.hasBodyBeenParsed = hasBodyBeenParsed;
    }

    public URI getUri() {
        return uri;
    }

    public void setOutputLength(int outputLength) {
        this.outputLength = outputLength;
    }

    public abstract void sendResponseHeaders(int statusCode) throws IOException;

    public Map<String, List<String>> getInboundHeaders() {
        return inboundHeaders;
    }

    public Map<String, List<String>> getOutboundHeaders() {
        return outboundHeaders;
    }
    
    public void setHeader(String name, String value){
        if(!outboundHeaders.containsKey(name)){
            outboundHeaders.put(name, new LinkedList<>());
        }
        outboundHeaders.get(name).add(value);
    }

    public OutputStream getOutput() {
        return output;
    }

    public InputStream getInput() {
        return input;
    }
    
    public void sendString(String toSend, String contentType) throws IOException{ sendString(toSend, contentType, 200); }
    public void sendString(String toSend, int status) throws IOException{ sendString(toSend, "text/plain; charset=UTF-8", status); }
    public void sendString(String toSend) throws IOException{ sendString(toSend, "text/plain; charset=UTF-8", 200); }
    
    public void sendString(String toSend, String contentType, int status) throws IOException{
        byte bytes[] = toSend.getBytes();
        setHeader("Content-Type", contentType);
        setOutputLength(bytes.length);
        sendResponseHeaders(status);
        output.write(bytes);
        output.flush();
    }
    
    public void setCookie(Cookie cookie){
        setHeader("Set-Cookie", cookie.toString());
    }
    
    public static class Cookie{
        private final String name;
        private final String value;
        
        private String path;
        
        private Date expireDate;
        private boolean httpOnly;

        public Cookie(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Cookie setPath(String path) {
            this.path = path;
            return this;
        }

        public Cookie setExpireDate(Date expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        public Cookie setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }
        
        @Override
        public String toString(){
            DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            
            StringBuilder bld = new StringBuilder();
            bld.append(name).append("=").append(value);
            if(httpOnly) bld.append("; HttpOnly");
            if(expireDate != null) bld.append("; Expires=").append(df.format(expireDate));
            if(path != null) bld.append("; Path=").append(path);
            return bld.toString();
        }
        
    }
    
}
