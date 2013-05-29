package org.gephi.rs.export.movie.riff.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * JPEG data.<br/>
 * @author abe
 */
public class JPEGData implements Chunk {
    /** (4byte)FOURCC コードを指定する。値は '00dc' でなければならない。*/
    private String fcc;
    /** (DWORD)構造体のサイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
    private long cb;
    /** JPEGデータ。*/
    private byte[] data;
    /** JPEGソースファイルパス。*/
    private String path;

    public JPEGData() {
        fcc = "00dc";
    }

    /**
     * JPEG data read.<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static JPEGData read(InputStream is) throws IOException {
        JPEGData ret = new JPEGData();
        byte buf[] = new byte[4];
        if (is.read(buf) != buf.length) return null;
        long size = ConvTool.hexToULong(buf, 0);
        ret.fcc = "00dc";
        if ((size % 2) != 0) {
            // 奇数の場合は1バイト加算
            size++;
        }
        ret.cb = size;
        ret.data = new byte[(int)ret.cb];
        if (is.read(ret.data) != ret.data.length) return null;
        return ret;
    }

    /**
     * RIFFフォーマット書き込み。<br/>
     * @param os
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    @Override
    public void write(OutputStream os) throws UnsupportedEncodingException, IOException {
        byte[] bufDWORD = new byte[4];

        os.write(ConvTool.getAscii(getFcc()));
        ConvTool.ulongToHex(getCalcCb(), bufDWORD);
        os.write(bufDWORD);

        if (path != null) {
            FileInputStream input = new FileInputStream(path);
            byte[] buf = new byte[1024];
            int size;
            while ((size = input.read(buf)) > 0) {
                os.write(buf, 0, size);
            }
            input.close();
            File file = new File(path);
            if ((file.length() % 2) != 0) {
                // 偶数ではない場合は1byte追加
                os.write(0);
            }
        } else {
            os.write(data);
        }
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
        long ret;
        if (path != null) {
            File file = new File(path);
            ret = file.length();
            if ((ret % 2) != 0) {
                // 偶数ではない場合+1
                ret++;
            }
        } else {
            ret = data.length;
        }
        return ret;
    }
    public byte[] getData() {
        return data;
    }
    public void setData(byte[] data) {
        this.data = data;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
}
