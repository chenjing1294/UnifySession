package net.gvsun.session.http;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public abstract class OnCommittedResponseWrapper extends HttpServletResponseWrapper {
    private boolean disableOnCommitted;
    private long contentLength;
    private long contentWritten;

    public OnCommittedResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    public void addHeader(String name, String value) {
        if ("Content-Length".equalsIgnoreCase(name)) {
            this.setContentLength(Long.parseLong(value));
        }

        super.addHeader(name, value);
    }

    public void setContentLengthLong(long len) {
        this.setContentLength(len);
        super.setContentLengthLong(len);
    }

    public void setContentLength(int len) {
        this.setContentLength((long) len);
        super.setContentLength(len);
    }

    private void setContentLength(long len) {
        this.contentLength = len;
        this.checkContentLength(0L);
    }

    private void disableOnResponseCommitted() {
        this.disableOnCommitted = true;
    }

    protected abstract void onResponseCommitted();

    public final void sendError(int sc) throws IOException {
        this.doOnResponseCommitted();
        super.sendError(sc);
    }

    public final void sendError(int sc, String msg) throws IOException {
        this.doOnResponseCommitted();
        super.sendError(sc, msg);
    }

    public final void sendRedirect(String location) throws IOException {
        this.doOnResponseCommitted();
        super.sendRedirect(location);
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return new OnCommittedResponseWrapper.SaveContextServletOutputStream(super.getOutputStream());
    }

    public PrintWriter getWriter() throws IOException {
        return new OnCommittedResponseWrapper.SaveContextPrintWriter(super.getWriter());
    }

    public void flushBuffer() throws IOException {
        this.doOnResponseCommitted();
        super.flushBuffer();
    }

    private void trackContentLength(boolean content) {
        this.checkContentLength(content ? 4L : 5L);
    }

    private void trackContentLength(char content) {
        this.checkContentLength(1L);
    }

    private void trackContentLength(Object content) {
        this.trackContentLength(String.valueOf(content));
    }

    private void trackContentLength(byte[] content) {
        this.checkContentLength(content != null ? (long) content.length : 0L);
    }

    private void trackContentLength(char[] content) {
        this.checkContentLength(content != null ? (long) content.length : 0L);
    }

    private void trackContentLength(int content) {
        this.trackContentLength(String.valueOf(content));
    }

    private void trackContentLength(float content) {
        this.trackContentLength(String.valueOf(content));
    }

    private void trackContentLength(double content) {
        this.trackContentLength(String.valueOf(content));
    }

    private void trackContentLengthLn() {
        this.trackContentLength("\r\n");
    }

    private void trackContentLength(String content) {
        this.checkContentLength((long) content.length());
    }

    private void checkContentLength(long contentLengthToWrite) {
        this.contentWritten += contentLengthToWrite;
        boolean isBodyFullyWritten = this.contentLength > 0L && this.contentWritten >= this.contentLength;
        int bufferSize = this.getBufferSize();
        boolean requiresFlush = bufferSize > 0 && this.contentWritten >= (long) bufferSize;
        if (isBodyFullyWritten || requiresFlush) {
            this.doOnResponseCommitted();
        }

    }

    private void doOnResponseCommitted() {
        if (!this.disableOnCommitted) {
            this.onResponseCommitted();
            this.disableOnResponseCommitted();
        }

    }

    private class SaveContextServletOutputStream extends ServletOutputStream {
        private final ServletOutputStream delegate;

        SaveContextServletOutputStream(ServletOutputStream delegate) {
            this.delegate = delegate;
        }

        public void write(int b) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(b);
            this.delegate.write(b);
        }

        public void flush() throws IOException {
            OnCommittedResponseWrapper.this.doOnResponseCommitted();
            this.delegate.flush();
        }

        public void close() throws IOException {
            OnCommittedResponseWrapper.this.doOnResponseCommitted();
            this.delegate.close();
        }

        public boolean equals(Object obj) {
            return this.delegate.equals(obj);
        }

        public int hashCode() {
            return this.delegate.hashCode();
        }

        public void print(boolean b) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(b);
            this.delegate.print(b);
        }

        public void print(char c) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(c);
            this.delegate.print(c);
        }

        public void print(double d) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(d);
            this.delegate.print(d);
        }

        public void print(float f) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(f);
            this.delegate.print(f);
        }

        public void print(int i) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(i);
            this.delegate.print(i);
        }

        public void print(long l) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength((float) l);
            this.delegate.print(l);
        }

        public void print(String s) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(s);
            this.delegate.print(s);
        }

        public void println() throws IOException {
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println();
        }

        public void println(boolean b) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(b);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(b);
        }

        public void println(char c) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(c);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(c);
        }

        public void println(double d) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(d);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(d);
        }

        public void println(float f) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(f);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(f);
        }

        public void println(int i) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(i);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(i);
        }

        public void println(long l) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength((float) l);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(l);
        }

        public void println(String s) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(s);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(s);
        }

        public void write(byte[] b) throws IOException {
            OnCommittedResponseWrapper.this.trackContentLength(b);
            this.delegate.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            OnCommittedResponseWrapper.this.checkContentLength((long) len);
            this.delegate.write(b, off, len);
        }

        public String toString() {
            return this.getClass().getName() + "[delegate=" + this.delegate.toString() + "]";
        }

        public boolean isReady() {
            return this.delegate.isReady();
        }

        public void setWriteListener(WriteListener writeListener) {
            this.delegate.setWriteListener(writeListener);
        }
    }

    private class SaveContextPrintWriter extends PrintWriter {
        private final PrintWriter delegate;

        SaveContextPrintWriter(PrintWriter delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        public void flush() {
            OnCommittedResponseWrapper.this.doOnResponseCommitted();
            this.delegate.flush();
        }

        public void close() {
            OnCommittedResponseWrapper.this.doOnResponseCommitted();
            this.delegate.close();
        }

        public boolean equals(Object obj) {
            return this.delegate.equals(obj);
        }

        public int hashCode() {
            return this.delegate.hashCode();
        }

        public String toString() {
            return this.getClass().getName() + "[delegate=" + this.delegate.toString() + "]";
        }

        public boolean checkError() {
            return this.delegate.checkError();
        }

        public void write(int c) {
            OnCommittedResponseWrapper.this.trackContentLength(c);
            this.delegate.write(c);
        }

        public void write(char[] buf, int off, int len) {
            OnCommittedResponseWrapper.this.checkContentLength((long) len);
            this.delegate.write(buf, off, len);
        }

        public void write(char[] buf) {
            OnCommittedResponseWrapper.this.trackContentLength(buf);
            this.delegate.write(buf);
        }

        public void write(String s, int off, int len) {
            OnCommittedResponseWrapper.this.checkContentLength((long) len);
            this.delegate.write(s, off, len);
        }

        public void write(String s) {
            OnCommittedResponseWrapper.this.trackContentLength(s);
            this.delegate.write(s);
        }

        public void print(boolean b) {
            OnCommittedResponseWrapper.this.trackContentLength(b);
            this.delegate.print(b);
        }

        public void print(char c) {
            OnCommittedResponseWrapper.this.trackContentLength(c);
            this.delegate.print(c);
        }

        public void print(int i) {
            OnCommittedResponseWrapper.this.trackContentLength(i);
            this.delegate.print(i);
        }

        public void print(long l) {
            OnCommittedResponseWrapper.this.trackContentLength((float) l);
            this.delegate.print(l);
        }

        public void print(float f) {
            OnCommittedResponseWrapper.this.trackContentLength(f);
            this.delegate.print(f);
        }

        public void print(double d) {
            OnCommittedResponseWrapper.this.trackContentLength(d);
            this.delegate.print(d);
        }

        public void print(char[] s) {
            OnCommittedResponseWrapper.this.trackContentLength(s);
            this.delegate.print(s);
        }

        public void print(String s) {
            OnCommittedResponseWrapper.this.trackContentLength(s);
            this.delegate.print(s);
        }

        public void print(Object obj) {
            OnCommittedResponseWrapper.this.trackContentLength(obj);
            this.delegate.print(obj);
        }

        public void println() {
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println();
        }

        public void println(boolean x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(char x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(int x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(long x) {
            OnCommittedResponseWrapper.this.trackContentLength((float) x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(float x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(double x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(char[] x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(String x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public void println(Object x) {
            OnCommittedResponseWrapper.this.trackContentLength(x);
            OnCommittedResponseWrapper.this.trackContentLengthLn();
            this.delegate.println(x);
        }

        public PrintWriter printf(String format, Object... args) {
            return this.delegate.printf(format, args);
        }

        public PrintWriter printf(Locale l, String format, Object... args) {
            return this.delegate.printf(l, format, args);
        }

        public PrintWriter format(String format, Object... args) {
            return this.delegate.format(format, args);
        }

        public PrintWriter format(Locale l, String format, Object... args) {
            return this.delegate.format(l, format, args);
        }

        public PrintWriter append(CharSequence csq) {
            OnCommittedResponseWrapper.this.checkContentLength((long) csq.length());
            return this.delegate.append(csq);
        }

        public PrintWriter append(CharSequence csq, int start, int end) {
            OnCommittedResponseWrapper.this.checkContentLength((long) (end - start));
            return this.delegate.append(csq, start, end);
        }

        public PrintWriter append(char c) {
            OnCommittedResponseWrapper.this.trackContentLength(c);
            return this.delegate.append(c);
        }
    }
}

