package com.mindoo.domino.jna.internal;

import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStream extends OutputStream {
    private OutputStream m_out1;
    private OutputStream m_out2;

    public TeeOutputStream(OutputStream out1, OutputStream ut2) {
        this.m_out1 = out1;
        this.m_out2 = ut2;
    }

    public void close() throws IOException {
        try {
            m_out1.close();
        } finally {
            m_out2.close();
        }
    }

    public void flush() throws IOException {
        m_out1.flush();
        m_out2.flush();
    }

    public void write(byte[] b) throws IOException {
        m_out1.write(b);
        m_out2.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        m_out1.write(b, off, len);
        m_out2.write(b, off, len);
    }

    public void write(int b) throws IOException {
        m_out1.write(b);
        m_out2.write(b);
    }
}

