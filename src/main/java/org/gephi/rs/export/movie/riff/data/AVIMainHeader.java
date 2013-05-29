package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * Main AVI Header.<br/>
 * 以下の構造体をJavaクラスとしています。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/cc352261.aspx">http://msdn.microsoft.com/ja-jp/library/cc352261.aspx</a>
 * <br/>
 * @author abe
 */
public class AVIMainHeader implements Chunk {
    /** ファイルにインデックスがあることを示す。 */
    public static final long AVIF_HASINDEX = 0x00000010;
    /** データのプレゼンテーションの順序を決定するために、ファイル内のチャンクの物理的な順序ではなく、インデックスをアプリケーションが使うことを示す。たとえば、このフラグを使用して、編集するフレームのリストを作成できる。*/
    public static final long AVIF_MUSTUSEINDEX = 0x00000020;
    /** ファイルがインターリーブされていることを示す。*/
    public static final long AVIF_ISINTERLEAVED = 0x00000100;
    /** ファイルが、リアルタイム ビデオのキャプチャ用に特別に割り当てられたファイルであることを示す。アプリケーションは、このフラグが設定されたファイルをオーバーライドする前に、ユーザーに警告を発する必要がある。これは、ユーザーがこのファイルをデフラグメントしている可能性が高いからである。*/
    public static final long AVIF_WASCAPTUREFILE = 0x00010000;
    /** ファイルに著作権のあるデータおよびソフトウェアが含まれていることを示す。このフラグが使われている場合、ソフトウェアはデータの複製を許可すべきではない。*/
    public static final long AVIF_COPYRIGHTED = 0x00020000;

    /** (4byte)FOURCCコード。'avih'固定。*/
    private String fcc;
    /** (DWORD)構造体サイズ。最初の8バイト分を差し引いた値を指定する。*/
    private long cb;
    /** (DWORD)フレーム間の間隔をマイクロ秒単位で指定する。この値はファイル全体のタイミングを示す。*/
    private long microSecPerFrame;
    /** (DWORD)ファイルの概算最大データレートを指定する。この値は、メインヘッダーおよびストリームヘッダーチャンクに含まれる他のパラメータに従ってAVIシーケンスを表示するために、システムが処理しなければならない毎秒のバイト数を示す。*/
    private long maxBytesPerSec;
    /** (DWORD)データのアライメントをバイト単位で指定する。この値の倍数にデータをパディングする。*/
    private long paddingGranularity;
    /** (DWORD)AVIF_HASINDEX,AVIF_MUSTUSEINDEX,AVIF_ISINTERLEAVED,AVIF_WASCAPTUREFILE,AVIF_COPYRIGHTEDの組み合わせ。*/
    private long flags;
    /** (DWORD)ファイル内のデータのフレームの総数を指定する。*/
    private long totalFrames;
    /** (DWORD)インターリーブされたファイルの開始フレームを指定する。インターリーブされたファイル以外では、0 を指定する。インターリーブされたファイルを作成する場合、ファイル内で AVI シーケンスの開始フレームより前にあるフレーム数を、このメンバに指定する。このメンバの内容に関する詳細については、『Video for Windows Programmer's Guide』の「Special Information for Interleaved Files」を参照すること。*/
    private long initialFrames;
    /** (DWORD)ファイル内のストリーム数を指定する。たとえば、オーディオとビデオを含むファイルには 2 つのストリームがある。*/
    private long streams;
    /** (DWORD)ファイルを読み取るためのバッファ サイズを指定する。一般に、このサイズはファイル内の最大のチャンクを格納するのに十分な大きさにする。0 に設定したり、小さすぎる値に設定した場合、再生ソフトウェアは再生中にメモリを再割り当てしなければならず、パフォーマンスが低下する。インターリーブされたファイルの場合、バッファ サイズはチャンクではなくレコード全体を読み取るのに十分な大きさでなければならない。*/
    private long suggestedBufferSize;
    /** (DWORD)AVI ファイルの幅を指定する (ピクセル単位)。*/
    private long width;
    /** (DWORD)AVI ファイルの高さを指定する (ピクセル単位)。*/
    private long height;
    /** (DWORD)予約済み。この配列はゼロに設定する。*/
    private long reserved[];

    public AVIMainHeader() {
        setFcc("avih");
        reserved = new long[4];
    }

    /**
     * Main AVI Header read.<br/>
     * @param is
     * @return
     * @throws IOException
     */
    public static AVIMainHeader read(InputStream is) throws IOException {
        AVIMainHeader ret = new AVIMainHeader();
        byte buf[] = new byte[4];
        if (is.read(buf) != buf.length) return null;
        long size = ConvTool.hexToULong(buf, 0);
        ret.setFcc("avih");
        ret.setCb(size);
        buf = new byte[(int)ret.getCb()];
        if (is.read(buf) != buf.length) return null;
        int idx = 0;

        ret.setMicroSecPerFrame(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setMaxBytesPerSec(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setPaddingGranularity(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setFlags(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setTotalFrames(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setInitialFrames(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setStreams(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setSuggestedBufferSize(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setWidth(ConvTool.hexToULong(buf, idx)); idx+=4;
        ret.setHeight(ConvTool.hexToULong(buf, idx)); idx+=4;
        long[] reserved = new long[4];
        for(int i=0; i<4; i++)
            reserved[i] = ConvTool.hexToULong(buf, idx); idx+=4;
        ret.setReserved(reserved);
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

        ConvTool.ulongToHex(this.getMicroSecPerFrame(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getMaxBytesPerSec(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getPaddingGranularity(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getFlags(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getTotalFrames(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getInitialFrames(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getStreams(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getSuggestedBufferSize(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getWidth(), bufDWORD);
        os.write(bufDWORD);
        ConvTool.ulongToHex(this.getHeight(), bufDWORD);
        os.write(bufDWORD);
        long[] reserveds = this.getReserved();
        for(long reserved : reserveds) {
            ConvTool.ulongToHex(reserved, bufDWORD);
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
        return 56;
    }
    public long getMicroSecPerFrame() {
        return microSecPerFrame;
    }
    public void setMicroSecPerFrame(long microSecPerFrame) {
        this.microSecPerFrame = microSecPerFrame;
    }
    public long getMaxBytesPerSec() {
        return maxBytesPerSec;
    }
    public void setMaxBytesPerSec(long maxBytesPerSec) {
        this.maxBytesPerSec = maxBytesPerSec;
    }
    public long getPaddingGranularity() {
        return paddingGranularity;
    }
    public void setPaddingGranularity(long paddingGranularity) {
        this.paddingGranularity = paddingGranularity;
    }
    public long getFlags() {
        return flags;
    }
    public void setFlags(long flags) {
        this.flags = flags;
    }
    public long getTotalFrames() {
        return totalFrames;
    }
    public void setTotalFrames(long totalFrames) {
        this.totalFrames = totalFrames;
    }
    public long getInitialFrames() {
        return initialFrames;
    }
    public void setInitialFrames(long initialFrames) {
        this.initialFrames = initialFrames;
    }
    public long getStreams() {
        return streams;
    }
    public void setStreams(long streams) {
        this.streams = streams;
    }
    public long getSuggestedBufferSize() {
        return suggestedBufferSize;
    }
    public void setSuggestedBufferSize(long suggestedBufferSize) {
        this.suggestedBufferSize = suggestedBufferSize;
    }
    public long getWidth() {
        return width;
    }
    public void setWidth(long width) {
        this.width = width;
    }
    public long getHeight() {
        return height;
    }
    public void setHeight(long height) {
        this.height = height;
    }
    public long[] getReserved() {
        return reserved;
    }
    public void setReserved(long[] reserved) {
        this.reserved = reserved;
    }
}
