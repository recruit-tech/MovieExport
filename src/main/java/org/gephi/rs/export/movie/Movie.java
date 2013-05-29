/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGOMCircleElement;
import org.apache.batik.dom.svg.SVGOMPathElement;
import org.apache.batik.dom.svg.SVGOMPolylineElement;
import org.apache.batik.dom.svg.SVGOMSVGElement;
import org.apache.batik.dom.svg.SVGOMTextElement;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.gephi.dynamic.DynamicUtilities;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicGraph;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.filters.api.Range;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder;
import org.gephi.filters.plugin.dynamic.DynamicRangeBuilder.DynamicRangeFilter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.GraphView;
import org.gephi.io.exporter.preview.ExporterBuilderSVG;
import org.gephi.io.exporter.preview.SVGExporter;
import org.gephi.layout.api.LayoutController;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingoldBuilder;
import org.gephi.layout.spi.Layout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.rs.export.movie.player.data.GephiBinary;
import org.gephi.rs.export.movie.player.data.GephiBinaryFrame;
//import static org.gephi.rs.export.movie.JpegImagesToMovie.createMediaLocator;
import org.openide.util.Lookup;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Gephiより動画ファイルを作成します。<br/>
 * <p>
 * 動画化はGEXFのStaticでもDynamicsでも可能ですがStaticの場合は必ず追加再生時間を指定してください。<br/>
 * ノード・エッジの各データに生存期間を付加することができるDynamics形式の場合はGEXFのstart・endを再生時間としています。<br/>
 * Dynamics形式の詳細は<a href="http://gexf.net/format/">GEXFファイルフォーマット</a>を参照してください。<br/>
 * 上記リンクの<a href="http://gexf.net/1.2draft/gexf-12draft-primer.pdf">Primer</a>の「5 Advanced Concepts III: Dynamics」でより詳細な内容が参照できます。<br/>
 * </p>
 * <p>
 * Gephiのデータファイル(*.gexf)から動画を作成する場合は基本的に以下の手順で行います。
 * <ol>
 * <li>Gephi toolkitでデータファイル読み込み
 * <li>Layout、Partition、Rankingの設定
 * <li>Previewの設定
 * <li>時系列に沿ってSVG出力
 * <li>SVG→JPEG変換 (Apache Batik使用)
 * <li>JPEG→動画変換 (FFmpeg使用)
 * </ol>
 * </p>
 * <p>
 * 上記手順の内、本クラスで4～6までの処理をカプセル化しています。<br/>
 * 1～3までの処理はGephi toolkitでの処理となります。<br/>
 * </p>
 * @author abe
 */
public class Movie {
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
    
    /** 拡大率固定でノード全体が表示できる大きさに調整。*/
    public static final int VIEW_PORT_MODE_NORMAL = 0;
    /** 拡大率をノード全体が表示できるように調整。*/
    public static final int VIEW_PORT_MODE_AUTO = 1;
    /** 表示矩形を手動で指定します。*/
    public static final int VIEW_PORT_MODE_USER = 2;

    /** 処理対象ワークスペース。*/
    private Workspace workspace = null;
    /** レイアウト。*/
    private Layout layout = null;
    /** グラフ。*/
    private Graph graph = null;

    /** コールバック。*/
    private MovieCallback callback = null;

    /** SVG出力スレッド。*/
    private Thread svgOutputThread = null;

    /**
     * レイアウト取得。<br/>
     * @return レイアウト
     */
    public Layout getLayout() {
        return layout;
    }

    /**
     * レイアウト設定。<br/>
     * 設定されたレイアウトはoutputJPEG実行時に適用されます。<br/>
     * outputJPEG実行時にnullの場合はFruchterman Reingoldレイアウタがデフォルトで使用されます。<br/>
     * @param layout レイアウト
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
        if (this.layout == null)
            this.layout = createDefaultLayout();
    }

    /**
     * コンストラクタ。<br/>
     * @param workspace 動画作成の元となるGephiのワークスペースを指定してください。nullの場合はデフォルトワークスペースを使用します。
     */
    public Movie(Workspace workspace) {
        this.workspace = workspace;
        if (workspace == null) {
            // デフォルトワークスペースを取得します。
            ProjectController project = Lookup.getDefault().lookup(ProjectController.class);
            project.newProject();
            this.workspace = project.getCurrentWorkspace();
        }
        // 設定レイアウト取得。
        LayoutController lc = Lookup.getDefault().lookup(LayoutController.class);
        if (lc != null) {
            layout = lc.getModel().getSelectedLayout();
        }
        if (layout == null) {
            layout = createDefaultLayout();
        }
    }

    /**
     * 再生時間をミリ秒で取得します。<br/>
     * @return 再生時間(ミリ秒)。Dynamic以外で再生時間がない場合は0.0を返します。それ以外の場合はnullを返します。
     */
    public Double getTime() {
        if (this.workspace == null)
            return null;
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);
        DynamicModel dm = dc.getModel();
        if (!dm.isDynamicGraph())
            return 0.0;
        return Double.valueOf(dm.getMax() - dm.getMin());
    }

    /**
     * SVG最大サイズ取得。<br/>
     * @param algoCount レイアウト計算回数
     * @return SVG抽出矩形。取得できない場合はnullを返します。
     */
    public Rectangle2D.Double getMaxRect(int algoCount) {
        if (this.workspace == null)
            return null;
        PreviewController preview = Lookup.getDefault().lookup(PreviewController.class);
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);
        DynamicModel dm = dc.getModel();
        if (dm.isDynamicGraph()) {
            FilterBuilder[] builders = Lookup.getDefault().lookup(DynamicRangeBuilder.class).getBuilders();
            DynamicRangeFilter dynamicRangeFilter = (DynamicRangeFilter) builders[0].getFilter();
            double dMin = dm.getMin();
            double dMax = dm.getMax();
            dynamicRangeFilter.setRange(new Range(dMin, dMax));
            layout.initAlgo();		// レイアウト初期化
            int a = 0;
            System.out.print("pre layout algo ");
            for(int i=0; i<algoCount && layout.canAlgo(); i++) {
                layout.goAlgo();
                if (++a % 10 == 0)
                    System.out.print(".");
            }
            System.out.println(" end");
        }
        preview.refreshPreview();
        PreviewModel model = preview.getModel();
        Point p = model.getTopLeftPosition();
        Dimension d = model.getDimensions();
        Rectangle2D.Double rect = new Rectangle2D.Double(p.x, p.y, d.width, d.height);
        return rect;
    }

    /**
     * 現在のSVGビューポート矩形取得。
     * @return 矩形(OpenGL座標系)
     */
    public Rectangle2D.Double getRect() {
        Rectangle2D.Double rect;
        try {
            PreviewController preview = Lookup.getDefault().lookup(PreviewController.class);
            // SVGのビューポート取得
            preview.refreshPreview();
            PreviewModel model = preview.getModel();
            Point p = model.getTopLeftPosition();
            Dimension d = model.getDimensions();
            rect = new Rectangle2D.Double(p.x, p.y, d.width, d.height);
        } catch (Exception e) {
            // Gexphi側で以下のような原因不明エラーが発生 (11時間ほど駆動した後) する場合があるのでここでムリヤリエラーを回避
            //
            //  java.lang.NullPointerException
            //  at org.gephi.preview.plugin.renderers.EdgeRenderer.preProcess(EdgeRenderer.java:156)
            //  at org.gephi.preview.PreviewControllerImpl.refreshPreview(PreviewControllerImpl.java:197)
            //  at org.gephi.preview.PreviewControllerImpl.refreshPreview(PreviewControllerImpl.java:124)
            //  at com.sharecrest.GephiMovie.getRect(GephiMovie.java:223)
            //  at com.sharecrest.GephiMovie.outputImage(GephiMovie.java:374)
            //  at ExampleTwitter.main(ExampleTwitter.java:112)
            //
            rect = new Rectangle2D.Double(0, 0, 100, 100);
        }
        return rect;
    }

    /**
     * SVGファイル出力。<br/>
     * Gephiのタイムラインデータより動画ファイル作成に必要なSVGファイルを連番で作成します。<br/>
     * 作成されるSVGファイルの枚数はパラメータにより可変ですが、以下の計算式で求められます。<br/>
     * <p>ファイル数 = 再生時間(ミリ秒) ÷ (1000 ÷ FPS) ÷ 再生速度</p>
     * @param path SVGファイル出力先ディレクトリを指定してください。
     * @param fps 1秒間に使用するSVGファイルの枚数を指定します。一般的に25FPSであれば人間の目から見て滑らかな動画となります。
     * @param speed 再生速度を指定します。1.0を指定した場合はGEXFの日時通りに再生します。倍速にする場合は2.0と指定してください。
     * @param algoInitCount レイアウト計算回数(初期算出)。
     * @param algoFPSCount レイアウト計算回数(FPS毎)。
     * @param start 再生開始時間をミリ秒で指定してください。0開始です。
     * @param end 再生終了時間をミリ秒で指定してください。getTime()で取得できる値が最大値となります。0を指定した場合はgetTime()と同値となります。
     * @param afterTime 再生終了後の追加再生時間をミリ秒で指定してください。
     */
    public void outputSVG(String path, float fps, double speed, int algoInitCount, int algoFPSCount,
                                double start, double end, double afterTime) {
        if (this.workspace == null)
                return;
        try {
            fwCSVFile = new FileWriter(path + "/" + fileName + ".csv");
        } catch (IOException e) {
            fwCSVFile = null;
        }
        boolean cancel = false;
        int nImageCnt = (int)(Math.ceil(afterTime / (1000.0 / fps)));	// 画像出力枚数
        String strImageCnt = String.format("%%0%dd", Integer.toString(nImageCnt).length());		// ファイル名連番用フォーマット
        int nIdx = 0;                   // ファイル連番
        double dMin = 0.0;              // 再生開始時間
        double dMax = 0.0;              // 再生終了時間

        ImageIO.setUseCache(false);

        PreviewController preview = Lookup.getDefault().lookup(PreviewController.class);
        
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);
        DynamicModel dm = dc.getModel();
        if (dm.isDynamicGraph()) {
            FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
            FilterBuilder[] builders = Lookup.getDefault().lookup(DynamicRangeBuilder.class).getBuilders();
            DynamicRangeFilter dynamicRangeFilter = (DynamicRangeFilter) builders[0].getFilter();
            Query dynamicQuery = filterController.createQuery(dynamicRangeFilter);
            if (callback != null) {
                callback.addDynamicFilter(filterController, dynamicQuery);
            }
            GraphView view = filterController.filter(dynamicQuery);
            graph = graphModel.getGraph(view);
            graphModel.setVisibleView(view);

            // 再生時間設定
            dMin = dm.getMin();		// 再生開始時間
            dMax = dm.getMax();		// 再生終了時間
            if ((end > 0.0) && ((dMin + end) < dMax))
                dMax = dMin + end;
            if (start > 0.0)
                dMin = (dMin + start) < dMax ? (dMin + start) : dMax;
            double dPlayTime = dMax - dMin;	// 再生時間
            nImageCnt = (int)(Math.ceil(dPlayTime / (1000.0 / fps) / speed * algoFPSCount) + (int)(afterTime / (1000.0 / fps)));	// 画像出力枚数
            strImageCnt = String.format("%%0%dd", Integer.toString(nImageCnt).length());		// ファイル名連番用フォーマット

            // レイアウト設定
            if (layout == null)
                layout = createDefaultLayout();
            dynamicRangeFilter.setRange(new Range(dMin, dMin));
            layout.initAlgo();		// レイアウト初期化
            int a = 0;
            System.out.print("pre layout algo ");
            for(int i=0; i<algoInitCount && layout.canAlgo(); i++) {
                layout.goAlgo();
                if (++a % 10 == 0)
                    System.out.print(".");
            }
            layout.endAlgo();
            System.out.println(" end");
            preview.refreshPreview();

            layout.initAlgo();		// レイアウト初期化

            // SVGファイル作成
            double dAdd = 1000.0 / fps * speed;	// 再生増分値(ミリ秒)
            int algoCnt = 1;
            for (double d=dMin; d<dMax; d+=dAdd) {
                if ((d+dAdd) > dMax) {
                    d = dMax;
                }

                // タイムライン進行
                dynamicRangeFilter.setRange(new Range(d, d));

                for(int afc=0; afc<algoFPSCount; afc++) {
                    // 画像ファイル名生成
                    String strIdx = String.format(strImageCnt, ++nIdx);
                    StringBuilder sbSvgPath = new StringBuilder(path);
                    sbSvgPath.append("/").append(strIdx).append(".svg");

                    System.out.println(String.format("現在 : %d / 総枚数 : %d", nIdx, nImageCnt));

                    preview.refreshPreview();
                
                    if (callback != null) {
                        if ((cancel = !frameCallback(graph, layout, d, dMin, dMax, strIdx, false))) {
                            // cancel
                            break;
                        }
                    }

                    // レイアウト進行
                    for(int i=0; i<algoCnt && layout.canAlgo(); i++)
                        layout.goAlgo();

                    // SVG出力
                    outputMemorySVG(sbSvgPath.toString());
                }
            }
        } else {
            graph = graphModel.getGraph();
            layout.initAlgo();		// レイアウト初期化
        }

        // 再生終了後の追加再生 (レイアウトの進行のみ行います。Staticデータにも有効です)
        if (!cancel && afterTime > 0.0) {
            // SVGファイル作成
            double dAdd = 1000.0 / fps;	// 再生増分値(ミリ秒)
            int algoCnt = 1;
            for (double d=0.0; d<afterTime; d+=dAdd) {
                // SVGファイル名生成
                String strIdx = String.format(strImageCnt, ++nIdx);
                StringBuilder sbSvgPath = new StringBuilder(path);
                sbSvgPath.append("/").append(strIdx).append(".svg");

                System.out.println(String.format("現在 : %d / 総枚数 : %d", nIdx, nImageCnt));

                preview.refreshPreview();

                if (callback != null) {
                    if (!frameCallback(graph, layout, d, dMin, dMax, strIdx, true)) {
                        // cancel
                        break;
                    }
                }

                // レイアウト進行0
                for(int i=0; i<algoCnt && layout.canAlgo(); i++)
                    layout.goAlgo();

                // SVG出力
                outputMemorySVG(sbSvgPath.toString());
            }
        }
        if (svgOutputThread != null) {
            // スレッド終了待ち
            try {
                svgOutputThread.join(10000);
            } catch (InterruptedException e) {}
        }
        if (imageReader != null) {
            imageReader.dispose();
            imageReader = null;
        }
        if (imageWriter != null) {
            imageWriter.dispose();
            imageWriter = null;
        }
        if (fwCSVFile != null) {
            try {
                fwCSVFile.flush();
                fwCSVFile.close();
                fwCSVFile = null;
            } catch (IOException e) {}
        }
    }

    private ImageReader imageReader = null;
    private ImageWriter imageWriter = null;
    
    /**
     * 予備情報CSVより画像ファイル出力。<br/>
     * @param path JPEGファイル出力先ディレクトリを指定してください。
     * @param fps 1秒間に使用するJPEGファイルの枚数を指定します。一般的に25FPSであれば人間の目から見て滑らかな動画となります。
     * @param viewPortMode SVGから抽出する部分画像の矩形を指定します。VIEW_PORT_MODE_NORMAL、VIEW_PORT_MODE_AUTO、VIEW_PORT_MODE_USERより選択してください。
     * @param scalingSpeed viewPortModeをVIEW_PORT_MODE_AUTOにした場合、矩形のサイズ変化のアニメーション速度をミリ秒で指定してください。(3000～5000が適当です)
     * @param rect viewPortModeをVIEW_PORT_MODE_USERにした場合の抽出矩形を指定してください。
     * @param start 再生開始時間をミリ秒で指定してください。0開始です。
     * @param end 再生終了時間をミリ秒で指定してください。getTime()で取得できる値が最大値となります。0を指定した場合はgetTime()と同値となります。
     * @param width 出力するJPEGファイルの幅を指定します。
     * @param height 出力するJPEGファイルの高さを指定します。
     * @param quality 出力するJPEGファイルの品質を指定します。(1.0fが最高品質、0.1fが最低品質となります)
     */
    public String outputImage(String path, float fps, int viewPortMode, double scalingSpeed, Rectangle2D.Double rect,
                                double start, double end, int width, int height, float quality) {
        if (lstCSVInfo == null) {
            readCSVFile(path);
        }
        System.out.println("画像出力中");

        int nImageWidth = width;
        int nImageHeight = height;

        Rectangle2D.Double rectPlay;            // 再生用抽出矩形
        Rectangle2D.Double rectReal;            // 現タイムラインの実抽出矩形
        Rectangle2D.Double rectTarget = null;   // 矩形アニメーション用の到達ターゲット矩形
        double dRectAdd = 1000.0 / fps;
        double dRectAnime = 0.0;        // 矩形アニメーション経過時間 (VIEW_PORT_MODE_AUTO時有効)
        
        switch (viewPortMode) {
        case VIEW_PORT_MODE_NORMAL:
        default:
            rectPlay = rectMax;
            break;
        case VIEW_PORT_MODE_AUTO:
            rectPlay = rectMax;
            break;
        case VIEW_PORT_MODE_USER:
            rectPlay = rect == null ? rectMax : rect;
            break;
        }

        int nIdx = 0;
        int nCnt = lstCSVInfo.size();
        Calendar cal;
        int nImageCnt = 0;
        for(MovieCSVInfo csvInfo : lstCSVInfo) {
            cal = Calendar.getInstance();
            System.out.print(String.format("%d / %d : %s ～", nIdx+1, nCnt, sdf.format(cal.getTime())));
            if (end == 0)
                end = csvInfo.dEnd - csvInfo.dStart;
            double cur = csvInfo.dCur - csvInfo.dStart;
            if ((cur < start) || (cur > end))
                continue;

            nImageCnt++;
            
            // 抽出矩形調整
            if (viewPortMode == VIEW_PORT_MODE_AUTO) {
                rectReal = csvInfo.viewBox;
                if (rectTarget == null) rectTarget = rectReal;
                if (rectPlay == null) rectPlay = rectReal;
                if (!rectTarget.contains(rectReal)) {
                    rectTarget = (Rectangle2D.Double)rectReal.clone();
                    dRectAnime = dRectAdd;
                }
                rectPlay = animationRect(rectPlay, rectTarget, scalingSpeed, dRectAnime);	// SVGに合わせて拡大縮小
                dRectAnime += dRectAdd;
            }

            File file = new File(csvInfo.svgFilePath);
            String imgPath = file.getPath().replaceAll("\\.svg", "") + ".jpg";

            try {
                Rectangle2D.Double rectOutput = (Rectangle2D.Double)rectPlay.clone();     // 出力画像サイズの比率に合わせて拡張
                if (nImageWidth > nImageHeight) {
                    double d = (double)nImageWidth / (double)nImageHeight * rectOutput.width;
                    rectOutput.x -= d / 2.0;
                    rectOutput.width += d;
                } else {
                    double d = (double)nImageHeight / (double)nImageWidth * rectOutput.height;
                    rectOutput.y -= d / 2.0;
                    rectOutput.height += d;
                }
                Document doc = getSVGDocument(csvInfo.svgFilePath);
//              svgModify((SVGOMDocument)doc, csvInfo);
                TranscoderInput input = new TranscoderInput(doc);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                TranscoderOutput output = new TranscoderOutput(baos);
                JPEGTranscoder t = new JPEGTranscoder();
                t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(nImageWidth));
                t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(nImageHeight));
                t.addTranscodingHint(JPEGTranscoder.KEY_AOI, rectOutput);
                t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(quality));
                t.transcode(input, output);

                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                if (imageReader == null) {
                    imageReader = ImageIO.getImageReadersByFormatName("jpeg").next();
                }
                imageReader.setInput(ImageIO.createImageInputStream(bais));
                BufferedImage bi = imageReader.read(0);
                Graphics graphics = bi.createGraphics();

                if (callback != null) {
                    if (!callback.frameImage(graphics, csvInfo)) {
                        // cancel
                        break;
                    }
                }

                if (imageWriter == null) {
                    imageWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
                }
                imageWriter.setOutput(ImageIO.createImageOutputStream(new File(imgPath)));
                bi.flush();
                imageWriter.write(bi);

                imageReader.reset();
                imageWriter.reset();
                baos.close();
                bais.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (TranscoderException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            cal = Calendar.getInstance();
            System.out.println(sdf.format(cal.getTime()));

            nIdx++;
        }
        if (imageReader != null) {
            imageReader.dispose();
            imageReader = null;
        }
        if (imageWriter != null) {
            imageWriter.dispose();
            imageWriter = null;
        }
        String strImageCnt = String.format("%%0%dd", Integer.toString(nImageCnt).length());		// ファイル名連番用フォーマット
        final String imageFileExt = ".jpg";		// 画像ファイル拡張子
        return path + "/" + strImageCnt + imageFileExt;
    }
    
    /**
     * 予備情報CSVよりプレイヤー用バイナリファイル出力。<br/>
     * @param path JPEGファイル出力先ディレクトリを指定してください。
     */
    public void outputBinary(String path) {
        if (lstCSVInfo == null) {
            readCSVFile(path);
        }
        try {
            String strFile = path + "/" + fileName + ".rs3";
            FileOutputStream fos = new FileOutputStream(strFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            GephiBinary gb = null;
            int nIdx = 0;
            for(MovieCSVInfo csvInfo : lstCSVInfo) {
                if (gb == null) {
                    gb = new GephiBinary();
                    gb.start = csvInfo.start;
                    gb.end = csvInfo.end;
                    gb.nodeMaxCnt = 0;
                    gb.edgeMaxCnt = 0;
                    gb.arrowMaxCnt = 0;
                    gb.frameCnt = lstCSVInfo.size();
                    gb.files = new String[gb.frameCnt];
                }
                File f = new File(csvInfo.svgFilePath);
                String strSvgFile = f.getName().replaceAll("svg", "rs3c");
                String strSvgPath = f.getParent() + "/" + strSvgFile;
                gb.files[nIdx++] = strSvgFile;

                GephiBinaryFrame gbf = new GephiBinaryFrame(0, 0, 0);
                gbf.cur = csvInfo.cur;
                createBinaryFrame(csvInfo.svgFilePath, gbf);
                gb.nodeMaxCnt = gb.nodeMaxCnt > gbf.nodes.length ? gb.nodeMaxCnt : gbf.nodes.length;
                gb.edgeMaxCnt = gb.edgeMaxCnt > gbf.edges.length ? gb.edgeMaxCnt : gbf.edges.length;
                gb.arrowMaxCnt = gb.arrowMaxCnt > gbf.arrows.length ? gb.arrowMaxCnt : gbf.arrows.length;
                if (callback != null) {
                    if (!callback.frameBinary(gb, gbf, csvInfo)) {
                        // cancell
                        break;
                    }
                }
                FileOutputStream fos2 = new FileOutputStream(strSvgPath);
                ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
                oos2.writeObject(gbf);
                oos2.flush();
                oos2.close();
            }        
            oos.writeObject(gb);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final double rad = Math.PI / 180.0;
    private final float rn = 10.0f;
    // 時計での12時がJava3Dの座標系では0.0度になります、ポリゴンは反時計回りに頂点を指定します
    private final float[] xn = {(float)(rn * Math.sin(0.0 * rad)), (float)(rn * Math.sin(240.0 * rad)), (float)(rn * Math.sin(120.0 * rad))};
    private final float[] yn = {(float)(rn * Math.cos(0.0 * rad)), (float)(rn * Math.cos(240.0 * rad)), (float)(rn * Math.cos(120.0 * rad))};

    private class NodeData {
        String strId;
        String strLabel;
        Point3f pos;

        public NodeData(String strId) {
            this.strId = strId;
            this.strLabel = "";
            this.pos = new Point3f();
        }
    }

    private class EdgeData {
        Point3f m;
        Point3f l;
    }

    private class ArrowData {
        Point3f p1;
        Point3f p2;
        Point3f p3;
    }
    
    /**
     * SVGファイルからプレイヤー用の".rs3c"バイナリファイルを作成。<br/>
     * @param svgFilePath
     * @param gbf 
     */
    public void createBinaryFrame(String svgFilePath, GephiBinaryFrame gbf) {
        try {
            Document doc = getSVGDocument(svgFilePath);
            SVGOMSVGElement svgRoot = (SVGOMSVGElement)doc.getDocumentElement();
            Element ele = null;

            Date cur;
            cur = sdf2.parse(sdf.format(gbf.cur));

            Map<String, NodeData> mapNodes = new HashMap<String, NodeData>();

            // <g id="node-labels">
            ele = svgRoot.getElementById("node-labels");
            if (ele != null) {
                NodeList nlLabels = ele.getChildNodes();
                int nCntLabels = nlLabels.getLength();
                for(int j=0; j<nCntLabels; j++) {
                    Node nodeLabel = nlLabels.item(j);
                    if (nodeLabel.getClass() == SVGOMTextElement.class) {
                        SVGOMTextElement eleLabel = (SVGOMTextElement)nodeLabel;
                        String strClass = eleLabel.getAttribute("class");
                        String label = eleLabel.getTextContent().replaceAll("[\n ]", "");
                        NodeData nd = mapNodes.get(strClass);
                        if (nd == null)
                            nd = new NodeData(strClass);
                        nd.strLabel = label;
                        mapNodes.put(strClass, nd);
                    }
                }
            }

            // <g id="nodes">
            ele = svgRoot.getElementById("nodes");
            if (ele != null) {
                NodeList nlNodes = ele.getChildNodes();
                int nCntNodes = nlNodes.getLength();
                for(int j=0; j<nCntNodes; j++) {
                    Node nodeNode = nlNodes.item(j);
                    if (nodeNode.getClass() == SVGOMCircleElement.class) {
                        SVGOMCircleElement eleNode = (SVGOMCircleElement)nodeNode;
                        String strClass = eleNode.getAttribute("class");

                        NodeData nd = mapNodes.get(strClass);
                        if (nd == null)
                            nd = new NodeData(strClass);
                        nd.pos.x = Float.parseFloat(eleNode.getAttribute("cx"));
                        nd.pos.y = Float.parseFloat(eleNode.getAttribute("cy"));
                        nd.pos.z = 0.0f;
                        mapNodes.put(strClass, nd);
                    }
                }
            }

            NodeData[] nds = mapNodes.values().toArray(new NodeData[0]);
            gbf.nodeCnt = nds.length;
            gbf.strIds = new String[gbf.nodeCnt];
            gbf.strLabels = new String[gbf.nodeCnt];
            gbf.nodes = new Point3f[gbf.nodeCnt * 3];
            gbf.nodesColor = new Color4f[gbf.nodeCnt * 3];
            float[] x = new float[3];
            float[] y = new float[3];
            float z;
            Color4f nodeCol = new Color4f(1.0f, 1.0f, 0.0f, 0.8f);
            for(int i=0; i<gbf.nodeCnt; i++) {
                NodeData nd = nds[i];
                gbf.strIds[i] = nd.strId;
                gbf.strLabels[i] = nd.strLabel;
                x[0] = xn[0]; x[1] = xn[1]; x[2] = xn[2];
                y[0] = yn[0]; y[1] = yn[1]; y[2] = yn[2];
                z = 0.5f;
                gbf.nodes[(i * 3) + 0] = new Point3f();
                gbf.nodes[(i * 3) + 0].x = nd.pos.x + x[0]; gbf.nodes[(i * 3) + 0].y = nd.pos.y + y[0]; gbf.nodes[(i * 3) + 0].z = z;
                gbf.nodes[(i * 3) + 1] = new Point3f();
                gbf.nodes[(i * 3) + 1].x = nd.pos.x + x[1]; gbf.nodes[(i * 3) + 1].y = nd.pos.y + y[1]; gbf.nodes[(i * 3) + 1].z = z;
                gbf.nodes[(i * 3) + 2] = new Point3f();
                gbf.nodes[(i * 3) + 2].x = nd.pos.x + x[2]; gbf.nodes[(i * 3) + 2].y = nd.pos.y + y[2]; gbf.nodes[(i * 3) + 2].z = z;
                gbf.nodesColor[(i * 3) + 0] = nodeCol;
                gbf.nodesColor[(i * 3) + 1] = nodeCol;
                gbf.nodesColor[(i * 3) + 2] = nodeCol;
            }

            // <g id="arrows">
            List<ArrowData> lstArrows = new ArrayList<ArrowData>();
            ele = svgRoot.getElementById("arrows");
            if (ele != null) {
                NodeList nlArrows = ele.getChildNodes();
                int nCntArrows = nlArrows.getLength();
                for(int j=0; j<nCntArrows; j++) {
                    Node nodeArrow = nlArrows.item(j);
                    if (nodeArrow.getClass() == SVGOMPolylineElement.class) {
                        SVGOMPolylineElement eleArrow = (SVGOMPolylineElement)nodeArrow;
                        String[] strPoss= eleArrow.getAttribute("points").split(" ");
                        String[] str1 = strPoss[0].split(",");
                        String[] str2 = strPoss[1].split(",");
                        String[] str3 = strPoss[2].split(",");
                        ArrowData ad = new ArrowData();
                        ad.p1 = new Point3f();
                        ad.p1.x = Float.parseFloat(str1[0]); ad.p1.y = Float.parseFloat(str1[1]); ad.p1.z = 1.0f;
                        ad.p2 = new Point3f();
                        ad.p2.x = Float.parseFloat(str2[0]); ad.p2.y = Float.parseFloat(str2[1]); ad.p2.z = 1.0f;
                        ad.p3 = new Point3f();
                        ad.p3.x = Float.parseFloat(str3[0]); ad.p3.y = Float.parseFloat(str3[1]); ad.p3.z = 1.0f;
                        lstArrows.add(ad);
                    }
                }
                int nArrowCnt = lstArrows.size();
                gbf.arrows = new Point3f[nArrowCnt * 3];
                gbf.arrowsColor = new Color4f[nArrowCnt * 3];
                Color4f arrowCol = new Color4f(0.8f, 0.8f, 0.8f, 1.0f);
                for(int i=0; i<nArrowCnt; i++) {
                    ArrowData ad = lstArrows.get(i);
                    gbf.arrows[(i*3)+0] = new Point3f();
                    gbf.arrows[(i*3)+0] = ad.p1;
                    gbf.arrows[(i*3)+1] = new Point3f();
                    gbf.arrows[(i*3)+1] = ad.p2;
                    gbf.arrows[(i*3)+2] = new Point3f();
                    gbf.arrows[(i*3)+2] = ad.p3;
                    gbf.arrowsColor[(i*3)+0] = arrowCol;
                    gbf.arrowsColor[(i*3)+1] = arrowCol;
                    gbf.arrowsColor[(i*3)+2] = arrowCol;
                }
            }

            // <g id="edges">
            List<EdgeData> lstEdges = new ArrayList<EdgeData>();
            ele = svgRoot.getElementById("edges");
            if (ele != null) {
                NodeList nlEdges = ele.getChildNodes();
                int nCntEdges = nlEdges.getLength();
                for(int j=0; j<nCntEdges; j++) {
                    Node nodeEdge = nlEdges.item(j);
                    if (nodeEdge.getClass() == SVGOMPathElement.class) {
                        SVGOMPathElement eleEdge = (SVGOMPathElement)nodeEdge;
                        String[] strPoss= eleEdge.getAttribute("d").split(" ");
                        String[] m = strPoss[1].split(",");
                        String[] l = strPoss[3].split(",");
                        EdgeData ed = new EdgeData();
                        ed.m = new Point3f();
                        ed.m.x = Float.parseFloat(m[0]);
                        ed.m.y = Float.parseFloat(m[1]);
                        ed.m.z = 0.0f;
                        ed.l = new Point3f();
                        ed.l.x = Float.parseFloat(l[0]);
                        ed.l.y = Float.parseFloat(l[1]);
                        ed.l.z = 0.0f;
                        lstEdges.add(ed);
                    }
                }
                gbf.edgeCnt = lstEdges.size();
                gbf.edges = new Point3f[gbf.edgeCnt * 2];
                gbf.edgesColor = new Color4f[gbf.edgeCnt * 2];
                Color4f edgeCol = new Color4f(0.8f, 0.8f, 0.8f, 1.0f);
                for(int i=0; i<gbf.edgeCnt; i++) {
                    EdgeData ed = lstEdges.get(i);
                    gbf.edges[(i*2)+0] = new Point3f();
                    gbf.edges[(i*2)+0] = ed.m;
                    gbf.edges[(i*2)+1] = new Point3f();
                    gbf.edges[(i*2)+1] = ed.l;
                    gbf.edgesColor[(i*2)+0] = edgeCol;
                    gbf.edgesColor[(i*2)+1] = edgeCol;
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * デフォルトレイアウト作成。<br/>
     * @return
     */
    private Layout createDefaultLayout() {
        if (this.workspace == null)
            return null;
        // レイアウト
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
        FruchtermanReingold layoutDefault = new FruchtermanReingold(new FruchtermanReingoldBuilder());
        layoutDefault.setGraphModel(graphModel);
        layoutDefault.resetPropertiesValues();
        layoutDefault.setArea(10000.0f);
        layoutDefault.setGravity(10.0);
        layoutDefault.setSpeed(1.0);
        return layoutDefault;
    }

    /**
     * 矩形を指定時間で拡縮する。<br/>
     * @param src 元の矩形
     * @param target 変形先の矩形
     * @param targetTime srcをtargetに合わせるための時間をミリ秒で指定。
     * @param time 経過時間をミリ秒で指定。
     * @return 経過時間に合わせた矩形を返す。
     */
    private Rectangle2D.Double animationRect(Rectangle2D.Double src, Rectangle2D.Double target, double targetTime, double time) {
        if (time > targetTime)
            return target;
        Rectangle2D.Double ret = new Rectangle2D.Double();
        double d = time / targetTime;
        ret.x = (int)(src.x + ((target.x - src.x) * d));
        ret.y = (int)(src.y + ((target.y - src.y) * d));
        ret.width = (int)(src.width + ((target.width - src.width) * d));
        ret.height = (int)(src.height + ((target.height - src.height) * d));
        return ret;
    }

    /**
     * SVGメモリ出力。<br/>
     * @param path null以外が指定された場合は、そのパスにSVGファイルを出力します
     * @return SVGイメージ
     */
    private ByteArrayInputStream outputMemorySVG(String path) {
        ByteArrayInputStream ret;
        // 以下の方法だとメモリリーク
        //ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        //SVGExporter svgExporter = (SVGExporter)ec.getExporter("svg");

        SVGExporter svgExporter = (SVGExporter)new ExporterBuilderSVG().buildExporter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos);
        svgExporter.setWorkspace(workspace);
        svgExporter.setWriter(osw);
        svgExporter.execute();
        byte[] arr = baos.toByteArray();
        ret = new ByteArrayInputStream(arr);
        svgExporter.cancel();

        if (path != null) {
/*
            svgOutputThread = new Thread(new SVGOutputThread(path, arr));
            svgOutputThread.start();
*/
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                fos.write(arr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {}
                }
            }
        }
        return ret;
    }

    /**
     * コールバック取得。<br/>
     * @return コールバック
     */
    public MovieCallback getCallback() {
        return callback;
    }

    /**
     * コールバック設定。<br/>
     * @param callback
     */
    public void setCallback(MovieCallback callback) {
        this.callback = callback;
    }
    
    private double preCur;
    private int preCntNodes;
    private int preCntEdges; 
    
    /**
     * フレーム描画。<br/>
     * @param graph グラフ。
     * @param layout レイアウト
     * @param cur 現在タイミング
     * @param start 開始タイミング
     * @param end 終了タイミング
     * @param idx 連番
     * @param after true=追加時間
     * @return 動画処理を続行する場合はtrue、停止させて現状で動画出力を行う場合はfalse
     */
    private boolean frameCallback(Graph graph, Layout layout, double cur, double start, double end, String idx, boolean after) {
        if (callback == null)
            return true;
        int cntNode;
        int cntEdge;
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);
        DynamicModel dm = dc.getModel();
        if (dm.isDynamicGraph()) {
            // 動的データ
            if (!after) {
                    // DYNAMIC
                    DynamicGraph dynamicGraph = dm.createDynamicGraph(graph);
                    Graph subGraph = dynamicGraph.getSnapshotGraph(cur);
                    cntNode = subGraph.getNodeCount();
                    cntEdge = subGraph.getEdgeCount();
                    subGraph.readUnlockAll();
                    subGraph.clear();			// Graphのスナップショットを取る場合は必ずclear()すること。しないとメモリリーク。
                preCur = cur;
                preCntNodes = cntNode;
                preCntEdges = cntEdge;
            } else {
                cur = preCur;
                cntNode = preCntNodes;
                cntEdge = preCntEdges;
            }
        } else {
            // 静的データ
            cur = 0;
            cntNode = graph.getNodeCount();
            cntEdge = graph.getEdgeCount();
        }
        String strCur = DynamicUtilities.getXMLDateStringFromDouble(cur);
        String strStart = DynamicUtilities.getXMLDateStringFromDouble(start);
        String strEnd = DynamicUtilities.getXMLDateStringFromDouble(end);
        // タイムテーブルCSV出力
        try {
            fwCSVFile.write(String.format("%s,%s,%s,%f,%f,%f,%d,%d,%s.svg\n", strCur, strStart, strEnd, cur, start, end, cntNode, cntEdge, idx));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return callback.frame(graph, layout, cur, start, end, idx);
    }

    /**
     * SVG出力用サブスレッド。<br/>
     * ノード・エッジ数が数万の単位になる場合は出力されるSVGが10MB(25万行)以上となるため、ファイル出力をスレッドに逃がします。<br/>
     * @author abe
     */
    private class SVGOutputThread implements Runnable {
        private String path;
        private byte[] data;

        /**
         * コンストラクタ。<br/>
         * @param path SVG出力先パス
         * @param data SVGデータ
         */
        public SVGOutputThread(String path, byte[] data) {
            this.path = path;
            this.data = data;
        }

        @Override
        public void run() {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                fos.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (fos != null) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException e) {}
                }
            }
        }
    }
    
    /** ファイル名。*/
    public static final String fileName = "output";
    /** 予備情報CSV出力用ライタ (サブスレッド内で使用します)。*/
    private FileWriter fwCSVFile = null;
    /** 予備情報。*/
    private List<MovieCSVInfo> lstCSVInfo = null;
    /** SVG矩形最大サイズ。*/
    private Rectangle2D.Double rectMax = null;
    /** SVG矩形最大サイズ。*/
    private Dimension sizeMax = null;
    
    /**
     * 予備情報CSVファイル読み込み。<br/>
     * @param path 
     */
    public List<MovieCSVInfo> readCSVFile(String path) {
        lstCSVInfo = new ArrayList<MovieCSVInfo>();
        try {
            String csvPath = path + "/" + fileName + ".csv";
            System.out.print("SVGマップ読み込み中 ");
            
            rectMax = new Rectangle2D.Double(0, 0, 0, 0);
            sizeMax = new Dimension(0, 0);
                
            // 全SVGを読み込み最大サイズを取得
            File f = new File(csvPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line;
            int nIdx = 0;
            while((line = br.readLine()) != null) {
                if (line.equals(""))
                    continue;
                if (nIdx++ % 100 == 0)
                    System.out.print(".");
                String[] flds = line.split(",", -1);
                if (flds.length == 9) {
                    MovieCSVInfo csvInfo = new MovieCSVInfo();
                    csvInfo.strCur = flds[0];
                    csvInfo.strStart = flds[1];
                    csvInfo.strEnd = flds[2];
                    csvInfo.dCur = Double.parseDouble(flds[3]);
                    csvInfo.dStart = Double.parseDouble(flds[4]);
                    csvInfo.dEnd = Double.parseDouble(flds[5]);
                    csvInfo.cur = DynamicUtilities.getDateFromDouble(csvInfo.dCur);
                    csvInfo.start = DynamicUtilities.getDateFromDouble(csvInfo.dStart);
                    csvInfo.end = DynamicUtilities.getDateFromDouble(csvInfo.dEnd);
                    csvInfo.nodeCnt = Integer.parseInt(flds[6]);
                    csvInfo.edgeCnt = Integer.parseInt(flds[7]);
                    csvInfo.svgFilePath = f.getParent() + "/" + flds[8];
                    // SVGデータ
                    if (getSVGData(csvInfo)) {
                        lstCSVInfo.add(csvInfo);
                    }
                }
            }
            System.out.println(" end " + Integer.toString(lstCSVInfo.size()));
            br.close();
        } catch (IOException e) {
            lstCSVInfo = null;
        }
        return lstCSVInfo;
    }
    
    /**
     * フレーム情報の取得。<br/>
     * @return 
     */
    public List<MovieCSVInfo> getCSVInfo() {
        return lstCSVInfo;
    }

    /**
     * フレーム情報上のSVGファイルよりサイズ情報のみ取得。<br/>
     * @param csvInfo
     * @return 
     */
    public boolean getSVGData(MovieCSVInfo csvInfo) {
        boolean ret = false;
        try {
            // SVGファイルの最初の8行 <svg>タグまで読み込み。全体を読み込むと時間が掛かるため。
            File f = new File(csvInfo.svgFilePath);
            BufferedReader br = new BufferedReader(new FileReader(f));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos);
            String line;
            boolean bSvgTagS = false;
            int n;
            while((line = br.readLine()) != null) {
                osw.write(line);
                osw.write("\n");
                if (bSvgTagS) {
                    if ((n = line.indexOf('>')) >= 0) {
                        if (!line.substring(n-1, n).equals("/"))
                            osw.write("</svg>");
                        break;
                    }
                } else {
                    n = line.indexOf('<');
                    if (n >= 0) {
                        n++;
                        if(line.substring(n, n+3).equals("svg")) {
                            if ((n = line.indexOf('>')) >= 0) {
                                if (!line.substring(n-1, n).equals("/"))
                                    osw.write("</svg>");
                                break;
                            }
                            bSvgTagS = true;
                        }
                    }
                }
            }
            osw.flush();
            br.close();

            // SVG読み込み
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory docFactory = new SAXSVGDocumentFactory(parser);
            byte[] xml = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(xml);
            org.w3c.dom.Document doc = docFactory.createDocument(null, bais);

            // XML読み込み
            Element svgRoot = doc.getDocumentElement();
            Attr attrWidth = svgRoot.getAttributeNodeNS(null, "width");
            Attr attrHeight = svgRoot.getAttributeNodeNS(null, "height");
            csvInfo.width = Integer.parseInt(attrWidth.getValue().replaceAll("px", ""));
            csvInfo.height = Integer.parseInt(attrHeight.getValue().replaceAll("px", ""));
            if (csvInfo.width > sizeMax.width) {
                sizeMax.width = csvInfo.width;
            }
            if (csvInfo.height > sizeMax.height) {
                sizeMax.height = csvInfo.height;
            }

            Attr attrViewBox = svgRoot.getAttributeNodeNS(null, "viewBox");
            String[] vb = attrViewBox.getValue().split(" ");
            csvInfo.viewBox = new Rectangle2D.Double();
            csvInfo.viewBox.x = Double.parseDouble(vb[0]);
            csvInfo.viewBox.y = Double.parseDouble(vb[1]);
            csvInfo.viewBox.width = Double.parseDouble(vb[2]);
            csvInfo.viewBox.height = Double.parseDouble(vb[3]);
            if (csvInfo.viewBox.x < rectMax.x) {
                rectMax.x = csvInfo.viewBox.x;
            }
            if (csvInfo.viewBox.y < rectMax.y) {
                rectMax.y = csvInfo.viewBox.y;
            }
            if (csvInfo.viewBox.width > rectMax.width) {
                rectMax.width = csvInfo.viewBox.width;
            }
            if (csvInfo.viewBox.height > rectMax.height) {
                rectMax.height = csvInfo.viewBox.height;
            }
            ret = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    /**
     * SVGファイルよりDocument取得。<br/>
     * @param svgFilePath
     * @return 
     */
    public org.w3c.dom.Document getSVGDocument(String svgFilePath) {
        org.w3c.dom.Document doc = null;
        try {
            // SVGデータ取得
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            InputStream is = new FileInputStream(svgFilePath);
            doc = f.createDocument(null, is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return doc;
    }
}
