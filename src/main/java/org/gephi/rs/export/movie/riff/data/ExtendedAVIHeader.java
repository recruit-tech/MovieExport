package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * ISFT.<br/>
 * <br/>
 * @author abe
 */
public class ExtendedAVIHeader implements Chunk {
    /** (4byte)FOURCC コードを指定する。値は 'dmlh' でなければならない。*/
    private String fcc;
    /** (DWORD)構造体のサイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
    private long cb;

    /** Field Framing Information.*/
    private String softwareInfo;

    public ExtendedAVIHeader() {
        fcc = "ISFT";
        softwareInfo = "";
    }

    /**
     * Stream Header read.<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static ExtendedAVIHeader read(InputStream is) throws IOException {
        ExtendedAVIHeader ret = new ExtendedAVIHeader();
        byte buf[] = new byte[4];
        if (is.read(buf) != buf.length) return null;
        long size = ConvTool.hexToULong(buf, 0);
        ret.fcc = "ISFT";
        ret.cb = size;
        buf = new byte[(int)ret.cb];
        if (is.read(buf) != buf.length) return null;
        int idx = 0;

        ret.softwareInfo = ConvTool.getString(buf, idx, buf.length);
        
        return ret;
    }

    /**
     * RIFFフォーマット書き込み。<br/>
     * @param os
     * @throws IOException
     */
    @Override
    public void write(OutputStream os) throws IOException {
        byte[] bufDWORD = new byte[4];

        os.write(ConvTool.getAscii(getFcc()));
        ConvTool.ulongToHex(getCalcCb(), bufDWORD);
        os.write(bufDWORD);

        StringBuilder sb = new StringBuilder(this.getSoftwareInfo());
        os.write(ConvTool.getAscii(sb.toString()));
        if (sb.toString().length() % 2 != 0)
            os.write(0);
    }

    @Override
    public String getFcc() {
        return fcc;
    }
    @Override
    public void setFcc(String fcc) {
        this.fcc = fcc;
    }
    @Override
    public long getCb() {
        return cb;
    }
    @Override
    public void setCb(long cb) {
        this.cb = cb;
    }
    @Override
    public long getCalcCb() {
        long n = softwareInfo.length();
        if (n % 2 != 0)
            n++;
        return n;
    }

    public String getSoftwareInfo() {
        return softwareInfo;
    }

    public void setSoftwareInfo(String softwareInfo) {
        this.softwareInfo = softwareInfo;
    }
}
