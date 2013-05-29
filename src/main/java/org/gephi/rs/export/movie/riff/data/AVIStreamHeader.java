package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.gephi.rs.export.movie.riff.ConvTool;

/**
 * Stream Header.<br/>
 * 以下の構造体をJavaクラスとしています。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/cc352263.aspx">http://msdn.microsoft.com/ja-jp/library/cc352263.aspx</a>
 * <br/>
 * @author abe
 */
public class AVIStreamHeader implements Chunk {
	/** このストリームをデフォルトで有効にしないことを示す。*/
	public static final long AVISF_DISABLED = 0x00000001;
	/** このビデオ ストリームにパレットの変更が含まれることを示す。このフラグは、再生ソフトウェアに対して、パレットをアニメーションする必要があることを警告する。*/
	public static final long AVISF_VIDEO_PALCHANGES = 0x00010000;

	/** (4byte)FOURCC コードを指定する。値は 'strh' でなければならない。*/
	private String fcc;
	/** (DWORD)構造体のサイズを指定する。最初の 8 バイト分を差し引いた値を指定する。*/
	private long cb;
	/**
	 * (4byte)ストリームに含まれるデータのタイプを指定する FOURCC を含む。ビデオおよびオーディオに対して以下の標準 AVI 値が定義されている。<br/>
	 * <ul>
	 * <li>'auds' オーディオストリーム
	 * <li>'mids' MIDIストリーム
	 * <li>'txts' テキストストリーム
	 * <li>'vids' ビデオストリーム
	 * </ul>
	 */
	private String fccType;
	/** (4byte)特定のデータ ハンドラを示す FOURCC を含む (オプション)。データ ハンドラは、ストリームに対して適切なハンドラである。オーディオ ストリームまたはビデオ ストリームについて、ストリームをデコードするコーデックを指定する。*/
	private String fccHandler;
	/** (DWORD)データ ストリームに対するフラグを含む。これらのフラグの上位ワードのビットは、ストリームに含まれるデータのタイプに固有である。標準フラグ(AVISF_DISABLED, AVISF_VIDEO_PALCHANGES)が定義されている。 */
	private long flags;
	/** (WORD)ストリーム タイプの優先順位を指定する。たとえば、複数のオーディオ ストリームを含むファイルでは、優先順位の最も高いストリームがデフォルトのストリームになる。*/
	private int priority;
	/** (WORD)*/
	private int language;
	/** (DWORD)インターリーブされたファイルで、オーディオ データがビデオ フレームからどのくらいスキューされているかを指定する。通常は、約 0.75 秒である。インターリーブされたファイルを作成する場合、ファイル内で AVI シーケンスの開始フレームより前にあるフレーム数を、このメンバに指定する。このメンバの内容に関する詳細については、『Video for Windows Programmer's Guide』の「Special Information for Interleaved Files」を参照すること。*/
	private long initialFrames;
	/** (DWORD)rate と共に使って、このストリームが使うタイム スケールを指定する。rate を scale で割ることにより、1 秒あたりのサンプル数が求められる。ビデオ ストリームの場合、これはフレーム レートである。オーディオ ストリームの場合、このレートは nBlockAlign バイトのオーディオの再生に必要な時間に相当する。これは PCM オーディオの場合はサンプル レートに等しくなる。*/
	private long scale;
	/** (DWORD)scaleを参照すること。*/
	private long rate;
	/** (DWORD)このストリームの開始タイムを指定する。単位は、メイン ファイル ヘッダーの dwRate および dwScale メンバによって定義される。Usually, this is zero, but it can specify a delay time for a stream that does not start concurrently with the file.*/
	private long start;
	/** (DWORD)このストリームの長さを指定する。単位は、ストリームのヘッダーの dwRate および dwScale メンバによって定義される。*/
	private long length;
	/** (DWORD)このストリームを読み取るために必要なバッファの大きさを指定する。通常は、ストリーム内の最大のチャンクに対応する値である。正しいバッファ サイズを使うことで、再生の効率が高まる。正しいバッファ サイズがわからない場合は、0 を指定する。*/
	private long suggestedBufferSize;
	/** (DWORD)ストリーム内のデータの品質を示す値を指定する。品質は、0 ～ 10,000 の範囲の値で示される。圧縮データの場合、これは通常、圧縮ソフトウェアに渡される品質パラメータの値を示す。-1 に設定した場合、ドライバはデフォルトの品質値を使う。*/
	private long quality;
	/** (DWORD)データの 1 サンプルのサイズを指定する。サンプルのサイズが変化する場合は、0 に設定する。この値が 0 でない場合、ファイル内で複数のデータ サンプルを 1 つのチャンクにグループ化できる。0 の場合、各データ サンプル (ビデオ フレームなど) はそれぞれ別のチャンクに含まれなければならない。ビデオ ストリームの場合、この値は通常 0 であるが、すべてのビデオ フレームが同じサイズであれば、0 以外の値にもできる。オーディオ ストリームの場合、この値はオーディオを記述する WAVEFORMATEX 構造体の nBlockAlign メンバと同じでなければならない。*/
	private long sampleSize;
	/** AVI メイン ヘッダー構造体の dwWidth および dwHeight メンバによって指定される動画矩形内のテキストまたはビデオ ストリームに対する転送先矩形を指定する。通常、rcFrame メンバは、複数のビデオ ストリームをサポートするために使われる。この矩形は、動画矩形に対応する座標に設定して、動画矩形全体を更新する。このメンバの単位はピクセルである。転送先矩形の左上隅は、動画矩形の左上隅からの相対指定となる。*/
	private RectangleFrame rcFrame;

	public AVIStreamHeader() {
		fcc = "strh";
		rcFrame = new RectangleFrame();
	}

	/**
	 * Stream Header read.<br/>
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static AVIStreamHeader read(InputStream is) throws IOException {
		AVIStreamHeader ret = new AVIStreamHeader();
		byte buf[] = new byte[4];
		if (is.read(buf) != buf.length) return null;
		long size = ConvTool.hexToULong(buf, 0);
		ret.fcc = "strh";
		ret.cb = size;
		buf = new byte[(int)ret.cb];
		if (is.read(buf) != buf.length) return null;
		int idx = 0;

		ret.fccType = ConvTool.getString(buf, idx, 4); idx+=4;
		ret.fccHandler = ConvTool.getString(buf, idx, 4); idx+=4;
		ret.flags = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.priority = ConvTool.hexToUInt(buf, idx); idx+=2;
		ret.language = ConvTool.hexToUInt(buf, idx); idx+=2;
		ret.initialFrames = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.scale = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.rate = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.start = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.length = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.suggestedBufferSize = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.quality = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.sampleSize = ConvTool.hexToULong(buf, idx); idx+=4;
		ret.rcFrame.left = ConvTool.hexToShort(buf, idx); idx+=2;
		ret.rcFrame.top = ConvTool.hexToShort(buf, idx); idx+=2;
		ret.rcFrame.right = ConvTool.hexToShort(buf, idx); idx+=2;
		ret.rcFrame.bottom = ConvTool.hexToShort(buf, idx); idx+=2;
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
		byte[] bufWORD = new byte[2];

		os.write(ConvTool.getAscii(getFcc()));
		ConvTool.ulongToHex(getCalcCb(), bufDWORD);
		os.write(bufDWORD);

		os.write(ConvTool.getAscii(this.getFccType()));
		os.write(ConvTool.getAscii(this.getFccHandler()));
		ConvTool.ulongToHex(this.getFlags(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.uintToHex(this.getPriority(), bufWORD);
		os.write(bufWORD);
		ConvTool.uintToHex(this.getLanguage(), bufWORD);
		os.write(bufWORD);
		ConvTool.ulongToHex(this.getInitialFrames(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getScale(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getRate(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getStart(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getLength(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getSuggestedBufferSize(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getQuality(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.ulongToHex(this.getSampleSize(), bufDWORD);
		os.write(bufDWORD);
		ConvTool.shortToHex(this.getRcFrame().left, bufWORD);
		os.write(bufWORD);
		ConvTool.shortToHex(this.getRcFrame().top, bufWORD);
		os.write(bufWORD);
		ConvTool.shortToHex(this.getRcFrame().right, bufWORD);
		os.write(bufWORD);
		ConvTool.shortToHex(this.getRcFrame().bottom, bufWORD);
		os.write(bufWORD);
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
		return (11 * 4) + (6 * 2);
	}
	public String getFccType() {
		return fccType;
	}
	public void setFccType(String fccType) {
		this.fccType = fccType;
	}
	public String getFccHandler() {
		return fccHandler;
	}
	public void setFccHandler(String fccHandler) {
		this.fccHandler = fccHandler;
	}
	public long getFlags() {
		return flags;
	}
	public void setFlags(long flags) {
		this.flags = flags;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public int getLanguage() {
		return language;
	}
	public void setLanguage(int language) {
		this.language = language;
	}
	public long getInitialFrames() {
		return initialFrames;
	}
	public void setInitialFrames(long initialFrames) {
		this.initialFrames = initialFrames;
	}
	public long getScale() {
		return scale;
	}
	public void setScale(long scale) {
		this.scale = scale;
	}
	public long getRate() {
		return rate;
	}
	public void setRate(long rate) {
		this.rate = rate;
	}
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public long getSuggestedBufferSize() {
		return suggestedBufferSize;
	}
	public void setSuggestedBufferSize(long suggestedBufferSize) {
		this.suggestedBufferSize = suggestedBufferSize;
	}
	public long getQuality() {
		return quality;
	}
	public void setQuality(long quality) {
		this.quality = quality;
	}
	public long getSampleSize() {
		return sampleSize;
	}
	public void setSampleSize(long sampleSize) {
		this.sampleSize = sampleSize;
	}
	public RectangleFrame getRcFrame() {
		return rcFrame;
	}
	public void setRcFrame(RectangleFrame rcFrame) {
		this.rcFrame = rcFrame;
	}

	/**
	 * Frame rectangle.<br/>
	 * @author abe
	 */
	public class RectangleFrame {
		public short left;
		public short top;
		public short right;
		public short bottom;
	}
}
