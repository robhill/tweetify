package tweetify;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A utility for interacting with REST style web services
 */
class Commander {

    private String name;
    private String prefix;
    private String suffix;
    private boolean trace;
    private boolean traceSends;
    private boolean log;
    private long lastCommandTime = 0;
    private long minimumCommandPeriod = 0L;
    private DocumentBuilder builder;
    private PrintStream logFile;
    private int commandsSent = 0;
    private int timeout = -1;
    private int tryCount = 5;
    private final int DEFAULT_TIMEOUT = 60 * 1000;

    /**
     * Creates a commander
     * @param name the name of this commander
     * @param prefix common url prefix for all commands
     * @param suffix common url suffix for all 'get' commands
     * @throws java.io.IOException
     */
    public Commander(String name, String prefix, String suffix) throws IOException {
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
        trace = Boolean.getBoolean("trace");
        traceSends = Boolean.getBoolean("traceSends");
        log = Boolean.getBoolean("log");
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Can't load parser " + e);
        }

        if (log) {
            String logname = name + ".log";
            try {
                logFile = new PrintStream(logname);
            } catch (IOException e) {
                System.err.println("Can't open " + logname);
            }
        }
        setTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * Enable or disable tracing of URL get/posts
     * @param traceSends
     */
    public void setTraceSends(boolean traceSends) {
        this.traceSends = traceSends;
    }

    /**
     * Enable or disable tracing of URL get/posts and the returned data
     * @param trace
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

    /**
     * Sets the maximum number of retries before giving up
     * @param retries
     */
    public void setRetries(int retries) {
        tryCount = retries + 1;
        if (tryCount < 1) {
            tryCount = 1;
        }
    }

    /**
     * Gets the maximum number of retries
     * @return the max number of retries
     */
    public int getRetries() {
        return tryCount - 1;
    }

    /**
     * Show some stats
     */
    public void showStats() {
        System.out.printf("Commands sent to %s: %d\n", name, commandsSent);
    }

    /**
     * Encode a parameter 
     * @param name the name to be encoded
     * @return an encoded form of the name
     */
    public String encode(String name) {
        try {
            String encodedName = URLEncoder.encode(name, "UTF-8");
            return encodedName;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * Sets the minimum period between consecutive commands
     * @param minPeriod the minimum period.
     */
    public void setMinimumCommandPeriod(long minPeriod) {
        minimumCommandPeriod = minPeriod;
    }

    /**
     * Sends the command as a GET
     * @param command the command to send
     * @return an XML document
     * @throws java.io.IOException
     */
    public Document sendCommand(String command) throws IOException {
        return sendCommand(command, false);
    }

    /**
     * Sends the command
     * @param command the command to send
     * @param usePost if true, use a POST instead of a Get
     * @return an XML document
     * @throws java.io.IOException
     */
    public Document sendCommand(String command, boolean usePost) throws IOException {
        Document document = null;
        InputStream is = sendCommandRaw(command, usePost);
        commandsSent++;

        synchronized (builder) {
            try {
                document = builder.parse(is);
            } catch (SAXException e) {
                throw new IOException("SAX Parse Error " + e);
            } finally {
                is.close();
            }
        }

        if (trace) {
            dumpDocument(document);
        }
        return document;
    }

    public Document postCommand(String command, Map<String, Object> params) throws IOException {
        Document document = null;
        if (trace || traceSends) {
            System.out.println("Posting-->     " + command);
            for (String key : params.keySet()) {
                System.out.printf("  %s:%s\n", key, params.get(key));
            }
        }
        InputStream is = post(command, params);
        commandsSent++;

        synchronized (builder) {
            try {
                document = builder.parse(is);
            } catch (SAXException e) {
                throw new IOException("SAX Parse Error " + e);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }

        if (trace) {
            dumpDocument(document);
        }
        return document;
    }

    public InputStream sendCommandRaw(String command) throws IOException {
        return sendCommandRaw(command, false);
    }

    public InputStream sendCommandRaw(String command, boolean usePost) throws IOException {
        try {
            String fullCommand = prefix + command + fixSuffix(command, suffix);

            long curGap = System.currentTimeMillis() - lastCommandTime;
            long delayTime = minimumCommandPeriod - curGap;


            delay(delayTime);

            // URL url = new URL(fullCommand);
            URI uri = new URI(fullCommand);
            URL url = uri.toURL();

            if (trace || traceSends) {
                System.out.println("Sending-->     " + url);
            }
            if (logFile != null) {
                logFile.println("Sending-->     " + url);
            }

            InputStream is = null;
            for (int i = 0; i < tryCount; i++) {
                try {
                    URLConnection urc = url.openConnection();

                    if (usePost) {
                        if (urc instanceof HttpURLConnection) {
                            ((HttpURLConnection) urc).setRequestMethod("POST");
                        }
                    }

                    if (getTimeout() != -1) {
                        urc.setReadTimeout(getTimeout());
                        urc.setConnectTimeout(getTimeout());
                    }
                    is = new BufferedInputStream(urc.getInputStream());
                    break;
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (IOException e) {
                    System.out.println(name + " Error: " + e + " cmd: " + command);
                }
            }

            lastCommandTime = System.currentTimeMillis();
            if (is == null) {
                System.out.println(name + " retry failure  cmd: " + url);
                throw new IOException("Can't send command");
            }
            return is;
        } catch (URISyntaxException ex) {
            throw new IOException("bad uri " + ex);
        }
    }

    // the suffix maybe a param that needs to start with & or ? depending
    // on whether or not this is the only parameter for the command. If the suffix
    // starts with a '&' then it is assumed to be a param, if the command doesn't have
    // any params (i.e. there's no  '?' in the command), then we replace the '&' with a '?'
    private String fixSuffix(String command, String suffix) {
        if (suffix.startsWith("&")) {
            if (command.indexOf("?") == -1) {
                suffix = suffix.replaceFirst("\\&", "?");
            }
        }
        return suffix;

    }

    private void delay(long time) {
        if (time < 0) {
            return;
        } else {
            try {
                Thread.sleep(time);
            } catch (InterruptedException ie) {
            }
        }
    }

    /**
     * A debugging method ... dumps a domdocument to
     * standard out
     */
    public static void dumpDocument(Document document) {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(document);
            Result result = new StreamResult(System.out);

            // Write the DOM document to the file
            // Get Transformer
            Transformer xformer =
                    TransformerFactory.newInstance().newTransformer();
            // Write to a file

            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            xformer.setOutputProperty(
                    "{http://xml.apache.org/xalan}indent-amount", "4");

            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            System.out.println("TransformerConfigurationException: " + e);
        } catch (TransformerException e) {
            System.out.println("TransformerException: " + e);
        }
    }

    public static String convertToString(Document document) {
        StringWriter sw = new StringWriter();
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(document);
            Result result = new StreamResult(sw);

            // Write the DOM document to the file
            // Get Transformer
            Transformer xformer =
                    TransformerFactory.newInstance().newTransformer();
            // Write to a file

            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            xformer.setOutputProperty(
                    "{http://xml.apache.org/xalan}indent-amount", "4");

            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            System.out.println("TransformerConfigurationException: " + e);
        } catch (TransformerException e) {
            System.out.println("TransformerException: " + e);
        }
        return sw.toString();
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    private InputStream post(String command, Map<String, Object> params) throws IOException {
        String NEWLINE = "\r\n";
        String PREFIX = "--";
        String BOUNDARY = "-----------" + Long.toString(System.currentTimeMillis(), 16);

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String fullCommand = prefix + command;

        URL url = null;
        try {
            URI uri = new URI(fullCommand);
            url = uri.toURL();
        } catch (URISyntaxException e) {
            throw new IOException("Bad URL " + e);
        } catch (MalformedURLException e) {
            throw new IOException("Bad URL " + e);
        }

        if (trace || traceSends) {
            System.out.println("Sending-->     " + url);
        }
        if (logFile != null) {
            logFile.println("Sending-->     " + url);
        }

        conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setAllowUserInteraction(false);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
        dos = new DataOutputStream(conn.getOutputStream());
        //dos = new DataOutputStream(System.out);

        for (String key : params.keySet()) {
            dos.writeBytes(PREFIX);
            dos.writeBytes(BOUNDARY);
            dos.writeBytes(NEWLINE);
            Object val = params.get(key);

            if (val instanceof File) {
                File file = (File) val;

                dos.writeBytes("Content-Disposition: form-data; name=\"file\";" + " filename=\"" + file.getName() + "\"" );
                dos.writeBytes(NEWLINE);
                dos.writeBytes("Content-Type: application/octet-stream");
                dos.writeBytes(NEWLINE);
                dos.writeBytes(NEWLINE);

                InputStream is = new FileInputStream(file);
                int r = 0;
                byte[] data = new byte[1024];
                while ((r = is.read(data, 0, data.length)) != -1) {
                    dos.write(data, 0, r);
                }
                is.close();
                dos.writeBytes(NEWLINE);
            } else {
                dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"");
                dos.writeBytes(NEWLINE);
                dos.writeBytes(NEWLINE);
                dos.writeBytes(val.toString());
                dos.writeBytes(NEWLINE);
            }
        }
        dos.writeBytes(PREFIX);
        dos.writeBytes(BOUNDARY);
        dos.writeBytes(PREFIX);
        dos.writeBytes(NEWLINE);
        // close streams
        dos.flush();
        dos.close();
        return conn.getInputStream();
    }
}
