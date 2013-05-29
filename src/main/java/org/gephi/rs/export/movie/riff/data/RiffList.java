package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * RIFF 'LIST' Data.<br/>
 * @author abe
 */
public class RiffList implements Chunk {
    /** (4byte)FOURCC コードを指定する。値は 'LIST' でなければならない。*/
    private String fcc;
    /** (DWORD)サイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
    private long cb;
    /** */
    private String name;
    /** LIST or chunk data.*/
    private Chunk[] chunks;

    /**
     * RIFFフォーマット、'LIST'読み込み。<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static RiffList read(InputStream is) throws IOException {
        RiffList ret = new RiffList();
        List<Chunk> chunks = new ArrayList<Chunk>();
        byte buf[] = new byte[4];
        is.read(buf);
        ret.setFcc("LIST");
        ret.setCb(ConvTool.hexToULong(buf, 0));
        if (ret.getCb() == 0)
            return null;
        long idx = 0;
        if (is.read(buf) != buf.length) return null;
        ret.setName(ConvTool.getString(buf, 0, 4));
        idx += 4;
        while (idx < ret.getCb()) {
            if (is.read(buf) != buf.length) break;
            String type = ConvTool.getString(buf, 0, 4);
            idx += 4;
            if (type.equals("LIST")) {
                RiffList rl = read(is);
                if (rl == null)
                    break;
                idx += rl.getCb() + 8;
                if (rl.chunks.length > 0)
                    chunks.add(rl);
            } else {
                if (type.equals("avih")) {
                    // Main AVI Header
                    AVIMainHeader aviMainHeader = AVIMainHeader.read(is);
                    idx += aviMainHeader.getCb() + 8;
                    chunks.add(aviMainHeader);
                } else if (type.equals("strh")) {
                    // Stream Header
                    AVIStreamHeader aviStreamHeader = AVIStreamHeader.read(is);
                    idx += aviStreamHeader.getCb() + 8;
                    chunks.add(aviStreamHeader);
                } else if (type.equals("strf")) {
                    // Stream Format
                    AVIStreamFormat aviStreamFormat = AVIStreamFormat.read(is);
                    idx += aviStreamFormat.getCb() + 8;
                    chunks.add(aviStreamFormat);
                } else if (type.equals("vprp")) {
                    VideoPropertiesHeader videoPropertiesHeader = VideoPropertiesHeader.read(is);
                    idx += videoPropertiesHeader.getCb() + 8;
                    chunks.add(videoPropertiesHeader);
                } else if (type.equals("ISFT")) {
                    ISFT isft = ISFT.read(is);
                    idx += isft.getCb() + 8;
                    chunks.add(isft);
                } else if (type.equals("00dc")) {
                    // JPEG data
                    JPEGData jpegData = JPEGData.read(is);
                    idx += jpegData.getCb() + 8;
                    chunks.add(jpegData);
                } else if (type.equals("idx1")) {
                    // AVI Index
                    AVIOldIndex aviOldIndex = AVIOldIndex.read(is);
                    idx += aviOldIndex.getCb() + 8;
                    chunks.add(aviOldIndex);
                } else {
                    if (is.read(buf) != buf.length) break;
                    long size = ConvTool.hexToULong(buf, 0);
                    is.skip(size);
                    idx += size + 4;
                }
            }
        }
        ret.setChunks(chunks.toArray(new Chunk[0]));
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
        os.write(ConvTool.getAscii(getFcc()));
        byte[] buf = new byte[4];
        ConvTool.ulongToHex(getCalcCb(), buf);
        os.write(buf);
        os.write(ConvTool.getAscii(getName()));
        for(Chunk chunk : chunks) {
            chunk.write(os);
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
        long ret = 4;
        for(Chunk chunk : chunks) {
            ret += chunk.getCalcCb() + 8;
        }
        return ret;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Chunk[] getChunks() {
        return chunks;
    }
    public void setChunks(Chunk[] chunks) {
        this.chunks = chunks;
    }
}
