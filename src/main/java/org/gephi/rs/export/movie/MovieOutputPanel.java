/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.gephi.dynamic.DynamicUtilities;
import org.gephi.dynamic.api.DynamicController;
import org.gephi.dynamic.api.DynamicModel;
import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.graph.api.Graph;
import org.gephi.layout.spi.Layout;
import org.gephi.rs.export.movie.player.Player;
import org.gephi.rs.export.movie.player.data.GephiBinary;
import org.gephi.rs.export.movie.player.data.GephiBinaryFrame;
import org.gephi.rs.export.movie.riff.MotionJPEG;
import org.gephi.utils.longtask.api.LongTaskErrorHandler;
import org.gephi.utils.longtask.api.LongTaskExecutor;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * 動画出力用ダイアログ。<br/>
 * @author abe
 */
public class MovieOutputPanel extends javax.swing.JPanel implements ActionListener, MovieCallback, Runnable, LongTask {

    private static final long serialVersionUID = 1L;

    /** 最後に出力されたプレイヤー用バイナリ(.rs3)のパスを保持。*/
    public static String playerFilePath = null;

    private int frameCnt;
    private int progresCnt;
    private int progresIdx;
    
    private boolean cancelled;
    private final LongTaskExecutor executor;
    private final LongTaskErrorHandler errorHandler;

    @Override
    public void setProgressTicket(ProgressTicket pt) {
        progressTicket = pt;
    }

    /**
     * 出力用スレッド。<br/>
     */
    @Override
    public void run() {
        // 処理進行状況表示用プログレスバー設定
        cancelled = false;
//        ProgressTicketProvider progressProvider = Lookup.getDefault().lookup(ProgressTicketProvider.class);
//        progressTicket = progressProvider.createTicket("Movie output", null);
        
        // AVIファイル, GephiPlayer用バイナリ, 連続JPEG, 連続SVG
        boolean avi = false;
        boolean player = false;
        boolean jpeg = false;
        progresCnt = 0;
        progresIdx = 0;
        switch (outputType) {
        case 0: // AVIファイル
            progresCnt = frameCnt;
            avi = true;
            break;
        case 1: // GephiPlayer用バイナリ
            progresCnt = frameCnt;
            player = true;
            break;
        case 2: // 連続JPEG (default)
            progresCnt = frameCnt;
            jpeg = true;
            break;
        case 3: // 連続SVG
            break;
        }
        if (!reuseFlag) {
            progresCnt += frameCnt;
        }
        Progress.setDisplayName(progressTicket, getMessage("MovieOutputPanel.taskDisplay"));
        Progress.start(progressTicket, progresCnt);

        int viewportMode = Movie.VIEW_PORT_MODE_AUTO;
        Rectangle2D.Double rect = null;
        switch(viewMode) {
        case 0:     // 自動調整
            viewportMode = Movie.VIEW_PORT_MODE_AUTO;
            break;
        case 1:     // 最大サイズ
            viewportMode = Movie.VIEW_PORT_MODE_NORMAL;
            break;
        case 2:     // カスタム
            viewportMode = Movie.VIEW_PORT_MODE_USER;
            rect = new Rectangle2D.Double(0, 0, customWidth, customHeight);
            break;
        }
        double start = (startTimeSet * 1000.0) - startTime;
        double end = (endTimeSet * 1000.0) - startTime;
        if (!cancelled && !reuseFlag) {
            // SVG出力
            movie.outputSVG(
                outputFolder,
                (float)fps,
                (double)speed,
                1,
                1,
                start,
                end,
                (double)addPlayTimeSet * 1000.0
            );
        }
        if (!cancelled && (avi || jpeg)) {
            // JPEG出力
            movie.outputImage(
                outputFolder,
                (float)fps,
                viewportMode, 
                adjustSpeed,
                rect,
                start,
                end,
                outputWidth,
                outputHeight,
                1.0f
            );
        }
        if (!cancelled && avi) {
            // 動画出力
            MotionJPEG motionJPEG = new MotionJPEG();
            List<String> lstImage = new ArrayList<String>();
            List<MovieCSVInfo> lstCSVInfo = movie.getCSVInfo();
            for(MovieCSVInfo csvInfo : lstCSVInfo) {
                lstImage.add(csvInfo.svgFilePath.replaceAll("\\.svg", ".jpg"));
            }
            try {
                motionJPEG.createMovie(outputWidth, outputHeight, fps, lstImage.toArray(new String[0]), outputFolder + "/" + Movie.fileName + ".avi");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (!cancelled && player) {
            // プレイヤー用バイナリ出力
            movie.outputBinary(outputFolder);
            playerFilePath = outputFolder + "/" + Movie.fileName + ".rs3";
            new Player(playerFilePath);
        }
        if (!cancelled) {
            StatusDisplayer.getDefault().setStatusText(getMessage("MovieOutputPanel.taskSuccess"));
        }
        Progress.finish(progressTicket);
    }
    
    public boolean cancel() {
        cancelled = true;
        return true;
    }

    public static String getSuffix(String fileName) {
        if (fileName == null)
            return null;
        int point = fileName.lastIndexOf(".");
        if (point != -1) {
            return fileName.substring(point + 1);
        }
        return fileName;
    }

    /**
     * フレーム描画。<br/>
     * @param graph グラフ。
     * @param layout レイアウト
     * @param cur 現在タイミング
     * @param start 開始タイミング
     * @param end 終了タイミング
     * @param idx 連番
     * @return 動画処理を続行する場合はtrue、中断する場合はfalse
     */
    @Override
    public boolean frame(Graph graph, Layout layout, double cur, double start, double end, String idx) {
        progresIdx++;
        Progress.progress(progressTicket, progresIdx);
        if (graph != null) {
            graph.readUnlockAll();
        }
        return !cancelled;
    }

    private Color colString = new Color(255, 255, 255);
    private Color colBack = new Color(32, 32, 32, 200);
    private Font fontString = new Font("MS UI Gothic", Font.PLAIN, 12);
    
    /**
     * 画像出力直前。<br/>
     * @param graphics 画像を編集する場合に使用してください。
     * @param info 画像の予備情報。
     * @return 動画処理を続行する場合はtrue、中断する場合はfalse
     */
    @Override
    public boolean frameImage(Graphics graphics, MovieCSVInfo info) {
        progresIdx++;
        Progress.progress(progressTicket, progresIdx);
        
        List<String> arrStr = new ArrayList<String>();
        if (outputDatetime) {
            arrStr.add(String.format("%s (%s ～ %s)", info.strCur, info.strStart, info.strEnd));
        }
        if (outputNodeCount) {
            // Node数出力
            arrStr.add(String.format("Nodes : %d", info.nodeCnt));
        }
        if (outputEdgeCount) {
            // Edge数出力
            arrStr.add(String.format("Edges : %d", info.edgeCnt));
        }

        // 画像へ描画
        if (graphics != null && arrStr.size() > 0) {
            graphics.setFont(fontString);
            graphics.setColor(colString);
            // 枠描画
            FontMetrics fm = graphics.getFontMetrics();
            int w = 0;
            int h = (fm.getHeight()+5) * arrStr.size() + 5;
            for(String str : arrStr) {
                int ww = fm.stringWidth(str) + 10;
                w = ww > w ? ww : w;
            }       
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics gimg = img.getGraphics();
            gimg.setColor(colBack);
            gimg.fillRect(0, 0, w, h);
            graphics.drawImage(img, outputWidth - w, outputHeight - h, null);

            // 文字描画
            int y = outputHeight - h + fm.getHeight();
            for(String str : arrStr) {
                graphics.drawString(str, (outputWidth - w) + 5, y);
                y += fm.getHeight()+5;
            }
        }
        return !cancelled;
    }

    /**
     * バイナリ出力直前。<br/>
     * @param gb バイナリヘッダ
     * @param gbf フレーム
     * @param info 画像の予備情報。
     * @return 動画処理を続行する場合はtrue、中断する場合はfalse
     */
    @Override
    public boolean frameBinary(GephiBinary gb, GephiBinaryFrame gbf, MovieCSVInfo info) {
        progresIdx++;
        Progress.progress(progressTicket, progresIdx);
        return !cancelled;
    }

    /**
     * Dynamic Rangeフィルタ設定。<br/>
     * Dynamic Rangeフィルタにサブフィルタを追加する場合に使用します。<br/>
     * outputImage()呼び出し時に、GEXFがDynamic形式の場合1回呼び出されます。<br/>
     * @param filterController フィルタコントローラ。
     * @param dynamicQuery Dynamic Rangeフィルタ。
     */
    @Override
    public void addDynamicFilter(FilterController filterController, Query dynamicQuery) {
    }
    
    private SimpleDateFormat sdf = new SimpleDateFormat(getMessage("Movie.DatetimeFormat"));
    private SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");

    private File file = null;
    private Movie movie = null;
    private double startTime = -1.0;    // 再生開始時間 (ミリ秒)  元となるDynamicデータ
    private double endTime = -1.0;      // 再生終了時間 (ミリ秒)  元となるDynamicデータ
    private double playTime = -1.0;     // 再生時間 (ミリ秒)  元となるDynamicデータ
    private boolean dynamic = false;    // GEXFがDYNAMICの場合はtrue
    
    /** 出力先フォルダ。*/
    private String outputFolder;
    /** 既存出力使用。*/
    private boolean reuseFlag;
    /** 出力形式 (0=AVIファイル, 1=GephiPlayer用バイナリ, 2=連続JPEG, 3=連続SVG)。 */
    private int outputType;
    /* 出力幅(pixel)。*/
    private int outputWidth;
    /* 出力高さ(pixel)。*/
    private int outputHeight;
    /** ビューモード (0=自動調整, 1=最大サイズ, 2=カスタム)。*/
    private int viewMode;
    /** ビュー矩形変形時間(ミリ秒)。*/
    private double adjustSpeed;
    /** ビュー矩形幅。*/
    private int customWidth;
    /** ビュー矩形高さ。*/
    private int customHeight;
    /** 再生速度。*/
    private int speed;
    /** FPS。*/
    private int fps;
    /** Node数出力。*/
    private boolean outputNodeCount;
    /** Edge数出力。*/
    private boolean outputEdgeCount;
    /** 日時出力。*/
    private boolean outputDatetime;
    /** 再生開始時間 (秒)。*/
    private int startTimeSet;
    /** 再生終了時間 (秒)。*/
    private int endTimeSet;
    /** 追加再生時間(秒)。*/
    private int addPlayTimeSet;
    
    private DialogDescriptor dlg = null;
    private ProgressTicket progressTicket;

    /**
     * OK、キャンセルボタンハンドラ
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == DialogDescriptor.OK_OPTION) {
            // 動画出力
            if (movie != null) {
                // 出力スレッド開始 (LongTaskの開始)
                String taskmsg = getMessage("MovieOutputPanel.taskMsg");
                executor.execute(this, this, taskmsg, errorHandler);
//                Thread t = new Thread(this);
//                t.start();
            }
        }
    }
    
    /**
     * Creates new form MovieOutputPanel
     */
    public MovieOutputPanel() {
        errorHandler = new LongTaskErrorHandler() {
            public void fatalError(Throwable t) {
                String message = t.getCause().getMessage();
                if (message == null || message.isEmpty()) {
                    message = t.getMessage();
                }
                NotifyDescriptor.Message msg = new NotifyDescriptor.Message(message, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(msg);
                //Logger.getLogger("").log(Level.WARNING, "", t.getCause());
            }
        };
        executor = new LongTaskExecutor(true, "Exporter", 10);
        
        initComponents();
        
        // 各画面値初期化
        outputFolder = "";      // 出力先フォルダ
        reuseFlag = false;      // 既存出力使用
        outputType = 0;         // 出力形式 (0=AVIファイル, 1=GephiPlayer用バイナリ, 2=連続JPEG, 3=連続SVG)
        outputWidth = 1920;     // 出力幅(pixel)
        outputHeight = 1080;    // 出力高さ(pixel)
        viewMode = 0;           // ビューモード
        adjustSpeed = 1000;     // ビュー矩形変形時間(ミリ秒)
        customWidth = 10000;    // ビュー矩形幅
        customHeight = 5625;    // ビュー矩形高さ
        speed = 1;              // 再生速度
        fps = 25;               // FPS
        outputNodeCount = true; // Node数出力
        outputEdgeCount = true; // Edge数出力
        outputDatetime = true;  // 日時出力
        startTimeSet = 0;       // 再生開始時間 (秒)
        endTimeSet = 0;         // 再生終了時間 (秒)
        addPlayTimeSet = 0;     // 追加再生時間(秒)
        // 各コントロールに反映
        outputFolderEdit.setText(outputFolder);                             // 出力先フォルダ
        useExistingCheckbox.setSelected(reuseFlag);                         // 既存出力使用
        outputWidthEdit.setText(Integer.toString(outputWidth));             // 出力幅
        outputHeightEdit.setText(Integer.toString(outputHeight));           // 出力高さ
        rectMovieSpeedEdit.setText(String.format("%d", (int)adjustSpeed));  // ビュー矩形変形時間
        customWidthEdit.setText(Integer.toString(customWidth));             // ビュー矩形幅
        customHeightEdit.setText(Integer.toString(customHeight));           // ビュー矩形高さ
        addPlayTimeSlider.setValue(addPlayTimeSet);
        addPlayTimeEdit.setText(Integer.toString(addPlayTimeSlider.getValue()));
        outputNodeCountCheckbox.setSelected(outputNodeCount);               // Node数出力
        outputEdgeCountCheckbox.setSelected(outputEdgeCount);               // Edge数出力
        outputDatetimeCheckbox.setSelected(outputDatetime);                 // 日時出力
        // 出力形式コンボボックスの内容設定
        String[] outputTypes = {
            getMessage("MovieOutputPanel.labelOutputType.model1"),
            getMessage("MovieOutputPanel.labelOutputType.model2"),
            getMessage("MovieOutputPanel.labelOutputType.model3"),
            getMessage("MovieOutputPanel.labelOutputType.model4")
        };
        outputTypeCombobox.setModel(new javax.swing.DefaultComboBoxModel(outputTypes));
        // ビューモードコンボボックスの内容設定
        String[] viewModes = {
            getMessage("MovieOutputPanel.labelViewMode.model1"),
            getMessage("MovieOutputPanel.labelViewMode.model2"),
            getMessage("MovieOutputPanel.labelViewMode.model3")
        };
        viewModeCombobox.setModel(new javax.swing.DefaultComboBoxModel(viewModes));

        // 各テキストフィールドにエラーチェック用のイベントを追加します。
        JTextField[] textFields = {outputFolderEdit, outputWidthEdit, outputHeightEdit, rectMovieSpeedEdit, customWidthEdit, customHeightEdit};
        for(JTextField textField : textFields) {
            textField.getDocument().addDocumentListener(new MovieOutputDefaultDocumentListener());
        }
        
        // 再生速度のテキスト変更時イベントを追加します。
        playSpeedEdit.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {}
            @Override
            public void insertUpdate(DocumentEvent e) {
                setSpeed(playSpeedEdit.getText());
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                setSpeed(playSpeedEdit.getText());
            }
        });
        
        // FPSのテキスト変更時イベントを追加します。
        fpsEdit.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {}
            @Override
            public void insertUpdate(DocumentEvent e) {
                setFPS(fpsEdit.getText());
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                setFPS(fpsEdit.getText());
            }
        });
        
        // 追加再生時間のテキスト変更時イベントを追加します。
        addPlayTimeEdit.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {}
            @Override
            public void insertUpdate(DocumentEvent e) {
                setAddPlayTime(addPlayTimeEdit.getText());
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                setAddPlayTime(addPlayTimeEdit.getText());
            }
        });
    }
    
    /**
     * ダイアログ表示。<br/>
     */
    public void showDialog() {
        dlg = new DialogDescriptor(this, getMessage("MovieOutputPanel.dialog-title"), false, this);
        double prePlayTime = playTime;
        extractionPlayTime();
        if (file != null && outputFolderEdit.getText().trim().equals("")) {
            outputFolderEdit.setText(file.getParent());
        }
        if (playTime != prePlayTime) {
            // Gephiプロジェクトの再生時間に変更があった場合のみダイアログの初期化を行います
            initDialogData();
        }
        checkOkButtonEnabled();
        calcLastOutputData();
        DialogDisplayer.getDefault().notify(dlg);
    }

    /**
     * ダイアログ設定の初期化。<br/>
     */
    private void initDialogData() {
        int nPlayTimeSec = (int)Math.round(playTime / 1000.0);      // 再生時間(秒)
        int nStartTimeSec = (int)Math.round(startTime / 1000.0);    // 再生開始時間(秒)
        int nEndTimeSec = (int)Math.round(endTime / 1000.0);        // 再生終了時間(秒)
        
        // オリジナル再生時間(開始)
        labelStartPlayTime.setText(sdf.format(DynamicUtilities.getDateFromDouble(startTime)));
        // オリジナル再生時間(終了)
        labelEndPlayTime.setText(sdf.format(DynamicUtilities.getDateFromDouble(endTime)));
        // オリジナル再生時間(秒)
        playTimeLabel.setText(Long.toString(nPlayTimeSec));
        
        //
        // ダイアログ各設定値の初期化 (下記以外の画面設定内容は前回と同じとするため何も変更しない)
        //
        // 再生速度
        playSpeedSlider.setEnabled(dynamic);
        playSpeedEdit.setEnabled(dynamic);
        // 開始時間
        startPlayTimeSlider.setMinimum(nStartTimeSec);
        startPlayTimeSlider.setMaximum(nEndTimeSec);
        startTimeSet = nStartTimeSec;     // 再生開始時間 (秒) 設定値
        startPlayTimeSlider.setValue(startTimeSet);
        startPlayTimeSlider.setEnabled(dynamic);
        startPlayTimeLabel.setText(sdf.format(DynamicUtilities.getDateFromDouble(startPlayTimeSlider.getValue() * 1000.0)));
        // 終了時間
        endPlayTimeSlider.setMinimum(nStartTimeSec);
        endPlayTimeSlider.setMaximum(nEndTimeSec);
        endTimeSet = nEndTimeSec;         // 再生終了時間 (秒) 設定値
        endPlayTimeSlider.setValue(endTimeSet);
        endPlayTimeSlider.setEnabled(dynamic);
        endPlayTimeLabel.setText(sdf.format(DynamicUtilities.getDateFromDouble(endPlayTimeSlider.getValue() * 1000.0)));
        // 日時出力
        outputDatetimeCheckbox.setEnabled(dynamic);
        if (!dynamic) {
            outputDatetime = false;
            outputDatetimeCheckbox.setSelected(outputDatetime);                 // 日時出力
        }
    }
    
    /**
     * 最終出力内容計算。<br/>
     */
    public void calcLastOutputData() {
        int nSpeed = 1;     // 倍率
        try {
            nSpeed = Integer.parseInt(playSpeedEdit.getText());
        } catch (NumberFormatException e) {}
        int nAddPlayTimeSec = 0;    // 追加再生時間
        double dAddPlayTime = 0.0;
        try {
            nAddPlayTimeSec = Integer.parseInt(addPlayTimeEdit.getText());
            dAddPlayTime = nAddPlayTimeSec * 1000.0;
        } catch (NumberFormatException e) {}
        int nFPS = 1;       // FPS
        try {
            nFPS = Integer.parseInt(fpsEdit.getText());
        } catch (NumberFormatException e) {}
        double dStartTime = startPlayTimeSlider.getValue() * 1000.0;
        double dEndTime = endPlayTimeSlider.getValue() * 1000.0;
        double dPlayTime = dEndTime - dStartTime;
        long nPlayTimeSec = Math.round(dPlayTime / 1000.0);
        // 再生時間(開始)
        startViewPlayTimeLabel.setText(sdf.format(DynamicUtilities.getDateFromDouble(dStartTime)));
        // 再生時間(終了)
        endViewPlayTimeLabel.setText(sdf.format(DynamicUtilities.getDateFromDouble(dEndTime)));
        // 再生時間(秒)
        viewPlayTimeLabel.setText(Long.toString((nPlayTimeSec / nSpeed) + nAddPlayTimeSec));
        // フレーム数
        frameCnt = (int)(Math.ceil(dPlayTime / (1000.0 / nFPS) / nSpeed) + (int)(dAddPlayTime / (1000.0 / nFPS)));	// 画像出力枚数
        frameCountLabel.setText(Integer.toString(frameCnt));
    }

    /**
     * 現在のGephiプロジェクトから再生時間を取得。<br/>
     * 但しデータ形式がDynamic意外は0となります。<br/>
     */
    public void extractionPlayTime() {
        DynamicController dc = Lookup.getDefault().lookup(DynamicController.class);
        DynamicModel dm = dc.getModel();
        if (dm.isDynamicGraph()) {
            dynamic = true;
            startTime = dm.getMin();
            endTime = dm.getMax();
            playTime = endTime - startTime;
        } else {
            dynamic = false;
            startTime = 0.0;
            endTime = 0.0;
            playTime = 0.0;
        }
    }

    /**
     * 出力先指定。<br/>
     * @param file 
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * 動画オブジェクト設定。<br/>
     * @param movie 
     */
    public void setMovie(Movie movie) {
        this.movie = movie;
        this.movie.setCallback(this);
    }
    
    /**
     * 動画オブジェクト取得
     * @return 
     */
    public Movie getMovie() {
        return movie;
    }
    
    /**
     * OKボタンの有効/無効設定。<br/>
     */
    public void checkOkButtonEnabled() {
        if (dlg != null) {
            boolean ok = true;
            // 出力先ディレトクリ、空またはデイレクトリではない場合はOK無効
            outputFolder = outputFolderEdit.getText().trim();
            if (outputFolder.equals("")) {
                ok = false;
            } else {
                File file = new File(outputFolder);
                if (!file.isDirectory()) {
                    ok = false;
                }
            }
            // 既存出力使用の無効/有効化と状態取得 (outputFolder内にcsvFileNameのCSVファィルが存在すれば再利用可能とする)
            File fileCSVFile = new File(outputFolder + "/" + Movie.fileName + ".csv");
            if (fileCSVFile.isFile()) {
                labelUseExisting.setEnabled(true);
                useExistingCheckbox.setEnabled(true);
                reuseFlag = useExistingCheckbox.isSelected();
            } else {
                labelUseExisting.setEnabled(false);
                useExistingCheckbox.setEnabled(false);
                reuseFlag = false;
            }
            // 出力形式取得
            outputType = outputTypeCombobox.getSelectedIndex();
            // 出力サイズにsQCIF(128,96)以下または8k(8192,4320)以上のサイズが指定された場合はOK無効
            try {
                int width = Integer.parseInt(outputWidthEdit.getText());
                int height = Integer.parseInt(outputHeightEdit.getText());
                if (width < 128 || width > 8192 || height < 96 || height > 4320) {
                    ok = false;
                } else {
                    outputWidth = width;
                    outputHeight = height;
                }
            } catch (NumberFormatException e) {
                ok = false;
            }
            // ビューモード
            viewMode = viewModeCombobox.getSelectedIndex();
            switch(viewMode) {
            case 0:     // 自動調節
                labelRectMovieSpeed.setEnabled(true);
                rectMovieSpeedEdit.setEnabled(true);
                labelRectMovieSpeedUnit.setEnabled(true);
                labelCustom.setEnabled(false);
                labelCustomWidth.setEnabled(false);
                customWidthEdit.setEnabled(false);
                labelCustomHeight.setEnabled(false);
                customHeightEdit.setEnabled(false);
                try {
                    int nAdjustSpeed = Integer.parseInt(rectMovieSpeedEdit.getText());
                    if (nAdjustSpeed < 0 || nAdjustSpeed > 9999) {
                        ok = false;
                    } else {
                        adjustSpeed = nAdjustSpeed;
                    }
                } catch (NumberFormatException e) {
                    ok = false;
                }
                break;
            case 1:     // 最大サイズ
                labelRectMovieSpeed.setEnabled(false);
                rectMovieSpeedEdit.setEnabled(false);
                labelRectMovieSpeedUnit.setEnabled(false);
                labelCustom.setEnabled(false);
                labelCustomWidth.setEnabled(false);
                customWidthEdit.setEnabled(false);
                labelCustomHeight.setEnabled(false);
                customHeightEdit.setEnabled(false);
                break;
            case 2:     // カスタム
                labelRectMovieSpeed.setEnabled(false);
                rectMovieSpeedEdit.setEnabled(false);
                labelRectMovieSpeedUnit.setEnabled(false);
                labelCustom.setEnabled(true);
                labelCustomWidth.setEnabled(true);
                customWidthEdit.setEnabled(true);
                labelCustomHeight.setEnabled(true);
                customHeightEdit.setEnabled(true);
                try {
                    int nWidth = Integer.parseInt(customWidthEdit.getText());
                    int nHeight = Integer.parseInt(customHeightEdit.getText());
                    if (nWidth < 0 || nWidth > 999999 || nHeight < 0 || nHeight > 999999) {
                        ok = false;
                    } else {
                        customWidth = nWidth;
                        customHeight = nHeight;
                    }
                } catch (NumberFormatException e) {
                    ok = false;
                }
                break;
            }
            // 再生速度が 1～999999 意外の場合はOK無効
            if (speed < 1 || speed > 999999) {
                ok = false;
            }
            // FPSが 1～60 意外の場合はOK無効
            if (fps < 1 || fps > 60) {
                ok = false;
            }
            // Node数出力
            outputNodeCount = outputNodeCountCheckbox.isSelected();
            // Edge数出力
            outputEdgeCount = outputEdgeCountCheckbox.isSelected();
            // 日時出力
            outputDatetime = outputDatetimeCheckbox.isSelected();
            if (dynamic) {
                // 開始時間
                // 終了時間
                if (startTimeSet > endTimeSet) {
                    ok = false;
                }
            }
            // 追加時間
            if (addPlayTimeSet < 0 || addPlayTimeSet > 36000) {
                ok = false;
            }
            dlg.setValid(ok);
        }
        calcLastOutputData();
    }
    
    /**
     * 再生速度設定。<br/>
     * @param speed 
     */
    public void setSpeed(int speed) {
        if (this.speed != speed) {
            if ((speed >= 1) && (speed <= 999999)) {
                this.speed = speed;
                if (playSpeedSlider.getValue() != speed) {
                    playSpeedSlider.setValue(speed);
                }
                if (!playSpeedEdit.getText().equals(Integer.toString(speed))) {
                    playSpeedEdit.setText(Integer.toString(speed));
                }
            } else {
                this.speed = -1;
            }
            checkOkButtonEnabled(); // OKボタンの有効/無効判定
        }
    }
    /**
     * 再生速度設定。<br/>
     * @param speed 
     */
    public void setSpeed(String speed) {
        int n = -1; 
        try {
            n = Integer.parseInt(speed);
        } catch (NumberFormatException e) {}
        setSpeed(n);
    }
    /**
     * 再生速度取得。<br/>
     * @return 
     */
    public int getSpeed() {
        return speed;
    }
    /**
     * FPS設定。<br/>
     * @param fps 
     */
    public void setFPS(int fps) {
        if (this.fps != fps) {
            if (fps >= 1 && fps <=60) {
                this.fps = fps;
                if (fpsSlider.getValue() != fps) {
                    fpsSlider.setValue(fps);
                }
                if (!fpsEdit.getText().equals(Integer.toString(fps))) {
                    fpsEdit.setText(Integer.toString(fps));
                }
            } else {
                this.fps = -1;
            }
            checkOkButtonEnabled(); // OKボタンの有効/無効判定
        }
    }
    /**
     * FPS設定。<br/>
     * @param fps 
     */
    public void setFPS(String fps) {
        int n = -1;
        try {
            n = Integer.parseInt(fps);
        } catch (NumberFormatException e) {}
        setFPS(n);
    }
    /**
     * FPS取得。<br/>
     * @return 
     */
    public int getFPS() {
        return fps;
    }
    /**
     * 追加再生時間設定。<br/>
     * @param addPlayTimeSet
     */
    public void setAddPlayTime(int addPlayTimeSet) {
        if (this.addPlayTimeSet != addPlayTimeSet) {
            if (addPlayTimeSet >= 0 && addPlayTimeSet <=36000) {
                this.addPlayTimeSet = addPlayTimeSet;
                if (addPlayTimeSlider.getValue() != addPlayTimeSet) {
                    addPlayTimeSlider.setValue(addPlayTimeSet);
                }
                if (!addPlayTimeEdit.getText().equals(Integer.toString(addPlayTimeSet))) {
                    addPlayTimeEdit.setText(Integer.toString(addPlayTimeSet));
                }
            } else {
                this.addPlayTimeSet = -1;
            }
            checkOkButtonEnabled(); // OKボタンの有効/無効判定
        }
    }
    /**
     * 追加再生時間設定。<br/>
     * @param addPlayTimeSet
     */
    public void setAddPlayTime(String addPlayTimeSet) {
        int n = -1;
        try {
            n = Integer.parseInt(addPlayTimeSet);
        } catch (NumberFormatException e) {}
        setAddPlayTime(n);
    }
    /**
     * 追加再生時間取得。<br/>
     * @return 
     */
    public int getAddPlayTime() {
        return addPlayTimeSet;
    }

    /**
     * 文字列取得。<br/>
     * @param resName
     * @return 
     */
    private String getMessage(String resName) {
        return NbBundle.getMessage(MovieOutputPanel.class, resName);
    }

    /**
     * テキストフィールドのテキスト変更時のデフォルトリスナ。<br/>
     * 共通のエラーチェックコードを実行します。<br/>
     */
    public class MovieOutputDefaultDocumentListener implements DocumentListener {
        @Override
        public void changedUpdate(DocumentEvent e) {}
        @Override
        public void insertUpdate(DocumentEvent e) {
            checkOkButtonEnabled(); // OKボタンの有効/無効判定
        }
        @Override
        public void removeUpdate(DocumentEvent e) {
            checkOkButtonEnabled(); // OKボタンの有効/無効判定
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        outputSettingPanel = new javax.swing.JPanel();
        labelPlaySpeedUnit = new javax.swing.JLabel();
        rectMovieSpeedEdit = new javax.swing.JTextField();
        playSpeedSlider = new javax.swing.JSlider();
        labelRectMovieSpeedUnit = new javax.swing.JLabel();
        playSpeedEdit = new javax.swing.JTextField();
        labelPlaySpeed = new javax.swing.JLabel();
        fpsSlider = new javax.swing.JSlider();
        fpsEdit = new javax.swing.JTextField();
        labelOutputType = new javax.swing.JLabel();
        outputTypeCombobox = new javax.swing.JComboBox();
        labelViewMode = new javax.swing.JLabel();
        viewModeCombobox = new javax.swing.JComboBox();
        labelOutputSize = new javax.swing.JLabel();
        labelOutputWidthUnit = new javax.swing.JLabel();
        labelFPS = new javax.swing.JLabel();
        labelRectMovieSpeed = new javax.swing.JLabel();
        labelCustom = new javax.swing.JLabel();
        outputWidthEdit = new javax.swing.JTextField();
        labelOutputWidth = new javax.swing.JLabel();
        labelOutputHeight = new javax.swing.JLabel();
        outputHeightEdit = new javax.swing.JTextField();
        labelOutputHeightUnit = new javax.swing.JLabel();
        customWidthEdit = new javax.swing.JTextField();
        labelCustomWidth = new javax.swing.JLabel();
        customHeightEdit = new javax.swing.JTextField();
        labelCustomHeight = new javax.swing.JLabel();
        labelOutputNodeCount = new javax.swing.JLabel();
        outputNodeCountCheckbox = new javax.swing.JCheckBox();
        labelOutputEdgeCount = new javax.swing.JLabel();
        outputEdgeCountCheckbox = new javax.swing.JCheckBox();
        labelOutputDatetime = new javax.swing.JLabel();
        outputDatetimeCheckbox = new javax.swing.JCheckBox();
        outputFolderButton = new javax.swing.JButton();
        outputFolderEdit = new javax.swing.JTextField();
        labelOutputFolder = new javax.swing.JLabel();
        playSettingPanel = new javax.swing.JPanel();
        labelStartPlayTime = new javax.swing.JLabel();
        labelStartTime = new javax.swing.JLabel();
        labelPlayTime = new javax.swing.JLabel();
        labelPlayTimeUnit = new javax.swing.JLabel();
        playTimeLabel = new javax.swing.JLabel();
        labelPlaytimeTo = new javax.swing.JLabel();
        labelEndPlayTime = new javax.swing.JLabel();
        labelAddPlayTime = new javax.swing.JLabel();
        endPlayTimeSlider = new javax.swing.JSlider();
        startPlayTimeLabel = new javax.swing.JLabel();
        startPlayTimeSlider = new javax.swing.JSlider();
        endPlayTimeLabel = new javax.swing.JLabel();
        labelEndTime = new javax.swing.JLabel();
        addPlayTimeEdit = new javax.swing.JTextField();
        labelAddPlayTimeUnit = new javax.swing.JLabel();
        addPlayTimeSlider = new javax.swing.JSlider();
        lastOutputViewPanel = new javax.swing.JPanel();
        labelViewPlayTime = new javax.swing.JLabel();
        endViewPlayTimeLabel = new javax.swing.JLabel();
        labelViewPlayTimeTo = new javax.swing.JLabel();
        startViewPlayTimeLabel = new javax.swing.JLabel();
        viewPlayTimeLabel = new javax.swing.JLabel();
        labelViewPlayTimeUnit = new javax.swing.JLabel();
        labelFrameCount = new javax.swing.JLabel();
        frameCountLabel = new javax.swing.JLabel();
        labelPlay = new javax.swing.JLabel();
        labelUseExisting = new javax.swing.JLabel();
        labelLastOutput = new javax.swing.JLabel();
        useExistingCheckbox = new javax.swing.JCheckBox();
        labelOutput = new javax.swing.JLabel();
        labelATLLogo = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        outputSettingPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        org.openide.awt.Mnemonics.setLocalizedText(labelPlaySpeedUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelPlaySpeedUnit.text")); // NOI18N

        rectMovieSpeedEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.rectMovieSpeedEdit.text")); // NOI18N

        playSpeedSlider.setMaximum(999999);
        playSpeedSlider.setMinimum(1);
        playSpeedSlider.setValue(1);
        playSpeedSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                playSpeedSliderStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(labelRectMovieSpeedUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelRectMovieSpeedUnit.text")); // NOI18N

        playSpeedEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.playSpeedEdit.text")); // NOI18N

        labelPlaySpeed.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelPlaySpeed, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelPlaySpeed.text")); // NOI18N

        fpsSlider.setMaximum(60);
        fpsSlider.setMinimum(1);
        fpsSlider.setValue(25);
        fpsSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                fpsSliderStateChanged(evt);
            }
        });

        fpsEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.fpsEdit.text")); // NOI18N

        labelOutputType.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelOutputType, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputType.text")); // NOI18N

        outputTypeCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "AVI", "GephiPlayer binaly", "SVG+JPEG", "SVG" }));
        outputTypeCombobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                outputTypeComboboxItemStateChanged(evt);
            }
        });

        labelViewMode.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelViewMode, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelViewMode.text")); // NOI18N

        viewModeCombobox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Auto", "Max size", "Custom" }));
        viewModeCombobox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewModeComboboxItemStateChanged(evt);
            }
        });

        labelOutputSize.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelOutputSize, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputSize.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelOutputWidthUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputWidthUnit.text")); // NOI18N

        labelFPS.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelFPS, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelFPS.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelRectMovieSpeed, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelRectMovieSpeed.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelCustom, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelCustom.text")); // NOI18N
        labelCustom.setEnabled(false);

        outputWidthEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputWidthEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelOutputWidth, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputWidth.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelOutputHeight, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputHeight.text")); // NOI18N

        outputHeightEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputHeightEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelOutputHeightUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputHeightUnit.text")); // NOI18N

        customWidthEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.customWidthEdit.text")); // NOI18N
        customWidthEdit.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(labelCustomWidth, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelCustomWidth.text")); // NOI18N
        labelCustomWidth.setEnabled(false);
        labelCustomWidth.setInheritsPopupMenu(false);

        customHeightEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.customHeightEdit.text")); // NOI18N
        customHeightEdit.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(labelCustomHeight, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelCustomHeight.text")); // NOI18N
        labelCustomHeight.setEnabled(false);

        labelOutputNodeCount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelOutputNodeCount, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputNodeCount.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(outputNodeCountCheckbox, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputNodeCountCheckbox.text")); // NOI18N
        outputNodeCountCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputNodeCountCheckboxActionPerformed(evt);
            }
        });

        labelOutputEdgeCount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelOutputEdgeCount, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputEdgeCount.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(outputEdgeCountCheckbox, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputEdgeCountCheckbox.text")); // NOI18N
        outputEdgeCountCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputEdgeCountCheckboxActionPerformed(evt);
            }
        });

        labelOutputDatetime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelOutputDatetime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputDatetime.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(outputDatetimeCheckbox, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputDatetimeCheckbox.text")); // NOI18N
        outputDatetimeCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputDatetimeCheckboxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout outputSettingPanelLayout = new javax.swing.GroupLayout(outputSettingPanel);
        outputSettingPanel.setLayout(outputSettingPanelLayout);
        outputSettingPanelLayout.setHorizontalGroup(
            outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputSettingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputSettingPanelLayout.createSequentialGroup()
                        .addComponent(labelViewMode)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(viewModeCombobox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(labelRectMovieSpeed)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rectMovieSpeedEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelRectMovieSpeedUnit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(labelCustom)
                        .addGap(5, 5, 5)
                        .addComponent(labelCustomWidth)
                        .addGap(4, 4, 4)
                        .addComponent(customWidthEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelCustomHeight)
                        .addGap(4, 4, 4)
                        .addComponent(customHeightEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(outputSettingPanelLayout.createSequentialGroup()
                        .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(outputSettingPanelLayout.createSequentialGroup()
                                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelPlaySpeed)
                                    .addComponent(labelFPS))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, outputSettingPanelLayout.createSequentialGroup()
                                        .addComponent(playSpeedSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(playSpeedEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(outputSettingPanelLayout.createSequentialGroup()
                                        .addComponent(fpsSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(fpsEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelPlaySpeedUnit))
                            .addGroup(outputSettingPanelLayout.createSequentialGroup()
                                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelOutputType)
                                    .addComponent(labelOutputSize))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(outputSettingPanelLayout.createSequentialGroup()
                                        .addComponent(labelOutputWidth)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(outputWidthEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelOutputWidthUnit)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(labelOutputHeight)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(outputHeightEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(labelOutputHeightUnit))
                                    .addComponent(outputTypeCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(outputSettingPanelLayout.createSequentialGroup()
                                .addComponent(labelOutputNodeCount)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(outputNodeCountCheckbox)
                                .addGap(14, 14, 14)
                                .addComponent(labelOutputEdgeCount)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(outputEdgeCountCheckbox)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(labelOutputDatetime)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(outputDatetimeCheckbox)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        outputSettingPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {labelFPS, labelPlaySpeed});

        outputSettingPanelLayout.setVerticalGroup(
            outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outputSettingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelOutputType)
                    .addComponent(outputTypeCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(labelOutputHeightUnit)
                        .addComponent(outputHeightEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelOutputHeight))
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(labelOutputSize)
                        .addComponent(labelOutputWidthUnit)
                        .addComponent(outputWidthEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelOutputWidth)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(customHeightEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelCustomHeight))
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(viewModeCombobox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelCustom)
                        .addComponent(labelRectMovieSpeed)
                        .addComponent(rectMovieSpeedEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelRectMovieSpeedUnit)
                        .addComponent(labelViewMode)
                        .addComponent(customWidthEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelCustomWidth)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(playSpeedEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelPlaySpeedUnit))
                    .addComponent(playSpeedSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelPlaySpeed))
                .addGap(10, 10, 10)
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelFPS)
                    .addComponent(fpsSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fpsEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(labelOutputEdgeCount)
                        .addComponent(outputEdgeCountCheckbox))
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(labelOutputDatetime)
                        .addComponent(outputDatetimeCheckbox))
                    .addGroup(outputSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(labelOutputNodeCount)
                        .addComponent(outputNodeCountCheckbox)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(outputFolderButton, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputFolderButton.text")); // NOI18N
        outputFolderButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                outputFolderButtonMouseClicked(evt);
            }
        });

        outputFolderEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.outputFolderEdit.text")); // NOI18N
        outputFolderEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outputFolderEditActionPerformed(evt);
            }
        });

        labelOutputFolder.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelOutputFolder, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutputFolder.text")); // NOI18N

        playSettingPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        org.openide.awt.Mnemonics.setLocalizedText(labelStartPlayTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelStartPlayTime.text")); // NOI18N

        labelStartTime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelStartTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelStartTime.text")); // NOI18N

        labelPlayTime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelPlayTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelPlayTime.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelPlayTimeUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelPlayTimeUnit.text")); // NOI18N

        playTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(playTimeLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.playTimeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelPlaytimeTo, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelPlaytimeTo.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelEndPlayTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelEndPlayTime.text")); // NOI18N

        labelAddPlayTime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelAddPlayTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelAddPlayTime.text")); // NOI18N

        endPlayTimeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                endPlayTimeSliderStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(startPlayTimeLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.startPlayTimeLabel.text")); // NOI18N

        startPlayTimeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                startPlayTimeSliderStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(endPlayTimeLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.endPlayTimeLabel.text")); // NOI18N

        labelEndTime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelEndTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelEndTime.text")); // NOI18N

        addPlayTimeEdit.setText(org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.addPlayTimeEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelAddPlayTimeUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelAddPlayTimeUnit.text")); // NOI18N

        addPlayTimeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                addPlayTimeSliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout playSettingPanelLayout = new javax.swing.GroupLayout(playSettingPanel);
        playSettingPanel.setLayout(playSettingPanelLayout);
        playSettingPanelLayout.setHorizontalGroup(
            playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(playSettingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(playSettingPanelLayout.createSequentialGroup()
                        .addComponent(labelPlayTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelStartPlayTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelPlaytimeTo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelEndPlayTime)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(playTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelPlayTimeUnit))
                    .addGroup(playSettingPanelLayout.createSequentialGroup()
                        .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(labelAddPlayTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelStartTime, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(labelEndTime, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(playSettingPanelLayout.createSequentialGroup()
                                .addComponent(startPlayTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(startPlayTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(playSettingPanelLayout.createSequentialGroup()
                                .addComponent(endPlayTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(4, 4, 4)
                                .addComponent(endPlayTimeLabel))
                            .addGroup(playSettingPanelLayout.createSequentialGroup()
                                .addComponent(addPlayTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(addPlayTimeEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelAddPlayTimeUnit)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        playSettingPanelLayout.setVerticalGroup(
            playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(playSettingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelPlayTime)
                    .addComponent(labelStartPlayTime)
                    .addComponent(labelEndPlayTime)
                    .addComponent(labelPlaytimeTo)
                    .addComponent(playTimeLabel)
                    .addComponent(labelPlayTimeUnit))
                .addGap(12, 12, 12)
                .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelStartTime)
                    .addComponent(startPlayTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(startPlayTimeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelEndTime)
                    .addComponent(endPlayTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(endPlayTimeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(playSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(addPlayTimeEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(labelAddPlayTimeUnit))
                    .addComponent(addPlayTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(labelAddPlayTime))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lastOutputViewPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        labelViewPlayTime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelViewPlayTime, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelViewPlayTime.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(endViewPlayTimeLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.endViewPlayTimeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelViewPlayTimeTo, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelViewPlayTimeTo.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(startViewPlayTimeLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.startViewPlayTimeLabel.text")); // NOI18N

        viewPlayTimeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(viewPlayTimeLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.viewPlayTimeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelViewPlayTimeUnit, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelViewPlayTimeUnit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelFrameCount, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelFrameCount.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(frameCountLabel, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.frameCountLabel.text")); // NOI18N

        javax.swing.GroupLayout lastOutputViewPanelLayout = new javax.swing.GroupLayout(lastOutputViewPanel);
        lastOutputViewPanel.setLayout(lastOutputViewPanelLayout);
        lastOutputViewPanelLayout.setHorizontalGroup(
            lastOutputViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lastOutputViewPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lastOutputViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(labelFrameCount)
                    .addComponent(labelViewPlayTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lastOutputViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lastOutputViewPanelLayout.createSequentialGroup()
                        .addComponent(startViewPlayTimeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelViewPlayTimeTo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(endViewPlayTimeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(viewPlayTimeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelViewPlayTimeUnit))
                    .addComponent(frameCountLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
        );
        lastOutputViewPanelLayout.setVerticalGroup(
            lastOutputViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lastOutputViewPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(lastOutputViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelViewPlayTime)
                    .addComponent(startViewPlayTimeLabel)
                    .addComponent(endViewPlayTimeLabel)
                    .addComponent(labelViewPlayTimeTo)
                    .addComponent(viewPlayTimeLabel)
                    .addComponent(labelViewPlayTimeUnit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lastOutputViewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelFrameCount)
                    .addComponent(frameCountLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.openide.awt.Mnemonics.setLocalizedText(labelPlay, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelPlay.text")); // NOI18N

        labelUseExisting.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        org.openide.awt.Mnemonics.setLocalizedText(labelUseExisting, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelUseExisting.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelLastOutput, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelLastOutput.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(useExistingCheckbox, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.useExistingCheckbox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(labelOutput, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelOutput.text")); // NOI18N

        labelATLLogo.setBackground(new java.awt.Color(153, 153, 153));
        org.openide.awt.Mnemonics.setLocalizedText(labelATLLogo, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.labelATLLogo.text")); // NOI18N

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/gephi/rs/export/movie/resource/ATL_logo_mini.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MovieOutputPanel.class, "MovieOutputPanel.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(outputSettingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(playSettingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(labelPlay)
                    .addComponent(labelLastOutput)
                    .addComponent(lastOutputViewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelOutput)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelATLLogo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 82, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(labelUseExisting))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(labelOutputFolder)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(useExistingCheckbox)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(outputFolderEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(outputFolderButton)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelATLLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(2, 2, 2)))
                        .addComponent(labelOutput))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelOutputFolder)
                            .addComponent(outputFolderEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(outputFolderButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelUseExisting)
                            .addComponent(useExistingCheckbox))
                        .addGap(15, 15, 15)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputSettingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labelPlay)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playSettingPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(labelLastOutput)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lastOutputViewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void outputFolderButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_outputFolderButtonMouseClicked
        // 出力先フォルダの選択ダイアログ表示
        JFileChooser filechooser;
        if (outputFolder.equals("")) {
           filechooser = new JFileChooser();
        } else {
           filechooser = new JFileChooser(outputFolder);
        }   
        filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int selected = filechooser.showOpenDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
            File file = filechooser.getSelectedFile();
            outputFolderEdit.setText(file.getAbsolutePath());
            checkOkButtonEnabled(); // OKボタンの有効/無効判定
        }
    }//GEN-LAST:event_outputFolderButtonMouseClicked

    private void playSpeedSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_playSpeedSliderStateChanged
        // 再生速度スライダー変更
        setSpeed(playSpeedSlider.getValue());
    }//GEN-LAST:event_playSpeedSliderStateChanged

    private void fpsSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_fpsSliderStateChanged
        // FPSスライダー変更
        setFPS(fpsSlider.getValue());
    }//GEN-LAST:event_fpsSliderStateChanged

    private void viewModeComboboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_viewModeComboboxItemStateChanged
        // ビューモード変更
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_viewModeComboboxItemStateChanged

    private void startPlayTimeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_startPlayTimeSliderStateChanged
        // 開始時間
        startTimeSet = startPlayTimeSlider.getValue();
        double dStartTime = startTimeSet * 1000.0;
        startPlayTimeLabel.setText(sdf.format(DynamicUtilities.getDateFromDouble(dStartTime)));
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_startPlayTimeSliderStateChanged

    private void endPlayTimeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_endPlayTimeSliderStateChanged
        // 終了時間
        endTimeSet = endPlayTimeSlider.getValue();
        double dEndTime = endTimeSet * 1000.0;
        endPlayTimeLabel.setText(sdf.format(DynamicUtilities.getDateFromDouble(dEndTime)));
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_endPlayTimeSliderStateChanged

    private void addPlayTimeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_addPlayTimeSliderStateChanged
        // 追加再生時間
        setAddPlayTime(addPlayTimeSlider.getValue());
    }//GEN-LAST:event_addPlayTimeSliderStateChanged

    private void outputNodeCountCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputNodeCountCheckboxActionPerformed
        // Node数出力有無
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_outputNodeCountCheckboxActionPerformed

    private void outputEdgeCountCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputEdgeCountCheckboxActionPerformed
        // Edge数出力有無
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_outputEdgeCountCheckboxActionPerformed

    private void outputDatetimeCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputDatetimeCheckboxActionPerformed
        // 日時出力有無
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_outputDatetimeCheckboxActionPerformed

    private void outputTypeComboboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_outputTypeComboboxItemStateChanged
        // 出力形式変更
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_outputTypeComboboxItemStateChanged

    private void outputFolderEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outputFolderEditActionPerformed
        // 出力先ディレクトリ変更
        checkOkButtonEnabled(); // OKボタンの有効/無効判定
    }//GEN-LAST:event_outputFolderEditActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addPlayTimeEdit;
    private javax.swing.JSlider addPlayTimeSlider;
    private javax.swing.JTextField customHeightEdit;
    private javax.swing.JTextField customWidthEdit;
    private javax.swing.JLabel endPlayTimeLabel;
    private javax.swing.JSlider endPlayTimeSlider;
    private javax.swing.JLabel endViewPlayTimeLabel;
    private javax.swing.JTextField fpsEdit;
    private javax.swing.JSlider fpsSlider;
    private javax.swing.JLabel frameCountLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel labelATLLogo;
    private javax.swing.JLabel labelAddPlayTime;
    private javax.swing.JLabel labelAddPlayTimeUnit;
    private javax.swing.JLabel labelCustom;
    private javax.swing.JLabel labelCustomHeight;
    private javax.swing.JLabel labelCustomWidth;
    private javax.swing.JLabel labelEndPlayTime;
    private javax.swing.JLabel labelEndTime;
    private javax.swing.JLabel labelFPS;
    private javax.swing.JLabel labelFrameCount;
    private javax.swing.JLabel labelLastOutput;
    private javax.swing.JLabel labelOutput;
    private javax.swing.JLabel labelOutputDatetime;
    private javax.swing.JLabel labelOutputEdgeCount;
    private javax.swing.JLabel labelOutputFolder;
    private javax.swing.JLabel labelOutputHeight;
    private javax.swing.JLabel labelOutputHeightUnit;
    private javax.swing.JLabel labelOutputNodeCount;
    private javax.swing.JLabel labelOutputSize;
    private javax.swing.JLabel labelOutputType;
    private javax.swing.JLabel labelOutputWidth;
    private javax.swing.JLabel labelOutputWidthUnit;
    private javax.swing.JLabel labelPlay;
    private javax.swing.JLabel labelPlaySpeed;
    private javax.swing.JLabel labelPlaySpeedUnit;
    private javax.swing.JLabel labelPlayTime;
    private javax.swing.JLabel labelPlayTimeUnit;
    private javax.swing.JLabel labelPlaytimeTo;
    private javax.swing.JLabel labelRectMovieSpeed;
    private javax.swing.JLabel labelRectMovieSpeedUnit;
    private javax.swing.JLabel labelStartPlayTime;
    private javax.swing.JLabel labelStartTime;
    private javax.swing.JLabel labelUseExisting;
    private javax.swing.JLabel labelViewMode;
    private javax.swing.JLabel labelViewPlayTime;
    private javax.swing.JLabel labelViewPlayTimeTo;
    private javax.swing.JLabel labelViewPlayTimeUnit;
    private javax.swing.JPanel lastOutputViewPanel;
    private javax.swing.JCheckBox outputDatetimeCheckbox;
    private javax.swing.JCheckBox outputEdgeCountCheckbox;
    private javax.swing.JButton outputFolderButton;
    private javax.swing.JTextField outputFolderEdit;
    private javax.swing.JTextField outputHeightEdit;
    private javax.swing.JCheckBox outputNodeCountCheckbox;
    private javax.swing.JPanel outputSettingPanel;
    private javax.swing.JComboBox outputTypeCombobox;
    private javax.swing.JTextField outputWidthEdit;
    private javax.swing.JPanel playSettingPanel;
    private javax.swing.JTextField playSpeedEdit;
    private javax.swing.JSlider playSpeedSlider;
    private javax.swing.JLabel playTimeLabel;
    private javax.swing.JTextField rectMovieSpeedEdit;
    private javax.swing.JLabel startPlayTimeLabel;
    private javax.swing.JSlider startPlayTimeSlider;
    private javax.swing.JLabel startViewPlayTimeLabel;
    private javax.swing.JCheckBox useExistingCheckbox;
    private javax.swing.JComboBox viewModeCombobox;
    private javax.swing.JLabel viewPlayTimeLabel;
    // End of variables declaration//GEN-END:variables
}
