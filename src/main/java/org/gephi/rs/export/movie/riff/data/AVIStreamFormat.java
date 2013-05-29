package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * Stream format.<br/>
 * @author abe
 */
public class AVIStreamFormat implements Chunk {
    /** (4byte)FOURCC コードを指定する。値は 'strf' でなければならない。*/
    private String fcc;
    /** (DWORD)構造体のサイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
    private long cb;
    /** */
    private Object data;

    public AVIStreamFormat() {
        fcc = "strf";
    }

    /**
     * Stream Format read.<br/>
     * 本来のRIFFフォーマットではWAVE等もサポートしますが、簡易化のためBITMAPのみ固定で返します。<br/>
     * Motion JPEG出力のみ主眼としますので、読み込みコードは試験用と割り切ってください。<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static AVIStreamFormat read(InputStream is) throws IOException {
        AVIStreamFormat ret = new AVIStreamFormat();
        Bitmapinfo bi = new Bitmapinfo();
        BitmapinfoHeader bmiHeader = new BitmapinfoHeader();
        bi.setBmiHeader(bmiHeader);
        bi.setBmiColors(null);

        byte buf[] = new byte[4];
        if (is.read(buf) != buf.length) return null;
        long size = ConvTool.hexToULong(buf, 0);
        ret.fcc = "strf";
        ret.cb = size;
        ret.data = bi;
        buf = new byte[(int)ret.cb];
        if (is.read(buf) != buf.length) return null;
        int idx = 0;

        bmiHeader.setSize(ConvTool.hexToULong(buf, idx)); idx+=4;
        bmiHeader.setWidth(ConvTool.hexToLong(buf, idx)); idx+=4;
        bmiHeader.setHeight(ConvTool.hexToLong(buf, idx)); idx+=4;
        bmiHeader.setPlanes(ConvTool.hexToUInt(buf, idx)); idx+=2;
        bmiHeader.setBitCount(ConvTool.hexToUInt(buf, idx)); idx+=2;
        bmiHeader.setCompression(ConvTool.hexToULong(buf, idx)); idx+=4;
        bmiHeader.setSizeImage(ConvTool.hexToULong(buf, idx)); idx+=4;
        bmiHeader.setxPelsMeter(ConvTool.hexToLong(buf, idx)); idx+=4;
        bmiHeader.setyPelsMeter(ConvTool.hexToLong(buf, idx)); idx+=4;
        bmiHeader.setClrUsed(ConvTool.hexToULong(buf, idx)); idx+=4;
        bmiHeader.setClrImportant(ConvTool.hexToULong(buf, idx)); idx+=4;
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
        byte[] bufWORD = new byte[2];

        os.write(ConvTool.getAscii(getFcc()));
        ConvTool.ulongToHex(getCalcCb(), bufDWORD);
        os.write(bufDWORD);

        if (data.getClass() == Bitmapinfo.class) {
            Bitmapinfo bi = (Bitmapinfo)data;
            BitmapinfoHeader bih = bi.getBmiHeader();
            RGBQuad[] rgbqs = bi.getBmiColors();
            ConvTool.ulongToHex(bih.getSize(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.longToHex(bih.getWidth(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.longToHex(bih.getHeight(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.uintToHex(bih.getPlanes(), bufWORD);
            os.write(bufWORD);
            ConvTool.uintToHex(bih.getBitCount(), bufWORD);
            os.write(bufWORD);
            ConvTool.ulongToHex(bih.getCompression(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(bih.getSizeImage(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.longToHex(bih.getxPelsMeter(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.longToHex(bih.getyPelsMeter(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(bih.getClrUsed(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(bih.getClrImportant(), bufDWORD);
            os.write(bufDWORD);
            if(rgbqs != null) {
                for(RGBQuad rgbq : rgbqs) {
                    os.write(rgbq.getBlue());
                    os.write(rgbq.getGreen());
                    os.write(rgbq.getRed());
                    os.write(rgbq.getReserved());
                }
            }
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
        long ret = 0;
        if (data.getClass() == Bitmapinfo.class) {
            Bitmapinfo bi = (Bitmapinfo)data;
            ret = 40;
            if (bi.getBmiColors() != null) {
                ret += bi.getBmiColors().length * 8;
            }
        }
        return ret;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
}
