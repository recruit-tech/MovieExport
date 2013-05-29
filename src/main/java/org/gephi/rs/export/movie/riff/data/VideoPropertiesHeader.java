package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * Video Properties Header.<br/>
 * 以下のPDF内のvprp構造体をJavaクラスとしています。<br/>
 * <a href="http://www.the-labs.com/Video/odmlff2-avidef.pdf">http://www.the-labs.com/Video/odmlff2-avidef.pdf</a>
 * <br/>
 * @author abe
 */
public class VideoPropertiesHeader implements Chunk {
    /** (4byte)FOURCC コードを指定する。値は 'vprp' でなければならない。*/
    private String fcc;
    /** (DWORD)構造体のサイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
    private long cb;

    /** (DWORD)VideoFormatToken.*/
    private long VideoFormatToken;
    /** (DWORD)VideoStandard.*/
    private long VideoStandard;
    /** (DWORD)dwVerticalRefreshRate.*/
    private long dwVerticalRefreshRate;
    /** (DWORD)dwHTotalInT.*/
    private long dwHTotalInT;
    /** (DWORD)dwVTotalInLines.*/
    private long dwVTotalInLines;
    /** (DWORD)dwFrameAspectRatio.*/
    private long dwFrameAspectRatio;
    /** (DWORD)dwFrameWidthInPixels.*/
    private long dwFrameWidthInPixels;
    /** (DWORD)dwFrameHeightInLines.*/
    private long dwFrameHeightInLines;
    /** (DWORD)nbFieldPerFrame.*/
    private long nbFieldPerFrame;
    /** Field Framing Information.*/
    private VIDEO_FIELD_DESC[] FieldInfo;

    public VideoPropertiesHeader() {
        fcc = "vprp";
        FieldInfo = new VIDEO_FIELD_DESC[0];
    }

    /**
     * Stream Header read.<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static VideoPropertiesHeader read(InputStream is) throws IOException {
        VideoPropertiesHeader ret = new VideoPropertiesHeader();
        byte buf[] = new byte[4];
        if (is.read(buf) != buf.length) return null;
        long size = ConvTool.hexToULong(buf, 0);
        ret.fcc = "vprp";
        ret.cb = size;
        buf = new byte[(int)ret.cb];
        if (is.read(buf) != buf.length) return null;
        int idx = 0;

        ret.VideoFormatToken = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.VideoStandard = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.dwVerticalRefreshRate = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.dwHTotalInT = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.dwVTotalInLines = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.dwFrameAspectRatio = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.dwFrameWidthInPixels = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.dwFrameHeightInLines = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.nbFieldPerFrame = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.FieldInfo = new VIDEO_FIELD_DESC[(int)ret.nbFieldPerFrame];
        for(long i=0; i<ret.nbFieldPerFrame; i++) {
            ret.FieldInfo[(int)i] = ret.new VIDEO_FIELD_DESC();
            ret.FieldInfo[(int)i].CompressedBMHeight = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].CompressedBMWidth = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].ValidBMHeight = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].ValidBMWidth = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].ValidBMXOffset = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].ValidBMYOffset = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].VideoXOffsetInT = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.FieldInfo[(int)i].VideoYValidStartLine = ConvTool.hexToULong(buf, idx); idx+=4;
        }
        
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

        ConvTool.ulongToHex(this.getVideoFormatToken(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getVideoStandard(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getDwVerticalRefreshRate(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getDwHTotalInT(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getDwVTotalInLines(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getDwFrameAspectRatio(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getDwFrameWidthInPixels(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getDwFrameHeightInLines(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getNbFieldPerFrame(), bufDWORD);
        os.write(bufDWORD);
        for(VIDEO_FIELD_DESC videoFieldDesclong : this.getFieldInfo()) {
            ConvTool.ulongToHex(videoFieldDesclong.CompressedBMHeight, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.CompressedBMWidth, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.ValidBMHeight, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.ValidBMWidth, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.ValidBMXOffset, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.ValidBMYOffset, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.VideoXOffsetInT, bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(videoFieldDesclong.VideoYValidStartLine, bufDWORD);
            os.write(bufDWORD);
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
        return (9 * 4) + (nbFieldPerFrame * (8 * 4));
    }

    public long getVideoFormatToken() {
        return VideoFormatToken;
    }

    public void setVideoFormatToken(long VideoFormatToken) {
        this.VideoFormatToken = VideoFormatToken;
    }

    public long getVideoStandard() {
        return VideoStandard;
    }

    public void setVideoStandard(long VideoStandard) {
        this.VideoStandard = VideoStandard;
    }

    public long getDwVerticalRefreshRate() {
        return dwVerticalRefreshRate;
    }

    public void setDwVerticalRefreshRate(long dwVerticalRefreshRate) {
        this.dwVerticalRefreshRate = dwVerticalRefreshRate;
    }

    public long getDwHTotalInT() {
        return dwHTotalInT;
    }

    public void setDwHTotalInT(long dwHTotalInT) {
        this.dwHTotalInT = dwHTotalInT;
    }

    public long getDwVTotalInLines() {
        return dwVTotalInLines;
    }

    public void setDwVTotalInLines(long dwVTotalInLines) {
        this.dwVTotalInLines = dwVTotalInLines;
    }

    public long getDwFrameAspectRatio() {
        return dwFrameAspectRatio;
    }

    public void setDwFrameAspectRatio(long dwFrameAspectRatio) {
        this.dwFrameAspectRatio = dwFrameAspectRatio;
    }

    public long getDwFrameWidthInPixels() {
        return dwFrameWidthInPixels;
    }

    public void setDwFrameWidthInPixels(long dwFrameWidthInPixels) {
        this.dwFrameWidthInPixels = dwFrameWidthInPixels;
    }

    public long getDwFrameHeightInLines() {
        return dwFrameHeightInLines;
    }

    public void setDwFrameHeightInLines(long dwFrameHeightInLines) {
        this.dwFrameHeightInLines = dwFrameHeightInLines;
    }

    public long getNbFieldPerFrame() {
        return nbFieldPerFrame;
    }

    public void setNbFieldPerFrame(long nbFieldPerFrame) {
        this.nbFieldPerFrame = nbFieldPerFrame;
    }

    public VIDEO_FIELD_DESC[] getFieldInfo() {
        return FieldInfo;
    }

    public void setFieldInfo(VIDEO_FIELD_DESC[] FieldInfo) {
        this.FieldInfo = FieldInfo;
    }

    /**
     * Field Framing Information.<br/>
     * @author abe
     */
    public class VIDEO_FIELD_DESC {
        public long CompressedBMHeight;
        public long CompressedBMWidth;
        public long ValidBMHeight;
        public long ValidBMWidth;
        public long ValidBMXOffset;
        public long ValidBMYOffset;
        public long VideoXOffsetInT;
        public long VideoYValidStartLine;
    }
}
