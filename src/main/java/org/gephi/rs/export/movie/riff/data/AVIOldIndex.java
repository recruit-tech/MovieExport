package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * AVI インデックス。<br/>
 * 以下の構造体をJavaクラスとしています。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/cc352254.aspx">http://msdn.microsoft.com/ja-jp/library/cc352254.aspx</a>
 * <br/>
 * @author abe
 */
public class AVIOldIndex implements Chunk {
    /** FOURCC コードを指定する。値は 'idx1' でなければならない。*/
    private String fcc;
    /** 構造体のサイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
    private long cb;
    /** */
    private AVIOldIndexEntry[] entries;

    public AVIOldIndex() {
        fcc = "idx1";
    }

    /**
     * AVI インデックス read.<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static AVIOldIndex read(InputStream is) throws IOException {
        AVIOldIndex ret = new AVIOldIndex();
        byte buf[] = new byte[4];
        if (is.read(buf) != buf.length) return null;
        long size = ConvTool.hexToULong(buf, 0);
        ret.fcc = "idx1";
        ret.cb = size;
        buf = new byte[(int)ret.cb];
        if (is.read(buf) != buf.length) return null;
        int idx = 0;
        int cnt = (int)(ret.cb / 16);
        ret.entries = new AVIOldIndexEntry[cnt];
        for(int i=0; i<cnt; i++) {
            ret.entries[i] = ret.new AVIOldIndexEntry();
            ret.entries[i].chunkId = ConvTool.getString(buf, 0, 4); idx+=4;
            ret.entries[i].flags = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.entries[i].offset = ConvTool.hexToULong(buf, idx); idx+=4;
            ret.entries[i].size = ConvTool.hexToULong(buf, idx); idx+=4;
        }
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

        for(AVIOldIndexEntry entry : entries) {
            os.write(ConvTool.getAscii(entry.getChunkId()));
            ConvTool.ulongToHex(entry.getFlags(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(entry.getOffset(), bufDWORD);
            os.write(bufDWORD);
            ConvTool.ulongToHex(entry.getSize(), bufDWORD);
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
        return entries.length * 16;
    }
    public AVIOldIndexEntry[] getEntries() {
        return entries;
    }
    public void setEntries(AVIOldIndexEntry[] entries) {
        this.entries = entries;
    }

    /**
     * AVI インデックスエントリ。<br/>
     * @author abe
     */
    public class AVIOldIndexEntry {
        /** データ チャンクはキー フレームである。*/
        public static final long AVIIF_KEYFRAME = 0x00000010;
        /** データ チャンクは 'rec' リストである。*/
        public static final long AVIIF_LIST = 0x00000001;
        /** データ チャンクはストリームのタイミングに影響しない。たとえば、このフラグはパレット変更の際に設定する。*/
        public static final long AVIIF_NO_TIME = 0x00000100;

        /**
         * AVI ファイル内のストリームを識別する FOURCC を指定する。FOURCC は 'xxyy' の形式でなければならない。xx はストリーム番号、yy はストリームの内容を識別する 2 桁の文字コードである。
         * <ul>
         * <li>'db' 非圧縮のビデオ フレーム。
         * <li>'dc' 圧縮されたビデオ フレーム。
         * <li>'pc' パレットの変更。
         * <li>'wb' オーディオ データ。
         * </ul>
         */
        private String chunkId;
        /** AVIIF_KEYFRAME,AVIIF_LIST,AVIIF_NO_TIMEの組み合わせ。*/
        private long flags;
        /** ファイル内のデータ チャンクの位置を指定する。値は、バイト単位で 'movi' リストの先頭からオフセットとして指定する必要がある。ただし、一部の AVI ファイルでは、ファイルの先頭からのオフセットとして指定する。*/
        private long offset;
        /** データ チャンクのサイズをバイト単位で指定する。*/
        private long size;

        public String getChunkId() {
            return chunkId;
        }
        public void setChunkId(String chunkId) {
            this.chunkId = chunkId;
        }
        public long getFlags() {
            return flags;
        }
        public void setFlags(long flags) {
            this.flags = flags;
        }
        public long getOffset() {
            return offset;
        }
        public void setOffset(long offset) {
            this.offset = offset;
        }
        public long getSize() {
            return size;
        }
        public void setSize(long size) {
            this.size = size;
        }
    }
}
