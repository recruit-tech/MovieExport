package org.gephi.rs.export.movie.riff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.gephi.rs.export.movie.riff.data.AVIMainHeader;
import org.gephi.rs.export.movie.riff.data.AVIOldIndex;
import org.gephi.rs.export.movie.riff.data.AVIOldIndex.AVIOldIndexEntry;
import org.gephi.rs.export.movie.riff.data.AVIStreamFormat;
import org.gephi.rs.export.movie.riff.data.AVIStreamHeader;
import org.gephi.rs.export.movie.riff.data.AVIStreamHeader.RectangleFrame;
import org.gephi.rs.export.movie.riff.data.Bitmapinfo;
import org.gephi.rs.export.movie.riff.data.BitmapinfoHeader;
import org.gephi.rs.export.movie.riff.data.Chunk;
import org.gephi.rs.export.movie.riff.data.ISFT;
import org.gephi.rs.export.movie.riff.data.JPEGData;
import org.gephi.rs.export.movie.riff.data.RiffList;
import org.gephi.rs.export.movie.riff.data.VideoPropertiesHeader;
import org.gephi.rs.export.movie.riff.data.VideoPropertiesHeader.VIDEO_FIELD_DESC;

/**
 * Motion JPEG出力。<br/>
 * Motion JPEGのAVIフォーマットを抽象化したRiffListオブジェクトが返されます。<br/>
 * RIFFフォーマットに独自のチャンクを追加する場合はoutputPathにnullを指定し、返されるRiffListオブジェクトを編集した後、writeメソッドでファイルに出力します。<br/>
 * 出力されるRIFFフォーマットは以下のイメージになります。<br/>
 * <pre>
 * RIFF ('AVI '
 *     LIST ('hdrl'
 *         'avih'(&lt;Main AVI Header&gt;)
 *         LIST ('strl'
 *             'strh'(&lt;Stream header&gt;)
 *             'strf'(&lt;Stream format&gt;)
 *         )
 *     )
 *     LIST ('movi'
 *        'dc00' (&lt;JPEG data&gt;)
 *        'dc00' (&lt;JPEG data&gt;)
 *        ...
 *        'dc00' (&lt;JPEG data&gt;)
 *     )
 *     'idx1' (&lt;AVI Index&gt;)
 * )
 * </pre>
 * AVI RIFFについては以下のサイトをご参照ください。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/cc352264.aspx">http://msdn.microsoft.com/ja-jp/library/cc352264.aspx</a>
 * <br/>
 * @author abe
 */
public class MotionJPEG {
    /**
     * 複数のjpeg画像より動画出力。<br/>
     * @param width 画像の幅
     * @param height 画像の高さ
     * @param frameRate フレームレート
     * @param imagePaths 画像パスの配列
     * @param outputPath 出力する動画ファイルのパス、nullの場合は出力されません
     * @return 作成されたMotion JPEGのオブジェクトが返されます
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public RiffList createMovie(int width, int height, int frameRate, String[] imagePaths, String outputPath) throws UnsupportedEncodingException, IOException {
        int totalFrame = imagePaths.length;
        long maxBufferSize = 0;
        File[] files = new File[imagePaths.length];
        for(int i=0; i<imagePaths.length; i++) {
            files[i] = new File(imagePaths[i]);
            if (files[i].length() > maxBufferSize)
                maxBufferSize = files[i].length();
        }

        RiffList avi = new RiffList();
        List<Chunk> aviChunks = new ArrayList<Chunk>();

        // 'hdrl'チャンク作成
        RiffList hdrl = createhdrl(frameRate, totalFrame, maxBufferSize, width, height);
        aviChunks.add(hdrl);
        
        // 'INFO'チャンク作成
        RiffList INFO = createINFO();
        aviChunks.add(INFO);
        
        // 'movi'チャンク作成
        RiffList movi = createmovi(files);
        aviChunks.add(movi);

        // 'idx1'チャンク作成
        aviChunks.add(createidx1(files));

        avi.setFcc("RIFF");
        avi.setName("AVI ");
        avi.setChunks(aviChunks.toArray(new Chunk[0]));

        if (outputPath != null) {
            FileOutputStream output = new FileOutputStream(outputPath);
            avi.write(output);
            output.flush();
            output.close();
        }
        return avi;
    }

    /**
     * 'hdrl'チャンク作成。<br/>
     * @param frameRate
     * @param totalFrame
     * @param maxBufferSize
     * @param width
     * @param height
     * @return
     */
    private RiffList createhdrl(int frameRate, int totalFrame, long maxBufferSize, int width, int height) {
        RiffList hdrl = new RiffList();
        hdrl.setFcc("LIST");
        hdrl.setName("hdrl");
        List<Chunk> hdrlChunks = new ArrayList<Chunk>();

        AVIMainHeader aviMainHeader = new AVIMainHeader();
        double rate = (1.0 / (double)frameRate) * 1000 * 1000;
        aviMainHeader.setMicroSecPerFrame((long)rate);
        //aviMainHeader.setMaxBytesPerSec(frameRate * 1000);
        aviMainHeader.setMaxBytesPerSec(25000);
        aviMainHeader.setPaddingGranularity(0);
        long flags = AVIMainHeader.AVIF_HASINDEX | AVIMainHeader.AVIF_ISINTERLEAVED | AVIMainHeader.AVIF_WASCAPTUREFILE;
        aviMainHeader.setFlags(flags);
        //aviMainHeader.setFlags(0x910);
        aviMainHeader.setTotalFrames(totalFrame);
        aviMainHeader.setInitialFrames(0);
        aviMainHeader.setStreams(1);
        aviMainHeader.setSuggestedBufferSize(maxBufferSize);
        aviMainHeader.setWidth(width);
        aviMainHeader.setHeight(height);
        hdrlChunks.add(aviMainHeader);

        RiffList strl = new RiffList();
        strl.setFcc("LIST");
        strl.setName("strl");
        List<Chunk> strlChunks = new ArrayList<Chunk>();
        {
            AVIStreamHeader aviStreamHeader = new AVIStreamHeader();
            aviStreamHeader.setFccType("vids");
            aviStreamHeader.setFccHandler("MJPG");
            aviStreamHeader.setFlags(0);
            aviStreamHeader.setPriority(0);
            aviStreamHeader.setLanguage(0);
            aviStreamHeader.setInitialFrames(0);
            aviStreamHeader.setScale(1);
            aviStreamHeader.setRate(frameRate);
            aviStreamHeader.setStart(0);
            aviStreamHeader.setLength(totalFrame);
            aviStreamHeader.setSuggestedBufferSize(maxBufferSize);
            aviStreamHeader.setQuality(0xFFFFFFFF);
            aviStreamHeader.setSampleSize(0);
            RectangleFrame rcFrame = aviStreamHeader.new RectangleFrame();
            rcFrame.top = 0;
            rcFrame.left = 0;
            rcFrame.bottom = (short)height;
            rcFrame.right = (short)width;
            aviStreamHeader.setRcFrame(rcFrame);
            strlChunks.add(aviStreamHeader);

            AVIStreamFormat aviStreamFormat = new AVIStreamFormat();
            Bitmapinfo bi = new Bitmapinfo();
            BitmapinfoHeader bmiHeader = new BitmapinfoHeader();
            bmiHeader.setSize(40);
            bmiHeader.setWidth(width);
            bmiHeader.setHeight(height);
            bmiHeader.setPlanes(1);
            bmiHeader.setBitCount(24);
            bmiHeader.setCompression(1196444237);
            bmiHeader.setSizeImage(6220800);
            bmiHeader.setxPelsMeter(0);
            bmiHeader.setyPelsMeter(0);
            bmiHeader.setClrUsed(0);
            bmiHeader.setClrImportant(0);
            bi.setBmiHeader(bmiHeader);
            bi.setBmiColors(null);
            aviStreamFormat.setData(bi);
            strlChunks.add(aviStreamFormat);

            VideoPropertiesHeader videoPropertiesHeader = new VideoPropertiesHeader();
            videoPropertiesHeader.setVideoFormatToken(0);
            videoPropertiesHeader.setVideoStandard(0);
            videoPropertiesHeader.setDwVerticalRefreshRate(3);
            videoPropertiesHeader.setDwHTotalInT(width);
            videoPropertiesHeader.setDwVTotalInLines(height);
            videoPropertiesHeader.setDwFrameAspectRatio(1048585);
            videoPropertiesHeader.setDwFrameWidthInPixels(width);
            videoPropertiesHeader.setDwFrameHeightInLines(height);
            videoPropertiesHeader.setNbFieldPerFrame(1);
            VideoPropertiesHeader.VIDEO_FIELD_DESC[] videoFieldDescs = new VIDEO_FIELD_DESC[1];
            videoFieldDescs[0] = videoPropertiesHeader.new VIDEO_FIELD_DESC();
            videoFieldDescs[0].CompressedBMHeight = height;
            videoFieldDescs[0].CompressedBMWidth = width;
            videoFieldDescs[0].ValidBMHeight = height;
            videoFieldDescs[0].ValidBMWidth = width;
            videoFieldDescs[0].ValidBMXOffset = 0;
            videoFieldDescs[0].ValidBMYOffset = 0;
            videoFieldDescs[0].VideoXOffsetInT = 0;
            videoFieldDescs[0].VideoYValidStartLine = 0;
            videoPropertiesHeader.setFieldInfo(videoFieldDescs);
            strlChunks.add(videoPropertiesHeader);
        }
        strl.setChunks(strlChunks.toArray(new Chunk[0]));
        hdrlChunks.add(strl);
        
        hdrl.setChunks(hdrlChunks.toArray(new Chunk[0]));
        return hdrl;
    }

    /**
     * 'INFO'チャンク作成。<br/>
     * @return
     */
    private RiffList createINFO() {
        RiffList info = new RiffList();
        info.setFcc("LIST");
        info.setName("INFO");
        List<Chunk> infoChunks = new ArrayList<Chunk>();
        ISFT isft = new ISFT();
        isft.setSoftwareInfo("Lavf54.59.107");
        infoChunks.add(isft);
        info.setChunks(infoChunks.toArray(new Chunk[0]));
        return info;
    }

    /**
     * 'movi'チャンク作成。<br/>
     * @param files
     * @return
     */
    private RiffList createmovi(File[] files) {
        RiffList movi = new RiffList();
        movi.setFcc("LIST");
        movi.setName("movi");
        List<Chunk> moviChunks = new ArrayList<Chunk>();

        for(File file : files) {
            JPEGData jpegData = new JPEGData();
            jpegData.setPath(file.getPath());
            moviChunks.add(jpegData);
        }

        movi.setChunks(moviChunks.toArray(new Chunk[0]));
        return movi;
    }

    /**
     * 'idx1'チャンク作成。<br/>
     * @param files
     * @return
     */
    private AVIOldIndex createidx1(File[] files) {
        AVIOldIndex aviOldIndex = new AVIOldIndex();
        List<AVIOldIndexEntry> entries = new ArrayList<AVIOldIndex.AVIOldIndexEntry>();
        long offset = 4;
        for(File file : files) {
            AVIOldIndexEntry entry = aviOldIndex.new AVIOldIndexEntry();
            entry.setChunkId("00dc");
            entry.setFlags(AVIOldIndexEntry.AVIIF_KEYFRAME);
            entry.setOffset(offset);
            entry.setSize(file.length());
            long n = file.length();
            if (file.length() % 2 != 0)
                n++;
            offset += 8 + n;
            entries.add(entry);
        }
        aviOldIndex.setEntries(entries.toArray(new AVIOldIndexEntry[0]));
        return aviOldIndex;
    }
}
