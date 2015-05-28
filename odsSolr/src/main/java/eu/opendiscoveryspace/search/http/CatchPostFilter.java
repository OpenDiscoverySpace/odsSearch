package eu.opendiscoveryspace.search.http;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

import java.net.URLDecoder;
import java.util.*;

/**
 * This http-servlet-filter allows a configured directory to be populated with the recent posts so as to witness
 * the post operations for debug purposes.
 * The filter needs to be configure in the web.xml
 *
 *
 <filter>
    <filter-name>CatchPostFilter</filter-name>
     <filter-class>eu.opendiscoveryspace.search.http.CatchPostFilter</filter-class>
 <init-param>
   <param-name>dir</param-name> <param-value>postBodies</param-value>
 </init-param>
 <init-param>
 <param-name>max</param-name> <param-value>100</param-value>
 </init-param>
 </filter>

 <filter-mapping>
    <filter-name>CatchPostFilter</filter-name>
     <url-pattern>/*</url-pattern>
 </filter-mapping>

 * */

public class CatchPostFilter implements Filter {

    private File directory = new File("postBodies");
    private int maxFiles = 100;
    ThreadLocal<ByteArrayOutputStream> buffer = new ThreadLocal<ByteArrayOutputStream>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        directory = new File(filterConfig.getInitParameter("dir"));
        maxFiles = Integer.parseInt(filterConfig.getInitParameter("max"));
    }

    @Override
    public void doFilter(final ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        synchronized(this) {
        HttpServletRequest hreq = (HttpServletRequest) request;
        String path = hreq.getPathTranslated();
        if (path == null) path = hreq.getServletPath();
        if (path == null) path = "unknown";
        String fileName = System.currentTimeMillis() + path.replaceAll("/", "_").replaceAll("\\\\", "_");
        if (!"POST".equals(hreq.getMethod())) {
            chain.doFilter(request, response);
            return;
        }


        // guess an extension
        String contentType = hreq.getHeader("Content-Type");
        if (contentType.contains(";")) contentType = contentType.substring(0, contentType.indexOf(";")).trim();
        System.err.println("Content-Type:" + contentType);
        if ((contentType.endsWith("/xml") || contentType.endsWith("+xml")))
            fileName = fileName + ".xml";
        else if ((contentType.startsWith("text")))
            fileName = fileName + ".txt";

        // read post body into buffer
        ByteArrayOutputStream bOut = buffer.get();
        if (bOut == null) {
            bOut = new ByteArrayOutputStream();
            buffer.set(bOut);
        }
        IOUtils.copy(request.getInputStream(), bOut);
        final byte[] bytes = bOut.toByteArray();


        // save it to file
        File file = new File(directory, fileName);
        if ("application/x-www-form-urlencoded".equals(contentType)) {
            fileName = fileName + ".properties";
            Writer out = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
            String s = bOut.toString("utf-8");
            String[] s1 = s.split("&");
            for (String p : s1) {
                int i = p.indexOf('=');
                if (i > -1) {
                    out.write(p.substring(0, i));
                    p = p.substring(i + 1);
                }
                out.write(URLDecoder.decode(p, "utf-8"));
            }
        } else
            FileUtils.writeByteArrayToFile(file, bytes, false);


        // remove old files
        List<File> files = Arrays.asList(directory.listFiles());
        Collections.sort(files, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                if (o1 == null && o2 == null) return 0;
                if (o1 == null) return 1;
                if (o2 == null) return -1;
                long d1 = ((File) o1).lastModified(), d2 = ((File) o2).lastModified();
                if (d1 < d2) return 1;
                if (d1 == d2) return 0;
                return -1;
            }
        });
        int numToDelete = Math.max(0, files.size() - maxFiles);
        for (File f : files) {
            if (numToDelete <= 0) break;
            f.delete();
            numToDelete--;
        }


        // relay

        final ByteArrayInputStream byIn = new ByteArrayInputStream(bytes);
        chain.doFilter(new HttpServletRequestWrapper(hreq) {
            @Override
            public ServletInputStream getInputStream() throws IOException {
                return new ServletInputStream() {
                    @Override
                    public int read() throws IOException {
                        return byIn.read();
                    }

                    @Override
                    public void close() throws IOException {
                        //super.close();
                    }

                    @Override
                    public int read(byte[] b) throws IOException {
                        return byIn.read(b);
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        return byIn.read(b, off, len);
                    }

                    @Override
                    public long skip(long n) throws IOException {
                        return byIn.skip(n);
                    }

                    @Override
                    public int available() throws IOException {
                        return byIn.available();
                    }

                    @Override
                    public synchronized void mark(int readlimit) {
                        byIn.mark(readlimit);
                    }

                    @Override
                    public synchronized void reset() throws IOException {
                        byIn.reset();
                    }

                    @Override
                    public boolean markSupported() {
                        return byIn.markSupported();
                    }

                    @Override
                    public int readLine(byte[] b, int off, int len) throws IOException {
                        return super.readLine(b, off, len);
                    }
                };
            }
        }, response);
    }
    }

    @Override
    public void destroy() {

    }
}
